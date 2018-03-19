package runwar.options;


import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;

//import static java.io.File.*;
//import static java.util.Arrays.*;
//
//import joptsimple.OptionParser;
//import joptsimple.OptionSet;

import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.PosixParser;
import runwar.logging.Logger;

import runwar.Server;
import runwar.logging.LoggerFactory;

import static runwar.options.ServerOptions.Keys;

public class CommandLineHandler {
    private static PosixParser parser;
    private static final String SYNTAX = " java -jar runwar.jar [-war] path/to/war [options]";
    private static final String HEADER = " The runwar lib wraps undertow with more awwsome. Defaults (parenthetical)";
    private static final String FOOTER = " source: https://github.com/cfmlprojects/runwar.git";
    
    public CommandLineHandler(){
    }

    @SuppressWarnings("static-access")
    private static Options getOptions() {
        final Options options = new Options();
        options.addOption( OptionBuilder
                .withLongOpt( Keys.CONFIG )
                .withDescription( "config file" )
                .hasArg().withArgName("file")
                .create("c") );
        
        options.addOption( OptionBuilder
                .withDescription( "path to war" )
                .hasArg()
                .withArgName("path")
                .create(Keys.WAR) );
        
        options.addOption( OptionBuilder
                .withLongOpt( "server-name" )
                .withDescription( "server name (default)" )
                .hasArg()
                .withArgName(Keys.NAME)
                .create(Keys.NAME) );
        
        options.addOption( OptionBuilder
                .withLongOpt( "context-path" )
                .withDescription( "context path.  (/)" )
                .hasArg().withArgName(Keys.CONTEXT)
                .create(Keys.CONTEXT) );
        
        options.addOption( OptionBuilder
                .withDescription( "host.  (127.0.0.1)" )
                .hasArg().withArgName(Keys.HOST)
                .create(Keys.HOST) );
        
        options.addOption( OptionBuilder
                .withLongOpt( Keys.PORT )
                .withDescription( "port number.  (8088)" )
                .hasArg().withArgName("http port").withType(Number.class)
                .create('p') );
        
        options.addOption( OptionBuilder
                .withLongOpt( "stop-port" )
                .withDescription( "stop listener port number. (8779)\n" )
                .hasArg().withArgName(Keys.PORT).withType(Number.class)
                .create(Keys.STOPSOCKET) );
        
        options.addOption( OptionBuilder
                .withLongOpt( "stop-password" )
                .withDescription( "Pasword checked when stopping server\n" )
                .hasArg().withArgName(Keys.PASSWORD)
                .create(Keys.PASSWORD) );
        
        options.addOption( OptionBuilder
                .withDescription( "stop backgrounded.  Optional stop-port" )
                .hasOptionalArg().withArgName(Keys.PORT)
                .hasOptionalArg().withArgName(Keys.PASSWORD)
                .withValueSeparator(' ')
                .create(Keys.STOP) );
        
        options.addOption( OptionBuilder
                .withLongOpt( "http-enable" )
                .withDescription( "Enable HTTP.  Default is true ,unless SSL or AJP are enabled." )
                .hasArg().withArgName("true|false").withType(Boolean.class)
                .create(Keys.HTTPENABLE) );
        
        options.addOption( OptionBuilder
                .withLongOpt( "ajp-enable" )
                .withDescription( "Enable AJP.  Default is false.  When enabled, http is disabled by default." )
                .hasArg().withArgName("true|false").withType(Boolean.class)
                .create(Keys.AJPENABLE) );
        
        options.addOption( OptionBuilder
                .withLongOpt( "urlrewrite-enable" )
                .withDescription( "Enable URL Rewriting.  Default is true." )
                .hasArg().withArgName("true|false").withType(Boolean.class)
                .create(Keys.URLREWRITEENABLE) );
        
        options.addOption( OptionBuilder
                .withLongOpt( "urlrewrite-file" )
                .withDescription( "URL rewriting config file." )
                .hasArg().withArgName("path/to/urlrewrite/file")
                .create(Keys.URLREWRITEFILE) );
        
        options.addOption( OptionBuilder
                .withLongOpt( "urlrewrite-check" )
                .withDescription( "URL rewriting config file realod check interval, 0 for every request. (disabled)" )
                .hasArg().withArgName("interval")
                .create(Keys.URLREWRITECHECK) );
        
        options.addOption( OptionBuilder
                .withLongOpt( "urlrewrite-statuspath" )
                .withDescription( "URL rewriting status path. (disabled)" )
                .hasArg().withArgName("path")
                .create(Keys.URLREWRITESTATUSPATH) );
        
        options.addOption( OptionBuilder
                .withLongOpt( "ssl-enable" )
                .withDescription( "Enable SSL.  Default is false.  When enabled, http is disabled by default." )
                .hasArg().withArgName("true|false").withType(Boolean.class)
                .create("sslenable") );
        
        options.addOption( OptionBuilder
                .withLongOpt( "ssl-port" )
                .withDescription( "SSL port.  Disabled if not set." )
                .hasArg().withArgName(Keys.PORT).withType(Number.class)
                .create(Keys.SSLPORT) );
        
        options.addOption( OptionBuilder
                .withLongOpt( "ssl-cert" )
                .withDescription( "SSL certificate file in x509 (PKS#12) format." )
                .hasArg().withArgName("certificate")
                .create(Keys.SSLCERT) );
        
        options.addOption( OptionBuilder
                .withLongOpt( "ssl-key" )
                .withDescription( "SSL private key file in DER (PKS#8) format." )
                .hasArg().withArgName("key")
                .create(Keys.SSLKEY) );
        
        options.addOption( OptionBuilder
                .withLongOpt( "ssl-keypass" )
                .withDescription( "SSL key passphrase." )
                .hasArg().withArgName("passphrase")
                .create(Keys.SSLKEYPASS) );
        
        options.addOption( OptionBuilder
                .withLongOpt( "ajp-port" )
                .withDescription( "AJP port.  Disabled if not set." )
                .hasArg().withArgName("ajp port").withType(Number.class)
                .create(Keys.AJPPORT) );
        
        options.addOption( OptionBuilder
                .withLongOpt( "log-dir" )
                .withDescription( "Log directory.  (WEB-INF/logs)" )
                .hasArg().withArgName("path/to/log/dir")
                .create(Keys.LOGDIR) );
        
        options.addOption( OptionBuilder
                .withLongOpt( "log-basename" )
                .withDescription( "Log file base name/prefix [default:server]" )
                .hasArg().withArgName("basename")
                .create(Keys.LOGBASENAME) );
        
        options.addOption( OptionBuilder
                .withLongOpt( "logrequests-dir" )
                .withDescription( "Log requests directory" )
                .hasArg().withArgName("/path/to/dir")
                .create(Keys.LOGREQUESTSDIR) );
        
        options.addOption( OptionBuilder
                .withLongOpt( "logrequests-basename" )
                .withDescription( "Requests log file base name/prefix  [default:request]" )
                .hasArg().withArgName("basename")
                .create(Keys.LOGREQUESTSBASENAME) );
        
        options.addOption( OptionBuilder
                .withLongOpt( "logrequests-enable" )
                .withDescription( "Enables or disable request logging [default:false]" )
                .hasArg().withArgName("true|false").withType(Boolean.class)
                .create(Keys.LOGREQUESTS) );
        
        options.addOption( OptionBuilder
                .withLongOpt( "logaccess-dir" )
                .withDescription( "Log access directory" )
                .hasArg().withArgName("/path/to/dir")
                .create(Keys.LOGACCESSDIR) );
        
        options.addOption( OptionBuilder
                .withLongOpt( "logaccess-basename" )
                .withDescription( "Access log file base name/prefix [default:access]" )
                .hasArg().withArgName("basename")
                .create(Keys.LOGACCESSBASENAME) );
        
        options.addOption( OptionBuilder
                .withLongOpt( "logaccess-enable" )
                .withDescription( "Enables or disable access logging [default:false]" )
                .hasArg().withArgName("true|false").withType(Boolean.class)
                .create(Keys.LOGACCESS) );
        
        options.addOption( OptionBuilder
                .withLongOpt( Keys.DIRS )
                .withDescription( "List of external directories to serve from" )
                .hasArg().withArgName("path,path,... or alias=path,..")
                .create("d") );
        
        options.addOption( OptionBuilder
                .withLongOpt( "lib-dirs" )
                .withDescription( "List of directories to add contents of to classloader" )
                .hasArg().withArgName("path,path,...")
                .create(Keys.LIBDIRS) );
        
        options.addOption( OptionBuilder
                .withLongOpt( Keys.JAR )
                .withDescription( "jar to be added to classpath" )
                .hasArg().withArgName("path")
                .create("j") );
        
        options.addOption( OptionBuilder
                .withLongOpt( Keys.BACKGROUND )
                .withDescription( "Run in background (true)" )
                .hasArg().withArgName("true|false").withType(Boolean.class)
                .create('b') );
        
        options.addOption( OptionBuilder
                .withLongOpt( Keys.OPENBROWSER )
                .withDescription( "Open default web browser after start (false)" )
                .hasArg().withArgName("true|false")
                .create("open") );
        
        options.addOption( OptionBuilder
                .withLongOpt( Keys.OPENURL )
                .withDescription( "URL to open browser to. (http://$host:$port)\n" )
                .hasArg().withArgName("url")
                .create("url") );
        
        options.addOption( OptionBuilder
                .withLongOpt( "pid-file" )
                .withDescription( "Process ID file." )
                .hasArg().withArgName(Keys.PIDFILE)
                .create(Keys.PIDFILE) );
        
        options.addOption( OptionBuilder
                .withLongOpt( Keys.TIMEOUT )
                .withDescription( "Startup timout for background process. (50)\n" )
                .hasArg().withArgName("seconds").withType(Number.class)
                .create("t") );
        
        options.addOption( OptionBuilder
                .withLongOpt( "log-level" )
                .withDescription( "log level [DEBUG|INFO|WARN|ERROR] (WARN)" )
                .hasArg().withArgName("level")
                .create("level") );
        
        options.addOption( OptionBuilder
                .withLongOpt( "debug-enable" )
                .withDescription( "set log level to debug" )
                .hasArg().withArgName("true|false").withType(Boolean.class)
                .create(Keys.DEBUG) );
        
        options.addOption( OptionBuilder
                .withLongOpt( Keys.PROCESSNAME )
                .withDescription( "Process name where applicable" )
                .hasArg().withArgName(Keys.NAME)
                .create("procname") );
        
        options.addOption( OptionBuilder
                .withLongOpt( "tray-enable" )
                .withDescription( "Enable/Disable system tray integration (true)" )
                .hasArg().withArgName("true|false").withType(Boolean.class)
                .create(Keys.TRAY) );
        
        options.addOption( OptionBuilder
                .withLongOpt( "tray-icon" )
                .withDescription( "tray icon and OS X dock icon png image" )
                .hasArg().withArgName("path")
                .create(Keys.ICON) );
        
        options.addOption( OptionBuilder
                .withLongOpt( "tray-config" )
                .withDescription( "tray menu config path" )
                .hasArg().withArgName("path")
                .create(Keys.TRAYCONFIG) );
        
        options.addOption( OptionBuilder
                .withLongOpt( "status-file" )
                .withDescription( "status file (started/stopped) path" )
                .hasArg().withArgName("path")
                .create(Keys.STATUSFILE) );
        
        options.addOption( OptionBuilder
                .withLongOpt( "web-xml-path" )
                .withDescription( "full path to default web.xml file for configuring the server" )
                .hasArg().withArgName("path")
                .create(Keys.WEBXMLPATH) );
        
        options.addOption( OptionBuilder
                .withLongOpt( "cfengine-name" )
                .withDescription( "name of cfml engine, defaults to lucee" )
                .hasArg().withArgName(Keys.NAME)
                .create(Keys.CFENGINE) );
        
        options.addOption( OptionBuilder
                .withLongOpt( "cfml-web-config" )
                .withDescription( "full path to cfml web context config directory" )
                .hasArg().withArgName("path")
                .create(Keys.CFWEBCONF) );
        
        options.addOption( OptionBuilder
                .withLongOpt( "cfml-server-config" )
                .withDescription( "full path to cfml server context config directory" )
                .hasArg().withArgName("path")
                .create(Keys.CFSERVERCONF) );
        
        options.addOption( OptionBuilder.withArgName( "property=value" )
                .withLongOpt( "sysprop" )
                .hasArgs(2)
                .withValueSeparator()
                .withDescription( "system property to set" )
                .create("D") );
        
        options.addOption( OptionBuilder
                .withLongOpt( "welcome-files" )
                .withDescription( "comma delinated list of welcome files used if no web.xml file exists" )
                .hasArg().withArgName("index.cfm,default.cfm,...")
                .create(Keys.WELCOMEFILES) );
        
        options.addOption( OptionBuilder
                .withLongOpt( "directory-index" )
                .withDescription( "enable directory browsing" )
                .hasArg().withArgName("true|false").withType(Boolean.class)
                .create(Keys.DIRECTORYINDEX) );
        
        options.addOption( OptionBuilder
                .withLongOpt( "cache-enable" )
                .withDescription( "enable static asset cache" )
                .hasArg().withArgName("true|false").withType(Boolean.class)
                .create(Keys.CACHE) );
        
        options.addOption( OptionBuilder
                .withLongOpt( "custom-httpstatus-enable" )
                .withDescription( "enable custom HTTP status code messages" )
                .hasArg().withArgName("true|false").withType(Boolean.class)
                .create(Keys.CUSTOMSTATUS) );
        
        options.addOption( OptionBuilder
                .withLongOpt( "transfer-min-size" )
                .withDescription( "Minimun transfer file size to offload to OS. (100)\n" )
                .hasArg().withArgName(Keys.TRANSFERMINSIZE).withType(Long.class)
                .create(Keys.TRANSFERMINSIZE) );
        
        options.addOption( OptionBuilder
                .withLongOpt( "sendfile-enable" )
                .withDescription( "enable sendfile" )
                .hasArg().withArgName("true|false").withType(Boolean.class)
                .create(Keys.SENDFILE) );
        
        options.addOption( OptionBuilder
                .withLongOpt( "gzip-enable" )
                .withDescription( "enable gzip" )
                .hasArg().withArgName("true|false").withType(Boolean.class)
                .create(Keys.GZIP) );
        
        options.addOption( OptionBuilder
                .withLongOpt( "mariadb4j-enable" )
                .withDescription( "enable MariaDB4j" )
                .hasArg().withArgName("true|false").withType(Boolean.class)
                .create(Keys.MARIADB4J) );
        
        options.addOption( OptionBuilder
                .withLongOpt( "mariadb4j-port" )
                .withDescription( "enable MariaDB4j" )
                .hasArg().withArgName(Keys.PORT).withType(Number.class)
                .create(Keys.MARIADB4JPORT) );
        
        options.addOption( OptionBuilder
                .withLongOpt( "mariadb4j-basedir" )
                .withDescription( "base directory.  (temp/mariadb4j)" )
                .hasArg().withArgName("path/to/base/dir")
                .create(Keys.MARIADB4JBASEDIR) );
        
        options.addOption( OptionBuilder
                .withLongOpt( "mariadb4j-datadir" )
                .withDescription( "data directory.  (temp/mariadb4j/data)" )
                .hasArg().withArgName("path/to/data/dir")
                .create(Keys.MARIADB4JDATADIR) );
        
        options.addOption( OptionBuilder
                .withLongOpt( "mariadb4j-import" )
                .withDescription( "SQL file to import." )
                .hasArg().withArgName("path/to/sql/file")
                .create(Keys.MARIADB4JIMPORT) );
        
        options.addOption( OptionBuilder
                .withLongOpt( "jvm-args" )
                .withDescription( "JVM arguments for background process." )
                .hasArg().withArgName("option=value,option=value")
                .create(Keys.JVMARGS) );
        
        options.addOption( OptionBuilder
                .withLongOpt( "error-pages" )
                .withDescription( "List of error codes and locations, no code or '1' will set the default" )
                .hasArg().withArgName("404=/location,500=/location")
                .create(Keys.ERRORPAGES) );
        
        options.addOption( OptionBuilder
                .withLongOpt( "servlet-rest-enable" )
                .withDescription( "Enable an embedded CFML server REST servlet" )
                .hasArg().withArgName("true|false").withType(Boolean.class)
                .create(Keys.SERVLETREST) );
        
        options.addOption( OptionBuilder
                .withLongOpt( "servlet-rest-mappings" )
                .withDescription( "Embedded CFML server REST servlet URL mapping paths, comma separated [/rest/*]" )
                .hasArg().withArgName("/rest/*,/api/*")
                .create(Keys.SERVLETRESTMAPPINGS) );
        
        options.addOption( OptionBuilder
                .withLongOpt( "filter-pathinfo-enable" )
                .withDescription( "Enable (*.cf[c|m])(/.*) handling, setting cgi.PATH_INFO to $2" )
                .hasArg().withArgName("true|false").withType(Boolean.class)
                .create(Keys.FILTERPATHINFO) );
        
        options.addOption( OptionBuilder
                .withLongOpt( "ssl-add-certs" )
                .withDescription( "Comma-separated list of additional SSL certificates to add to the store." )
                .hasArg().withArgName("/path/to/cert,/path/to/cert")
                .create(Keys.SSLADDCERTS) );
        
        options.addOption( OptionBuilder
                .withLongOpt( "basicauth-enable" )
                .withDescription( "Enable Basic Auth" )
                .hasArg().withArgName("true|false").withType(Boolean.class)
                .create(Keys.BASICAUTHENABLE) );
        
        options.addOption( OptionBuilder
                .withLongOpt( "basicauth-users" )
                .withDescription( "List of users and passwords, comma separated and equals separated." )
                .hasArg().withArgName("bob=secret,alice=12345")
                .create("users") );
        
        options.addOption( OptionBuilder
                .withLongOpt( "buffer-size" )
                .withDescription( "buffer size" )
                .hasArg().withArgName("size").withType(Number.class)
                .create(Keys.BUFFERSIZE) );
        
        options.addOption( OptionBuilder
                .withLongOpt( "io-threads" )
                .withDescription( "number of IO threads" )
                .hasArg().withArgName("size").withType(Number.class)
                .create(Keys.IOTHREADS) );
        
        options.addOption( OptionBuilder
                .withLongOpt( "worker-threads" )
                .withDescription( "number of worker threads" )
                .hasArg().withArgName("size").withType(Number.class)
                .create(Keys.WORKERTHREADS) );
        
        options.addOption( OptionBuilder
                .withLongOpt( "direct-buffers" )
                .withDescription( "Enable direct buffers" )
                .hasArg().withArgName("true|false").withType(Boolean.class)
                .create(Keys.DIRECTBUFFERS) );
        options.addOption( OptionBuilder
                .withLongOpt( "load-balance" )
                .withDescription( "Comma-separated list of servers to start and load balance." )
                .hasArg().withArgName("http://localhost:8081,http://localhost:8082")
                .create(Keys.LOADBALANCE) );
        
        options.addOption( OptionBuilder
                .withLongOpt( "directory-refresh" )
                .withDescription( "Refresh the direcotry list with each request. *DEV ONLY* not thread-safe" )
                .hasArg().withArgName("true|false").withType(Boolean.class)
                .create(Keys.DIRECTORYREFRESH) );
        
        options.addOption( OptionBuilder
                .withLongOpt( "proxy-peeraddress" )
                .withDescription( "Enable peer address proxy headers" )
                .hasArg().withArgName("true|false").withType(Boolean.class)
                .create(Keys.PROXYPEERADDRESS) );
        
        options.addOption( OptionBuilder
                .withLongOpt( "http2-enable" )
                .withDescription( "Enable HTTP2" )
                .hasArg().withArgName("true|false").withType(Boolean.class)
                .create(Keys.HTTP2) );
        
        options.addOption( OptionBuilder
                .withLongOpt( "secure-cookies" )
                .withDescription( "Set httpOnly and secure cookie flags" )
                .hasArg().withArgName("true|false").withType(Boolean.class)
                .create(Keys.SECURECOOKIES) );
        
        options.addOption( new Option( "h", Keys.HELP, false, "print this message" ) );
        options.addOption( new Option( "v", "version", false, "print runwar version and undertow version" ) );
        
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
            CommandLine line = parser.parse( getOptions(), args );
            // parse the command line arguments
            if (line.hasOption("c")) {
                String config = line.getOptionValue("c");
                serverOptions = new ConfigParser(getFile(config)).getServerOptions();
            }

            if (line.hasOption(Keys.DEBUG)) {
                Boolean debug= Boolean.valueOf(line.getOptionValue(Keys.DEBUG));
                serverOptions.setDebug(debug);
                if(debug) {
                    serverOptions.setLoglevel(Keys.DEBUG);
                }
            }

            if (line.hasOption("level") && line.getOptionValue("level").length() > 0) {
                serverOptions.setLoglevel(line.getOptionValue("level"));
            }
            
            if (line.hasOption(Keys.WAR)) {
                String warPath = line.getOptionValue(Keys.WAR);
                serverOptions.setWarFile(getFile(warPath));
            } 
            if (line.hasOption(Keys.LOGBASENAME)) {
                serverOptions.setLogFileName(line.getOptionValue(Keys.LOGBASENAME));
            }

            if (line.hasOption(Keys.LOGDIR)) {
                serverOptions.setLogDir(line.getOptionValue(Keys.LOGDIR));
            }
            return serverOptions;
        }
        catch( Exception exp ) {
            exp.printStackTrace();
        }
        return null;
    }

    @SuppressWarnings("static-access")
    public static ServerOptions parseArguments(String[] args, ServerOptions serverOptions) {
//        parser = new OptionParser();
//        parser.acceptsAll(asList("c",ServerOptions.CONFIG)).withRequiredArg()
//        .describedAs( "config file" )
//        .ofType( File.class );
        Logger log = LoggerFactory.getLogger(CommandLineHandler.class);
        serverOptions.setCommandLineArgs(args);
        parser = new PosixParser();

        /*
        String json = "";
        Object[] opts2 = options.getOptions().toArray();
        Option[] opts = new Option[opts2.length];
        for (int i = 0; i < opts2.length; i++) {
            opts[i] = (Option) opts2[i];
        }
        
        Arrays.sort(opts, new Comparator<Option>() {
            public int compare(Option o1, Option o2) {
                String name = o2.getLongOpt() != null ? o2.getLongOpt() : "";
                String name2 = o1.getLongOpt() != null ? o1.getLongOpt() : "";
                return name2.compareTo(name);
            }
        });
        for (int i = 0; i < opts.length; i++) {
            Option op = (Option) opts[i];
            String argName, name, description, type, alias = "";
            name = op.getLongOpt() != null ? op.getLongOpt() : "";
            description = op.getDescription() != null ? op.getDescription().trim() : "";
            type = op.getType() != null ? op.getType().toString() : "";
            alias = op.getOpt() != null ? op.getOpt() : "";
            argName = op.getArgName() != null ? op.getArgName() : "";
            json += String.format("\"%s\": { \"description\": \"%s\", \"type\": \"%s\", \"alias\": \"%s\", \"arg\":\"%s\" },\n",name,description,type,alias,argName);
        }
        System.out.println("{" + json + "}");
         */
        try {
            CommandLine line = parser.parse( getOptions(), args );
            // parse the command line arguments
            if (line.hasOption(Keys.HELP)) {
                printUsage("Options",0);
            }
            if (line.hasOption("version")) {
                Server.printVersion();
                System.exit(0);
            }
            if (line.hasOption("c")) {
                String config = line.getOptionValue("c");
                log.debug("Loading config from file: " + getFile(config));
                serverOptions = new ConfigParser(getFile(config)).getServerOptions();
            }
            
            if (line.hasOption(Keys.NAME)) {
                serverOptions.setServerName(line.getOptionValue(Keys.NAME));
            }
            
            if (line.hasOption(Keys.DEBUG)) {
                Boolean debug= Boolean.valueOf(line.getOptionValue(Keys.DEBUG));
                serverOptions.setDebug(debug);
                if(debug) {
                    serverOptions.setLoglevel(Keys.DEBUG);
                    log.debug("Enabling debug mode");
                }
            }
            
            if (line.hasOption("level") && line.getOptionValue("level").length() > 0) {
                serverOptions.setLoglevel(line.getOptionValue("level"));
            }
            
            if (line.hasOption(Keys.BACKGROUND)) {
                serverOptions.setBackground(Boolean.valueOf(line.getOptionValue(Keys.BACKGROUND)));
            }
            if (line.hasOption(Keys.LIBDIRS) && line.getOptionValue(Keys.LIBDIRS).length() > 0) {
                String[] list = line.getOptionValue(Keys.LIBDIRS).split(",");
                for (String path : list) {
                    File lib = new File(path);
                    if (!lib.exists() || !lib.isDirectory())
                        printUsage("No such lib directory "+path,1);
                }               
                serverOptions.setLibDirs(line.getOptionValue(Keys.LIBDIRS));
            }
            if (line.hasOption(Keys.WELCOMEFILES) && line.getOptionValue(Keys.WELCOMEFILES).length() > 0) {
                serverOptions.setWelcomeFiles(line.getOptionValue(Keys.WELCOMEFILES).split(","));
            }
            if (line.hasOption(Keys.JAR)) {
                File jar = new File(line.getOptionValue(Keys.JAR));
                if (!jar.exists() || jar.isDirectory())
                    printUsage("No such jar "+jar,1);
                serverOptions.setJarURL(jar.toURI().toURL());
            }
            
            if (line.hasOption(Keys.TIMEOUT)) {
                serverOptions.setLaunchTimeout(((Number)line.getParsedOptionValue(Keys.TIMEOUT)).intValue() * 1000);
            }
            if (line.hasOption(Keys.PASSWORD)) {
                serverOptions.setStopPassword(line.getOptionValue(Keys.PASSWORD).toCharArray());
            }
            if (line.hasOption(Keys.STOPSOCKET)) {
                serverOptions.setSocketNumber(((Number)line.getParsedOptionValue(Keys.STOPSOCKET)).intValue());
            }
            if (line.hasOption(Keys.WAR)) {
                String warPath = line.getOptionValue(Keys.WAR);
                serverOptions.setWarFile(getFile(warPath));
            } else if (!line.hasOption(Keys.STOP) && !line.hasOption("c") && !line.hasOption(Keys.LOADBALANCE)) {
                printUsage("Must specify -war path/to/war, or -stop [-stop-socket]",1);
            } 
            if(line.hasOption("D")){
                final String[] properties = line.getOptionValues("D");
                for (int i = 0; i < properties.length; i++) {
                    log.debug("setting system property: %s", properties[i].toString()+'='+properties[i+1].toString());
                    System.setProperty(properties[i].toString(),properties[i+1].toString());
                    i++;
                }
            }
            
            if (line.hasOption(Keys.WEBXMLPATH)) {
                String webXmlPath = line.getOptionValue(Keys.WEBXMLPATH);
                File webXmlFile = new File(webXmlPath);
                if(webXmlFile.exists()) {
                    serverOptions.setWebXmlFile(webXmlFile);
                } else {
                    throw new RuntimeException("Could not find web.xml! " + webXmlPath);
                }
            }
            
            if (line.hasOption(Keys.STOP)) {
                serverOptions.setAction(Keys.STOP);
                String[] values = line.getOptionValues(Keys.STOP);
                if(values != null && values.length > 0) {
                    serverOptions.setSocketNumber(Integer.parseInt(values[0])); 
                }
                if(values != null && values.length >= 1) {
                    serverOptions.setStopPassword(values[1].toCharArray()); 
                }
            } else {
                serverOptions.setAction("start");
            }
            
            if (line.hasOption(Keys.CONTEXT)) {
                serverOptions.setContextPath(line.getOptionValue(Keys.CONTEXT));
            }
            if (line.hasOption(Keys.HOST)) {
                serverOptions.setHost(line.getOptionValue(Keys.HOST));
            }
            if (line.hasOption("p")) {
                serverOptions.setPortNumber(((Number)line.getParsedOptionValue("p")).intValue());
            }
            if (line.hasOption(Keys.AJPENABLE)) {
                serverOptions.setEnableAJP(Boolean.valueOf(line.getOptionValue(Keys.AJPENABLE)));
            }
            if (line.hasOption(Keys.AJPPORT)) {
                // disable http if no http port is specified
                serverOptions.setEnableHTTP(line.hasOption(Keys.PORT))
                .setEnableAJP(true).setAJPPort(((Number)line.getParsedOptionValue(Keys.AJPPORT)).intValue());
            }
            if (line.hasOption(Keys.SSLPORT)) {
                if(!line.hasOption(Keys.HTTPENABLE)) {
                    serverOptions.setEnableHTTP(false);
                }
                if(!line.hasOption(Keys.SECURECOOKIES)) {
                    serverOptions.setSecureCookies(true);
                }
                serverOptions.setEnableSSL(true).setSSLPort(((Number)line.getParsedOptionValue(Keys.SSLPORT)).intValue());
            }
            if (line.hasOption(Keys.SSLCERT)) {
                serverOptions.setSSLCertificate(getFile(line.getOptionValue(Keys.SSLCERT)));
                if (!line.hasOption(Keys.SSLKEY) || !line.hasOption(Keys.SSLKEY)) {
                    throw new RuntimeException("Using a SSL certificate requires -sslkey /path/to/file and -sslkeypass pass**** arguments!");  	
                }
            }
            if (line.hasOption(Keys.SSLKEY)) {
                serverOptions.setSSLKey(getFile(line.getOptionValue(Keys.SSLKEY)));
            }
            if (line.hasOption(Keys.SSLKEYPASS)) {
                serverOptions.setSSLKeyPass(line.getOptionValue(Keys.SSLKEYPASS).toCharArray());
            }
            if (line.hasOption(Keys.SSLENABLE)) {
                if(!line.hasOption(Keys.HTTPENABLE)) {
                    serverOptions.setEnableHTTP(false);
                }
                if(!line.hasOption(Keys.SECURECOOKIES)) {
                    serverOptions.setSecureCookies(true);
                }
                serverOptions.setEnableSSL(Boolean.valueOf(line.getOptionValue(Keys.SSLENABLE)));
            }
            if (line.hasOption(Keys.HTTPENABLE)) {
                serverOptions.setEnableHTTP(Boolean.valueOf(line.getOptionValue(Keys.HTTPENABLE)));
            }
            if (line.hasOption(Keys.URLREWRITEFILE)) {
                serverOptions.setURLRewriteFile(getFile(line.getOptionValue(Keys.URLREWRITEFILE)));
                if(!line.hasOption(Keys.URLREWRITEENABLE)) {
                    serverOptions.setEnableURLRewrite(true);
                }
            }
            if (line.hasOption(Keys.URLREWRITEENABLE)) {
                serverOptions.setEnableURLRewrite(Boolean.valueOf(line.getOptionValue(Keys.URLREWRITEENABLE)));
            }
            if (line.hasOption(Keys.URLREWRITECHECK) && line.getOptionValue(Keys.URLREWRITECHECK).length() > 0) {
                serverOptions.setURLRewriteCheckInterval(line.getOptionValue(Keys.URLREWRITECHECK));
            }
            if (line.hasOption(Keys.URLREWRITESTATUSPATH) && line.getOptionValue(Keys.URLREWRITESTATUSPATH).length() > 0) {
                serverOptions.setURLRewriteStatusPath(line.getOptionValue(Keys.URLREWRITESTATUSPATH));
            }
            if (line.hasOption(Keys.LOGDIR)) {
                serverOptions.setLogDir(line.getOptionValue(Keys.LOGDIR));
            }
            if (line.hasOption(Keys.LOGBASENAME)) {
                serverOptions.setLogFileName(line.getOptionValue(Keys.LOGBASENAME));
            }
            if (line.hasOption(Keys.DIRS)) {
                serverOptions.setCfmlDirs(line.getOptionValue(Keys.DIRS));
            }
            if (line.hasOption(Keys.LOGREQUESTSBASENAME)) {
                serverOptions.logRequestsEnable(true);
                serverOptions.setLogRequestsBaseFileName(line.getOptionValue(Keys.LOGREQUESTSBASENAME));
            }
            if (line.hasOption(Keys.LOGREQUESTSDIR)) {
                serverOptions.logRequestsEnable(true);
                serverOptions.setLogRequestsDir(getFile(line.getOptionValue(Keys.LOGREQUESTSDIR)));
            }
            if (line.hasOption(Keys.LOGREQUESTS)) {
                serverOptions.logRequestsEnable(Boolean.valueOf(line.getOptionValue(Keys.LOGREQUESTS)));
            }
            if (line.hasOption(Keys.LOGACCESSBASENAME)) {
                serverOptions.logAccessEnable(true);
                serverOptions.setLogAccessBaseFileName(line.getOptionValue(Keys.LOGACCESSBASENAME));
            }
            if (line.hasOption(Keys.LOGACCESSDIR)) {
                serverOptions.logAccessEnable(true);
                serverOptions.setLogAccessDir(getFile(line.getOptionValue(Keys.LOGACCESSDIR)));
            }
            if (line.hasOption(Keys.LOGACCESS)) {
                serverOptions.logAccessEnable(Boolean.valueOf(line.getOptionValue(Keys.LOGACCESS)));
            }
            
            if (line.hasOption(Keys.OPENBROWSER)) {
                serverOptions.setOpenbrowser(Boolean.valueOf(line.getOptionValue("open")));
            }
            if (line.hasOption(Keys.OPENURL)) {
                serverOptions.setOpenbrowserURL(line.getOptionValue(Keys.OPENURL));
                if(!line.hasOption(Keys.OPENBROWSER))
                    serverOptions.setOpenbrowser(true);
            }
            
            if (line.hasOption(Keys.PIDFILE)) {
                serverOptions.setPidFile(line.getOptionValue(Keys.PIDFILE));
            }
            
            if (line.hasOption(Keys.PROCESSNAME)) {
                serverOptions.setProcessName(line.getOptionValue(Keys.PROCESSNAME));
            }
            
            if (line.hasOption(Keys.TRAY)) {
                serverOptions.setTrayEnabled(Boolean.valueOf(line.getOptionValue(Keys.TRAY)));
            }
            if (line.hasOption(Keys.ICON)) {
                serverOptions.setIconImage(line.getOptionValue(Keys.ICON));
            }
            if (line.hasOption(Keys.TRAYCONFIG) && line.getOptionValue(Keys.TRAYCONFIG).length() > 0) {
                serverOptions.setTrayConfig(getFile(line.getOptionValue(Keys.TRAYCONFIG)));
            }
            
            if (line.hasOption(Keys.STATUSFILE) && line.getOptionValue(Keys.STATUSFILE).length() > 0) {
                serverOptions.setStatusFile(getFile(line.getOptionValue(Keys.STATUSFILE)));
            }
            
            if (line.hasOption(Keys.CFENGINE)) {
                serverOptions.setCFEngineName(line.getOptionValue(Keys.CFENGINE));
            }
            if (line.hasOption(Keys.CFSERVERCONF)) {
                serverOptions.setCFMLServletConfigServerDir(line.getOptionValue(Keys.CFSERVERCONF));
            }
            if (line.hasOption(Keys.CFWEBCONF)) {
                serverOptions.setCFMLServletConfigWebDir(line.getOptionValue(Keys.CFWEBCONF));
            }
            if (line.hasOption(Keys.DIRECTORYINDEX)) {
                serverOptions.setDirectoryListingEnabled(Boolean.valueOf(line.getOptionValue(Keys.DIRECTORYINDEX)));
            }
            if (line.hasOption(Keys.CACHE)) {
                serverOptions.setCacheEnabled(Boolean.valueOf(line.getOptionValue(Keys.CACHE)));
            }
            if (line.hasOption(Keys.CUSTOMSTATUS)) {
                serverOptions.setCustomHTTPStatusEnabled(Boolean.valueOf(line.getOptionValue(Keys.CUSTOMSTATUS)));
            }
            if (line.hasOption(Keys.TRANSFERMINSIZE)) {
                serverOptions.setTransferMinSize(Long.valueOf(line.getOptionValue(Keys.TRANSFERMINSIZE)));
            }
            if (line.hasOption(Keys.SENDFILE)) {
                serverOptions.setSendfileEnabled(Boolean.valueOf(line.getOptionValue(Keys.SENDFILE)));
            }
            if (line.hasOption(Keys.GZIP)) {
                serverOptions.setGzipEnabled(Boolean.valueOf(line.getOptionValue(Keys.GZIP)));
            }
            if (line.hasOption(Keys.MARIADB4J)) {
                serverOptions.setMariaDB4jEnabled(Boolean.valueOf(line.getOptionValue(Keys.MARIADB4J)));
            }
            if (line.hasOption(Keys.MARIADB4JPORT) && line.getOptionValue(Keys.MARIADB4JPORT).length() > 0) {
                serverOptions.setMariaDB4jPort(Integer.valueOf(line.getOptionValue(Keys.MARIADB4JPORT)));
            }
            if (line.hasOption(Keys.MARIADB4JBASEDIR) && line.getOptionValue(Keys.MARIADB4JBASEDIR).length() > 0) {
                serverOptions.setMariaDB4jBaseDir(new File(line.getOptionValue(Keys.MARIADB4JBASEDIR)));
            }
            if (line.hasOption(Keys.MARIADB4JDATADIR) && line.getOptionValue(Keys.MARIADB4JDATADIR).length() > 0) {
                serverOptions.setMariaDB4jDataDir(new File(line.getOptionValue(Keys.MARIADB4JDATADIR)));
            }
            if (line.hasOption(Keys.MARIADB4JIMPORT) && line.getOptionValue(Keys.MARIADB4JIMPORT).length() > 0) {
                serverOptions.setMariaDB4jImportSQLFile(new File(line.getOptionValue(Keys.MARIADB4JIMPORT)));
            }
            if (line.hasOption(Keys.JVMARGS) && line.getOptionValue(Keys.JVMARGS).length() > 0) {
                List<String> jvmArgs = new ArrayList<String>();
                String[] jvmArgArray = line.getOptionValue(Keys.JVMARGS).split("(?<!\\\\);");
                for(String arg : jvmArgArray) {
                    jvmArgs.add(arg.replaceAll("\\\\;", ";"));
                }
                serverOptions.setJVMArgs(jvmArgs);
            }
            if (line.hasOption(Keys.ERRORPAGES) && line.getOptionValue(Keys.ERRORPAGES).length() > 0) {
                serverOptions.setErrorPages(line.getOptionValue(Keys.ERRORPAGES));
            }
            if (line.hasOption(Keys.SERVLETREST) && line.getOptionValue(Keys.SERVLETREST).length() > 0) {
                serverOptions.setServletRestEnabled(Boolean.valueOf(line.getOptionValue(Keys.SERVLETREST)));
            }
            if (line.hasOption(Keys.SERVLETRESTMAPPINGS) && line.getOptionValue(Keys.SERVLETRESTMAPPINGS).length() > 0) {
                serverOptions.setServletRestMappings(line.getOptionValue(Keys.SERVLETRESTMAPPINGS));
            }
            if (line.hasOption(Keys.FILTERPATHINFO) && line.getOptionValue(Keys.FILTERPATHINFO).length() > 0) {
                serverOptions.setFilterPathInfoEnabled(Boolean.valueOf(line.getOptionValue(Keys.FILTERPATHINFO)));
            }
            if (line.hasOption(Keys.SSLADDCERTS) && line.getOptionValue(Keys.SSLADDCERTS).length() > 0) {
                serverOptions.setSSLAddCerts(line.getOptionValue(Keys.SSLADDCERTS));
            }
            if (line.hasOption(Keys.BASICAUTHENABLE)) {
                serverOptions.setEnableBasicAuth(Boolean.valueOf(line.getOptionValue(Keys.BASICAUTHENABLE)));
            }
            if (line.hasOption("users") && line.getOptionValue("users").length() > 0) {
                if(!line.hasOption(Keys.BASICAUTHENABLE) || line.hasOption(Keys.BASICAUTHENABLE) && Boolean.valueOf(line.getOptionValue(Keys.BASICAUTHENABLE))) {
                    serverOptions.setEnableBasicAuth(true);
                }
                serverOptions.setBasicAuth(line.getOptionValue("users"));
            }
            if (line.hasOption(Keys.BUFFERSIZE) && line.getOptionValue(Keys.BUFFERSIZE).length() > 0) {
                serverOptions.setBufferSize(Integer.valueOf(line.getOptionValue(Keys.BUFFERSIZE)));
            }
            if (line.hasOption(Keys.IOTHREADS) && line.getOptionValue(Keys.IOTHREADS).length() > 0) {
                serverOptions.setIoThreads(Integer.valueOf(line.getOptionValue(Keys.IOTHREADS)));
            }
            if (line.hasOption(Keys.WORKERTHREADS) && line.getOptionValue(Keys.WORKERTHREADS).length() > 0) {
                serverOptions.setWorkerThreads(Integer.valueOf(line.getOptionValue(Keys.WORKERTHREADS)));
            }
            if (line.hasOption(Keys.DIRECTBUFFERS)) {
                serverOptions.setDirectBuffers(Boolean.valueOf(line.getOptionValue(Keys.DIRECTBUFFERS)));
            }
            if (line.hasOption(Keys.LOADBALANCE) && line.getOptionValue(Keys.LOADBALANCE).length() > 0) {
                serverOptions.setLoadBalance(line.getOptionValue(Keys.LOADBALANCE));
            }
            if (line.hasOption(Keys.DIRECTORYREFRESH) && line.getOptionValue(Keys.DIRECTORYREFRESH).length() > 0) {
                serverOptions.setDirectoryListingRefreshEnabled(Boolean.valueOf(line.getOptionValue(Keys.DIRECTORYREFRESH)));
            }
            if (line.hasOption(Keys.PROXYPEERADDRESS) && line.getOptionValue(Keys.PROXYPEERADDRESS).length() > 0) {
                serverOptions.setProxyPeerAddressEnabled(Boolean.valueOf(line.getOptionValue(Keys.PROXYPEERADDRESS)));
            }
            if (line.hasOption(Keys.HTTP2) && line.getOptionValue(Keys.HTTP2).length() > 0) {
                if(!line.hasOption(Keys.SECURECOOKIES)) {
                    serverOptions.setSecureCookies(true);
                }
                serverOptions.setHTTP2Enabled(Boolean.valueOf(line.getOptionValue(Keys.HTTP2)));
            }
            
            if (line.hasOption(Keys.SECURECOOKIES) && line.getOptionValue(Keys.SECURECOOKIES).length() > 0) {
                serverOptions.setSecureCookies(Boolean.valueOf(line.getOptionValue(Keys.SECURECOOKIES)));
            }
            
            if(serverOptions.getLoglevel().equals(Keys.TRACE)) {
                for (Option arg : line.getOptions()) {
                    log.debug(arg.toString());
//                    log.debug(arg.getValue());
                }
            }
            return serverOptions;
        }
        catch( Exception exp ) {
            exp.printStackTrace();
            String msg = exp.getMessage();
            if(msg == null){
                msg = "null : "+exp.getStackTrace()[0].toString();
                if(exp.getStackTrace().length > 0) {
                    msg += '\n' + exp.getStackTrace()[1].toString();
                }
            } else {
                msg = exp.getClass().getName() + " " + msg;
            }
            printUsage(msg,1);
        }
        return null;
    }

    static File getFile(String path) {
        File file = new File(path);
        if(!file.exists() || file == null) {
            throw new RuntimeException("File not found: " + path +" (" + file.getAbsolutePath() + ")");
        }
      return file;
    }

    static void printUsage(String message, int exitCode) {
        PrintWriter pw = new PrintWriter(System.out);
        HelpFormatter formatter = new HelpFormatter();
        //formatter.printHelp( SYNTAX, options,false);
        if(exitCode == 0) {
            pw.println("USAGE   " + SYNTAX);
            pw.println(HEADER + '\n');
            pw.println(message);
            @SuppressWarnings("unchecked")
            List<Option> optList = new ArrayList<Option>(getOptions().getOptions());
            Collections.sort(optList, new Comparator<Option>() {
                public int compare(Option o1, Option o2) {
                    if(o1.getOpt().equals(Keys.WAR)) {return -1;} else if(o2.getOpt().equals(Keys.WAR)) {return 1;}
                    if(o1.getOpt().equals("p")) {return -1;} else if(o2.getOpt().equals("p")) {return 1;}
                    if(o1.getOpt().equals("c")) { return -1; } else if(o2.getOpt().equals("c")) {return 1;}
                    if(o1.getOpt().equals(Keys.CONTEXT)) { return -1; } else if(o2.getOpt().equals(Keys.CONTEXT)) {return 1;}
                    if(o1.getOpt().equals("d")) { return -1; } else if(o2.getOpt().equals("d")) {return 1;}
                    if(o1.getOpt().equals("b")) { return -1; } else if(o2.getOpt().equals("b")) {return 1;}
                    if(o1.getOpt().equals("h")) {return 1;} else if(o2.getOpt().equals("h")) {return -1;}
                    if(o1.getOpt().equals("url")) {return 1;} else if(o2.getOpt().equals("url")) {return -1;}
                    if(o1.getOpt().equals("open")) {return 1;} else if(o2.getOpt().equals("open")) {return -1;}
                    if(o1.getOpt().equals(Keys.STOPSOCKET)) {return 1;} else if(o2.getOpt().equals(Keys.STOPSOCKET)) {return -1;}
                    if(o1.getOpt().equals(Keys.STOP)) {return 1;} else if(o2.getOpt().equals(Keys.STOP)) {return -1;}
                    return o1.getOpt().compareTo(o2.getOpt());
                }});
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
//            formatter.printHelp(80, SYNTAX, message + '\n' + HEADER, options, FOOTER, false);
        } else {
            System.out.println("USAGE:  " + SYNTAX + '\n' + message);
        }
        System.exit(exitCode);
    }


}
