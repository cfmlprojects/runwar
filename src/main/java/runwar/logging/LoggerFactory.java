package runwar.logging;

import runwar.options.ServerOptions;
import runwar.options.ServerOptionsImpl;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import org.apache.log4j.Appender;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.RollingFileAppender;
import com.jcabi.log.MulticolorLayout;

//import org.jboss.logmanager.LogContext;
//import org.jboss.logmanager.LogManager;
//import org.jboss.logmanager.PropertyConfigurator;

public class LoggerFactory {

    private static volatile boolean initialized = false;
    private static volatile String logFile;
    private static volatile String logLevel;
    private static volatile String logPattern;
    private static volatile List<Appender> appenders;
    private static volatile List<Logger> loggers;
    private static volatile List<Logger> urlrewriteLoggers;
    private static volatile RollingFileAppender rewriteLogAppender;
    private static volatile ConsoleAppender consoleAppender;

    public static synchronized void configure(ServerOptions serverOptions) {

        Logger.getRootLogger().getLoggerRepository().resetConfiguration();

        logLevel = serverOptions.getLoglevel().toUpperCase();
        logPattern = "%color{%m%n}";
        appenders = new ArrayList<>();
        loggers = new ArrayList<>();
        Level level = Level.toLevel(logLevel);

        consoleAppender = consoleAppender(logPattern);
        appenders.add(consoleAppender);
        Logger.getRootLogger().setLevel(Level.WARN);
        Logger.getRootLogger().addAppender(consoleAppender);

        Logger DORKBOX_LOG = Logger.getLogger("dorkbox.systemTray.SystemTray");
        loggers.add(DORKBOX_LOG);

        Logger JBOSS_LOG = Logger.getLogger("org.jboss.logging");
        loggers.add(JBOSS_LOG);

        Logger UNDERTOW_LOG = Logger.getLogger("io.undertow.servlet");
        loggers.add(UNDERTOW_LOG);

        Logger UNDERTOW_REQUEST_LOG = Logger.getLogger("io.undertow.request");
        loggers.add(UNDERTOW_REQUEST_LOG);

        Logger UNDERTOW_IO_LOG = Logger.getLogger("io.undertow");
        loggers.add(UNDERTOW_IO_LOG);

        Logger XNIO_LOG = Logger.getLogger("org.xnio.nio");
        loggers.add(XNIO_LOG);

        Logger HTTP_CLIENT_LOG = Logger.getLogger("org.apache.http.client.protocol");
        loggers.add(HTTP_CLIENT_LOG);

        Logger RUNWAR_SERVER = Logger.getLogger("runwar.server");
        loggers.add(RUNWAR_SERVER);

        Logger RUNWAR_CONTEXT = Logger.getLogger("runwar.context");
        loggers.add(RUNWAR_CONTEXT);

        Logger RUNWAR_CONFIG = Logger.getLogger("runwar.config");
        loggers.add(RUNWAR_CONFIG);

        Logger RUNWAR_SECURITY = Logger.getLogger("runwar.security");
        loggers.add(RUNWAR_SECURITY);

        Logger RUNWAR_REQUEST = Logger.getLogger("runwar.request");
        loggers.add(RUNWAR_REQUEST);

        Logger RUNWAR_BACKGROUND = Logger.getLogger("runwar.background");
        RUNWAR_BACKGROUND.addAppender(consoleAppender("%m%n"));
        RUNWAR_BACKGROUND.setLevel(Level.TRACE);
        RUNWAR_BACKGROUND.setAdditivity(false);
        
        if (serverOptions.getURLRewriteLog() != null) {
            // errLogFile = serverOptions.getLogDir().getPath() + '/' +
            // serverOptions.getLogFileName() + ".err.txt";
            rewriteLogAppender = new RollingFileAppender();
            rewriteLogAppender.setName("URLRewriteFileLogger");
            rewriteLogAppender.setFile(serverOptions.getURLRewriteLog().getAbsolutePath());
            // rewriteLogAppender.setLayout(new
            // PatternLayout(serverOptions.getLogPattern()));
            rewriteLogAppender.setLayout(new PatternLayout("[%-5p] %c{2}: %m%n"));
            rewriteLogAppender.setThreshold(Level.toLevel(logLevel));
            rewriteLogAppender.setAppend(true);
            rewriteLogAppender.setMaxFileSize("10MB");
            rewriteLogAppender.setMaxBackupIndex(3);
            rewriteLogAppender.activateOptions();
        }

        RUNWAR_SERVER.setLevel(level);
        RUNWAR_CONTEXT.setLevel(level);
        RUNWAR_CONFIG.setLevel(Level.INFO);
        RUNWAR_SECURITY.setLevel(Level.WARN);
        RUNWAR_REQUEST.setLevel(Level.WARN);
        DORKBOX_LOG.setLevel(Level.ERROR);
        UNDERTOW_LOG.setLevel(Level.WARN);
        HTTP_CLIENT_LOG.setLevel(Level.WARN);

        if (serverOptions.isDebug() || !logLevel.equalsIgnoreCase("info")) {
            logPattern = "[%color{%-5p}] %c: %color{%m%n}";
               ((MulticolorLayout) consoleAppender.getLayout()).setConversionPattern(logPattern);

            if (logLevel.equalsIgnoreCase("trace")) {
                DORKBOX_LOG.setLevel(level);
                appenders.forEach(DORKBOX_LOG::addAppender);
                UNDERTOW_LOG.setLevel(level);
                HTTP_CLIENT_LOG.setLevel(level);
                RUNWAR_CONFIG.setLevel(level);
                RUNWAR_SERVER.setLevel(level);
                RUNWAR_CONTEXT.setLevel(level);
                RUNWAR_SECURITY.setLevel(level);
                RUNWAR_REQUEST.setLevel(level);
                Logger.getRootLogger().setLevel(level);
                configureUrlRewriteLoggers(true);
            } else {
                RUNWAR_REQUEST.setLevel(Level.DEBUG);
                configureUrlRewriteLoggers(false);
            }
        }

        if (serverOptions.hasLogDir()) {
            // errLogFile = serverOptions.getLogDir().getPath() + '/' +
            // serverOptions.getLogFileName() + ".err.txt";
            logFile = serverOptions.getLogDir().getPath() + '/' + serverOptions.getLogFileName() + ".out.txt";
            RollingFileAppender fa = new RollingFileAppender();
            fa.setName("FileLogger");
            fa.setFile(logFile);
            fa.setLayout(new PatternLayout(serverOptions.getLogPattern()));
            fa.setThreshold(Level.toLevel(logLevel));
            fa.setAppend(true);
            fa.setMaxFileSize("10MB");
            fa.setMaxBackupIndex(10);
            fa.activateOptions();
            appenders.add(fa);
            Logger.getRootLogger().addAppender(fa);
        }
        Logger.getRootLogger().addAppender(consoleAppender);

        loggers.forEach(logger -> appenders.forEach(appender -> {
            logger.addAppender(appender);
            logger.setAdditivity(false);
        }));

        initialized = true;

        if (System.getProperty("runwar.dumploggerstyles") != null) {
            RunwarLogger.LOG.trace("This is a TRACE message");
            RunwarLogger.LOG.debug("This is a DEBUG message");
            RunwarLogger.LOG.warn("This is a WARN message");
            RunwarLogger.LOG.error("This is an ERROR message");
        }
    }

    private static ConsoleAppender consoleAppender(String pattern) {
        ConsoleAppender appender = new ConsoleAppender();
        MulticolorLayout layout = new MulticolorLayout();
        layout.setConversionPattern(pattern);
        layout.setLevels("TRACE:1;32,DEBUG:1;33,INFO:1;34,WARN:38;5;208,ERROR:1;31,FATAL:1;40");
        appender.setLayout(layout);
        appender.setName("CONSOLE");
        appender.setThreshold(Level.toLevel(logLevel));
        appender.activateOptions();
        return appender;
    }

    public static boolean isInitialized() {
        return initialized;
    }

    public static synchronized boolean initialize() {
        return initialize(false);
    }

    public static synchronized boolean initialize(boolean force) {
        if (!initialized || force)
            configure(new ServerOptionsImpl().setLogDir(""));
        return initialized;
    }

    public static void configureUrlRewriteLoggers(boolean isTrace) {
//        if(urlrewriteLoggers != null) {
//        	RunwarLogger.LOG.trace("urlrewrite logger has alredy been configured");
//            return;
//        }
        Logger REWRITE_CONDITION_LOG = Logger.getLogger("org.tuckey.web.filters.urlrewrite.Condition");
        Logger REWRITE_RULE_LOG = Logger.getLogger("org.tuckey.web.filters.urlrewrite.RuleBase");
        Logger REWRITE_SUBSTITUTION_LOG = Logger
                .getLogger("org.tuckey.web.filters.urlrewrite.substitution.VariableReplacer");
        Logger REWRITE_EXECUTION_LOG = Logger.getLogger("org.tuckey.web.filters.urlrewrite.RuleExecutionOutput");
        Logger REWRITE_WRITER_LOG = Logger.getLogger("org.tuckey.web.filters.urlrewrite.UrlRewriter");
        Logger REWRITE_URL_LOG = Logger.getLogger("org.tuckey.web.filters.urlrewrite");
        Logger REWRITE_FILTER = Logger.getLogger("org.tuckey.web.filters.urlrewrite.UrlRewriteFilter");
        Logger REWRITE_LOG = Logger.getLogger("org.tuckey.web.filters.urlrewrite.utils.Log");
        urlrewriteLoggers = new ArrayList<>();
        urlrewriteLoggers.add(REWRITE_CONDITION_LOG);
        urlrewriteLoggers.add(REWRITE_RULE_LOG);
        urlrewriteLoggers.add(REWRITE_SUBSTITUTION_LOG);
        urlrewriteLoggers.add(REWRITE_EXECUTION_LOG);
        urlrewriteLoggers.add(REWRITE_WRITER_LOG);
        urlrewriteLoggers.add(REWRITE_URL_LOG);
        urlrewriteLoggers.add(REWRITE_FILTER);
        urlrewriteLoggers.add(REWRITE_LOG);

        if (rewriteLogAppender != null) {
            RunwarLogger.CONF_LOG.infof("Enabling URL rewrite log: %s", rewriteLogAppender.getFile());
            urlrewriteLoggers.forEach(logger -> {
                logger.addAppender(rewriteLogAppender);
                logger.setAdditivity(false);
            });
        }

        if (isTrace) {
            RunwarLogger.CONF_LOG.infof("Enabling URL rewrite log level: %s", "TRACE");
            urlrewriteLoggers.forEach(logger -> {
                logger.setLevel(Level.TRACE);
                logger.addAppender(consoleAppender("[%color{%-5p}] %c{2}: %color{%m%n}"));
                logger.setAdditivity(false);
            });
        } else {
            RunwarLogger.CONF_LOG.infof("Enabling URL rewrite log level: %s", "DEBUG");
            urlrewriteLoggers.forEach(logger -> {
                logger.setLevel(Level.WARN);
                logger.addAppender(consoleAppender("[%color{%-5p}] %c{2}: %color{%m%n}"));
                logger.setAdditivity(false);
            });
            REWRITE_EXECUTION_LOG.setLevel(Level.DEBUG);
            REWRITE_WRITER_LOG.setLevel(Level.DEBUG);
        }
    }

    public static void listLoggers() {
        for (Enumeration<?> loggers = LogManager.getCurrentLoggers(); loggers.hasMoreElements();) {
            Logger logger = (Logger) loggers.nextElement();
            System.out.println("Logger: " + logger.getName());
            for (Enumeration<?> appenders = logger.getAllAppenders(); appenders.hasMoreElements();) {
                Appender appender = (Appender) appenders.nextElement();
                System.out.println("  appender: " + appender.getName());
            }
        }
    }

}
