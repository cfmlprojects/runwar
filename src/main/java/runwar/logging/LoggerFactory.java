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
    private static ServerOptions serverOptions;

    public static synchronized void configure(ServerOptions options) {

        Logger.getRootLogger().getLoggerRepository().resetConfiguration();
        serverOptions = options;
        logLevel = serverOptions.logLevel().toUpperCase();
        appenders = new ArrayList<>();
        loggers = new ArrayList<>();
        Level level = Level.toLevel(logLevel);
        consoleAppender = consoleAppender(serverOptions.getLogPattern());
        appenders.add(consoleAppender);
        Logger.getRootLogger().setLevel(Level.WARN);
        Logger.getRootLogger().addAppender(consoleAppender);

        Logger DORKBOX_LOG = Logger.getLogger("dorkbox.systemTray.SystemTray");
        loggers.add(DORKBOX_LOG);

        Logger OSCACHE_LOG = Logger.getLogger("com.opensymphony.oscache.base.Config");
        loggers.add(OSCACHE_LOG);

        Logger JBOSS_LOG = Logger.getLogger("org.jboss.logging");
        loggers.add(JBOSS_LOG);

        Logger UNDERTOW_LOG = Logger.getLogger("io.undertow.servlet");
        loggers.add(UNDERTOW_LOG);

        Logger UNDERTOW_PREDICATE_LOG = Logger.getLogger("io.undertow.predicate");
        loggers.add(UNDERTOW_PREDICATE_LOG);

        Logger UNDERTOW_REQUEST_DUMPER_LOG = Logger.getLogger("io.undertow.request.dump");
        loggers.add(UNDERTOW_REQUEST_DUMPER_LOG);
        
        Logger UNDERTOW_IO_LOG = Logger.getLogger("io.undertow");
        loggers.add(UNDERTOW_IO_LOG);

        Logger XNIO_LOG = Logger.getLogger("org.xnio");
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
        
        if (serverOptions.urlRewriteLog() != null) {
            rewriteLogAppender = new RollingFileAppender();
            rewriteLogAppender.setName("URLRewriteFileLogger");
            rewriteLogAppender.setFile(serverOptions.urlRewriteLog().getAbsolutePath());
            rewriteLogAppender.setLayout(new PatternLayout(serverOptions.getLogPattern()));
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
        UNDERTOW_IO_LOG.setLevel(Level.WARN);
        XNIO_LOG.setLevel(Level.WARN);
        HTTP_CLIENT_LOG.setLevel(Level.WARN);
        UNDERTOW_REQUEST_DUMPER_LOG.setLevel(Level.INFO);
        System.setProperty("org.eclipse.jetty.LEVEL", "WARN");


        if (serverOptions.debug() || !logLevel.equalsIgnoreCase("info")) {

            if (logLevel.equalsIgnoreCase("trace")) {
                DORKBOX_LOG.setLevel(level);
                appenders.forEach(DORKBOX_LOG::addAppender);
                UNDERTOW_LOG.setLevel(level);
                UNDERTOW_PREDICATE_LOG.setLevel(level);
                HTTP_CLIENT_LOG.setLevel(level);
                RUNWAR_CONFIG.setLevel(level);
                RUNWAR_SERVER.setLevel(level);
                RUNWAR_CONTEXT.setLevel(level);
                RUNWAR_SECURITY.setLevel(level);
                
                // This logger is only used in the resource mapper and is really chatty
                // Consider a setting to enable it only when troubleshooting file system mapping issues
                if( serverOptions.resourceManagerLogging() ) {
                    RUNWAR_REQUEST.setLevel(level);	
                }
                
                Logger.getRootLogger().setLevel(level);
                configureUrlRewriteLoggers(true);
            } else {
                RUNWAR_REQUEST.setLevel(Level.INFO);
                RUNWAR_SECURITY.setLevel(Level.DEBUG);
                UNDERTOW_PREDICATE_LOG.setLevel(Level.DEBUG);
                configureUrlRewriteLoggers(false);
            }
        }

        if (serverOptions.hasLogDir()) {
            logFile = serverOptions.logDir().getPath() + '/' + serverOptions.logFileName() + ".out.txt";
            RollingFileAppender fa = new RollingFileAppender();
            fa.setName("FileLogger");
            fa.setFile(logFile);
            fa.setLayout(new PatternLayout(serverOptions.logPattern()));
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
        PatternLayout layout = new PatternLayout();
        layout.setConversionPattern(pattern);
        appender.setLayout(layout);
        appender.setName("rw.console");
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
            configure(new ServerOptionsImpl().logDir(""));
        return initialized;
    }

    public static void configureUrlRewriteLoggers(boolean isTrace) {
        boolean hadLoggers = urlrewriteLoggers == null;
        Logger REWRITE_CONDITION_LOG = Logger.getLogger("org.tuckey.web.filters.urlrewrite.Condition");
        Logger REWRITE_RULE_LOG = Logger.getLogger("org.tuckey.web.filters.urlrewrite.RuleBase");
        Logger REWRITE_SUBSTITUTION_LOG = Logger
                .getLogger("org.tuckey.web.filters.urlrewrite.substitution.VariableReplacer");
        Logger REWRITE_EXECUTION_LOG = Logger.getLogger("org.tuckey.web.filters.urlrewrite.RuleExecutionOutput");
        Logger REWRITE_WRITER_LOG = Logger.getLogger("org.tuckey.web.filters.urlrewrite.UrlRewriter");
        Logger REWRITE_URL_LOG = Logger.getLogger("org.tuckey.web.filters.urlrewrite");
        Logger REWRITE_FILTER = Logger.getLogger("runwar.util.UrlRewriteFilter");
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
                logger.addAppender(consoleAppender(serverOptions.getLogPattern()));
                logger.setAdditivity(false);
            });
        } else {
            if(!hadLoggers){
                RunwarLogger.CONF_LOG.infof("Enabling URL rewrite log level: %s", "DEBUG");
            }
            urlrewriteLoggers.forEach(logger -> {
                logger.setLevel(Level.WARN);
                logger.addAppender(consoleAppender(serverOptions.getLogPattern()));
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
