package runwar.logging;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import runwar.logging.Logger;
//import org.jboss.logging.Logger; 

import runwar.Server;

public class LogSubverter {

    public static final Set<String> loggers = new HashSet<String>(Arrays.asList(new String[] {
            "RunwarLogger",
            "runwar.Start",
            "org.jboss.logging",
            "org.xnio.Xnio",
            "org.xnio.nio.NioXnio",
            "io.undertow.UndertowLogger"
    }));

    private static Logger log = Logger.getLogger("RunwarLogger");


    public static void subvertLoggers(String level) {
        subvertJDKLoggers(level);
        subvertLog4jLoggers(level);
    }
    
    
    public static void subvertJDKLoggers(String level) {
        System.setProperty("java.util.logging.ConsoleHandler.formatter", "java.util.logging.SimpleFormatter");
        System.setProperty("java.util.logging.SimpleFormatter.format", "%1$tY-%1$tm-%1$td %1$tH:%1$tM:%1$tS %4$s %3$s %5$s%6$s%n");
        java.util.logging.ConsoleHandler chandler = new java.util.logging.ConsoleHandler();
        java.util.logging.Level LEVEL = null;
        if(level.trim().toUpperCase().equals("TRACE"))
            LEVEL = JDKLevel.TRACE;
        if(level.trim().toUpperCase().equals("WARN"))
            LEVEL = JDKLevel.WARN;
        if(level.trim().toUpperCase().equals("DEBUG"))
            LEVEL = JDKLevel.DEBUG;
        if(level.trim().toUpperCase().equals("ERROR"))
            LEVEL = JDKLevel.ERROR;
        if(level.trim().toUpperCase().equals("FATAL"))
            LEVEL = JDKLevel.FATAL;
        chandler.setLevel(LEVEL);
        java.util.logging.LogManager logManager = java.util.logging.LogManager.getLogManager();
//        try {
//            InputStream fis = Start.class.getClassLoader().getResourceAsStream("resources/logging.properties");
//            logManager.readConfiguration(fis);
//            fis.close();
//            log.debug("starting myApp");
//        } 
//        catch(IOException e) {
//            e.printStackTrace();
//        }
        
        for(Enumeration<String> loggerNames = logManager.getLoggerNames(); loggerNames.hasMoreElements();){
            String name = loggerNames.nextElement();
            java.util.logging.Logger nextLogger = logManager.getLogger(name);
            if(loggers.contains(name) && nextLogger != null) {
                log.debugLazy("JDK loggers detected, level to "+level);
                nextLogger.setUseParentHandlers(false);
                nextLogger.setLevel(LEVEL);
                if(nextLogger.getHandlers() != null) {
                    for(java.util.logging.Handler handler : nextLogger.getHandlers()) {
                        nextLogger.removeHandler(handler);
                    }
                    nextLogger.addHandler(chandler);                    
                }
            }
        }
    }

    public static void subvertLog4jLoggers(String level) {
        // sometimes log4j is in use?  TODO: sort the class loading
        Properties log4jprops = new Properties();
        log4jprops.put("log4j.rootLogger", level.toUpperCase()+", stdout");
        log4jprops.put("log4j.appender.stdout", "org.apache.log4j.ConsoleAppender");
        log4jprops.put("log4j.appender.stdout.Target", "System.out");
        log4jprops.put("log4j.appender.stdout.layout", "org.apache.log4j.PatternLayout");
        log4jprops.put("log4j.appender.stdout.layout.ConversionPattern", "%d{yy/MM/dd HH:mm:ss} %p %c{2}: %m%n");
        log4jprops.put("RunwarLogger", level.toUpperCase()+", stdout");
        log4jprops.put("org.jboss.logging", level.toUpperCase()+", stdout");
        ClassLoader cl = Server.getClassLoader();
        if(cl == null)
            cl = Thread.currentThread().getContextClassLoader();
        try{
            Class<?> LogManager = cl.loadClass("org.apache.log4j.LogManager");
            Method resetConfiguration = LogManager.getMethod("resetConfiguration",new Class[]{});
            resetConfiguration.invoke(null, new Object[]{});
            /*
            Class<?> RootLogger = cl.loadClass("org.jboss.logging.Log4jLogger");
            Method rootLogger = RootLogger.getMethod("getRootLogger",new Class[]{});
            Object root = rootLogger.invoke(null, new Object[]{});
            Method getLoggerRepository = root.getClass().getMethod("getLoggerRepository", new Class[]{});
            Object repo = getLoggerRepository.invoke(root, new Object[]{});
            Method resetConfiguration = repo.getClass().getMethod("resetConfiguration", new Class[]{});
            resetConfiguration.invoke(repo, new Object[]{});
             */
            log.debugLazy("log4j loggers detected, configuration reset to "+level);
        } catch (ClassNotFoundException ignored) {
            log.debugLazy("No log4j loggers detected");
//            ignored.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        try{
            Class<?> PropertyConfigurator = cl.loadClass("org.apache.log4j.PropertyConfigurator");
            Method configure = PropertyConfigurator.getMethod("configure",new Class[]{Properties.class});
            configure.invoke(PropertyConfigurator.getConstructor().newInstance(), new Object[]{log4jprops});
        } catch (ClassNotFoundException ignored) {
        } catch (Exception e) {
            e.printStackTrace();
        }
        
//        Logger rootLogger = Logger.getRootLogger();
//        rootLogger.setLevel(org.apache.log4j.Level.DEBUG);
////        org.apache.log4j.Logger.getRootLogger().getLoggerRepository().resetConfiguration();
        
//        ConsoleAppender console = new ConsoleAppender(); //create appender
//        String PATTERN = "%d [%p|%c|%C{1}] %m%n";
//        console.setLayout(new PatternLayout(PATTERN)); 
//        console.setThreshold(Level.ALL);
//        console.activateOptions();
//        org.apache.log4j.Logger.getRootLogger().addAppender(console);
//
//        Enumeration currentLoggers = LogManager.getCurrentLoggers();
//        while(currentLoggers.hasMoreElements()) {
//            org.apache.log4j.Logger logger = (org.apache.log4j.Logger)currentLoggers.nextElement();
//            System.err.println(logger.getName());
//            org.apache.log4j.Logger l = LogManager.getLogger(logger.getName());
//            l.setLevel(org.apache.log4j.Level.DEBUG);
//        }
        
        
    }
    
    public static java.util.logging.Level translate(final org.jboss.logging.Logger.Level level) {
        if (level != null) switch (level) {
            case FATAL: return JDKLevel.FATAL;
            case ERROR: return JDKLevel.ERROR;
            case WARN:  return JDKLevel.WARN;
            case INFO:  return JDKLevel.INFO;
            case DEBUG: return JDKLevel.DEBUG;
            case TRACE: return JDKLevel.TRACE;
        }
        return JDKLevel.ALL;
    }



}
