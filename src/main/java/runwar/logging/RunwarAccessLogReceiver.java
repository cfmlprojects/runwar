package runwar.logging;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Deque;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;

import io.undertow.UndertowLogger;
import io.undertow.server.handlers.accesslog.AccessLogReceiver;
import io.undertow.server.handlers.accesslog.LogFileHeaderGenerator;

public class RunwarAccessLogReceiver implements AccessLogReceiver, Runnable, Closeable {
    private static final String DEFAULT_LOG_SUFFIX = "log";

    private final Executor logWriteExecutor;

    private final Deque<String> pendingMessages;

    // 0 = not running
    // 1 = queued
    // 2 = running
    @SuppressWarnings("unused")
    private volatile int state = 0;

    private static final AtomicIntegerFieldUpdater<RunwarAccessLogReceiver> stateUpdater = AtomicIntegerFieldUpdater
            .newUpdater(RunwarAccessLogReceiver.class, "state");

    private long changeOverPoint;
    private String currentDateString;
    private boolean forceLogRotation;

    private final Path outputDirectory;
    private final Path defaultLogFile;

    private final String logBaseName;
    private final String logNameSuffix;

    private Writer writer = null;

    private volatile boolean closed = false;
    private boolean initialRun = true;
    private final boolean rotate;
    private final LogFileHeaderGenerator fileHeaderGenerator;

    public RunwarAccessLogReceiver(final Executor logWriteExecutor, final File outputDirectory,
            final String logBaseName) {
        this(logWriteExecutor, outputDirectory.toPath(), logBaseName, null);
    }

    public RunwarAccessLogReceiver(final Executor logWriteExecutor, final File outputDirectory,
            final String logBaseName, final String logNameSuffix) {
        this(logWriteExecutor, outputDirectory.toPath(), logBaseName, logNameSuffix, true);
    }

    public RunwarAccessLogReceiver(final Executor logWriteExecutor, final File outputDirectory,
            final String logBaseName, final String logNameSuffix, boolean rotate) {
        this(logWriteExecutor, outputDirectory.toPath(), logBaseName, logNameSuffix, rotate);
    }

    public RunwarAccessLogReceiver(final Executor logWriteExecutor, final Path outputDirectory,
            final String logBaseName) {
        this(logWriteExecutor, outputDirectory, logBaseName, null);
    }

    public RunwarAccessLogReceiver(final Executor logWriteExecutor, final Path outputDirectory,
            final String logBaseName, final String logNameSuffix) {
        this(logWriteExecutor, outputDirectory, logBaseName, logNameSuffix, true);
    }

    public RunwarAccessLogReceiver(final Executor logWriteExecutor, final Path outputDirectory,
            final String logBaseName, final String logNameSuffix, boolean rotate) {
        this(logWriteExecutor, outputDirectory, logBaseName, logNameSuffix, rotate, null);
    }

    private RunwarAccessLogReceiver(final Executor logWriteExecutor, final Path outputDirectory,
            final String logBaseName, final String logNameSuffix, boolean rotate, LogFileHeaderGenerator fileHeader) {
        this.logWriteExecutor = logWriteExecutor;
        this.outputDirectory = outputDirectory;
        this.logBaseName = logBaseName;
        this.rotate = rotate;
        this.fileHeaderGenerator = fileHeader;
        this.logNameSuffix = (logNameSuffix != null) ? logNameSuffix : DEFAULT_LOG_SUFFIX;
        this.pendingMessages = new ConcurrentLinkedDeque<>();
        this.defaultLogFile = outputDirectory.resolve(logBaseName + '.' + this.logNameSuffix);
        calculateChangeOverPoint();
    }

    private void calculateChangeOverPoint() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.add(Calendar.DATE, 1);
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        currentDateString = df.format(new Date());
        // if there is an existing default log file, use the date last modified
        // instead of the current date
        if (Files.exists(defaultLogFile)) {
            try {
                currentDateString = df.format(new Date(Files.getLastModifiedTime(defaultLogFile).toMillis()));
            } catch (IOException e) {
                // ignore. use the current date if exception happens.
            }
        }
        changeOverPoint = calendar.getTimeInMillis();
    }

    @Override
    public void logMessage(final String message) {
        this.pendingMessages.add(message);
        int state = stateUpdater.get(this);
        if (state == 0) {
            if (stateUpdater.compareAndSet(this, 0, 1)) {
                logWriteExecutor.execute(this);
            }
        }
    }

    /**
     * processes all queued log messages
     */
    @Override
    public void run() {
        if (!stateUpdater.compareAndSet(this, 1, 2)) {
            return;
        }
        if (forceLogRotation) {
            doRotate();
        } else if (initialRun && Files.exists(defaultLogFile)) {
            // if there is an existing log file check if it should be rotated
            long lm = 0;
            try {
                lm = Files.getLastModifiedTime(defaultLogFile).toMillis();
            } catch (IOException e) {
                UndertowLogger.ROOT_LOGGER.errorRotatingAccessLog(e);
            }
            Calendar c = Calendar.getInstance();
            c.setTimeInMillis(changeOverPoint);
            c.add(Calendar.DATE, -1);
            if (lm <= c.getTimeInMillis()) {
                doRotate();
            }
        }
        initialRun = false;
        List<String> messages = new ArrayList<>();
        String msg;
        // only grab at most 1000 messages at a time
        for (int i = 0; i < 1000; ++i) {
            msg = pendingMessages.poll();
            if (msg == null) {
                break;
            }
            messages.add(msg);
        }
        try {
            if (!messages.isEmpty()) {
                writeMessage(messages);
            }
        } finally {
            stateUpdater.set(this, 0);
            // check to see if there is still more messages
            // if so then run this again
            if (!pendingMessages.isEmpty() || forceLogRotation) {
                if (stateUpdater.compareAndSet(this, 0, 1)) {
                    logWriteExecutor.execute(this);
                }
            } else if (closed) {
                try {
                    if (writer != null) {
                        writer.flush();
                        writer.close();
                        writer = null;
                    }
                } catch (IOException e) {
                    UndertowLogger.ROOT_LOGGER.errorWritingAccessLog(e);
                }
            }
        }
    }

    /**
     * For tests only. Blocks the current thread until all messages are written
     * Just does a busy wait.
     * <p/>
     * DO NOT USE THIS OUTSIDE OF A TEST
     */
    void awaitWrittenForTest() throws InterruptedException {
        while (!pendingMessages.isEmpty() || forceLogRotation) {
            Thread.sleep(10);
        }
        while (state != 0) {
            Thread.sleep(10);
        }
    }

    private void writeMessage(final List<String> messages) {
        if (System.currentTimeMillis() > changeOverPoint) {
            doRotate();
        }
        try {
            if (writer == null) {
                writer = Files.newBufferedWriter(defaultLogFile, StandardCharsets.UTF_8, StandardOpenOption.APPEND,
                        StandardOpenOption.CREATE);
                if (Files.size(defaultLogFile) == 0 && fileHeaderGenerator != null) {
                    String header = fileHeaderGenerator.generateHeader();
                    if (header != null) {
                        writer.write(header);
                        writer.write("\n");
                        writer.flush();
                    }
                }
            }
            for (String message : messages) {
                writer.write(message);
                writer.write('\n');
            }
            writer.flush();
        } catch (IOException e) {
            UndertowLogger.ROOT_LOGGER.errorWritingAccessLog(e);
        }
    }

    private void doRotate() {
        forceLogRotation = false;
        if (!rotate) {
            return;
        }
        try {
            if (writer != null) {
                writer.flush();
                writer.close();
                writer = null;
            }
            if (!Files.exists(defaultLogFile)) {
                return;
            }
            Path newFile = outputDirectory.resolve(logBaseName + currentDateString + "." + logNameSuffix);
            int count = 0;
            while (Files.exists(newFile)) {
                ++count;
                newFile = outputDirectory.resolve(logBaseName + currentDateString + "-" + count + "." + logNameSuffix);
            }
            Files.move(defaultLogFile, newFile);
        } catch (IOException e) {
            UndertowLogger.ROOT_LOGGER.errorRotatingAccessLog(e);
        } finally {
            calculateChangeOverPoint();
        }
    }

    /**
     * forces a log rotation. This rotation is performed in an async manner, you
     * cannot rely on the rotation being performed immediately after this method
     * returns.
     */
    public void rotate() {
        forceLogRotation = true;
        if (stateUpdater.compareAndSet(this, 0, 1)) {
            logWriteExecutor.execute(this);
        }
    }

    @Override
    public void close() throws IOException {
        closed = true;
        if (stateUpdater.compareAndSet(this, 0, 1)) {
            logWriteExecutor.execute(this);
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Executor logWriteExecutor;
        private Path outputDirectory;
        private String logBaseName;
        private String logNameSuffix;
        private boolean rotate;
        private LogFileHeaderGenerator logFileHeaderGenerator;

        public Executor getLogWriteExecutor() {
            return logWriteExecutor;
        }

        public Builder setLogWriteExecutor(Executor logWriteExecutor) {
            this.logWriteExecutor = logWriteExecutor;
            return this;
        }

        public Path getOutputDirectory() {
            return outputDirectory;
        }

        public Builder setOutputDirectory(Path outputDirectory) {
            this.outputDirectory = outputDirectory;
            return this;
        }

        public String getLogBaseName() {
            return logBaseName;
        }

        public Builder setLogBaseName(String logBaseName) {
            this.logBaseName = logBaseName;
            return this;
        }

        public String getLogNameSuffix() {
            return logNameSuffix;
        }

        public Builder setLogNameSuffix(String logNameSuffix) {
            this.logNameSuffix = logNameSuffix;
            return this;
        }

        public boolean isRotate() {
            return rotate;
        }

        public Builder setRotate(boolean rotate) {
            this.rotate = rotate;
            return this;
        }

        public LogFileHeaderGenerator getLogFileHeaderGenerator() {
            return logFileHeaderGenerator;
        }

        public Builder setLogFileHeaderGenerator(LogFileHeaderGenerator logFileHeaderGenerator) {
            this.logFileHeaderGenerator = logFileHeaderGenerator;
            return this;
        }

        public RunwarAccessLogReceiver build() {
            return new RunwarAccessLogReceiver(logWriteExecutor, outputDirectory, logBaseName, logNameSuffix, rotate,
                    logFileHeaderGenerator);
        }
    }
}