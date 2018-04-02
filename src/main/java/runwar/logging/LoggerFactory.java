package runwar.logging;

import runwar.options.ServerOptions;

import java.io.IOException;
import java.util.Properties;

import org.apache.log4j.xml.DOMConfigurator;

//import org.jboss.logmanager.LogContext;
//import org.jboss.logmanager.LogManager;
//import org.jboss.logmanager.PropertyConfigurator;


public class LoggerFactory {

    private static boolean initialized = false;
    private static volatile String logFile;
    private static volatile String errLogFile;
    private static volatile String logLevel;
    private static volatile String logPattern;
    
    public static void init(ServerOptions serverOptions) {
/*
        final Properties defaultProperties = new Properties();
        try {
            defaultProperties.load(LoggerFactory.class.getResourceAsStream("expression-logging.properties"));
            final LogContext logContext = LogContext.create();
            final PropertyConfigurator configurator = new PropertyConfigurator(logContext);
            configurator.configure(defaultProperties);
        } catch (IOException e) {
            e.printStackTrace();
        }
*/
        System.setProperty("org.jboss.logging.provider", "log4j");
//        System.setProperty("log4j2.skipJansi", "false");
//        System.setProperty("log4j.debug", "true");
//        System.setProperty("log4j.configuration", "file:runwar/log4j.properties");
        logFile = serverOptions.getLogDir().getPath() + '/' + serverOptions.getLogFileName() + ".out.txt";
        errLogFile = serverOptions.getLogDir().getPath() + '/' + serverOptions.getLogFileName() + ".err.txt";
        logLevel = serverOptions.getLoglevel().toUpperCase();
        logPattern = "%m%n";
        if(serverOptions.isDebug() || !serverOptions.getLoglevel().equalsIgnoreCase("info")) {
            logPattern = "[%color{%-5p}] %c: %m%n";
//            System.setProperty(org.slf4j.impl.SimpleLogger.DEFAULT_LOG_LEVEL_KEY, "DEBUG");
        } else if(serverOptions.getLoglevel().equalsIgnoreCase("trace")) {
//            System.setProperty(org.slf4j.impl.SimpleLogger.DEFAULT_LOG_LEVEL_KEY, "TRACE");
        }
        System.setProperty("runwar.logfile", logFile);
        System.setProperty("runwar.errlogfile", errLogFile);
        System.setProperty("runwar.loglevel", logLevel);
        System.setProperty("runwar.logpattern", logPattern);
        DOMConfigurator.configure(LoggerFactory.class.getClassLoader().getResource("log4j.filelog.xml"));
        initialized = true;
        if(System.getProperty("runwar.dumploggerstyles") != null) {
            RunwarLogger.LOG.trace("This is a TRACE message");
            RunwarLogger.LOG.debug("This is a DEBUG message");
            RunwarLogger.LOG.warn("This is a WARN message");
            RunwarLogger.LOG.error("This is an ERROR message");
        }
    }
    
    public static boolean defaults() {
        System.setProperty("log4j.configuration", "log4j.xml");
        System.setProperty("log4j.debug", "false");
        System.setProperty("org.jboss.logging.provider", "log4j");
        System.setProperty("runwar.logfile", "");
        System.setProperty("runwar.errlogfile", "");
        System.setProperty("runwar.loglevel", "WARN" );
        System.setProperty("runwar.logpattern", "%d{yyyy-MM-dd HH:mm:ss} %-5p %c - %m%n" );
        initialized = true;
        return initialized;
    }

}
