package runwar.options;

import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import net.minidev.json.JSONValue;
import net.minidev.json.parser.ParseException;
import runwar.LaunchUtil;
import runwar.Server;
import runwar.logging.RunwarLogger;
import runwar.options.ServerOptions.Keys;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.*;
import java.util.stream.Collectors;

import static runwar.logging.RunwarLogger.CONF_LOG;

public class ConfigParser {

    private ServerOptions serverOptions;
    private File configFile;

    public ConfigParser(File config){
        if(!config.exists()) {
            String message = "Configuration file does not exist: " + config.getAbsolutePath();
            CONF_LOG.error(message);
            throw new RuntimeException(message);
        }
        serverOptions = new ServerOptionsImpl();
        serverOptions.configFile(config);
        configFile = config;
        parseOptions(config);
    }

    public ConfigParser(String json){
        serverOptions = new ServerOptionsImpl();
        try {
            JSONObject jsonConfig = (JSONObject) JSONValue.parseWithException(json);
            parseOptions(jsonConfig);
        } catch (ParseException e) {
            throw new RuntimeException("Could not load configuration: " + e.getMessage());
        }
    }

    public ServerOptions getServerOptions(){
        return serverOptions;
    }
    
    private ServerOptions parseOptions(File config) {
        JSONObject jsonConfig;
        String configFilePath = "unknown";
        try {
            configFilePath = config.getCanonicalPath();
            jsonConfig = (JSONObject) JSONValue.parseWithException(LaunchUtil.readFile(config));
            return parseOptions(jsonConfig);
        } catch (ParseException | IOException e1) {
            System.out.println("Could not load " + configFilePath + " : " + e1.getMessage());
            throw new RuntimeException("Could not load " + configFilePath + " : " + e1.getMessage());
        }
    }


    private ServerOptions parseOptions(JSONObject jsonConfig) {
        if(jsonConfig != null){
            JSONOption serverConfig = new JSONOption(jsonConfig);
            if (serverConfig.hasOption(Keys.HELP)) {
                printUsage("Options",0);
            }
            if (serverConfig.hasOption(Keys.LOGLEVEL)) {
                serverOptions.logLevel(serverConfig.getOptionValue(Keys.LOGLEVEL));
            }

            if (serverConfig.hasOption(Keys.NAME)) {
                serverOptions.serverName(serverConfig.getOptionValue(Keys.NAME));
            }

            if (serverConfig.hasOption(Keys.DEBUG)) {
                Boolean debug = Boolean.valueOf(serverConfig.getOptionValue(Keys.DEBUG));
                serverOptions.debug(debug);
                if(debug)serverOptions.logLevel(Keys.DEBUG);
                if(serverConfig.hasOption(Keys.LOGLEVEL)) {
                    System.out.println("Warning:  debug overrides logLevel (both are specified, setting level to " + serverOptions.logLevel() + ")");
                }
            }

            if (serverConfig.hasOption(Keys.TRACE)) {
                Boolean trace = Boolean.valueOf(serverConfig.getOptionValue(Keys.TRACE));
                serverOptions.debug(trace);
                if(trace)serverOptions.logLevel(Keys.TRACE);
                if(serverConfig.hasOption(Keys.LOGLEVEL)) {
                    System.out.println("Warning:  trace overrides logLevel (both are specified, setting level to " + serverOptions.logLevel() + ")");
                }
            }
            
            if (serverConfig.hasOption(Keys.BACKGROUND)) {
                serverOptions.background(Boolean.valueOf(serverConfig.getOptionValue(Keys.BACKGROUND)));
            }
            if (serverConfig.g("app").hasOption(Keys.LIBDIRS)) {
                serverConfig.put(Keys.LIBS, serverConfig.g("app").getOptionValue(Keys.LIBDIRS));
            }
            if (serverConfig.hasOption(Keys.LIBS)) {
                String[] list = serverConfig.getOptionValue(Keys.LIBS).split(",");
                for (String path : list) {
                    File lib = new File(path);
                    if (!lib.exists() || !lib.isDirectory())
                        printUsage("No such lib directory "+path,1);
                }               
                serverOptions.libDirs(serverConfig.getOptionValue(Keys.LIBS));
            }

            if (serverConfig.g("web").hasOption(Keys.WELCOMEFILES)) {
                List<String> welcomeFiles = new ArrayList<>();
                try{
                    serverConfig.g("web").getJSONArray(Keys.WELCOMEFILES).forEach(o -> welcomeFiles.add((String) o));
                } catch (ClassCastException e){
                    Arrays.stream(serverConfig.g("web").getOptionValue(Keys.WELCOMEFILES).split(",")).forEach(o -> welcomeFiles.add((String) o));
                }
                if(welcomeFiles.size() > 0) {
                    serverOptions.welcomeFiles(welcomeFiles.toArray(new String[0]));
                }
            }
            if (serverConfig.hasOption(Keys.WELCOMEFILES)) {
                serverOptions.welcomeFiles(serverConfig.getOptionValue(Keys.WELCOMEFILES).split(","));
            }

            if (serverConfig.hasOption(Keys.JAR)) {
                 File jar = new File(serverConfig.getOptionValue(Keys.JAR));
                    try {
                        serverOptions.jarURL(jar.toURI().toURL());
                        if (!jar.exists() || jar.isDirectory())
                            printUsage("No such jar "+jar,1);
                    } catch (MalformedURLException e) {
                        printUsage("No such jar "+jar,1);
                        e.printStackTrace();
                    }
            }
            
            if (serverConfig.hasOption(Keys.STARTTIMEOUT)) {
                serverConfig.put(Keys.TIMEOUT,serverConfig.getOptionValue(Keys.STARTTIMEOUT));
            }
            if (serverConfig.hasOption(Keys.TIMEOUT)) {
                serverOptions.launchTimeout(((Number)serverConfig.getParsedOptionValue(Keys.TIMEOUT)).intValue() * 1000);
            }
            if (serverConfig.hasOption(Keys.PASSWORD)) {
                serverOptions.stopPassword(serverConfig.getOptionValue(Keys.PASSWORD).toCharArray());
            }
            if (serverConfig.hasOption(Keys.STOPSOCKET)) {
                serverConfig.put("stop-port",serverConfig.getOptionValue(Keys.STOPSOCKET));
            }
            if (serverConfig.hasOption("stop-port")) {
                serverOptions.stopPort(((Number)serverConfig.getParsedOptionValue("stop-port")).intValue());
            }

            if(serverConfig.g("web").getOptionValue("webroot") != null) {
                serverConfig.put(Keys.WAR,serverConfig.g("web").getOptionValue("webroot"));
            }
            if(serverConfig.g("app").getOptionValue("WARPath") != null) {
                serverConfig.put(Keys.WAR,serverConfig.g("app").getOptionValue("WARPath"));
            }
            if (serverConfig.hasOption(Keys.WAR)) {
                String warPath = serverConfig.getOptionValue(Keys.WAR);
                serverOptions.warFile(getFile(warPath));
            } else if (!serverConfig.hasOption(Keys.STOP) && !serverConfig.hasOption("c") && serverOptions.warFile() == null) {
                printUsage("Must specify -war path/to/war, or -stop [-stop-socket]",1);
            } 
            if(serverConfig.hasOption("D")){
                final String[] properties = serverConfig.getOptionValue("D").split(" ");
                for (int i = 0; i < properties.length; i++) {
                    CONF_LOG.debugf("setting system property: %s", properties[i] +'='+properties[i+1]);
                    System.setProperty(properties[i],properties[i+1]);
                    i++;
                }
            }

            if (serverConfig.g("app").hasOption("webXML")) {
                serverConfig.put(Keys.WEBXMLPATH, serverConfig.g("app").getOptionValue("webXML"));
            }
            if (serverConfig.hasOption(Keys.WEBXMLPATH)) {
                String webXmlPath = serverConfig.getOptionValue(Keys.WEBXMLPATH);
                File webXmlFile = new File(webXmlPath);
                if(webXmlFile.exists()) {
                    serverOptions.webXmlFile(webXmlFile);
                } else {
                    throw new RuntimeException("Could not find web.xml! " + webXmlPath);
                }
            }

            if (serverConfig.hasOption(Keys.STOP)) {
                serverOptions.action(Keys.STOP);
                String[] values = serverConfig.getOptionValue(Keys.STOP).split(" ");
                if(values != null && values.length > 0) {
                    serverOptions.stopPort(Integer.parseInt(values[0]));
                }
                if(values != null && values.length >= 1) {
                    serverOptions.stopPassword(values[1].toCharArray());
                }
            } else {
                serverOptions.action("start");
            }

            if (serverConfig.hasOption(Keys.CONTEXT)) {
                serverOptions.contextPath(serverConfig.getOptionValue(Keys.CONTEXT));
            }

            if (serverConfig.g("web").getOptionValue(Keys.HOST) != null) {
                serverConfig.put(Keys.HOST, serverConfig.g("web").getOptionValue(Keys.HOST));
            }
            if (serverConfig.hasOption(Keys.HOST)) {
                serverOptions.host(serverConfig.getOptionValue(Keys.HOST));
            }

            if (serverConfig.g("web").g("http").getOptionValue(Keys.PORT) != null) {
                serverConfig.put(Keys.PORT, serverConfig.g("web").g("http").getOptionValue(Keys.PORT));
            }
            if (serverConfig.hasOption(Keys.PORT)) {
                serverOptions.httpPort(((Number)serverConfig.getParsedOptionValue(Keys.PORT)).intValue());
            }

            if (serverConfig.g("web").g("ajp").getOptionValue(Keys.PORT) != null) {
                serverConfig.put(Keys.AJPPORT, serverConfig.g("web").g("ajp").getOptionValue(Keys.PORT));
            }
            if (serverConfig.hasOption(Keys.AJPPORT)) {
                serverOptions.httpEnable(false)
                    .ajpEnable(true).ajpPort(((Number)serverConfig.getParsedOptionValue(Keys.AJPPORT)).intValue());
            }

            if (serverConfig.g("web").g("ssl").getOptionValue(Keys.PORT) != null) {
                serverConfig.put(Keys.SSLPORT, serverConfig.g("web").g("ssl").getOptionValue(Keys.PORT));
            }
            if (serverConfig.hasOption(Keys.SSLPORT)) {
                serverOptions.httpEnable(false).secureCookies(true).sslEnable(true).sslPort(((Number)serverConfig.getParsedOptionValue(Keys.SSLPORT)).intValue());
            }

            if (serverConfig.g("web").g("ssl").getOptionValue("certFile") != null) {
                serverOptions.sslCertificate(getFile(serverConfig.g("web").g("ssl").getOptionValue("certFile")));
                if (!serverConfig.g("web").g("ssl").hasOption("keyFile") || !serverConfig.g("web").g("ssl").hasOption("keyPass")) {
                    throw new RuntimeException("Using a SSL certificate requires -sslkey /path/to/file and -sslkeypass pass**** arguments!");   
                }
            }
            if (serverConfig.hasOption(Keys.SSLCERT)) {
                serverOptions.sslCertificate(getFile(serverConfig.getOptionValue(Keys.SSLCERT)));
                if (!serverConfig.hasOption(Keys.SSLKEY) || !serverConfig.hasOption(Keys.SSLKEY)) {
                    throw new RuntimeException("Using a SSL certificate requires -sslkey /path/to/file and -sslkeypass pass**** arguments!");   
                }
            }

            if (serverConfig.g("web").g("ssl").getOptionValue("certFile") != null) {
                serverConfig.put(Keys.SSLKEY, serverConfig.g("web").g("ssl").getOptionValue("keyFile"));
            }
            if (serverConfig.hasOption(Keys.SSLKEY)) {
                serverOptions.sslKey(getFile(serverConfig.getOptionValue(Keys.SSLKEY)));
            }

            if (serverConfig.g("web").g("ssl").getOptionValue("keyPass") != null) {
                serverConfig.put(Keys.SSLKEYPASS, serverConfig.g("web").g("ssl").getOptionValue("keyPass"));
            }
            if (serverConfig.hasOption(Keys.SSLKEYPASS)) {
                serverOptions.sslKeyPass(serverConfig.getOptionValue(Keys.SSLKEYPASS).toCharArray());
            }

            if (serverConfig.g("web").g("ajp").hasOption("enable")) {
                serverConfig.put(Keys.AJPENABLE, serverConfig.g("web").g("ajp").getOptionValue("enable"));
            }
            if (serverConfig.hasOption(Keys.AJPENABLE)) {
                serverOptions.ajpEnable(Boolean.valueOf(serverConfig.getOptionValue(Keys.AJPENABLE)));
            }

            if (serverConfig.g("web").g("ssl").hasOption("enable")) {
                serverConfig.put(Keys.SSLENABLE, serverConfig.g("web").g("ssl").getOptionValue("enable"));
            }
            if (serverConfig.hasOption(Keys.SSLENABLE)) {
                serverOptions.httpEnable(false).sslEnable(Boolean.valueOf(serverConfig.getOptionValue(Keys.SSLENABLE)));
            }

            if (serverConfig.g("web").g("http").hasOption("enable")) {
                serverConfig.put(Keys.HTTPENABLE, serverConfig.g("web").g("http").getOptionValue("enable"));
            }
            if (serverConfig.hasOption(Keys.HTTPENABLE)) {
                serverOptions.httpEnable(Boolean.valueOf(serverConfig.getOptionValue(Keys.HTTPENABLE)));
            }
            
            if (serverConfig.g("web").g("rewrites").hasOption(Keys.CONFIG)) {
                serverConfig.put(Keys.URLREWRITEFILE, serverConfig.g("web").g("rewrites").getOptionValue(Keys.CONFIG));
            }
            if (serverConfig.hasOption(Keys.URLREWRITEFILE)) {
                serverOptions.urlRewriteFile(getFile(serverConfig.getOptionValue(Keys.URLREWRITEFILE)));
            }
            if (serverConfig.hasOption(Keys.URLREWRITELOG)) {
                serverOptions.urlRewriteLog(getFile(serverConfig.getOptionValue(Keys.URLREWRITELOG)));
                if(!serverConfig.hasOption(Keys.URLREWRITEENABLE)) {
                    serverOptions.urlRewriteEnable(true);
                }
            }

            if (serverConfig.g("web").g("rewrites").hasOption("enable")) {
                serverConfig.put(Keys.URLREWRITEENABLE, serverConfig.g("web").g("rewrites").getOptionValue("enable"));
            }
            if (serverConfig.hasOption(Keys.URLREWRITEENABLE)) {
                serverOptions.urlRewriteEnable(Boolean.valueOf(serverConfig.getOptionValue(Keys.URLREWRITEENABLE)));
            }

            if (serverConfig.g("web").g("rewrites").hasOption("configReloadSeconds")) {
                serverConfig.put(Keys.URLREWRITECHECK, serverConfig.g("web").g("rewrites").getOptionValue("configReloadSeconds"));
            }
            if (serverConfig.hasOption(Keys.URLREWRITECHECK) && serverConfig.getOptionValue(Keys.URLREWRITECHECK).length() > 0) {
                serverOptions.urlRewriteCheckInterval(serverConfig.getOptionValue(Keys.URLREWRITECHECK));
            }

            if (serverConfig.g("web").g("rewrites").hasOption("statusPath")) {
                serverConfig.put(Keys.URLREWRITESTATUSPATH, serverConfig.g("web").g("rewrites").getOptionValue("statusPath"));
            }
            if (serverConfig.hasOption(Keys.URLREWRITESTATUSPATH) && serverConfig.getOptionValue(Keys.URLREWRITESTATUSPATH).length() > 0) {
                serverOptions.urlRewriteStatusPath(serverConfig.getOptionValue(Keys.URLREWRITESTATUSPATH));
            }

            if (serverConfig.g("app").hasOption(Keys.LOGDIR)) {
                serverConfig.put(Keys.LOGDIR, serverConfig.g("app").getOptionValue(Keys.LOGDIR));
            }
            if (serverConfig.hasOption(Keys.LOGDIR) || serverConfig.g("app").getOptionValue(Keys.LOGDIR) != null) {
                if(serverConfig.hasOption(Keys.LOGDIR)) {
                    serverOptions.logDir(serverConfig.getOptionValue(Keys.LOGDIR));
                } else {
                    serverOptions.logDir(serverConfig.g("app").getOptionValue(Keys.LOGDIR));
                }
            } else {
                if(serverOptions.warFile() != null){
                    File warFile = serverOptions.warFile();
                    String logDir;
                    if(warFile.isDirectory() && new File(warFile,"WEB-INF").exists()) {
                        logDir = warFile.getPath() + "/WEB-INF/logs/";
                    } else {
                        String serverConfigDir = System.getProperty("cfml.server.config.dir");
                        if(serverConfigDir == null) {
                            logDir = new File(Server.getThisJarLocation().getParentFile(),"server/log/").getAbsolutePath();
                        } else {
                            logDir = new File(serverConfigDir,"log/").getAbsolutePath();                        
                        }
                    }
                    serverOptions.logDir(logDir);
                }
            }
            if (serverConfig.g("web").get("aliases") != null) {
                final StringBuilder dirs = new StringBuilder();
                serverConfig.g("web").get("aliases").forEach( (alias,path)-> 
                    dirs.append(alias + "=" + path + "," ) 
                );
                serverOptions.contentDirs(dirs.toString().replaceAll(",$", ""));
            }
            if (serverConfig.g("web").hasOption(Keys.CONTENTDIRS)) {
                CONF_LOG.trace("Loading contentDirs");
                Set<String> contentDirectories = new HashSet<>();
                serverConfig.g("web").getJSONArray(Keys.CONTENTDIRS).forEach(o -> contentDirectories.add((String)o));
                serverOptions.contentDirectories(contentDirectories);
            } else {
                CONF_LOG.trace("No contentDirs to load");
            }
            if (serverConfig.hasOption(Keys.DIRS)) {
                serverOptions.contentDirs(serverConfig.getOptionValue(Keys.DIRS));
            }
            if (serverConfig.hasOption(Keys.LOGREQUESTSBASENAME)) {
                serverOptions.logRequestsEnable(true);
                serverOptions.logRequestsBaseFileName(serverConfig.getOptionValue(Keys.LOGREQUESTSBASENAME));
            }
            if (serverConfig.hasOption(Keys.LOGREQUESTSDIR)) {
                serverOptions.logRequestsEnable(true);
                serverOptions.logRequestsDir(getFile(serverConfig.getOptionValue(Keys.LOGREQUESTSDIR)));
            }
            if (serverConfig.hasOption(Keys.LOGREQUESTS)) {
                serverOptions.logRequestsEnable(Boolean.valueOf(serverConfig.getOptionValue(Keys.LOGREQUESTS)));
            }
            if (serverConfig.hasOption(Keys.LOGACCESSBASENAME)) {
                serverOptions.logAccessEnable(true);
                serverOptions.logAccessBaseFileName(serverConfig.getOptionValue(Keys.LOGACCESSBASENAME));
            }
            if (serverConfig.hasOption(Keys.LOGACCESSDIR)) {
                serverOptions.logAccessEnable(true);
                serverOptions.logAccessDir(getFile(serverConfig.getOptionValue(Keys.LOGACCESSDIR)));
            }
            if (serverConfig.hasOption(Keys.LOGACCESS)) {
                serverOptions.logAccessEnable(Boolean.valueOf(serverConfig.getOptionValue(Keys.LOGACCESS)));
            }
            
            if (serverConfig.hasOption("openBrowser")) {
                serverConfig.put(Keys.OPENBROWSER, serverConfig.getOptionValue("openBrowser"));
            }
            if (serverConfig.hasOption(Keys.OPENBROWSER)) {
                serverOptions.openbrowser(Boolean.valueOf(serverConfig.getOptionValue(Keys.OPENBROWSER)));
            }
            if (serverConfig.hasOption("openBrowserURL")) {
                serverConfig.put(Keys.OPENURL, serverConfig.getOptionValue("openBrowserURL"));
            }
            if (serverConfig.hasOption(Keys.OPENURL)) {
                serverOptions.openbrowserURL(serverConfig.getOptionValue(Keys.OPENURL));
            }

            if (serverConfig.hasOption(Keys.PIDFILE)) {
                serverOptions.pidFile(serverConfig.getOptionValue(Keys.PIDFILE));
            }

            if (serverConfig.hasOption(Keys.PROCESSNAME)) {
                serverOptions.processName(serverConfig.getOptionValue(Keys.PROCESSNAME));
            }

            if (serverConfig.hasOption(Keys.TRAY)) {
                serverOptions.trayEnable(Boolean.valueOf(serverConfig.getOptionValue(Keys.TRAY)));
            }
            
            if (serverConfig.hasOption(Keys.DOCK)) {
                serverOptions.dockEnable(Boolean.valueOf(serverConfig.getOptionValue(Keys.DOCK)));
            }
            
            if (serverConfig.hasOption("trayicon")) {
                serverConfig.put(Keys.ICON, serverConfig.getOptionValue("trayicon"));
            }
            if (serverConfig.hasOption(Keys.ICON)) {
                serverOptions.iconImage(serverConfig.getOptionValue(Keys.ICON));
            }
            
            if (serverConfig.hasOption("trayOptions")) {
                serverOptions.trayConfig(serverConfig.getJSONArray("trayOptions"));
            }

            if (serverConfig.hasOption(Keys.TRAYCONFIG)) {
                serverOptions.trayConfig(getFile(serverConfig.getOptionValue(Keys.TRAYCONFIG)));
            }
            
            if (serverConfig.hasOption(Keys.PREDICATEFILE)) {
                serverOptions.predicateFile(getFile(serverConfig.getOptionValue(Keys.PREDICATEFILE)));
            }

            if (serverConfig.hasOption(Keys.STATUSFILE)) {
                serverOptions.statusFile(getFile(serverConfig.getOptionValue(Keys.STATUSFILE)));
            }
            

            if (serverConfig.hasOption(Keys.CFENGINE)) {
            	serverOptions.cfEngineName(serverConfig.getOptionValue(Keys.CFENGINE));
            }
            if (serverConfig.hasOption(Keys.CFSERVERCONF)) {
                serverOptions.cfmlServletConfigServerDir(serverConfig.getOptionValue(Keys.CFSERVERCONF));
            }
            if (serverConfig.hasOption(Keys.CFWEBCONF)) {
                serverOptions.cfmlServletConfigWebDir(serverConfig.getOptionValue(Keys.CFWEBCONF));
            }
            if (serverConfig.hasOption(Keys.DIRECTORYINDEX)) {
                serverOptions.directoryListingEnable(Boolean.valueOf(serverConfig.getOptionValue(Keys.DIRECTORYINDEX)));
            }
            if (serverConfig.hasOption(Keys.CACHE)) {
                serverOptions.cacheEnable(Boolean.valueOf(serverConfig.getOptionValue(Keys.CACHE)));
            }
            if (serverConfig.hasOption(Keys.CUSTOMSTATUS)) {
                serverOptions.customHTTPStatusEnable(Boolean.valueOf(serverConfig.getOptionValue(Keys.CUSTOMSTATUS)));
            }
            if (serverConfig.hasOption(Keys.TRANSFERMINSIZE)) {
                serverOptions.transferMinSize(Long.valueOf(serverConfig.getOptionValue(Keys.TRANSFERMINSIZE)));
            }
            if (serverConfig.hasOption(Keys.SENDFILE)) {
                serverOptions.sendfileEnable(Boolean.valueOf(serverConfig.getOptionValue(Keys.SENDFILE)));
            }
            if (serverConfig.hasOption(Keys.GZIP)) {
                serverOptions.gzipEnable(Boolean.valueOf(serverConfig.getOptionValue(Keys.GZIP)));
            }
            if (serverConfig.hasOption(Keys.MARIADB4J)) {
                serverOptions.mariaDB4jEnable(Boolean.valueOf(serverConfig.getOptionValue(Keys.MARIADB4J)));
            }
            if (serverConfig.hasOption(Keys.MARIADB4JPORT) && serverConfig.getOptionValue(Keys.MARIADB4JPORT).length() > 0) {
                serverOptions.mariaDB4jPort(Integer.valueOf(serverConfig.getOptionValue(Keys.MARIADB4JPORT)));
            }
            if (serverConfig.hasOption(Keys.MARIADB4JBASEDIR) && serverConfig.getOptionValue(Keys.MARIADB4JBASEDIR).length() > 0) {
                serverOptions.mariaDB4jBaseDir(new File(serverConfig.getOptionValue(Keys.MARIADB4JBASEDIR)));
            }
            if (serverConfig.hasOption(Keys.MARIADB4JDATADIR) && serverConfig.getOptionValue(Keys.MARIADB4JDATADIR).length() > 0) {
                serverOptions.mariaDB4jDataDir(new File(serverConfig.getOptionValue(Keys.MARIADB4JDATADIR)));
            }
            if (serverConfig.hasOption(Keys.MARIADB4JIMPORT) && serverConfig.getOptionValue(Keys.MARIADB4JIMPORT).length() > 0) {
                serverOptions.mariaDB4jImportSQLFile(new File(serverConfig.getOptionValue(Keys.MARIADB4JIMPORT)));
            }
            if (serverConfig.hasOption(Keys.JVMARGS) && serverConfig.getOptionValue(Keys.JVMARGS).length() > 0) {
                List<String> jvmArgs = new ArrayList<String>();
                String[] jvmArgArray = serverConfig.getOptionValue(Keys.JVMARGS).split("(?<!\\\\);");
                for(String arg : jvmArgArray) {
                    jvmArgs.add(arg.replaceAll("\\\\;", ";"));
                }
                serverOptions.jvmArgs(jvmArgs);
            }

            if (serverConfig.g("web").get(Keys.ERRORPAGES) != null) {
                final StringBuilder pages = new StringBuilder();
                serverConfig.g("web").get(Keys.ERRORPAGES).forEach( (code,path)-> 
                pages.append(code.toLowerCase().equals("default") ? path + "," : code + "=" + path + "," ) 
                );
                String pagesStr = pages.toString().replaceAll(",$", "");
                serverConfig.put(Keys.ERRORPAGES, pagesStr);
            }
            if (serverConfig.hasOption(Keys.ERRORPAGES)) {
                try {
                    serverOptions.errorPages(serverConfig.getOptionValue(Keys.ERRORPAGES));
                } catch (Exception e) {
                    RunwarLogger.LOG.error("Could not parse errorPages:" + serverConfig.getOptionValue(Keys.ERRORPAGES));
                }
            }

            if (serverConfig.hasOption(Keys.SERVLETREST) && serverConfig.getOptionValue(Keys.SERVLETREST).length() > 0) {
                serverOptions.servletRestEnable(Boolean.valueOf(serverConfig.getOptionValue(Keys.SERVLETREST)));
            }

            if (serverConfig.g("app").hasOption("restMappings")) {
                serverConfig.put(Keys.SERVLETRESTMAPPINGS, serverConfig.g("app").getOptionValue("restMappings"));
            }
            if (serverConfig.hasOption(Keys.SERVLETRESTMAPPINGS) && serverConfig.getOptionValue(Keys.SERVLETRESTMAPPINGS).length() > 0) {
                serverOptions.servletRestMappings(serverConfig.getOptionValue(Keys.SERVLETRESTMAPPINGS));
            }

            if (serverConfig.hasOption(Keys.FILTERPATHINFO) && serverConfig.getOptionValue(Keys.FILTERPATHINFO).length() > 0) {
                serverOptions.filterPathInfoEnable(Boolean.valueOf(serverConfig.getOptionValue(Keys.FILTERPATHINFO)));
            }

            if (serverConfig.hasOption(Keys.SSLADDCERTS) && serverConfig.getOptionValue(Keys.SSLADDCERTS).length() > 0) {
                serverOptions.sslAddCerts(serverConfig.getOptionValue(Keys.SSLADDCERTS));
            }

            if (serverConfig.g("web").g(Keys.BASICAUTHENABLE).hasOption("enable")) {
                serverConfig.put(Keys.BASICAUTHENABLE,serverConfig.g("web").g(Keys.BASICAUTHENABLE).getOptionValue("enable"));
            }

            if (serverConfig.hasOption(Keys.BASICAUTHENABLE)) {
                serverOptions.basicAuthEnable((Boolean.valueOf(serverConfig.getOptionValue(Keys.BASICAUTHENABLE))));
            }

            if (serverConfig.g("web").g(Keys.BASICAUTHENABLE).hasOption("users")) {
                final StringBuilder dirs = new StringBuilder();
                serverConfig.g("web").g(Keys.BASICAUTHENABLE).get("users").forEach( (user,pass)-> 
                    dirs.append(user + "=" + pass + "," ) 
                );
                serverConfig.put(Keys.BASICAUTHENABLE, dirs.toString().replaceAll(",$", ""));
            }
            if (serverConfig.hasOption(Keys.BASICAUTHENABLE) && serverConfig.getOptionValue(Keys.BASICAUTHENABLE).length() > 0) {
                if(!serverConfig.hasOption(Keys.BASICAUTHENABLE) || serverConfig.hasOption(Keys.BASICAUTHENABLE) && Boolean.valueOf(serverConfig.getOptionValue(Keys.BASICAUTHENABLE))) {
                    serverOptions.basicAuthEnable(true);
                }
                serverOptions.basicAuth(serverConfig.getOptionValue(Keys.BASICAUTHENABLE));
            }
            if (serverConfig.hasOption(Keys.BUFFERSIZE) && serverConfig.getOptionValue(Keys.BUFFERSIZE).length() > 0) {
                serverOptions.bufferSize(Integer.valueOf(serverConfig.getOptionValue(Keys.BUFFERSIZE)));
            }
            if (serverConfig.hasOption(Keys.IOTHREADS) && serverConfig.getOptionValue(Keys.IOTHREADS).length() > 0) {
                serverOptions.ioThreads(Integer.valueOf(serverConfig.getOptionValue(Keys.IOTHREADS)));
            }
            if (serverConfig.hasOption(Keys.WORKERTHREADS) && serverConfig.getOptionValue(Keys.WORKERTHREADS).length() > 0) {
                serverOptions.workerThreads(Integer.valueOf(serverConfig.getOptionValue(Keys.WORKERTHREADS)));
            }
            if (serverConfig.hasOption(Keys.DIRECTBUFFERS)) {
                serverOptions.directBuffers(Boolean.valueOf(serverConfig.getOptionValue(Keys.DIRECTBUFFERS)));
            }
            if (serverConfig.hasOption(Keys.LOADBALANCE) && serverConfig.getOptionValue(Keys.LOADBALANCE).length() > 0) {
                serverOptions.loadBalance(serverConfig.getOptionValue(Keys.LOADBALANCE));
            }
            if (serverConfig.hasOption(Keys.DIRECTORYREFRESH) && serverConfig.getOptionValue(Keys.DIRECTORYREFRESH).length() > 0) {
                serverOptions.directoryListingRefreshEnable(Boolean.valueOf(serverConfig.getOptionValue(Keys.DIRECTORYREFRESH)));
            }
            if (serverConfig.hasOption(Keys.PROXYPEERADDRESS) && serverConfig.getOptionValue(Keys.PROXYPEERADDRESS).length() > 0) {
                serverOptions.proxyPeerAddressEnable(Boolean.valueOf(serverConfig.getOptionValue(Keys.PROXYPEERADDRESS)));
            }
            if (serverConfig.hasOption(Keys.HTTP2) && serverConfig.getOptionValue(Keys.HTTP2).length() > 0) {
                if(!serverConfig.hasOption(Keys.SECURECOOKIES)) {
                    serverOptions.secureCookies(true);
                }
                serverOptions.http2Enable(Boolean.valueOf(serverConfig.getOptionValue(Keys.HTTP2)));
            }

            if (serverConfig.hasOption(Keys.SECURECOOKIES) && serverConfig.getOptionValue(Keys.SECURECOOKIES).length() > 0) {
                serverOptions.secureCookies(Boolean.valueOf(serverConfig.getOptionValue(Keys.SECURECOOKIES)));
            }

            if (serverConfig.hasOption(Keys.COOKIEHTTPONLY) && serverConfig.getOptionValue(Keys.COOKIEHTTPONLY).length() > 0) {
                serverOptions.cookieHttpOnly(Boolean.valueOf(serverConfig.getOptionValue(Keys.COOKIEHTTPONLY)));
            }

            if (serverConfig.hasOption(Keys.COOKIESECURE) && serverConfig.getOptionValue(Keys.COOKIESECURE).length() > 0) {
                serverOptions.cookieSecure(Boolean.valueOf(serverConfig.getOptionValue(Keys.COOKIESECURE)));
            }

            if (serverConfig.hasOption(Keys.SSLECCDISABLE)) {
                serverOptions.sslEccDisable(Boolean.valueOf(serverConfig.getOptionValue(Keys.SSLECCDISABLE)));
            }

            if (serverConfig.hasOption(Keys.SSLSELFSIGN)) {
                serverOptions.sslSelfSign(Boolean.valueOf(serverConfig.getOptionValue(Keys.SSLSELFSIGN)));
            }
            
            if (serverConfig.hasOption(Keys.BROWSER)) {
                serverOptions.sslSelfSign(Boolean.valueOf(serverConfig.getOptionValue(Keys.BROWSER)));
            }

            if (serverConfig.hasOption(Keys.WEBINF)) {
                String webInfPath = serverConfig.getOptionValue(Keys.WEBINF);
                File webinfDir = new File(webInfPath);
                if(webinfDir.exists()) {
                    serverOptions.webInfDir(webinfDir);
                } else {
                    throw new RuntimeException("Could not find WEB-INF! " + webInfPath);
                }
            }

            if (serverConfig.hasOption(Keys.SERVICE)) {
                serverOptions.service(Boolean.valueOf(serverConfig.getOptionValue(Keys.SERVICE)));
            }

            if(serverOptions.logLevel().equals(Keys.DEBUG)) {
                Iterator<String> optionsIterator = serverConfig.getOptions().iterator();
                while(optionsIterator.hasNext()) {
                    RunwarLogger.LOG.debug(optionsIterator.next());
                }
            }
        }
        return serverOptions;
    }
    
    private void printUsage(String string, int exitCode) {
//        CommandLineHandler.printUsage(string, exitCode);
    }

    private File getFile(String filePath) {
        if(configFile == null){ // loaded from json, but need to work out paths, so:
            configFile = new File("server.json");
        }
        return CommandLineHandler.getFile(configFile.getAbsoluteFile().getParentFile().getAbsolutePath() + '/' + filePath);
    }
    
    private class JSONOption {
        private JSONObject jsonConfig;

        public JSONOption(JSONObject jsonConfig) {
            this.jsonConfig = jsonConfig;
        }

        public Number getParsedOptionValue(String string) {
            return Integer.parseInt(getOptionValue(string));
        }

        public ArrayList<String> getOptions() {
            Iterator<String> keys = jsonConfig.keySet().iterator();
            ArrayList<String> options = new ArrayList<String>();
            while(keys.hasNext()) {
                String key = keys.next();
                options.add(key+"="+jsonConfig.get(key).toString());
            }
            return options;
        }

        public String getOptionValue(String key) {
            key = getKeyNoCase(key);
            if(hasOption(key)){
              return jsonConfig.get(key).toString();
            }
            return null;
        }

        public JSONOption g(String key) {
            key = getKeyNoCase(key);
          return new JSONOption((JSONObject) jsonConfig.get(key));
        }

        public JSONObject get(String key) {
            key = getKeyNoCase(key);
            return (JSONObject) jsonConfig.get(key);
        }

        public void put(String key, String value) {
            jsonConfig.put(key,value);
        }
        
        public JSONArray getJSONArray(String key) {
            key = getKeyNoCase(key);
            return (JSONArray) jsonConfig.get(key);
        }

        public String getKeyNoCase(String dirtyKey) {
            if(dirtyKey == null)
                return dirtyKey;
            String result = jsonConfig.keySet().stream()
                    .filter(map -> dirtyKey.toLowerCase().equals(map.toLowerCase()))
                    .map(map->map)
                    .collect(Collectors.joining());
            return result.length() > 0 ? result : dirtyKey;
        }

        public boolean hasOption(String key) {
            key = getKeyNoCase(key);
            if(key == null)
                return false;
            return jsonConfig.containsKey(key) && jsonConfig.get(key).toString().length() > 0;
        }
    }

}
