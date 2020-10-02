package runwar;

import java.awt.Desktop;
import java.awt.GraphicsEnvironment;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.net.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.jar.JarOutputStream;
import java.util.jar.Pack200;
import java.util.zip.GZIPInputStream;

import com.vdurmont.semver4j.Semver;
import dorkbox.notify.Notify;
import dorkbox.notify.Pos;
import dorkbox.util.ActionHandler;
import dorkbox.util.OS;
import runwar.logging.LoggerFactory;
import runwar.logging.RunwarLogger;
import runwar.options.ServerOptions;

public class LaunchUtil {

    private static boolean relaunching;
    private static final int KB = 1024;
    public static final Set<String> replicateProps = new HashSet<String>(Arrays.asList(new String[]{"cfml.cli.home",
        "cfml.server.config.dir", "cfml.web.config.dir", "cfml.server.trayicon", "cfml.server.dockicon"}));

    private static final String OS_NAME = System.getProperty("os.name");
    private static String uname;
    private static String linuxRelease;

    static {
        LoggerFactory.initialize();
    }

    public static void initializeLogging() {
        System.out.println("Initialized logging");
        LoggerFactory.initialize();
    }

    public static File getJreExecutable() throws FileNotFoundException {
        String jreDirectory = System.getProperty("java.home");
        if (jreDirectory == null) {
            throw new IllegalStateException("java.home");
        }
        final String javaPath = System.getProperty("java.home") + File.separator + "bin" + File.separator + "java"
                + (File.separator.equals("\\") ? ".exe" : "");
        File exe = new File(javaPath);
        if (!exe.isFile()) {
            throw new FileNotFoundException(exe.toString());
        }
        return exe;
    }

    public static File getJarDir(Class<?> aclass) {
        URL url;
        String extURL;
        try {
            url = aclass.getProtectionDomain().getCodeSource().getLocation();
        } catch (SecurityException ex) {
            url = aclass.getResource(aclass.getSimpleName() + ".class");
        }
        extURL = url.toExternalForm();
        if (extURL.endsWith(".jar")) // from getCodeSource
        {
            extURL = extURL.substring(0, extURL.lastIndexOf("/"));
        } else {  // from getResource
            String suffix = "/" + (aclass.getName()).replace(".", "/") + ".class";
            extURL = extURL.replace(suffix, "");
            if (extURL.startsWith("jar:") && extURL.endsWith(".jar!")) {
                extURL = extURL.substring(4, extURL.lastIndexOf("/"));
            }
        }
        try {
            url = new URL(extURL);
        } catch (MalformedURLException mux) {
        }
        try {
            return new File(url.toURI());
        } catch (URISyntaxException ex) {
            return new File(url.getPath());
        }
    }

    public static void launch(List<String> cmdarray, int timeout) throws IOException, InterruptedException {
        launch(cmdarray, timeout, true);
    }

    public static void launch(List<String> cmdarray, int timeout, boolean andExit) throws IOException, InterruptedException {
        LoggerFactory.initialize();
        boolean serverIsUp = false;
        ProcessBuilder processBuilder = new ProcessBuilder(cmdarray);
        processBuilder.redirectErrorStream(true);
        Process process = processBuilder.start();
        Thread.sleep(500);
        InputStream is = process.getInputStream();
        InputStreamReader isr = new InputStreamReader(is);
        BufferedReader br = new BufferedReader(isr);

        RunwarLogger.LOG.debug("launching background process with these args: ");
        // Pretty print out all the process args being sent to the background server.
        StringBuilder formattedArgs = new StringBuilder();
        formattedArgs.append("\n  ");
        for (String arg : cmdarray) {
            // Don't print these.  They don't do anything and are just clutter.
            if (arg.startsWith("--jvm-args")) {
                continue;
            }

            if (arg.startsWith("-")) {
                formattedArgs.append("\n  ");
            }
            formattedArgs.append("  " + arg);
        }
        RunwarLogger.LOG.debug("args ->" + formattedArgs.toString());

        RunwarLogger.LOG.debug("timeout of " + timeout / 1000 + " seconds");
        String line;
        int exit = -1;
        long start = System.currentTimeMillis();
        System.out.print("Starting in background - ");
        while ((System.currentTimeMillis() - start) < timeout && !serverIsUp) {
            if (br.ready() && (line = br.readLine()) != null) {
                // Outputs your process execution
                try {
                    exit = process.exitValue();
                    if (exit == 0) {
                        RunwarLogger.LOG.debug(line);
                        // Process finished
                        while ((line = br.readLine()) != null) {
                            RunwarLogger.LOG.debug(line);
                        }
                        if (andExit) {
                            System.exit(0);
                        }
                        serverIsUp = true;
                        break;
                    } else if (exit == 1) {
                        printExceptionLine(line);
                        while ((line = br.readLine()) != null) {
                            printExceptionLine(line);
                        }
                        System.exit(1);
                    }
                } catch (IllegalThreadStateException t) {
                    // This exceptions means the process has not yet finished.
                    // decide to continue, exit(0), or exit(1)
                    serverIsUp = processOutout(line, process, andExit);
                }
            }
        }
        if ((System.currentTimeMillis() - start) > timeout && !serverIsUp) {
            process.destroy();
            System.out.println();
            System.err.println("ERROR: Startup exceeded timeout of " + timeout / 1000 + " seconds - aborting!");
            System.exit(1);
        }
        System.out.println("Server is up - ");
        if (andExit) {
            System.exit(0);
        } else {
            relaunching = false;
            System.out.println("Not exiting.");
        }
    }

    private static boolean processOutout(String line, Process process, boolean exitWhenUp) {
        RunwarLogger.BACKGROUNDED_LOG.debug(line);
        if (line.indexOf("Server is up - ") != -1) {
            // start up was successful, quit out
//             System.out.println(line);
            if (exitWhenUp) {
                System.exit(0);
            } else {
                return true;
            }
        } else if (line.indexOf("Exception in thread \"main\" java.lang.RuntimeException") != -1) {
            return false;
        }
        return false;
    }

    public static void printExceptionLine(String line) {
        final String msg = "java.lang.RuntimeException: ";
        RunwarLogger.LOG.debug(line);
        String formatted = line.contains(msg) ? line.substring(line.indexOf(msg) + msg.length()) : line;
        formatted = formatted.matches("^\\s+at runwar.Start.*") ? "" : formatted.trim();
        if (formatted.length() > 0) {
            System.err.println(formatted);
        }
    }

    public static void relaunchAsBackgroundProcess(ServerOptions serverOptions, boolean andExit) {
        serverOptions.background(false);
        relaunchAsBackgroundProcess(serverOptions.launchTimeout(), serverOptions.commandLineArgs(),
                serverOptions.jvmArgs(), serverOptions.processName(), andExit);
    }

    public static void relaunchAsBackgroundProcess(int timeout, String[] args, List<String> jvmArgs, String processName) {
        relaunchAsBackgroundProcess(timeout, args, jvmArgs, processName, true);
    }

    public static void relaunchAsBackgroundProcess(int timeout, String[] args, List<String> jvmArgs, String processName, boolean andExit) {
        try {
            if (relaunching) {
                return;
            }
            relaunching = true;
            LoggerFactory.initialize();
            String path = LaunchUtil.class.getProtectionDomain().getCodeSource().getLocation().getPath();
            RunwarLogger.LOG.info("Starting background " + processName + " from: " + path + " ");
            String decodedPath = URLDecoder.decode(path, "UTF-8");
            decodedPath = new File(decodedPath).getPath();
            List<String> cmdarray = new ArrayList<String>();
            cmdarray.add(getJreExecutable().toString());
            List<String> VMArgs = jvmArgs != null ? jvmArgs : getCurrentVMArgs();
            for (String arg : VMArgs) {
                cmdarray.add(arg);
            }
            cmdarray.add("-jar");
            for (String propertyName : replicateProps) {
                String property = System.getProperty(propertyName);
                if (property != null) {
                    cmdarray.add("-D" + propertyName + "=" + property);
                }
            }
            cmdarray.add(decodedPath);
            for (String arg : args) {
                cmdarray.add(arg);
            }
            launch(cmdarray, timeout, andExit);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static void appendToBuffer(List<String> resultBuffer, StringBuffer buf) {
        if (buf.length() > 0) {
            resultBuffer.add(buf.toString());
            buf.setLength(0);
        }
    }

    public static List<String> getCurrentVMArgs() {
        RuntimeMXBean RuntimemxBean = ManagementFactory.getRuntimeMXBean();
        List<String> arguments = RuntimemxBean.getInputArguments();
        return arguments;
    }

    public static String[] tokenizeArgs(String argLine) {
        List<String> resultBuffer = new java.util.ArrayList<String>();

        if (argLine != null) {
            int z = argLine.length();
            boolean insideQuotes = false;
            StringBuffer buf = new StringBuffer();

            for (int i = 0; i < z; ++i) {
                char c = argLine.charAt(i);
                if (c == '"') {
                    appendToBuffer(resultBuffer, buf);
                    insideQuotes = !insideQuotes;
                } else if (c == '\\') {
                    if ((z > i + 1) && ((argLine.charAt(i + 1) == '"') || (argLine.charAt(i + 1) == '\\'))) {
                        buf.append(argLine.charAt(i + 1));
                        ++i;
                    } else {
                        buf.append("\\");
                    }
                } else {
                    if (insideQuotes) {
                        buf.append(c);
                    } else {
                        if (Character.isWhitespace(c)) {
                            appendToBuffer(resultBuffer, buf);
                        } else {
                            buf.append(c);
                        }
                    }
                }
            }
            appendToBuffer(resultBuffer, buf);

        }

        String[] result = new String[resultBuffer.size()];
        return resultBuffer.toArray(result);
    }

    public enum MessageType {
        INFO, WARNING, ERROR
    }

    public static void displayMessage(String type, String text) {
        displayMessage("RunWAR", type, text);
    }

    public static void displayMessage(String type, String text, int hideAfter) {
        displayMessage("RunWAR", type, text, hideAfter);
    }

    public static void displayMessage(String processName, String type, String text) {
        displayMessage(processName, type, text, 5000);
    }

    public static void displayMessage(String processName, String type, String text, int hideAfter) {
        try {
            if (type.toLowerCase().startsWith("warn")) {
                displayMessage(processName, text, MessageType.WARNING, hideAfter);
            } else if (type.toLowerCase().startsWith("error")) {
                displayMessage(processName, text, MessageType.ERROR, hideAfter);
            } else {
                displayMessage(processName, text, MessageType.INFO, hideAfter);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void printMessage(String title, String text, MessageType type) {
        if (type == MessageType.ERROR) {
            System.err.println(title + " " + text);
        } else {
            System.out.println(title + " " + text);
        }
    }

    public static void displayMessage(String title, String text, MessageType type) {
        displayMessage(title, text, type, 5000);
    }

    public static void displayMessage(String title, String text, MessageType type, int hideAfter) {
        if (GraphicsEnvironment.isHeadless()) {
            printMessage(title, text, type);
            return;
        }
        try {
            Pos position = OS.isMacOsX() ? Pos.TOP_RIGHT : Pos.BOTTOM_RIGHT;
            final Notify notify = Notify.create()
                    .title(title)
                    .text(text)
                    .hideAfter(hideAfter)
                    .position(position)
                    .darkStyle()
                    .onAction(new ActionHandler<Notify>() {
                        @Override
                        public void handle(final Notify arg0) {
                        }
                    });

            // ensure the messages disappear
            Timer timer = new Timer(true);
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    try {
                        notify.close();
                    } catch (Exception any) {
                    };
                }
            },
                    hideAfter * 2);

            switch (type) {
                case INFO:
                    notify.showInformation();
                    break;

                case WARNING:
                    notify.showWarning();
                    break;

                case ERROR:
                    notify.showError();
                    break;

                default:
                    notify.show();
                    break;
            }
        } catch (Exception e) {
            printMessage(title, text, type);
            //RunwarLogger.ROOT_LOGGER.error(e);
        }
    }

    public static void browseDirectory(String path) throws IOException {
        File directory = new File(path);
        if (isMac()) {
            // Mac tries to open the .app rather than browsing it.  Instead, pass a child with -R to select it in finder
            File[] files = directory.listFiles();
            if (files.length > 0) {
                // Get first child
                File child = directory.listFiles()[0];
                if (execute(new String[]{"open", "-R", child.getCanonicalPath()})) {
                    return;
                }
            }
        } else {
            try {
                // The default, java recommended usage
                Desktop d = Desktop.getDesktop();
                d.open(directory);
                return;
            } catch (IOException io) {
                if (isLinux()) {
                    // Fallback on xdg-open for Linux
                    if (execute(new String[]{"xdg-open", path})) {
                        return;
                    }
                }
                throw io;
            }
        }
        throw new IOException("Unable to open " + path);
    }

    public static void openURL(String url, String preferred_browser) {
        BrowserOpener.openURL(url, preferred_browser);
    }

    public static String getResourceAsString(String path) {
        InputStream streamPath = LaunchUtil.class.getClassLoader().getResourceAsStream(path);
        if (streamPath == null) {
            return null;
        }
        return readStream(streamPath);
    }

    public static void unzipInteralZip(ClassLoader classLoader, String resourcePath, File libDir, boolean debug) {
        if (debug) {
            System.out.println("Extracting " + resourcePath);
        }
        libDir.mkdir();
        URL resource = classLoader.getResource(resourcePath);
        if (resource == null) {
            System.err.println("Could not find the " + resourcePath + " on classpath!");
            System.exit(1);
        }
        unzipResource(resource, libDir, debug);
    }

    public static void unzipResource(URL resource, File libDir, boolean debug) {
        class PrintDot extends TimerTask {

            public void run() {
                System.out.print(".");
            }
        }
        Timer timer = new Timer();
        PrintDot task = new PrintDot();
        timer.schedule(task, 0, 2000);

        try {
            BufferedInputStream bis = new BufferedInputStream(resource.openStream());
            JarInputStream jis = new JarInputStream(bis);
            JarEntry je = null;
            while ((je = jis.getNextJarEntry()) != null) {
                java.io.File f = new java.io.File(libDir.toString() + java.io.File.separator + je.getName());
                if (je.isDirectory()) {
                    f.mkdirs();
                    continue;
                }
                File parentDir = new File(f.getParent());
                if (!parentDir.exists()) {
                    parentDir.mkdirs();
                }
                FileOutputStream fileOutStream = new FileOutputStream(f);
                writeStreamTo(jis, fileOutStream, 8 * KB);
                if (f.getPath().endsWith("pack.gz")) {
                    unpack(f);
                    fileOutStream.close();
                    f.delete();
                }
                fileOutStream.close();
            }

        } catch (Exception exc) {
            task.cancel();
            exc.printStackTrace();
        }
        task.cancel();

    }

    public static void cleanUpUnpacked(File libDir) {
        if (libDir.exists() && libDir.listFiles(new ExtFilter(".gz")).length > 0) {
            for (File gz : libDir.listFiles(new ExtFilter(".gz"))) {
                try {
                    gz.delete();
                } catch (Exception e) {
                }
            }
        }
    }

    public static void removePreviousLibs(File libDir) {
        if (libDir.exists() && libDir.listFiles(new ExtFilter(".jar")).length > 0) {
            for (File previous : libDir.listFiles(new ExtFilter(".jar"))) {
                try {
                    previous.delete();
                } catch (Exception e) {
                    System.err.println("Could not delete previous lib: " + previous.getAbsolutePath());
                }
            }
        }
    }

    public static void unpack(File inFile) {
        JarOutputStream out = null;
        InputStream in = null;
        String inName = inFile.getPath();
        String outName;

        if (inName.endsWith(".pack.gz")) {
            outName = inName.substring(0, inName.length() - 8);
        } else if (inName.endsWith(".pack")) {
            outName = inName.substring(0, inName.length() - 5);
        } else {
            outName = inName + ".unpacked";
        }
        try {
            Pack200.Unpacker unpacker = Pack200.newUnpacker();
            out = new JarOutputStream(new FileOutputStream(outName));
            in = new FileInputStream(inName);
            if (inName.endsWith(".gz")) {
                in = new GZIPInputStream(in);
            }
            unpacker.unpack(in, out);
        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException ex) {
                    System.err.println("Error closing file: " + ex.getMessage());
                }
            }
            if (out != null) {
                try {
                    out.flush();
                    out.close();
                } catch (IOException ex) {
                    System.err.println("Error closing file: " + ex.getMessage());
                }
            }
        }
    }

    public static void copyInternalFile(ClassLoader classLoader, String resourcePath, File dest) {
        URL resource = classLoader.getResource(resourcePath);
        try {
            copyStream(resource.openStream(), dest);
        } catch (IOException e) {
            RunwarLogger.LOG.error("Error copying file.", e);
        }

    }

    public static String readStream(InputStream is) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PrintStream outPrint = new PrintStream(out);
        try {
            int content;
            while ((content = is.read()) != -1) {
                // convert to char and display it
                outPrint.print((char) content);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (is != null) {
                    is.close();
                }
                outPrint.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
        return out.toString();
    }

    public static String readFile(File source) {
        try {
            return readStream(new FileInputStream(source));
        } catch (FileNotFoundException e) {
            RunwarLogger.LOG.error("Error reading file.", e);
        }
        return null;
    }

    public static void copyFile(File source, File dest) {
        try {
            copyStream(new FileInputStream(source), dest);
        } catch (FileNotFoundException e) {
            RunwarLogger.LOG.error("Error copying file.", e);
        }
    }

    public static void copyStream(InputStream bis, File dest) {
        try {
            FileOutputStream output = new FileOutputStream(dest);
            writeStreamTo(bis, output, 8 * KB);
            output.close();
        } catch (Exception e) {
            RunwarLogger.LOG.error("Error copying stream.", e);
        }
    }

    public static int writeStreamTo(final InputStream input, final OutputStream output, int bufferSize)
            throws IOException {
        int available = Math.min(input.available(), 256 * KB);
        byte[] buffer = new byte[Math.max(bufferSize, available)];
        int answer = 0;
        int count = input.read(buffer);
        while (count >= 0) {
            output.write(buffer, 0, count);
            answer += count;
            count = input.read(buffer);
        }
        return answer;
    }

    public static void deleteRecursive(File f) throws IOException {
        if (f.isDirectory()) {
            for (File c : f.listFiles()) {
                deleteRecursive(c);
            }
        }
        if (!f.delete()) {
            System.err.println("Could not delete file: " + f.getAbsolutePath());
        }
    }

    public static class ExtFilter implements FilenameFilter {

        private String ext;

        public ExtFilter(String extension) {
            ext = extension;
        }

        public boolean accept(File dir, String name) {
            return name.toLowerCase().endsWith(ext);
        }
    }

    public static class PrefixFilter implements FilenameFilter {

        private String prefix;

        public PrefixFilter(String prefix) {
            this.prefix = prefix;
        }

        public boolean accept(File dir, String name) {
            return name.toLowerCase().startsWith(prefix);
        }
    }

    public static void assertMinimumJavaVersion(String minVersion) {
        Semver systemJavaVersion = new Semver(System.getProperty("java.version", "").replace('_', '.'), Semver.SemverType.LOOSE);
        Semver minimumJavaVersion = new Semver(minVersion, Semver.SemverType.LOOSE);
        System.out.println("Java version " + systemJavaVersion.toString() + " (requires >= " + minVersion + ")");
        if (systemJavaVersion.toStrict().isLowerThan(minimumJavaVersion.toStrict())) {
            System.out.println("** Requires Java " + minimumJavaVersion.toStrict() + " or later, current: " + systemJavaVersion.toStrict());
            System.exit(1);
        }
    }

    public static boolean versionLowerThanOrEqualTo(String version, String minVersion) {
        Semver systemJavaVersion = new Semver(version.replace('_', '.'), Semver.SemverType.LOOSE);
        Semver minimumJavaVersion = new Semver(minVersion, Semver.SemverType.LOOSE);
        return systemJavaVersion.isLowerThanOrEqualTo(minimumJavaVersion);
    }

    public static boolean versionGreaterThanOrEqualTo(String version, String minVersion) {
        Semver systemJavaVersion = new Semver(version.replace('_', '.'), Semver.SemverType.LOOSE);
        Semver minimumJavaVersion = new Semver(minVersion, Semver.SemverType.LOOSE);
        return systemJavaVersion.isGreaterThanOrEqualTo(minimumJavaVersion);
    }

    public static String getOS() {
        return OS_NAME;
    }

    public static String getDataDirectory() {
        String parent;
        String folder = "runwar";
        if (isWindows()) {
            parent = System.getenv("APPDATA");
        } else if (isMac()) {
            parent = System.getProperty("user.home") + File.separator + "Library" + File.separator + "Application Support";
        } else if (isUnix()) {
            parent = System.getProperty("user.home");
            folder = "." + folder;
        } else {
            parent = System.getProperty("user.dir");
        }
        return parent + File.separator + folder;
    }

    public static boolean isWindows() {
        return (OS_NAME.contains("win"));
    }

    public static boolean isMac() {
        return (OS_NAME.contains("mac"));
    }

    public static boolean isLinux() {
        return (OS_NAME.contains("linux"));
    }

    public static boolean isUnix() {
        return (OS_NAME.contains("nix") || OS_NAME.contains("nux") || OS_NAME.indexOf("aix") > 0 || OS_NAME.contains("sunos"));
    }

    public static boolean isSolaris() {
        return (OS_NAME.contains("sunos"));
    }

    public static boolean isUbuntu() {
        getUname();
        return uname != null && uname.contains("Ubuntu");
    }

    public static boolean isFedora() {
        getLinuxRelease();
        return linuxRelease != null && linuxRelease.contains("Fedora");
    }

    public static String getLinuxRelease() {
        if (isLinux() && linuxRelease == null) {
            String[] releases = {"/etc/lsb-release", "/etc/redhat-release"};
            for (String release : releases) {
                String result = execute(new String[]{"cat", release}, null, false);
                if (!result.isEmpty()) {
                    linuxRelease = result;
                    break;
                }
            }
        }
        return linuxRelease;
    }

    public static String getUname() {
        if (isLinux() && uname == null) {
            uname = execute(new String[]{"uname", "-a"}, null, false);
        }
        return uname;
    }

    public static String[] envp = null;

    public static boolean execute(String[] commandArray) {
        try {
            // Create and execute our new process
            Process p = Runtime.getRuntime().exec(commandArray, envp);
            p.waitFor();
            return p.exitValue() == 0;
        } catch (InterruptedException ex) {
        } catch (IOException ex) {
        }
        return false;
    }

    public static String execute(String[] commandArray, String[] searchFor, boolean caseSensitive) {
        BufferedReader stdInput = null;
        try {
            Process p = Runtime.getRuntime().exec(commandArray, envp);
            stdInput = new BufferedReader(new InputStreamReader(p.getInputStream(), "UTF-8"));
            String s;
            while ((s = stdInput.readLine()) != null) {
                if (searchFor == null) {
                    return s.trim();
                }
                for (String search : searchFor) {
                    if (caseSensitive) {
                        if (s.contains(search.trim())) {
                            return s.trim();
                        }
                    } else {
                        if (s.toLowerCase().contains(search.toLowerCase().trim())) {
                            return s.trim();
                        }
                    }
                }
            }
        } catch (IOException ex) {
            RunwarLogger.LOG.error("Error executing command", ex);
        } finally {
            if (stdInput != null) {
                try {
                    stdInput.close();
                } catch (Exception ignore) {
                }
            }
        }
        return "";
    }

    public static final String SUN_JAVA_COMMAND = "sun.java.command";

    /**
     * Restart the current Java application
     *
     * @param runBeforeRestart some custom code to be run before restarting
     * @throws IOException if one happens
     */
    public static void restartApplication(Runnable runBeforeRestart) throws IOException {
        try {
            // vm arguments
            List<String> vmArguments = ManagementFactory.getRuntimeMXBean().getInputArguments();
            StringBuffer vmArgsOneLine = new StringBuffer();
            for (String arg : vmArguments) {
                // if it's the agent argument : we ignore it otherwise the
                // address of the old application and the new one will be in
                // conflict
                if (!arg.contains("-agentlib")) {
                    vmArgsOneLine.append(arg);
                    vmArgsOneLine.append(" ");
                }
            }
            // init the command to execute, add the vm args
            final StringBuffer cmd = new StringBuffer(getJreExecutable().toString() + " " + vmArgsOneLine);

            // program main and program arguments
            String[] mainCommand = System.getProperty(SUN_JAVA_COMMAND).split(" ");
            // program main is a jar
            if (mainCommand[0].endsWith(".jar")) {
                // if it's a jar, add -jar mainJar
                cmd.append("-jar " + new File(mainCommand[0]).getPath());
            } else {
                // else it's a .class, add the classpath and mainClass
                cmd.append("-cp \"" + System.getProperty("java.class.path") + "\" " + mainCommand[0]);
            }
            // finally add program arguments
            for (int i = 1; i < mainCommand.length; i++) {
                cmd.append(" ");
                cmd.append(mainCommand[i]);
            }
            // execute the command in a shutdown hook, to be sure that all the
            // resources have been disposed before restarting the application
            Runtime.getRuntime().addShutdownHook(new Thread() {
                @Override
                public void run() {
                    try {
                        Runtime.getRuntime().exec(cmd.toString());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
            // execute some custom code before restarting
            if (runBeforeRestart != null) {
                runBeforeRestart.run();
            }
            // exit
            System.exit(0);
        } catch (Exception e) {
            // something went wrong
            throw new IOException("Error while trying to restart the application", e);
        }
    }

    public static int getPortOrErrorOut(int portNumber, String host) {
        try (ServerSocket nextAvail = new ServerSocket(portNumber, 1, getInetAddress(host))) {
            portNumber = nextAvail.getLocalPort();
            nextAvail.close();
            return portNumber;
        } catch (java.net.BindException e) {
            throw new RuntimeException("Error getting port " + portNumber + "!  Cannot start:  " + e.getMessage());
        } catch (UnknownHostException e) {
            throw new RuntimeException("Unknown host (" + host + ")");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static InetAddress getInetAddress(String host) {
        try {
            return InetAddress.getByName(host);
        } catch (UnknownHostException e) {
            throw new RuntimeException("Error getting inet address for " + host);
        }
    }

}
