package runwar;

import java.io.File;
import java.io.InputStream;
import java.util.Properties;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * A bootstrap class for starting server using an embedded war
 * 
 */
public final class RunEmbeddedWar {
    private static String WAR_POSTFIX = ".war";
    private static String WAR_NAME = "cfdistro";
    private static String WAR_FILENAME = WAR_NAME + WAR_POSTFIX;

    public static void main(String[] args) throws Exception {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

        String sConfigFile = "runwar.properties";
        InputStream in = classLoader.getResourceAsStream(sConfigFile);
        if (in == null) {
            // File not found! (Manage the problem)
        }
        Properties props = new java.util.Properties();
        props.load(in);
        WAR_NAME = props.getProperty("war.name");
        WAR_POSTFIX = ".war";
        WAR_FILENAME = WAR_NAME + WAR_POSTFIX;
        System.out.println(props.toString());
        System.out.println("Starting...");

        // File warFile = File.createTempFile(WAR_NAME + "-", WAR_POSTFIX);
        File currentDir = new File(RunEmbeddedWar.class.getProtectionDomain().getCodeSource().getLocation().toURI())
                .getParentFile();
        File warFile = new File(currentDir.getCanonicalPath() + "/" + WAR_FILENAME);
        File warDir = new File(currentDir.getCanonicalPath() + "/" + WAR_NAME);

        if (warDir.exists()) {
            System.out.println("Not extracting, as WAR directory already exists: " + warDir.getCanonicalPath());
        } else {
            warDir.mkdir();
            System.out.println("Extracting " + WAR_FILENAME + " to " + warFile + " ...");
            LaunchUtil.unzipInteralZip(classLoader, WAR_FILENAME, warDir, false);
            System.out.println("Extracted " + WAR_FILENAME);
            LaunchUtil.copyInternalFile(classLoader, "server.json", new File(currentDir,"server.json"));
        }

        System.out.println("Launching server...");

        List<String> argsList = new ArrayList<String>();
        if (args != null) {
            argsList.addAll(Arrays.asList(args));
        }
        argsList.add("-c");
        argsList.add(new File(currentDir,"server.json").getCanonicalPath());
        System.out.println(argsList);

        if (props.getProperty("open.url") != null) {
            new Server(3);
        }
        Start.main(argsList.toArray(new String[argsList.size()]));
//        System.exit(0);
    }
}
