package runwar.options;

import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.PosixParser;

import runwar.Server;

import static runwar.options.ServerOptions.Keys;
import static runwar.logging.RunwarLogger.CONF_LOG;

public class CommandLineHandler {
    
    private static PosixParser parser;
    private static final String SYNTAX = " java -jar runwar.jar [-war] path/to/war [options]";
    private static final String HEADER = " The runwar lib wraps undertow with more awwsome. Defaults (parenthetical)";
    private static final String FOOTER = " source: https://github.com/Ortus-Solutions/runwar.git";
    
    public CommandLineHandler() {
    }
    
    @SuppressWarnings("static-access")
    private static Options getOptions() {
        final Options options = new Options();
        options.addOption(OptionBuilder
                .withLongOpt(Keys.CONFIG)
                .withDescription("config file")
                .hasArg().withArgName("file")
                .create("c"));
        
        options.addOption(OptionBuilder
                .withDescription("path to war")
                .hasArg()
                .withArgName("path")
                .create(Keys.WAR));
        
        options.addOption(OptionBuilder
                .withLongOpt("server-name")
                .withDescription("server name (default)")
                .hasArg()
                .withArgName(Keys.NAME)
                .create(Keys.NAME));
        
        options.addOption(OptionBuilder
                .withLongOpt("context-path")
                .withDescription("context path.  (/)")
                .hasArg().withArgName(Keys.CONTEXT)
                .create(Keys.CONTEXT));
        
        options.addOption(OptionBuilder
                .withDescription("host.  (127.0.0.1)")
                .hasArg().withArgName(Keys.HOST)
                .create(Keys.HOST));
        
        options.addOption(OptionBuilder
                .withLongOpt(Keys.PORT)
                .withDescription("port number.  (8088)")
                .hasArg().withArgName("http port").withType(Number.class)
                .create('p'));
        
        options.addOption(OptionBuilder
                .withLongOpt("stop-port")
                .withDescription("stop listener port number. (8779)\n")
                .hasArg().withArgName(Keys.PORT).withType(Number.class)
                .create(Keys.STOPSOCKET));
        
        options.addOption(OptionBuilder
                .withLongOpt("stop-password")
                .withDescription("Pasword checked when stopping server\n")
                .hasArg().withArgName(Keys.PASSWORD)
                .create(Keys.PASSWORD));
        
        options.addOption(OptionBuilder
                .withDescription("stop backgrounded.  Optional stop-port")
                .hasOptionalArg().withArgName(Keys.PORT)
                .hasOptionalArg().withArgName(Keys.PASSWORD)
                .withValueSeparator(' ')
                .create(Keys.STOP));
        
        options.addOption(OptionBuilder
                .withLongOpt("http-enable")
                .withDescription("Enable HTTP.  Default is true ,unless SSL or AJP are enable.")
                .hasArg().withArgName("true|false").withType(Boolean.class)
                .create(Keys.HTTPENABLE));
        
        options.addOption(OptionBuilder
                .withLongOpt("ajp-enable")
                .withDescription("Enable AJP.  Default is false.  When enable, http is disabled by default.")
                .hasArg().withArgName("true|false").withType(Boolean.class)
                .create(Keys.AJPENABLE));
        
        options.addOption(OptionBuilder
                .withLongOpt("urlrewrite-enable")
                .withDescription("Enable URL Rewriting.  Default is true.")
                .hasArg().withArgName("true|false").withType(Boolean.class)
                .create(Keys.URLREWRITEENABLE));
        
        options.addOption(OptionBuilder
                .withLongOpt("urlrewrite-log")
                .withDescription("URL rewriting log file.")
                .hasArg().withArgName("path/to/urlrewrite/log")
                .create(Keys.URLREWRITELOG));
        
        options.addOption(OptionBuilder
                .withLongOpt("urlrewrite-file")
                .withDescription("URL rewriting config file.")
                .hasArg().withArgName("path/to/urlrewrite/file")
                .create(Keys.URLREWRITEFILE));
        
        options.addOption(OptionBuilder
                .withLongOpt("urlrewrite-check")
                .withDescription("URL rewriting config file realod check interval, 0 for every request. (disabled)")
                .hasArg().withArgName("interval")
                .create(Keys.URLREWRITECHECK));
        
        options.addOption(OptionBuilder
                .withLongOpt("urlrewrite-statuspath")
                .withDescription("URL rewriting status path. (disabled)")
                .hasArg().withArgName("path")
                .create(Keys.URLREWRITESTATUSPATH));
        
        options.addOption(OptionBuilder
                .withLongOpt("ssl-enable")
                .withDescription("Enable SSL.  Default is false.  When enable, http is disabled by default.")
                .hasArg().withArgName("true|false").withType(Boolean.class)
                .create("sslenable"));
        
        options.addOption(OptionBuilder
                .withLongOpt("ssl-port")
                .withDescription("SSL port.  Disabled if not set.")
                .hasArg().withArgName(Keys.PORT).withType(Number.class)
                .create(Keys.SSLPORT));
        
        options.addOption(OptionBuilder
                .withLongOpt("ssl-cert")
                .withDescription("SSL certificate file in x509 (PKS#12) format.")
                .hasArg().withArgName("certificate")
                .create(Keys.SSLCERT));
        
        options.addOption(OptionBuilder
                .withLongOpt("ssl-key")
                .withDescription("SSL private key file in DER (PKS#8) format.")
                .hasArg().withArgName("key")
                .create(Keys.SSLKEY));
        
        options.addOption(OptionBuilder
                .withLongOpt("ssl-keypass")
                .withDescription("SSL key passphrase.")
                .hasArg().withArgName("passphrase")
                .create(Keys.SSLKEYPASS));
        
        options.addOption(OptionBuilder
                .withLongOpt("ajp-port")
                .withDescription("AJP port.  Disabled if not set.")
                .hasArg().withArgName("ajp port").withType(Number.class)
                .create(Keys.AJPPORT));
        
        options.addOption(OptionBuilder
                .withLongOpt("log-dir")
                .withDescription("Log directory.  (WEB-INF/logs)")
                .hasArg().withArgName("path/to/log/dir")
                .create(Keys.LOGDIR));
        
        options.addOption(OptionBuilder
                .withLongOpt("log-basename")
                .withDescription("Log file base name/prefix [default:server]")
                .hasArg().withArgName("basename")
                .create(Keys.LOGBASENAME));
        
        options.addOption(OptionBuilder
                .withLongOpt("logrequests-dir")
                .withDescription("Log requests directory")
                .hasArg().withArgName("/path/to/dir")
                .create(Keys.LOGREQUESTSDIR));
        
        options.addOption(OptionBuilder
                .withLongOpt("logrequests-basename")
                .withDescription("Requests log file base name/prefix  [default:request]")
                .hasArg().withArgName("basename")
                .create(Keys.LOGREQUESTSBASENAME));
        
        options.addOption(OptionBuilder
                .withLongOpt("logrequests-enable")
                .withDescription("Enables or disable request logging [default:false]")
                .hasArg().withArgName("true|false").withType(Boolean.class)
                .create(Keys.LOGREQUESTS));
        
        options.addOption(OptionBuilder
                .withLongOpt("logaccess-dir")
                .withDescription("Log access directory")
                .hasArg().withArgName("/path/to/dir")
                .create(Keys.LOGACCESSDIR));
        
        options.addOption(OptionBuilder
                .withLongOpt("logaccess-basename")
                .withDescription("Access log file base name/prefix [default:access]")
                .hasArg().withArgName("basename")
                .create(Keys.LOGACCESSBASENAME));
        
        options.addOption(OptionBuilder
                .withLongOpt("logaccess-enable")
                .withDescription("Enables or disable access logging [default:false]")
                .hasArg().withArgName("true|false").withType(Boolean.class)
                .create(Keys.LOGACCESS));
        
        options.addOption(OptionBuilder
                .withLongOpt(Keys.DIRS)
                .withDescription("List of external directories to serve from")
                .hasArg().withArgName("path,path,... or alias=path,..")
                .create("d"));
        
        options.addOption(OptionBuilder
                .withLongOpt("lib-dirs")
                .withDescription("List of directories to add contents of to classloader")
                .hasArg().withArgName("path,path,...")
                .create(Keys.LIBDIRS));
        
        options.addOption(OptionBuilder
                .withLongOpt(Keys.JAR)
                .withDescription("jar to be added to classpath")
                .hasArg().withArgName("path")
                .create("j"));
        
        options.addOption(OptionBuilder
                .withLongOpt(Keys.BACKGROUND)
                .withDescription("Run in background (true)")
                .hasArg().withArgName("true|false").withType(Boolean.class)
                .create('b'));
        
        options.addOption(OptionBuilder
                .withLongOpt(Keys.OPENBROWSER)
                .withDescription("Open default web browser after start (false)")
                .hasArg().withArgName("true|false")
                .create("open"));
        
        options.addOption(OptionBuilder
                .withLongOpt(Keys.OPENURL)
                .withDescription("URL to open browser to. (http://$host:$port)\n")
                .hasArg().withArgName("url")
                .create("url"));
        
        options.addOption(OptionBuilder
                .withLongOpt("pid-file")
                .withDescription("Process ID file.")
                .hasArg().withArgName(Keys.PIDFILE)
                .create(Keys.PIDFILE));
        
        options.addOption(OptionBuilder
                .withLongOpt(Keys.TIMEOUT)
                .withDescription("Startup timout for background process. (50)\n")
                .hasArg().withArgName("seconds").withType(Number.class)
                .create("t"));
        
        options.addOption(OptionBuilder
                .withLongOpt("log-level")
                .withDescription("log level [DEBUG|INFO|WARN|ERROR] (WARN)")
                .hasArg().withArgName("level")
                .create("level"));
        
        options.addOption(OptionBuilder
                .withLongOpt("debug-enable")
                .withDescription("set log level to debug")
                .hasArg().withArgName("true|false").withType(Boolean.class)
                .create(Keys.DEBUG));
        
        options.addOption(OptionBuilder
                .withLongOpt(Keys.PROCESSNAME)
                .withDescription("Process name where applicable")
                .hasArg().withArgName(Keys.NAME)
                .create("procname"));
        
        options.addOption(OptionBuilder
                .withLongOpt("tray-enable")
                .withDescription("Enable/Disable system tray integration (true)")
                .hasArg().withArgName("true|false").withType(Boolean.class)
                .create(Keys.TRAY));
        
        options.addOption(OptionBuilder
                .withLongOpt("dock-enable")
                .withDescription("Enable/Disable dock icon for Mac OS X Users (true)")
                .hasArg().withArgName("true|false").withType(Boolean.class)
                .create(Keys.DOCK));
        
        options.addOption(OptionBuilder
                .withLongOpt("default-shell")
                .withDescription("Set the default Shell for arbitrary actions from tray menu")
                .hasArg().withArgName(Keys.SHELL)
                .create(Keys.SHELL));
        
        options.addOption(OptionBuilder
                .withLongOpt("tray-icon")
                .withDescription("tray icon and OS X dock icon png image")
                .hasArg().withArgName("path")
                .create(Keys.ICON));
        
        options.addOption(OptionBuilder
                .withLongOpt("tray-config")
                .withDescription("tray menu config path")
                .hasArg().withArgName("path")
                .create(Keys.TRAYCONFIG));
        
        options.addOption(OptionBuilder
                .withLongOpt("predicate-file")
                .withDescription("predicates definitions path")
                .hasArg().withArgName("path")
                .create(Keys.PREDICATEFILE));
        
        options.addOption(OptionBuilder
                .withLongOpt("status-file")
                .withDescription("status file (started/stopped) path")
                .hasArg().withArgName("path")
                .create(Keys.STATUSFILE));
        
        options.addOption(OptionBuilder
                .withLongOpt("web-xml-path")
                .withDescription("full path to default web.xml file for configuring the server")
                .hasArg().withArgName("path")
                .create(Keys.WEBXMLPATH));
        
        options.addOption(OptionBuilder
                .withLongOpt("cfengine-name")
                .withDescription("name of cfml engine, defaults to lucee")
                .hasArg().withArgName(Keys.NAME)
                .create(Keys.CFENGINE));
        
        options.addOption(OptionBuilder
                .withLongOpt("cfml-web-config")
                .withDescription("full path to cfml web context config directory")
                .hasArg().withArgName("path")
                .create(Keys.CFWEBCONF));
        
        options.addOption(OptionBuilder
                .withLongOpt("cfml-server-config")
                .withDescription("full path to cfml server context config directory")
                .hasArg().withArgName("path")
                .create(Keys.CFSERVERCONF));
        
        options.addOption(OptionBuilder.withArgName("property=value")
                .withLongOpt("sysprop")
                .hasArgs(2)
                .withValueSeparator()
                .withDescription("system property to set")
                .create("D"));
        
        options.addOption(OptionBuilder
                .withLongOpt("welcome-files")
                .withDescription("comma delinated list of welcome files used if no web.xml file exists")
                .hasArg().withArgName("index.cfm,default.cfm,...")
                .create(Keys.WELCOMEFILES));
        
        options.addOption(OptionBuilder
                .withLongOpt("directory-index")
                .withDescription("enable directory browsing")
                .hasArg().withArgName("true|false").withType(Boolean.class)
                .create(Keys.DIRECTORYINDEX));
        
        options.addOption(OptionBuilder
                .withLongOpt("cache-enable")
                .withDescription("enable static asset cache")
                .hasArg().withArgName("true|false").withType(Boolean.class)
                .create(Keys.CACHE));
        
        options.addOption(OptionBuilder
                .withLongOpt("custom-httpstatus-enable")
                .withDescription("enable custom HTTP status code messages")
                .hasArg().withArgName("true|false").withType(Boolean.class)
                .create(Keys.CUSTOMSTATUS));
        
        options.addOption(OptionBuilder
                .withLongOpt("transfer-min-size")
                .withDescription("Minimun transfer file size to offload to OS. (100)\n")
                .hasArg().withArgName(Keys.TRANSFERMINSIZE).withType(Long.class)
                .create(Keys.TRANSFERMINSIZE));
        
        options.addOption(OptionBuilder
                .withLongOpt("sendfile-enable")
                .withDescription("enable sendfile")
                .hasArg().withArgName("true|false").withType(Boolean.class)
                .create(Keys.SENDFILE));
        
        options.addOption(OptionBuilder
                .withLongOpt("gzip-enable")
                .withDescription("enable gzip")
                .hasArg().withArgName("true|false").withType(Boolean.class)
                .create(Keys.GZIP));
        
        options.addOption(OptionBuilder
                .withLongOpt("gzip-predicate")
                .withDescription("GZIP predicate")
                .hasArg().create(Keys.GZIP_PREDICATE));

        options.addOption(OptionBuilder
                .withLongOpt("mariadb4j-enable")
                .withDescription("enable MariaDB4j")
                .hasArg().withArgName("true|false").withType(Boolean.class)
                .create(Keys.MARIADB4J));
        
        options.addOption(OptionBuilder
                .withLongOpt("mariadb4j-port")
                .withDescription("enable MariaDB4j")
                .hasArg().withArgName(Keys.PORT).withType(Number.class)
                .create(Keys.MARIADB4JPORT));
        
        options.addOption(OptionBuilder
                .withLongOpt("mariadb4j-basedir")
                .withDescription("base directory.  (temp/mariadb4j)")
                .hasArg().withArgName("path/to/base/dir")
                .create(Keys.MARIADB4JBASEDIR));
        
        options.addOption(OptionBuilder
                .withLongOpt("mariadb4j-datadir")
                .withDescription("data directory.  (temp/mariadb4j/data)")
                .hasArg().withArgName("path/to/data/dir")
                .create(Keys.MARIADB4JDATADIR));
        
        options.addOption(OptionBuilder
                .withLongOpt("mariadb4j-import")
                .withDescription("SQL file to import.")
                .hasArg().withArgName("path/to/sql/file")
                .create(Keys.MARIADB4JIMPORT));
        
        options.addOption(OptionBuilder
                .withLongOpt("jvm-args")
                .withDescription("JVM arguments for background process.")
                .hasArg().withArgName("option=value,option=value")
                .create(Keys.JVMARGS));
        
        options.addOption(OptionBuilder
                .withLongOpt("error-pages")
                .withDescription("List of error codes and locations, no code or '1' will set the default")
                .hasArg().withArgName("404=/location,500=/location")
                .create(Keys.ERRORPAGES));
        
        options.addOption(OptionBuilder
                .withLongOpt("servlet-rest-enable")
                .withDescription("Enable an embedded CFML server REST servlet")
                .hasArg().withArgName("true|false").withType(Boolean.class)
                .create(Keys.SERVLETREST));
        
        options.addOption(OptionBuilder
                .withLongOpt("servlet-rest-mappings")
                .withDescription("Embedded CFML server REST servlet URL mapping paths, comma separated [/rest/*]")
                .hasArg().withArgName("/rest/*,/api/*")
                .create(Keys.SERVLETRESTMAPPINGS));
        
        options.addOption(OptionBuilder
                .withLongOpt("filter-pathinfo-enable")
                .withDescription("Enable (*.cf[c|m])(/.*) handling, setting cgi.PATH_INFO to $2")
                .hasArg().withArgName("true|false").withType(Boolean.class)
                .create(Keys.FILTERPATHINFO));
        
        options.addOption(OptionBuilder
                .withLongOpt("ssl-add-certs")
                .withDescription("Comma-separated list of additional SSL certificates to add to the store.")
                .hasArg().withArgName("/path/to/cert,/path/to/cert")
                .create(Keys.SSLADDCERTS));
        
        options.addOption(OptionBuilder
                .withLongOpt("basicauth-enable")
                .withDescription("Enable Basic Auth")
                .hasArg().withArgName("true|false").withType(Boolean.class)
                .create(Keys.BASICAUTHENABLE));
        
        options.addOption(OptionBuilder
                .withLongOpt("basicauth-users")
                .withDescription("List of users and passwords, comma separated and equals separated.")
                .hasArg().withArgName("bob=secret,alice=12345")
                .create("users"));
        
        options.addOption(OptionBuilder
                .withLongOpt("buffer-size")
                .withDescription("buffer size")
                .hasArg().withArgName("size").withType(Number.class)
                .create(Keys.BUFFERSIZE));
        
        options.addOption(OptionBuilder
                .withLongOpt("io-threads")
                .withDescription("number of IO threads")
                .hasArg().withArgName("size").withType(Number.class)
                .create(Keys.IOTHREADS));
        
        options.addOption(OptionBuilder
                .withLongOpt("worker-threads")
                .withDescription("number of worker threads")
                .hasArg().withArgName("size").withType(Number.class)
                .create(Keys.WORKERTHREADS));
        
        options.addOption(OptionBuilder
                .withLongOpt("direct-buffers")
                .withDescription("Enable direct buffers")
                .hasArg().withArgName("true|false").withType(Boolean.class)
                .create(Keys.DIRECTBUFFERS));
        options.addOption(OptionBuilder
                .withLongOpt("load-balance")
                .withDescription("Comma-separated list of servers to start and load balance.")
                .hasArg().withArgName("http://localhost:8081,http://localhost:8082")
                .create(Keys.LOADBALANCE));
        
        options.addOption(OptionBuilder
                .withLongOpt("directory-refresh")
                .withDescription("Refresh the direcotry list with each request. *DEV ONLY* not thread-safe")
                .hasArg().withArgName("true|false").withType(Boolean.class)
                .create(Keys.DIRECTORYREFRESH));
        
        options.addOption(OptionBuilder
                .withLongOpt("proxy-peeraddress")
                .withDescription("Enable peer address proxy headers")
                .hasArg().withArgName("true|false").withType(Boolean.class)
                .create(Keys.PROXYPEERADDRESS));
        
        options.addOption(OptionBuilder
                .withLongOpt("http2-enable")
                .withDescription("Enable HTTP2")
                .hasArg().withArgName("true|false").withType(Boolean.class)
                .create(Keys.HTTP2));
        
        options.addOption(OptionBuilder
                .withLongOpt("secure-cookies")
                .withDescription("Set httpOnly and secure cookie flags")
                .hasArg().withArgName("true|false").withType(Boolean.class)
                .create(Keys.SECURECOOKIES));
        
        options.addOption(OptionBuilder
                .withLongOpt("cookie-httponly")
                .withDescription("Set cookie 'http-only' header")
                .hasArg().withArgName("true|false").withType(Boolean.class)
                .create(Keys.COOKIEHTTPONLY));
        
        options.addOption(OptionBuilder
                .withLongOpt("cookie-secure")
                .withDescription("Set cookie 'secure' header")
                .hasArg().withArgName("true|false").withType(Boolean.class)
                .create(Keys.COOKIESECURE));
        
        options.addOption(OptionBuilder
                .withLongOpt("webinf-path")
                .withDescription("Set WEB-INF path")
                .hasArg().withArgName("path/to/WEB-INF")
                .create(Keys.WEBINF));
        
        options.addOption(OptionBuilder
                .withLongOpt("ssl-eccdisable")
                .withDescription("Disable EC SSL algorithms")
                .hasArg().withArgName("true|false").withType(Boolean.class)
                .create(Keys.SSLECCDISABLE));
        
        options.addOption(OptionBuilder
                .withLongOpt("ssl-selfsign")
                .withDescription("Generate a self-signed certificate, use -sslcert and -sslkey parameters to specify key paths")
                .hasArg().withArgName("true|false").withType(Boolean.class)
                .create(Keys.SSLSELFSIGN));
        
        options.addOption(OptionBuilder
                .withLongOpt("service")
                .withDescription("Generate and run a service configuration")
                .hasArg().withArgName("true|false").withType(Boolean.class)
                .create(Keys.SERVICE));
        
        options.addOption(OptionBuilder
                .withLongOpt("xnio-options")
                .withDescription("List of XNIO options")
                .hasArg().withArgName("WORKER_IO_THREADS=16,TCP_NODELAY=false")
                .create(Keys.XNIOOPTIONS));
        
        options.addOption(OptionBuilder
                .withLongOpt("undertow-options")
                .withDescription("List of Undertow options")
                .hasArg().withArgName("MAX_PARAMETERS=12,MAX_HEADERS=200")
                .create(Keys.UNDERTOWOPTIONS));
        
        options.addOption(OptionBuilder
                .withLongOpt("preferred-browser")
                .withDescription("Browser to be used when a URL is Opened")
                .hasArg().withArgName("firefox, chrome, opera, konqueror, epiphany, mozilla, netscape")
                .create(Keys.BROWSER));
        
        options.addOption(OptionBuilder
                .withLongOpt("default-servlet-allowed-ext")
                .withDescription("Additional allowed extensions to add to the default list.")
                .hasArg().withArgName("log,foo,bar")
                .create(Keys.DEFAULTSERVLETALLOWEDEXT));
        
        options.addOption(OptionBuilder
                .withLongOpt("case-sensitive-web-server")
                .withDescription("Experimental- force case sensitive or insensitive checks on web server")
                .hasArg().withArgName("true|false")
                .create(Keys.CASESENSITIVEWEBSERVER));
        
        options.addOption(OptionBuilder
                .withLongOpt("resource-manager-logging")
                .withDescription("Enable low level file system logging in resource manager")
                .hasArg().withArgName("true|false")
                .create(Keys.RESOURCEMANAGERLOGGING));
        
        options.addOption(OptionBuilder
                .withLongOpt("cache-servlet-paths")
                .withDescription("Enable file system caching in resource manager of servlet.getRealPath() calls")
                .hasArg().withArgName("true|false")
                .create(Keys.CACHESERVLETPATHS));
        
        options.addOption(new Option("h", Keys.HELP, false, "print this message"));
        options.addOption(new Option("v", "version", false, "print runwar version and undertow version"));
        
        return options;
    }
    
    public static ServerOptions parseArguments(String[] args) {
        ServerOptions serverOptions = new ServerOptionsImpl();
        serverOptions = parseArguments(args, serverOptions);
        return serverOptions;
    }
    
    public static ServerOptions parseLogArguments(String[] args) {
        ServerOptions serverOptions = new ServerOptionsImpl();
        serverOptions = parseLogArguments(args, serverOptions);
        return serverOptions;
    }
    
    public static ServerOptions parseLogArguments(String[] args, ServerOptions serverOptions) {
        parser = new PosixParser();
        
        try {
            CommandLine line = parser.parse(getOptions(), args);
            
            if (line.hasOption(Keys.DEBUG)) {
                boolean debug = Boolean.valueOf(line.getOptionValue(Keys.DEBUG));
                serverOptions.debug(debug);
                if (debug) {
                    serverOptions.logLevel(Keys.DEBUG);
                }
            }
            
            if (hasOptionValue(line, "level")) {
                serverOptions.logLevel(line.getOptionValue("level"));
            }
            
            if (line.hasOption(Keys.WAR)) {
                String warPath = line.getOptionValue(Keys.WAR);
                serverOptions.warFile(getFile(warPath));
            }
            if (hasOptionValue(line, Keys.LOGBASENAME)) {
                serverOptions.logFileName(line.getOptionValue(Keys.LOGBASENAME));
            }
            
            if (hasOptionValue(line, Keys.LOGDIR)) {
                serverOptions.logDir(line.getOptionValue(Keys.LOGDIR));
            } else {
                serverOptions.logDir();
            }
            return serverOptions;
        } catch (Exception exp) {
            exp.printStackTrace();
        }
        return null;
    }
    
    @SuppressWarnings("static-access")
    public static ServerOptions parseArguments(String[] args, ServerOptions serverOptions) {
        serverOptions.commandLineArgs(args);
        parser = new PosixParser();
        try {
            CommandLine line = parser.parse(getOptions(), args);
            // parse the command line arguments
            if (line.hasOption(Keys.HELP)) {
                printUsage("Options", 0);
            }
            if (line.hasOption("version")) {
                Server.printVersion();
                System.exit(0);
            }
            if (line.hasOption("c")) {
                String config = line.getOptionValue("c");
                CONF_LOG.debug("Loading config from file: " + getFile(config));
                serverOptions = new ConfigParser(getFile(config)).getServerOptions();
            }
            
            if (hasOptionValue(line, Keys.NAME)) {
                serverOptions.serverName(line.getOptionValue(Keys.NAME));
            }
            
            if (line.hasOption(Keys.DEBUG)) {
                Boolean debug = Boolean.valueOf(line.getOptionValue(Keys.DEBUG));
                serverOptions.debug(debug);
                if (debug) {
                    serverOptions.logLevel(Keys.DEBUG);
                    CONF_LOG.debug("Enabling debug mode");
                }
            }
            
            if (hasOptionValue(line, "level")) {
                serverOptions.logLevel(line.getOptionValue("level"));
            }
            
            if (line.hasOption(Keys.BACKGROUND)) {
                serverOptions.background(Boolean.valueOf(line.getOptionValue(Keys.BACKGROUND)));
            }
            if (hasOptionValue(line, Keys.LIBDIRS)) {
                String[] list = line.getOptionValue(Keys.LIBDIRS).split(",");
                for (String path : list) {
                    File lib = new File(path);
                    if (!lib.exists() || !lib.isDirectory()) {
                        printUsage("No such lib directory " + path, 1);
                    }
                }
                serverOptions.libDirs(line.getOptionValue(Keys.LIBDIRS));
            }
            if (hasOptionValue(line, Keys.WELCOMEFILES)) {
                serverOptions.welcomeFiles(line.getOptionValue(Keys.WELCOMEFILES).split(","));
            }
            if (hasOptionValue(line, Keys.JAR)) {
                File jar = new File(line.getOptionValue(Keys.JAR));
                if (!jar.exists() || jar.isDirectory()) {
                    printUsage("No such jar " + jar, 1);
                }
                serverOptions.jarURL(jar.toURI().toURL());
            }
            
            if (hasOptionValue(line, Keys.TIMEOUT)) {
                serverOptions.launchTimeout(((Number) line.getParsedOptionValue(Keys.TIMEOUT)).intValue() * 1000);
            }
            if (line.hasOption(Keys.PASSWORD)) {
                serverOptions.stopPassword(line.getOptionValue(Keys.PASSWORD).toCharArray());
            }
            if (line.hasOption(Keys.STOPSOCKET)) {
                serverOptions.stopPort(((Number) line.getParsedOptionValue(Keys.STOPSOCKET)).intValue());
            }
            if (hasOptionValue(line, Keys.WAR)) {
                String warPath = line.getOptionValue(Keys.WAR);
                serverOptions.warFile(getFile(warPath));
            } else if (!line.hasOption(Keys.STOP) && !line.hasOption("c") && !line.hasOption(Keys.LOADBALANCE) && !line.hasOption(Keys.SSLSELFSIGN)) {
                printUsage("Must specify -war path/to/war, or -stop [-stop-socket]", 1);
            }
            if (line.hasOption("D")) {
                final String[] properties = line.getOptionValues("D");
                for (int i = 0; i < properties.length; i++) {
                    CONF_LOG.debugf("setting system property: %s", properties[i].toString() + '=' + properties[i + 1].toString());
                    System.setProperty(properties[i].toString(), properties[i + 1].toString());
                    i++;
                }
            }
            
            if (hasOptionValue(line, Keys.WEBXMLPATH)) {
                String webXmlPath = line.getOptionValue(Keys.WEBXMLPATH);
                File webXmlFile = new File(webXmlPath);
                if (webXmlFile.exists()) {
                    serverOptions.webXmlFile(webXmlFile);
                } else {
                    throw new RuntimeException("Could not find web.xml! " + webXmlPath);
                }
            }
            
            if (line.hasOption(Keys.STOP)) {
                serverOptions.action(Keys.STOP);
                String[] values = line.getOptionValues(Keys.STOP);
                if (values != null && values.length > 0) {
                    serverOptions.stopPort(Integer.parseInt(values[0]));
                }
                if (values != null && values.length >= 1) {
                    serverOptions.stopPassword(values[1].toCharArray());
                }
            } else {
                serverOptions.action("start");
            }
            
            if (hasOptionValue(line, Keys.CONTEXT)) {
                serverOptions.contextPath(line.getOptionValue(Keys.CONTEXT));
            }
            if (hasOptionValue(line, Keys.HOST)) {
                serverOptions.host(line.getOptionValue(Keys.HOST));
            }
            if (hasOptionValue(line, "p")) {
                serverOptions.httpPort(((Number) line.getParsedOptionValue("p")).intValue());
            }
            if (hasOptionValue(line, Keys.AJPENABLE)) {
                serverOptions.ajpEnable(Boolean.valueOf(line.getOptionValue(Keys.AJPENABLE)));
            }
            if (hasOptionValue(line, Keys.AJPPORT)) {
                // disable http if no http port is specified
                serverOptions.httpEnable(hasOptionValue(line, Keys.PORT))
                        .ajpEnable(true).ajpPort(((Number) line.getParsedOptionValue(Keys.AJPPORT)).intValue());
            }
            if (hasOptionValue(line, Keys.SSLPORT)) {
                if (!hasOptionValue(line, Keys.HTTPENABLE)) {
                    CONF_LOG.trace("SSL enable and http not explicitly enable; disabling http");
                    serverOptions.httpEnable(false);
                }
                if (!hasOptionValue(line, Keys.SECURECOOKIES)) {
                    CONF_LOG.trace("SSL enable and secure cookies explicitly disabled; enabling secure cookies");
                    serverOptions.secureCookies(true);
                }
                serverOptions.sslEnable(true).sslPort(((Number) line.getParsedOptionValue(Keys.SSLPORT)).intValue());
            }
            if (hasOptionValue(line, Keys.SSLSELFSIGN)) {
                serverOptions.sslSelfSign(Boolean.valueOf(line.getOptionValue(Keys.SSLSELFSIGN)));
            }
            if (hasOptionValue(line, Keys.SSLCERT)) {
                File certFile = serverOptions.sslSelfSign() ? new File(line.getOptionValue(Keys.SSLCERT)) : getFile(line.getOptionValue(Keys.SSLCERT));
                serverOptions.sslCertificate(certFile);
                if (!hasOptionValue(line, Keys.SSLKEY) || !hasOptionValue(line, Keys.SSLKEY)) {
                    throw new RuntimeException("Using a SSL certificate requires -sslkey /path/to/file and -sslkeypass pass**** arguments!");
                }
                if (!hasOptionValue(line, Keys.SSLENABLE)) {
                    CONF_LOG.trace("SSL not enable and cert specified; enabling SSL");
                    serverOptions.sslEnable(true);
                }
                
            }
            if (hasOptionValue(line, Keys.SSLKEY)) {
                File keyFile = serverOptions.sslSelfSign() ? new File(line.getOptionValue(Keys.SSLKEY)) : getFile(line.getOptionValue(Keys.SSLKEY));
                serverOptions.sslKey(keyFile);
                if (!hasOptionValue(line, Keys.SSLENABLE)) {
                    CONF_LOG.trace("https not enable and key specified; enabling SSL");
                    serverOptions.sslEnable(true);
                }
            }
            if (hasOptionValue(line, Keys.SSLKEYPASS)) {
                serverOptions.sslKeyPass(line.getOptionValue(Keys.SSLKEYPASS).toCharArray());
            }
            if (hasOptionValue(line, Keys.SSLENABLE)) {
                if (!hasOptionValue(line, Keys.HTTPENABLE)) {
                    serverOptions.httpEnable(false);
                }
                if (!hasOptionValue(line, Keys.SECURECOOKIES)) {
                    serverOptions.secureCookies(true);
                }
                serverOptions.sslEnable(Boolean.valueOf(line.getOptionValue(Keys.SSLENABLE)));
            }
            if (line.hasOption(Keys.HTTPENABLE)) {
                serverOptions.httpEnable(Boolean.valueOf(line.getOptionValue(Keys.HTTPENABLE)));
            }
            if (hasOptionValue(line, Keys.URLREWRITEFILE)) {
                serverOptions.urlRewriteFile(getFile(line.getOptionValue(Keys.URLREWRITEFILE)));
                if (!line.hasOption(Keys.URLREWRITEENABLE)) {
                    serverOptions.urlRewriteEnable(true);
                }
            }
            if (hasOptionValue(line, Keys.URLREWRITELOG)) {
                serverOptions.urlRewriteLog(new File(line.getOptionValue(Keys.URLREWRITELOG)));
                if (!line.hasOption(Keys.URLREWRITEENABLE)) {
                    serverOptions.urlRewriteEnable(true);
                }
            }
            if (line.hasOption(Keys.URLREWRITEENABLE)) {
                serverOptions.urlRewriteEnable(Boolean.valueOf(line.getOptionValue(Keys.URLREWRITEENABLE)));
            }
            if (hasOptionValue(line, Keys.URLREWRITECHECK)) {
                serverOptions.urlRewriteCheckInterval(line.getOptionValue(Keys.URLREWRITECHECK));
            }
            if (hasOptionValue(line, Keys.URLREWRITESTATUSPATH)) {
                serverOptions.urlRewriteStatusPath(line.getOptionValue(Keys.URLREWRITESTATUSPATH));
            }
            if (hasOptionValue(line, Keys.LOGDIR)) {
                serverOptions.logDir(line.getOptionValue(Keys.LOGDIR));
            }
            if (hasOptionValue(line, Keys.LOGBASENAME)) {
                serverOptions.logFileName(line.getOptionValue(Keys.LOGBASENAME));
            }
            if (hasOptionValue(line, Keys.DIRS)) {
                serverOptions.contentDirs(line.getOptionValue(Keys.DIRS));
            }
            if (hasOptionValue(line, Keys.LOGREQUESTSBASENAME)) {
                serverOptions.logRequestsEnable(true);
                serverOptions.logRequestsBaseFileName(line.getOptionValue(Keys.LOGREQUESTSBASENAME));
            }
            if (hasOptionValue(line, Keys.LOGREQUESTSDIR)) {
                serverOptions.logRequestsEnable(true);
                serverOptions.logRequestsDir(getFile(line.getOptionValue(Keys.LOGREQUESTSDIR)));
            }
            if (line.hasOption(Keys.LOGREQUESTS)) {
                serverOptions.logRequestsEnable(Boolean.valueOf(line.getOptionValue(Keys.LOGREQUESTS)));
            }
            if (hasOptionValue(line, Keys.LOGACCESSBASENAME)) {
                serverOptions.logAccessEnable(true);
                serverOptions.logAccessBaseFileName(line.getOptionValue(Keys.LOGACCESSBASENAME));
            }
            if (hasOptionValue(line, Keys.LOGACCESSDIR)) {
                serverOptions.logAccessEnable(true);
                serverOptions.logAccessDir(getFile(line.getOptionValue(Keys.LOGACCESSDIR)));
            }
            if (hasOptionValue(line, Keys.LOGACCESS)) {
                serverOptions.logAccessEnable(Boolean.valueOf(line.getOptionValue(Keys.LOGACCESS)));
            }

            if (hasOptionValue(line, Keys.OPENBROWSER)) {
                serverOptions.openbrowser(Boolean.valueOf(line.getOptionValue("open")));
            }
            
            if (hasOptionValue(line, Keys.DEFAULTSERVLETALLOWEDEXT)) {
                serverOptions.defaultServletAllowedExt(line.getOptionValue(Keys.DEFAULTSERVLETALLOWEDEXT));
            }
            
            if (hasOptionValue(line, Keys.CASESENSITIVEWEBSERVER)) {
                serverOptions.caseSensitiveWebServer(Boolean.valueOf(line.getOptionValue(Keys.CASESENSITIVEWEBSERVER)));
            }
            
            if (hasOptionValue(line, Keys.RESOURCEMANAGERLOGGING)) {
                serverOptions.resourceManagerLogging(Boolean.valueOf(line.getOptionValue(Keys.RESOURCEMANAGERLOGGING)));
            }
            
            if (hasOptionValue(line, Keys.CACHESERVLETPATHS)) {
                serverOptions.cacheServletPaths(Boolean.valueOf(line.getOptionValue(Keys.CACHESERVLETPATHS)));
            }
            
            if (line.hasOption(Keys.OPENURL)) {
                serverOptions.openbrowserURL(line.getOptionValue(Keys.OPENURL));
                if (!line.hasOption(Keys.OPENBROWSER)) {
                    serverOptions.openbrowser(true);
                }
            }
            
            if (hasOptionValue(line, Keys.PIDFILE)) {
                serverOptions.pidFile(line.getOptionValue(Keys.PIDFILE));
            }
            
            if (hasOptionValue(line, Keys.PROCESSNAME)) {
                serverOptions.processName(line.getOptionValue(Keys.PROCESSNAME));
            }
            
            if (hasOptionValue(line, Keys.TRAY)) {
                serverOptions.trayEnable(Boolean.valueOf(line.getOptionValue(Keys.TRAY)));
            }
            if (hasOptionValue(line, Keys.DOCK)) {
                serverOptions.dockEnable(Boolean.valueOf(line.getOptionValue(Keys.DOCK)));
            }
            if (hasOptionValue(line, Keys.ICON)) {
                serverOptions.iconImage(line.getOptionValue(Keys.ICON));
            }
            if (hasOptionValue(line, Keys.TRAYCONFIG)) {
                serverOptions.trayConfig(getFile(line.getOptionValue(Keys.TRAYCONFIG)));
            }
            if (hasOptionValue(line, Keys.PREDICATEFILE)) {
                serverOptions.predicateFile(getFile(line.getOptionValue(Keys.PREDICATEFILE)));
            }
            
            if (hasOptionValue(line, Keys.STATUSFILE)) {
                serverOptions.statusFile(getFile(line.getOptionValue(Keys.STATUSFILE)));
            }
            
            if (hasOptionValue(line, Keys.CFENGINE)) {
                serverOptions.cfEngineName(line.getOptionValue(Keys.CFENGINE));
            }
            if (hasOptionValue(line, Keys.CFSERVERCONF)) {
                serverOptions.cfmlServletConfigServerDir(line.getOptionValue(Keys.CFSERVERCONF));
            }
            if (hasOptionValue(line, Keys.CFWEBCONF)) {
                serverOptions.cfmlServletConfigWebDir(line.getOptionValue(Keys.CFWEBCONF));
            }
            if (hasOptionValue(line, Keys.DIRECTORYINDEX)) {
                serverOptions.directoryListingEnable(Boolean.valueOf(line.getOptionValue(Keys.DIRECTORYINDEX)));
            }
            if (hasOptionValue(line, Keys.CACHE)) {
                serverOptions.cacheEnable(Boolean.valueOf(line.getOptionValue(Keys.CACHE)));
            }
            if (hasOptionValue(line, Keys.CUSTOMSTATUS)) {
                serverOptions.customHTTPStatusEnable(Boolean.valueOf(line.getOptionValue(Keys.CUSTOMSTATUS)));
            }
            if (hasOptionValue(line, Keys.TRANSFERMINSIZE)) {
                serverOptions.transferMinSize(Long.valueOf(line.getOptionValue(Keys.TRANSFERMINSIZE)));
            }
            if (hasOptionValue(line, Keys.SENDFILE)) {
                serverOptions.sendfileEnable(Boolean.valueOf(line.getOptionValue(Keys.SENDFILE)));
            }
            if (hasOptionValue(line, Keys.GZIP)) {
                serverOptions.gzipEnable(Boolean.valueOf(line.getOptionValue(Keys.GZIP)));
            }
            if (hasOptionValue(line, Keys.GZIP_PREDICATE)) {
                serverOptions.gzipPredicate(line.getOptionValue(Keys.GZIP_PREDICATE));
            }
            if (hasOptionValue(line, Keys.MARIADB4J)) {
                serverOptions.mariaDB4jEnable(Boolean.valueOf(line.getOptionValue(Keys.MARIADB4J)));
            }
            if (hasOptionValue(line, Keys.MARIADB4JPORT)) {
                serverOptions.mariaDB4jPort(Integer.valueOf(line.getOptionValue(Keys.MARIADB4JPORT)));
            }
            if (hasOptionValue(line, Keys.MARIADB4JBASEDIR)) {
                serverOptions.mariaDB4jBaseDir(new File(line.getOptionValue(Keys.MARIADB4JBASEDIR)));
            }
            if (hasOptionValue(line, Keys.MARIADB4JDATADIR)) {
                serverOptions.mariaDB4jDataDir(new File(line.getOptionValue(Keys.MARIADB4JDATADIR)));
            }
            if (hasOptionValue(line, Keys.MARIADB4JIMPORT)) {
                serverOptions.mariaDB4jImportSQLFile(new File(line.getOptionValue(Keys.MARIADB4JIMPORT)));
            }
            if (hasOptionValue(line, Keys.JVMARGS)) {
                List<String> jvmArgs = new ArrayList<String>();
                // A \\ is an escaped backslash and a \; is an escaped semicolon
                String[] jvmArgArray = line.getOptionValue(Keys.JVMARGS).replaceAll("\\\\\\\\", "__backSlash__" ).replaceAll("\\\\;", "__semicolon__" ).split(";");
                for (String arg : jvmArgArray) {
                    jvmArgs.add(arg.replaceAll("__semicolon__", ";").replaceAll("__backSlash__", "\\\\"));
                }
                serverOptions.jvmArgs(jvmArgs);
            }
            if (hasOptionValue(line, Keys.ERRORPAGES)) {
                serverOptions.errorPages(line.getOptionValue(Keys.ERRORPAGES));
            }
            if (hasOptionValue(line, Keys.SERVLETREST)) {
                serverOptions.servletRestEnable(Boolean.valueOf(line.getOptionValue(Keys.SERVLETREST)));
            }
            if (hasOptionValue(line, Keys.SERVLETRESTMAPPINGS)) {
                serverOptions.servletRestMappings(line.getOptionValue(Keys.SERVLETRESTMAPPINGS));
                if (!hasOptionValue(line, Keys.SERVLETREST)) {
                    serverOptions.servletRestEnable(true);
                }
            }
            if (hasOptionValue(line, Keys.FILTERPATHINFO)) {
                serverOptions.filterPathInfoEnable(Boolean.valueOf(line.getOptionValue(Keys.FILTERPATHINFO)));
            }
            if (hasOptionValue(line, Keys.SSLADDCERTS)) {
                serverOptions.sslAddCerts(line.getOptionValue(Keys.SSLADDCERTS));
            }
            if (hasOptionValue(line, Keys.BASICAUTHENABLE)) {
                serverOptions.basicAuthEnable(Boolean.valueOf(line.getOptionValue(Keys.BASICAUTHENABLE)));
            }
            if (hasOptionValue(line, "users")) {
                if (!hasOptionValue(line, Keys.BASICAUTHENABLE) || line.hasOption(Keys.BASICAUTHENABLE) && Boolean.valueOf(line.getOptionValue(Keys.BASICAUTHENABLE))) {
                    serverOptions.basicAuthEnable(true);
                }
                serverOptions.basicAuth(line.getOptionValue("users"));
            }
            if (hasOptionValue(line, Keys.BUFFERSIZE)) {
                serverOptions.bufferSize(Integer.valueOf(line.getOptionValue(Keys.BUFFERSIZE)));
            }
            if (hasOptionValue(line, Keys.IOTHREADS)) {
                serverOptions.ioThreads(Integer.valueOf(line.getOptionValue(Keys.IOTHREADS)));
            }
            if (hasOptionValue(line, Keys.WORKERTHREADS)) {
                serverOptions.workerThreads(Integer.valueOf(line.getOptionValue(Keys.WORKERTHREADS)));
            }
            if (line.hasOption(Keys.DIRECTBUFFERS)) {
                serverOptions.directBuffers(Boolean.valueOf(line.getOptionValue(Keys.DIRECTBUFFERS)));
            }
            if (hasOptionValue(line, Keys.LOADBALANCE)) {
                serverOptions.loadBalance(line.getOptionValue(Keys.LOADBALANCE));
            }
            if (hasOptionValue(line, Keys.DIRECTORYREFRESH)) {
                serverOptions.directoryListingRefreshEnable(Boolean.valueOf(line.getOptionValue(Keys.DIRECTORYREFRESH)));
            }
            if (hasOptionValue(line, Keys.PROXYPEERADDRESS)) {
                serverOptions.proxyPeerAddressEnable(Boolean.valueOf(line.getOptionValue(Keys.PROXYPEERADDRESS)));
            }
            if (hasOptionValue(line, Keys.HTTP2)) {
                serverOptions.http2Enable(Boolean.valueOf(line.getOptionValue(Keys.HTTP2)));
            }
            
            if (hasOptionValue(line, Keys.SECURECOOKIES)) {
                serverOptions.secureCookies(Boolean.valueOf(line.getOptionValue(Keys.SECURECOOKIES)));
            }
            
            if (hasOptionValue(line, Keys.COOKIEHTTPONLY)) {
                serverOptions.cookieHttpOnly(Boolean.valueOf(line.getOptionValue(Keys.COOKIEHTTPONLY)));
            }
            
            if (hasOptionValue(line, Keys.COOKIESECURE)) {
                serverOptions.cookieSecure(Boolean.valueOf(line.getOptionValue(Keys.COOKIESECURE)));
            }
            
            if (hasOptionValue(line, Keys.SSLECCDISABLE)) {
                serverOptions.sslEccDisable(Boolean.valueOf(line.getOptionValue(Keys.SSLECCDISABLE)));
            }
            
            if (hasOptionValue(line, Keys.WEBINF)) {
                String webInfPath = line.getOptionValue(Keys.WEBINF);
                File webinfDir = new File(webInfPath);
                if (webinfDir.exists()) {
                    serverOptions.webInfDir(webinfDir);
                } else {
                    throw new RuntimeException("Could not find WEB-INF! " + webInfPath);
                }
            }
            
            if (hasOptionValue(line, Keys.SERVICE)) {
                serverOptions.service(Boolean.valueOf(line.getOptionValue(Keys.SERVICE)));
            }
            
            if (hasOptionValue(line, Keys.XNIOOPTIONS)) {
                serverOptions.xnioOptions(line.getOptionValue(Keys.XNIOOPTIONS));
            }
            
            if (hasOptionValue(line, Keys.UNDERTOWOPTIONS)) {
                serverOptions.undertowOptions(line.getOptionValue(Keys.UNDERTOWOPTIONS));
            }
            
            if (hasOptionValue(line, Keys.BROWSER)) {
                serverOptions.browser(line.getOptionValue(Keys.BROWSER));
            }
            
            if (serverOptions.logLevel().equals(Keys.TRACE)) {
                for (Option arg : line.getOptions()) {
                    CONF_LOG.debug(arg.toString());
//                    CONF_LOG.debug(arg.getValue());
                }
            }
            return serverOptions;
        } catch (Exception exp) {
            exp.printStackTrace();
            String msg = exp.getMessage();
            if (msg == null) {
                msg = "null : " + exp.getStackTrace()[0].toString();
                if (exp.getStackTrace().length > 0) {
                    msg += '\n' + exp.getStackTrace()[1].toString();
                }
            } else {
                msg = exp.getClass().getName() + " " + msg;
            }
            printUsage(msg, 1);
        }
        return null;
    }
    
    private static boolean hasOptionValue(CommandLine line, String key) {
        if (line.hasOption(key) && line.getOptionValue(key).length() > 0) {
            return true;
        }
        return false;
    }
    
    static File getFile(String path) {
        File file = new File(path);
        if (!file.exists() || file == null) {
            throw new RuntimeException("File not found: " + path + " (" + file.getAbsolutePath() + ")");
        }
        return file;
    }
    
    static void printUsage(String message, int exitCode) {
        PrintWriter pw = new PrintWriter(System.out);
        HelpFormatter formatter = new HelpFormatter();
        if (exitCode == 0) {
            pw.println("USAGE   " + SYNTAX);
            pw.println(HEADER + '\n');
            pw.println(message);
            @SuppressWarnings("unchecked")
            List<Option> optList = new ArrayList<Option>(getOptions().getOptions());
            Collections.sort(optList, new Comparator<Option>() {
                public int compare(Option o1, Option o2) {
                    if (o1.getOpt().equals(Keys.WAR)) {
                        return -1;
                    } else if (o2.getOpt().equals(Keys.WAR)) {
                        return 1;
                    }
                    if (o1.getOpt().equals("p")) {
                        return -1;
                    } else if (o2.getOpt().equals("p")) {
                        return 1;
                    }
                    if (o1.getOpt().equals("c")) {
                        return -1;
                    } else if (o2.getOpt().equals("c")) {
                        return 1;
                    }
                    if (o1.getOpt().equals(Keys.CONTEXT)) {
                        return -1;
                    } else if (o2.getOpt().equals(Keys.CONTEXT)) {
                        return 1;
                    }
                    if (o1.getOpt().equals("d")) {
                        return -1;
                    } else if (o2.getOpt().equals("d")) {
                        return 1;
                    }
                    if (o1.getOpt().equals("b")) {
                        return -1;
                    } else if (o2.getOpt().equals("b")) {
                        return 1;
                    }
                    if (o1.getOpt().equals("h")) {
                        return 1;
                    } else if (o2.getOpt().equals("h")) {
                        return -1;
                    }
                    if (o1.getOpt().equals("url")) {
                        return 1;
                    } else if (o2.getOpt().equals("url")) {
                        return -1;
                    }
                    if (o1.getOpt().equals("open")) {
                        return 1;
                    } else if (o2.getOpt().equals("open")) {
                        return -1;
                    }
                    if (o1.getOpt().equals(Keys.STOPSOCKET)) {
                        return 1;
                    } else if (o2.getOpt().equals(Keys.STOPSOCKET)) {
                        return -1;
                    }
                    if (o1.getOpt().equals(Keys.STOP)) {
                        return 1;
                    } else if (o2.getOpt().equals(Keys.STOP)) {
                        return -1;
                    }
                    return o1.getOpt().compareTo(o2.getOpt());
                }
            });
            for (java.util.Iterator<Option> it = optList.iterator(); it.hasNext();) {
                Option option = it.next();
                StringBuffer optBuf = new StringBuffer();
                if (option.getOpt() == null) {
                    optBuf.append("  ").append("   ").append("--").append(option.getLongOpt());
                } else {
                    optBuf.append("  ").append("-").append(option.getOpt());
                    if (option.hasLongOpt()) {
                        optBuf.append(',').append("--").append(option.getLongOpt());
                    }
                }
                if (option.hasArg()) {
                    String argName = option.getArgName();
                    if (argName != null && argName.length() == 0) {
                        // if the option has a blank argname
                        optBuf.append(' ');
                    } else {
                        optBuf.append(option.hasLongOpt() ? " " : " ");
                        optBuf.append("<").append(argName != null ? option.getArgName() : "arg").append(">");
                    }
                }
                pw.println(optBuf.toString());
                formatter.printWrapped(pw, 78, 4, "    " + option.getDescription());
                pw.println();
            }
            pw.println(FOOTER);
            pw.flush();
        } else {
            System.out.println("USAGE:  " + SYNTAX + '\n' + message);
        }
        System.exit(exitCode);
    }
    
}
