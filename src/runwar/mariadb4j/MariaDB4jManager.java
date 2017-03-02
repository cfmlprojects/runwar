package runwar.mariadb4j;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLClassLoader;
import java.nio.file.Files;

import runwar.LaunchUtil;
import runwar.logging.Logger;
import static runwar.util.Reflection.invoke;
import static runwar.util.Reflection.method;
import static runwar.util.Reflection.load;

public class MariaDB4jManager {

    private File baseDir;
    private File dataDir;
    private Object server;
    private String username, password, dbName = null;
    private int port;
    private ClassLoader classLoader;
    private static Logger log = Logger.getLogger("RunwarLogger");
    volatile boolean isShuttingDown;

    public MariaDB4jManager(ClassLoader _classLoader) {
        classLoader = _classLoader;
    }

    public boolean canLoad() {
        try {
            log.debug("Checking for MariaDB4j class avaiability");
            load(classLoader, "ch.vorburger.mariadb4j.DBConfigurationBuilder");
            return true;
        } catch (Exception e) {
            log.debug("MariaDB4j classes are not available");
            return false;
        }
    }

    public void start(int port, File baseDirectory, File dataDirectory, File importSQLFile) throws IOException {
        this.port = port;
        System.out.println("Starting MariaDB server on port " + port);
        if (baseDirectory == null) {
            baseDir = Files.createTempDirectory("mariadb4j").toFile();
        } else {
            baseDir = baseDirectory;
        }
        System.out.println("MariaDB4j base directory " + baseDir.getAbsolutePath());
        if (dataDirectory == null) {
            dataDir = new File(baseDir.getAbsolutePath(), "data-dir");
        } else {
            dataDir = dataDirectory;
        }
        System.out.println("MariaDB4j datadirectory " + dataDir.getAbsolutePath());

        Class<?> builderClass = load(classLoader, "ch.vorburger.mariadb4j.DBConfigurationBuilder");
        if (builderClass == null) {
            throw new RuntimeException("COULD NOT LOAD DB CLASS");
        }
        Object builder = invoke(method(builderClass, "newBuilder"), null);

        invoke(method(builderClass, "setPort", int.class), builder, port);
        invoke(method(builderClass, "setBaseDir", String.class), builder, baseDir.getAbsolutePath());
        invoke(method(builderClass, "setDataDir", String.class), builder, dataDir.getAbsolutePath());

        Class<?> DBClass = load(classLoader, "ch.vorburger.mariadb4j.DB");
        ClassLoader OCL = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(classLoader);
            server = invoke(
                    method(DBClass, "newEmbeddedDB", load(classLoader, "ch.vorburger.mariadb4j.DBConfiguration")),
                    DBClass, method(builderClass, "build").invoke(builder));
            invoke(method(server.getClass(), "start"), server);
            System.out.println("Started MariaDB4j server");
            if (importSQLFile != null) {
                if (importSQLFile.exists()) {
                    // source(String resource, String username, String password,
                    // String dbName)
                    System.out.println("importing SQL file: " + importSQLFile.getAbsolutePath());
                    run(importSQLFile, username, password, dbName);
                } else {
                    log.error("Could not load MariaDB4j SQL file, file does not exist: "
                            + importSQLFile.getAbsolutePath());
                    System.out.println("Could not load MariaDB4j SQL file, file does not exist: : "
                            + importSQLFile.getAbsolutePath());
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            Thread.currentThread().setContextClassLoader(OCL);
        }
        isShuttingDown = false;
        // INSTANCE = this;
        // final Thread mainThread = Thread.currentThread();
        // Runtime.getRuntime().addShutdownHook(new Thread() {
        // public void run() {
        // try {
        // if(!isShuttingDown)
        // INSTANCE.stop();
        // mainThread.join();
        // } catch ( Exception e) {
        // e.printStackTrace();
        // }
        // }
        // });

    }

    public void run(File file, String username, String password, String dbName) throws IOException {
        FileInputStream from = new FileInputStream(file);
        // run("source " + file.getAbsolutePath(), username, password, dbName);
        run("[MariaDB4j] importing " + file.getAbsolutePath(), from, username, password, dbName);
    }

    public void createDB(String dbName) throws IOException {
        ClassLoader OCL = Thread.currentThread().getContextClassLoader();
        try {
            invoke(method(server.getClass(), "createDB", String.class), server, dbName);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            Thread.currentThread().setContextClassLoader(OCL);
        }
    }

    public void run(String logInfoText, InputStream fromIS, String username, String password, String dbName)
            throws IOException {
        ClassLoader OCL = Thread.currentThread().getContextClassLoader();
        try {
            java.lang.reflect.Method runMethod = method(server.getClass(), "run", String.class, InputStream.class,
                    String.class, String.class, String.class);
            // dirty hack so we don't have to pass in a potentially huge string
            runMethod.setAccessible(true);
            invoke(runMethod, server, logInfoText, fromIS, username, password, dbName);
        } catch (Exception e) {
            try {
                // this will print out the SQL exception if the problem was
                // there
                System.out.println("[MariaDB4j] Error running SQL: ");
                System.out.print("    ");
                System.out.println(e.getCause().getCause().getMessage());
            } catch (Exception e2) {
                e.printStackTrace();
            }
        } finally {
            if (fromIS != null) {
                fromIS.close();
            }
            Thread.currentThread().setContextClassLoader(OCL);
        }
    }

    public void run(String command, String username, String password, String dbName) {
        System.out.println("[MariaDB4j] Running SQL command: " + command);
        ClassLoader OCL = Thread.currentThread().getContextClassLoader();
        try {
            invoke(method(server.getClass(), "run", String.class, String.class, String.class, String.class), server,
                    command, username, password, dbName);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            Thread.currentThread().setContextClassLoader(OCL);
        }
    }

    public void stop() throws InterruptedException {
        isShuttingDown = true;
        System.out.println("*** Stopping MariaDB server on port " + port + " ...");
//        LaunchUtil.displayMessage("info", "Stopping embedded mariadb server on port " + port + " ...");
        ClassLoader OCL = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(classLoader);
        try {
            invoke(method(server.getClass(), "stop"), server);
            System.out.println("*** Stopped MariaDB server on port " + port);
        } catch (Exception e) {
            System.out.println("*** Error stopping MariaDB server on port " + port);
            e.printStackTrace();
        } finally {
            Thread.currentThread().setContextClassLoader(OCL);
        }
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getDbName() {
        return dbName;
    }

    public void setDbName(String dbName) {
        this.dbName = dbName;
    }

}