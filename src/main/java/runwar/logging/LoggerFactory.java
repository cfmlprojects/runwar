package runwar.logging;

import runwar.options.ServerOptions;
import runwar.options.ServerOptionsImpl;

import java.util.ArrayList;
import java.util.List;
import org.apache.log4j.Appender;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.RollingFileAppender;
import com.jcabi.log.MulticolorLayout;

//import org.jboss.logmanager.LogContext;
//import org.jboss.logmanager.LogManager;
//import org.jboss.logmanager.PropertyConfigurator;

public class LoggerFactory {

    private static boolean initialized = false;
    private static volatile String logFile;
    private static volatile String logLevel;
    private static volatile String logPattern;
    private static List<Appender> appenders;
    private static List<Logger> loggers;
    private static RollingFileAppender rewriteLogAppender;

    public static void configure(ServerOptions serverOptions) {
        Logger.getRootLogger().getLoggerRepository().resetConfiguration();
        logLevel = serverOptions.getLoglevel().toUpperCase();
        logPattern = "%m%n";
        appenders = new ArrayList<Appender>();
        loggers = new ArrayList<Logger>();
        Level level = Level.toLevel(logLevel);

        ConsoleAppender console = new ConsoleAppender();
        MulticolorLayout layout = new MulticolorLayout();
        layout.setConversionPattern(logPattern);
        layout.setLevels("TRACE:1;32,DEBUG:1;33,INFO:1;34,WARN:1;43,ERROR:37;41,FATAL:37;40");
        console.setLayout(layout);
        console.setThreshold(Level.toLevel(logLevel));
        console.activateOptions();
        appenders.add(console);
        Logger.getRootLogger().addAppender(console);

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

        Logger HTTP_CLIENT_LOG = Logger.getLogger("org.apache.http.client.protocol");
        loggers.add(HTTP_CLIENT_LOG);

        Logger RUNWAR_SERVER = Logger.getLogger("runwar.server");
        loggers.add(RUNWAR_SERVER);

        Logger RUNWAR_CONFIG = Logger.getLogger("runwar.config");
        loggers.add(RUNWAR_CONFIG);

        Logger RUNWAR_SECURITY = Logger.getLogger("runwar.security");
        loggers.add(RUNWAR_SECURITY);

        Logger RUNWAR_REQUEST = Logger.getLogger("runwar.request");
        loggers.add(RUNWAR_REQUEST);

        if (serverOptions.getURLRewriteLog() != null) {
            // errLogFile = serverOptions.getLogDir().getPath() + '/' +
            // serverOptions.getLogFileName() + ".err.txt";
            rewriteLogAppender = new RollingFileAppender();
            rewriteLogAppender.setName("URLRewriteFileLogger");
            rewriteLogAppender.setFile(serverOptions.getURLRewriteLog().getAbsolutePath());
            rewriteLogAppender.setLayout(new PatternLayout("%d %-5p [%c{1}] %m%n"));
            rewriteLogAppender.setThreshold(Level.toLevel(logLevel));
            rewriteLogAppender.setAppend(true);
            rewriteLogAppender.setMaxFileSize("10MB");
            rewriteLogAppender.setMaxBackupIndex(3);
            rewriteLogAppender.activateOptions();
        }

        if (serverOptions.isDebug() || !logLevel.equalsIgnoreCase("info")) {
            logPattern = "[%color{%-5p}] %c: %m%n";
            layout.setConversionPattern(logPattern);

            if (logLevel.equalsIgnoreCase("trace")) {
                DORKBOX_LOG.setLevel(level);
                appenders.forEach(appender -> {
                    DORKBOX_LOG.addAppender(appender);
                });
                UNDERTOW_LOG.setLevel(level);
                HTTP_CLIENT_LOG.setLevel(level);
                RUNWAR_SERVER.setLevel(level);
                RUNWAR_CONFIG.setLevel(level);
                RUNWAR_SECURITY.setLevel(level);
                RUNWAR_REQUEST.setLevel(level);
                configureUrlRewriteLoggers(true);
            } else {
                RUNWAR_SERVER.setLevel(level);
                DORKBOX_LOG.setLevel(Level.ERROR);
                UNDERTOW_LOG.setLevel(Level.WARN);
                HTTP_CLIENT_LOG.setLevel(Level.WARN);
                RUNWAR_CONFIG.setLevel(Level.WARN);
                RUNWAR_SECURITY.setLevel(Level.WARN);
                RUNWAR_REQUEST.setLevel(Level.WARN);
                configureUrlRewriteLoggers(true);
            }
        }

        if (serverOptions.hasLogDir()) {
            // errLogFile = serverOptions.getLogDir().getPath() + '/' +
            // serverOptions.getLogFileName() + ".err.txt";
            logFile = serverOptions.getLogDir().getPath() + '/' + serverOptions.getLogFileName() + ".out.txt";
            RollingFileAppender fa = new RollingFileAppender();
            fa.setName("FileLogger");
            fa.setFile(logFile);
            fa.setLayout(new PatternLayout("%d %-5p [%c{1}] %m%n"));
            fa.setThreshold(Level.toLevel(logLevel));
            fa.setAppend(true);
            fa.setMaxFileSize("10MB");
            fa.setMaxBackupIndex(10);
            fa.activateOptions();
            appenders.add(fa);
            Logger.getRootLogger().addAppender(fa);
        }
        Logger.getRootLogger().addAppender(console);

        loggers.forEach(logger -> {
            appenders.forEach(appender -> {
                logger.addAppender(appender);
                logger.setAdditivity(false);
            });
        });

        initialized = true;

        if (System.getProperty("runwar.dumploggerstyles") != null) {
            RunwarLogger.LOG.trace("This is a TRACE message");
            RunwarLogger.LOG.debug("This is a DEBUG message");
            RunwarLogger.LOG.warn("This is a WARN message");
            RunwarLogger.LOG.error("This is an ERROR message");
        }
    }

    public static boolean isInitialized() {
        return initialized;
    }

    public static boolean initialize() {
        if (!initialized)
            configure(new ServerOptionsImpl().setLogDir(""));
        return initialized;
    }

    public static void configureUrlRewriteLoggers(boolean isTrace) {
        Logger REWRITE_CONDITION_LOG = Logger.getLogger(" org.tuckey.web.filters.urlrewrite.Condition");
        Logger REWRITE_RULE_LOG = Logger.getLogger(" org.tuckey.web.filters.urlrewrite.RuleBase");
        Logger REWRITE_SUBSTITUTION_LOG = Logger
                .getLogger(" org.tuckey.web.filters.urlrewrite.substitution.VariableReplacer");
        Logger REWRITE_EXECUTION_LOG = Logger.getLogger(" org.tuckey.web.filters.urlrewrite.RuleExecutionOutput");
        Logger REWRITE_WRITER_LOG = Logger.getLogger(" org.tuckey.web.filters.urlrewrite.UrlRewriter");
        Logger REWRITE_URL_LOG = Logger.getLogger(" org.tuckey.web.filters.urlrewrite.RewrittenUrl");

        if (rewriteLogAppender != null) {
            RunwarLogger.CONF_LOG.warnf("Enabling URL rewrite log: %s", rewriteLogAppender.getFile());
            REWRITE_CONDITION_LOG.addAppender(rewriteLogAppender);
            REWRITE_RULE_LOG.addAppender(rewriteLogAppender);
            REWRITE_SUBSTITUTION_LOG.addAppender(rewriteLogAppender);
            REWRITE_EXECUTION_LOG.addAppender(rewriteLogAppender);
            REWRITE_WRITER_LOG.addAppender(rewriteLogAppender);
            REWRITE_URL_LOG.addAppender(rewriteLogAppender);
        }

        if (isTrace) {
            RunwarLogger.CONF_LOG.infof("Enabling URL rewrite log level: %s", "TRACE");
            REWRITE_CONDITION_LOG.setLevel(Level.TRACE);
            REWRITE_RULE_LOG.setLevel(Level.TRACE);
            REWRITE_SUBSTITUTION_LOG.setLevel(Level.TRACE);
            REWRITE_EXECUTION_LOG.setLevel(Level.TRACE);
            REWRITE_WRITER_LOG.setLevel(Level.TRACE);
            REWRITE_URL_LOG.setLevel(Level.TRACE);
        } else {
            RunwarLogger.CONF_LOG.infof("Enabling URL rewrite log level: %s", "DEBUG");
            REWRITE_CONDITION_LOG.setLevel(Level.DEBUG);
            REWRITE_RULE_LOG.setLevel(Level.DEBUG);
            REWRITE_SUBSTITUTION_LOG.setLevel(Level.DEBUG);
            REWRITE_EXECUTION_LOG.setLevel(Level.DEBUG);
            REWRITE_WRITER_LOG.setLevel(Level.DEBUG);
            REWRITE_URL_LOG.setLevel(Level.DEBUG);
        }
        REWRITE_CONDITION_LOG.info("EEEEEE");

    }

}
