package runwar.logging;

import java.util.ArrayList;
import java.util.HashMap;

import runwar.Server;

public class Logger {
    private static boolean loggingIsInitialized = false;
    private static org.jboss.logging.Logger logger;
    private static String loggerName;
    private static ArrayList<HashMap<String, Object>> lazyMessages;
    private static org.jboss.logging.Logger.Level logLevel;

    public Logger(String name) {
        loggerName = name;
        lazyMessages = new ArrayList<HashMap<String, Object>>();
    }
    public void debug(Object message) {
        log(org.jboss.logging.Logger.Level.DEBUG, message);
    }
    public void debug(String string, Exception e) {
        log(org.jboss.logging.Logger.Level.DEBUG, string, e);
    }
    public void debugf(String format, Object object) {
        logf(org.jboss.logging.Logger.Level.DEBUG,format,object);
    }
    public void debugLazy(Object message) {
        HashMap<String, Object> lazyMessage = new HashMap<String, Object>();
        lazyMessage.put("level",org.jboss.logging.Logger.Level.DEBUG);
        lazyMessage.put("message",message);
        lazyMessages.add(lazyMessage);
    }

    public void trace(Object message) {
        log(org.jboss.logging.Logger.Level.TRACE, message);
    }
    public void trace(String string, Exception e) {
        log(org.jboss.logging.Logger.Level.TRACE, string, e);
    }
    public void tracef(String format, Object object) {
        logf(org.jboss.logging.Logger.Level.TRACE,format,object);
    }
    
    public void error(Object object) {
        log(org.jboss.logging.Logger.Level.ERROR, object);
    }
    public void error(String string, Exception e) {
        log(org.jboss.logging.Logger.Level.ERROR, string, e);
    }
    public void errorf(String format, Object object) {
        logf(org.jboss.logging.Logger.Level.ERROR,format,object);
    }

    public void warn(Object object) {
        log(org.jboss.logging.Logger.Level.WARN, object);
    }
    public void warn(String string, Exception e) {
        log(org.jboss.logging.Logger.Level.WARN, string, e);
    }
    public void warnf(String format, Object object) {
        logf(org.jboss.logging.Logger.Level.WARN,format,object);
    }

    
    public void info(Object object) {
        log(org.jboss.logging.Logger.Level.INFO, object);
    }
    public void info(String string, Exception e) {
        log(org.jboss.logging.Logger.Level.INFO, string, e);
    }
    public void info(String format, Object object) {
        logf(org.jboss.logging.Logger.Level.INFO,format,object);
    }
    
    public void fatal(Object object) {
        log(org.jboss.logging.Logger.Level.FATAL, object);
    }
    public void fatal(String string, Exception e) {
        log(org.jboss.logging.Logger.Level.FATAL, string, e);
    }
    public void fatal(String format, Object object) {
        logf(org.jboss.logging.Logger.Level.FATAL,format,object);
    }
    
    private static void log(org.jboss.logging.Logger.Level level, Object object) {
        if(!loggingIsInitialized)
            initLogging();
        
        switch(level) {
        case DEBUG:
            logger.debug(object);
            break;
        case WARN:
            logger.warn(object);
            break;
        case INFO:
            logger.info(object);
            break;
        case TRACE:
            logger.trace(object);
            break;
        case FATAL:
            logger.fatal(object);
            break;
        default:
            logger.debug(object);
            break;
        }
    }
    
    private static void logf(org.jboss.logging.Logger.Level level, String format, Object object) {
        if(!loggingIsInitialized)
            initLogging();
        
        switch(level) {
        case DEBUG:
            logger.debugf(format,object);
            break;
        case WARN:
            logger.warnf(format,object);
            break;
        case INFO:
            logger.infof(format,object);
            break;
        case TRACE:
            logger.tracef(format,object);
            break;
        case FATAL:
            logger.fatalf(format,object);
            break;
        default:
            logger.debugf(format,object);
            break;
        }
    }
        
    private static void log(org.jboss.logging.Logger.Level level, String message, Exception exception) {
        if(!loggingIsInitialized)
            initLogging();
        
        switch(level) {
        case DEBUG:
            logger.debug(message,exception);
            break;
        case WARN:
            logger.warn(message,exception);
            break;
        case INFO:
            logger.info(message,exception);
            break;
        case TRACE:
            logger.trace(message,exception);
            break;
        case FATAL:
            logger.fatal(message,exception);
            break;
        default:
            logger.debug(message,exception);
            break;
        }
    }
    
    // log4j logging is lazily constructed; it gets initialized
    // the first time the invoking app calls a log method
    private static void initLogging() {
        logger = org.jboss.logging.Logger.getLogger("RunwarLogger");
        LogSubverter.subvertLoggers(Server.getServerOptions().getLoglevel());
        logLevel = org.jboss.logging.Logger.Level.valueOf(Server.getServerOptions().getLoglevel());
        loggingIsInitialized = true;
        for(HashMap<String, Object> lm : lazyMessages) {
            org.jboss.logging.Logger.Level level = (org.jboss.logging.Logger.Level)lm.get("level");
            log(level,loggerName + lm.get("message"));
        }

        //org.apache.log4j.Logger debugLogger = org.apache.log4j.LoggerFactory.getLogger("DebugLogger");
        //debugLogger.addAppender(someConfiguredFileAppender);
    }
    
    public static Logger getLogger(String string) {
        return new Logger(string);
    }
}

final class JDKLevel extends java.util.logging.Level {

    private static final long serialVersionUID = 1L;

    protected JDKLevel(final String name, final int value) {
        super(name, value);
    }

    protected JDKLevel(final String name, final int value, final String resourceBundleName) {
        super(name, value, resourceBundleName);
    }

    public static final JDKLevel FATAL = new JDKLevel("FATAL", 1100);
    public static final JDKLevel ERROR = new JDKLevel("ERROR", 1000);
    public static final JDKLevel WARN = new JDKLevel("WARN", 900);
    public static final JDKLevel INFO = new JDKLevel("INFO", 800);
    public static final JDKLevel DEBUG = new JDKLevel("DEBUG", 500);
    public static final JDKLevel TRACE = new JDKLevel("TRACE", 400);
}