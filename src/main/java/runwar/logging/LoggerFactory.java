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

        System.setProperty("log4j.configuration", "log4j.filelog.xml");
        logFile = serverOptions.getLogDir().getPath() + '/' + serverOptions.getLogFileName() + "log";
        logLevel = serverOptions.getLoglevel().toUpperCase();
        logPattern = "%m%n";
        if(serverOptions.isDebug() || !serverOptions.getLoglevel().equalsIgnoreCase("info")) {
            logPattern = "[%color{%-5p}] %c: %m%n";
//            System.setProperty(org.slf4j.impl.SimpleLogger.DEFAULT_LOG_LEVEL_KEY, "DEBUG");
        } else if(serverOptions.getLoglevel().equalsIgnoreCase("trace")) {
//            System.setProperty(org.slf4j.impl.SimpleLogger.DEFAULT_LOG_LEVEL_KEY, "TRACE");
        }
        System.setProperty("runwar.logfile", logFile);
        System.setProperty("runwar.loglevel", logLevel);
        System.setProperty("runwar.logpattern", logPattern);
        DOMConfigurator.configure(LoggerFactory.class.getClassLoader().getResource("log4j.filelog.xml"));
        initialized = true;
    }
    
    public static boolean defaults() {
        System.setProperty("log4j.configuration", "log4j.xml");
        System.setProperty("log4j.debug", "true");
        System.setProperty("org.jboss.logging.provider", "log4j");
        System.setProperty("runwar.logfile", "");
        System.setProperty("runwar.loglevel", "DEBUG" );
        System.setProperty("runwar.logpattern", "%d{yyyy-MM-dd HH:mm:ss} %-5p %c:%L - %m%n" );
        initialized = true;
        return initialized;
    }

}
