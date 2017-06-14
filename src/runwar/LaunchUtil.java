package runwar;

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
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
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

import javax.swing.JOptionPane;

import dorkbox.notify.Notify;
import dorkbox.notify.Pos;
import dorkbox.util.ActionHandler;
import dorkbox.util.OS;
import runwar.logging.Logger;
import runwar.options.ServerOptions;

public class LaunchUtil {

    private static Logger log = Logger.getLogger("RunwarLogger");
    private static boolean relaunching;
    private static final int KB = 1024;
    private static String processName;
    public static final Set<String> replicateProps = new HashSet<String>(Arrays.asList(new String[] { "cfml.cli.home",
            "cfml.server.config.dir", "cfml.web.config.dir", "cfml.server.trayicon", "cfml.server.dockicon" }));

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
        // if(debug)System.out.println("Java: "+javaPath);
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
        if (extURL.endsWith(".jar"))   // from getCodeSource
            extURL = extURL.substring(0, extURL.lastIndexOf("/"));
        else {  // from getResource
            String suffix = "/"+(aclass.getName()).replace(".", "/")+".class";
            extURL = extURL.replace(suffix, "");
            if (extURL.startsWith("jar:") && extURL.endsWith(".jar!"))
                extURL = extURL.substring(4, extURL.lastIndexOf("/"));
        }
        try {
            url = new URL(extURL);
        } catch (MalformedURLException mux) {
        }
        try {
            return new File(url.toURI());
        } catch(URISyntaxException ex) {
            return new File(url.getPath());
        }
    }

    public static void launch(List<String> cmdarray, int timeout) throws IOException, InterruptedException {
        launch(cmdarray, timeout, true);
    }

    public static void launch(List<String> cmdarray, int timeout, boolean andExit) throws IOException, InterruptedException {
        // byte[] buffer = new byte[1024];
        boolean serverIsUp = false;
        ProcessBuilder processBuilder = new ProcessBuilder(cmdarray);
        processBuilder.redirectErrorStream(true);
        Process process = processBuilder.start();
        Thread.sleep(500);
        InputStream is = process.getInputStream();
        InputStreamReader isr = new InputStreamReader(is);
        BufferedReader br = new BufferedReader(isr);
        log.debug("launching: " + cmdarray.toString());
        log.debug("timeout of " + timeout / 1000 + " seconds");
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
                        // Process finished
                        while ((line = br.readLine()) != null) {
                            log.debug(line);
                        }
                        if(andExit) {
                            System.exit(0);
                        }
                        serverIsUp = true;
                        break;
                    } else if (exit == 1) {
                        System.out.println();
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
            Thread.sleep(100);
        }
        if ((System.currentTimeMillis() - start) > timeout && !serverIsUp) {
            process.destroy();
            System.out.println();
            System.err.println("ERROR: Startup exceeded timeout of " + timeout / 1000 + " seconds - aborting!");
            System.exit(1);
        }
        System.out.println("Server is up - ");
        if(andExit) {
            System.exit(0);
        } else {
            relaunching = false;
            System.out.println("Not exiting.");
        }
    }

    private static boolean processOutout(String line, Process process, boolean exitWhenUp) {
        log.info("processoutput: " + line);
        if (line.indexOf("Server is up - ") != -1) {
            // start up was successful, quit out
            System.out.println(line);
            if(exitWhenUp) {
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
        log.debug(line);
        String formatted = line.contains(msg) ? line.substring(line.indexOf(msg) + msg.length()) : line;
        formatted = formatted.matches("^\\s+at runwar.Start.*") ? "" : formatted.trim();
        if (formatted.length() > 0) {
            System.err.println(formatted);
        }
    }

    public static void relaunchAsBackgroundProcess(ServerOptions serverOptions, boolean andExit) {
        serverOptions.setBackground(false);
        relaunchAsBackgroundProcess(serverOptions.getLaunchTimeout(), serverOptions.getCommandLineArgs(),
                serverOptions.getJVMArgs(), serverOptions.getProcessName(), andExit);
    }

    public static void relaunchAsBackgroundProcess(int timeout, String[] args, List<String> jvmArgs, String processName) {
        relaunchAsBackgroundProcess(timeout, args, jvmArgs, processName, true);
    }

    public static void relaunchAsBackgroundProcess(int timeout, String[] args, List<String> jvmArgs, String processName, boolean andExit) {
        try {
            if (relaunching)
                return;
            relaunching = true;
            String path = LaunchUtil.class.getProtectionDomain().getCodeSource().getLocation().getPath();
            log.info("Starting background " + processName + " from: " + path + " ");
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
        try{
            if(type.toLowerCase().startsWith("warn")) {
                displayMessage(processName, text, MessageType.WARNING);
            } else if (type.toLowerCase().startsWith("error")) {
                displayMessage(processName, text, MessageType.ERROR);
            } else {
                displayMessage(processName, text, MessageType.INFO);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public static void printMessage(String title, String text, MessageType type) {
        if(type == MessageType.ERROR) {
            System.err.println(title + " " + text);
        } else {
            System.out.println(title + " " + text);
        }
    }
    
    public static void displayMessage(String title, String text, MessageType type) {
        if(GraphicsEnvironment.isHeadless()) {
            printMessage(title, text, type);
            return;
        }
        int hideAfter = 5000;
        try {
            Pos position = OS.isMacOsX() ? Pos.TOP_RIGHT : Pos.BOTTOM_RIGHT;
            final Notify notify = Notify.create()
                    .title(title)
                    .text(text)
                    .hideAfter(hideAfter)
                    .position(position)
                    // .setScreen(0)
                    .darkStyle()
                    //.shake(1300, 10)
                    // .hideCloseButton()
                    .onAction(new ActionHandler<Notify>() {
                        @Override
                        public void handle(final Notify arg0) {
//                        System.out.println("Notification clicked on!");
                        }
                    });

            // ensure the messages disappear
            Timer timer = new Timer(true); timer.schedule(new TimerTask() { 
                @Override public void run() { try{notify.close();} catch (Exception any) {};} }
            , hideAfter*2);
            
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
            //log.error(e);
        }
    }

    public static void openURL(String url) {
        String osName = System.getProperty("os.name");
        if (url == null) {
            System.out.println("ERROR: No URL specified to open the browser to!");
            return;
        }
        try {
            System.out.println(url);
            if (osName.startsWith("Mac OS")) {
                Class<?> fileMgr = Class.forName("com.apple.eio.FileManager");
                Method openURL = fileMgr.getDeclaredMethod("openURL", new Class[] { String.class });
                openURL.invoke(null, new Object[] { url });
            } else if (osName.startsWith("Windows"))
                Runtime.getRuntime().exec("rundll32 url.dll,FileProtocolHandler " + url);
            else { // assume Unix or Linux
                // try default first
                try{
                    Class<?> desktopClass = Class.forName("java.awt.Desktop");
                    Object desktopObject = desktopClass.getMethod("getDesktop", (Class[]) null).invoke(null, (Object[]) null);
                    Method openURL = desktopClass.getDeclaredMethod("browse", new Class[] { URI.class });
                    openURL.invoke(desktopObject, new Object[] {new URI(url)});
                } catch (Exception e) {
                    String[] browsers = { "firefox", "chrome", "opera", "konqueror", "epiphany", "mozilla", "netscape" };
                    String browser = null;
                    for (int count = 0; count < browsers.length && browser == null; count++)
                        if (Runtime.getRuntime().exec(new String[] { "which", browsers[count] }).waitFor() == 0)
                            browser = browsers[count];
                    if (browser == null)
                        throw new Exception("Could not find web browser");
                    else
                        Runtime.getRuntime().exec(new String[] { browser, url });
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, e.getMessage() + ":\n" + e.getLocalizedMessage());
        }
    }

    public static String getResourceAsString(String path) {
        return readStream(LaunchUtil.class.getClassLoader().getResourceAsStream(path));
    }

    public static void unzipInteralZip(ClassLoader classLoader, String resourcePath, File libDir, boolean debug) {
        if (debug)
            System.out.println("Extracting " + resourcePath);
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
            if (inName.endsWith(".gz"))
                in = new GZIPInputStream(in);
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
            log.error(e);
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
                if (is != null)
                    is.close();
                if (outPrint != null)
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
            log.error(e);
        }
        return null;
    }

    public static void copyFile(File source, File dest) {
        try {
            copyStream(new FileInputStream(source), dest);
        } catch (FileNotFoundException e) {
            log.error(e);
        }
    }

    public static void copyStream(InputStream bis, File dest) {
        try {
            FileOutputStream output = new FileOutputStream(dest);
            writeStreamTo(bis, output, 8 * KB);
            output.close();
        } catch (FileNotFoundException e) {
            log.error(e);
        } catch (IOException e) {
            log.error(e);
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
          for (File c : f.listFiles())
              deleteRecursive(c);
        }
        if (!f.delete())
            System.err.println("Could not delete file: " + f.getAbsolutePath());
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

    public static void assertJavaVersion8() {
        String version = System.getProperty("java.version");
        System.out.println("Java version " + version);
        if (version.charAt(0) == '1' && Integer.parseInt(version.charAt(2) + "") < 8) {
            System.out.println("** Requires Java 1.8 or later");
            System.out.println("The HTTP2 spec requires certain cyphers that are not present in older JVM's");
            System.out.println("See section 9.2.2 of the HTTP2 specification for details");
            System.exit(1);
        }
    }

}
