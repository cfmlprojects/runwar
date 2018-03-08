package runwar.options;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import runwar.LaunchUtil;
import runwar.Server;
import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import net.minidev.json.JSONValue;
import net.minidev.json.parser.ParseException;
import static runwar.options.ServerOptions.Keys;

public class ConfigParser {

    private static Logger log = LoggerFactory.getLogger(ConfigParser.class);
    private ServerOptions serverOptions;

    public ConfigParser(File config){
        serverOptions = new ServerOptionsImpl();
        serverOptions.setConfigFile(config);
        parseOptions(config);
    }

    public ServerOptions getServerOptions(){
        return serverOptions;
    }
    
    private ServerOptions parseOptions(File config) {
        JSONObject jsonConfig = null;
        String configFilePath = "unknown";
        try {
            configFilePath = config.getCanonicalPath();
            jsonConfig = (JSONObject) JSONValue.parseWithException(LaunchUtil.readFile(config));
        } catch (ParseException | IOException e1) {
            System.out.println("Could not load " + configFilePath + " : " + e1.getMessage());
            throw new RuntimeException("Could not load " + configFilePath + " : " + e1.getMessage());
        }
        if(jsonConfig != null){
            JSONOption serverConfig = new JSONOption(jsonConfig);
            if (serverConfig.hasOption(Keys.HELP)) {
                printUsage("Options",0);
            }
            if (serverConfig.hasOption(Keys.LOGLEVEL)) {
                serverOptions.setLoglevel(serverConfig.getOptionValue(Keys.LOGLEVEL));
            }

            if (serverConfig.hasOption(Keys.NAME)) {
                serverOptions.setServerName(serverConfig.getOptionValue(Keys.NAME));
            }

            if (serverConfig.hasOption(Keys.DEBUG)) {
                Boolean debug= Boolean.valueOf(serverConfig.getOptionValue(Keys.DEBUG));
                serverOptions.setDebug(debug);
                if(debug)serverOptions.setLoglevel(Keys.DEBUG);
                if(serverConfig.hasOption(Keys.LOGLEVEL)) {
                    System.out.println("Warning:  debug overrides loglevel (both are specified, setting level to " + serverOptions.getLoglevel() + ")");
                }
            }

            if (serverConfig.hasOption(Keys.TRACE)) {
                Boolean trace = Boolean.valueOf(serverConfig.getOptionValue(Keys.TRACE));
                serverOptions.setDebug(trace);
                if(trace)serverOptions.setLoglevel(Keys.TRACE);
                if(serverConfig.hasOption(Keys.LOGLEVEL)) {
                    System.out.println("Warning:  trace overrides loglevel (both are specified, setting level to " + serverOptions.getLoglevel() + ")");
                }
            }
            
            if (serverConfig.hasOption(Keys.BACKGROUND)) {
                serverOptions.setBackground(Boolean.valueOf(serverConfig.getOptionValue(Keys.BACKGROUND)));
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
                serverOptions.setLibDirs(serverConfig.getOptionValue(Keys.LIBS));
            }

            if (serverConfig.g("web").hasOption(Keys.WELCOMEFILES)) {
                serverConfig.put(Keys.WELCOMEFILES,serverConfig.g("web").getOptionValue(Keys.WELCOMEFILES));
            }
            if (serverConfig.hasOption(Keys.WELCOMEFILES)) {
                serverOptions.setWelcomeFiles(serverConfig.getOptionValue(Keys.WELCOMEFILES).split(","));
            }

            if (serverConfig.hasOption(Keys.JAR)) {
                 File jar = new File(serverConfig.getOptionValue(Keys.JAR));
                    try {
                        serverOptions.setJarURL(jar.toURI().toURL());
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
                serverOptions.setLaunchTimeout(((Number)serverConfig.getParsedOptionValue(Keys.TIMEOUT)).intValue() * 1000);
            }
            if (serverConfig.hasOption(Keys.PASSWORD)) {
                serverOptions.setStopPassword(serverConfig.getOptionValue(Keys.PASSWORD).toCharArray());
            }
            if (serverConfig.hasOption(Keys.STOPSOCKET)) {
                serverConfig.put("stop-port",serverConfig.getOptionValue(Keys.STOPSOCKET));
            }
            if (serverConfig.hasOption("stop-port")) {
                serverOptions.setSocketNumber(((Number)serverConfig.getParsedOptionValue("stop-port")).intValue());
            }

            if(serverConfig.g("web").getOptionValue("webroot") != null) {
                serverConfig.put(Keys.WAR,serverConfig.g("web").getOptionValue("webroot"));
            }
            if(serverConfig.g("app").getOptionValue("WARPath") != null) {
                serverConfig.put(Keys.WAR,serverConfig.g("app").getOptionValue("WARPath"));
            }
            if (serverConfig.hasOption(Keys.WAR)) {
                String warPath = serverConfig.getOptionValue(Keys.WAR);
                serverOptions.setWarFile(getFile(warPath));
            } else if (!serverConfig.hasOption(Keys.STOP) && !serverConfig.hasOption("c") && serverOptions.getWarFile() == null) {
                printUsage("Must specify -war path/to/war, or -stop [-stop-socket]",1);
            } 
            if(serverConfig.hasOption("D")){
                final String[] properties = serverConfig.getOptionValue("D").split(" ");
                for (int i = 0; i < properties.length; i++) {
                    log.debug("setting system property: %s", properties[i].toString()+'='+properties[i+1].toString());
                    System.setProperty(properties[i].toString(),properties[i+1].toString());
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
                    serverOptions.setWebXmlFile(webXmlFile);
                } else {
                    throw new RuntimeException("Could not find web.xml! " + webXmlPath);
                }
            }

            if (serverConfig.hasOption(Keys.STOP)) {
                serverOptions.setAction(Keys.STOP);
                String[] values = serverConfig.getOptionValue(Keys.STOP).split(" ");
                if(values != null && values.length > 0) {
                    serverOptions.setSocketNumber(Integer.parseInt(values[0])); 
                }
                if(values != null && values.length >= 1) {
                    serverOptions.setStopPassword(values[1].toCharArray()); 
                }
            } else {
                serverOptions.setAction("start");
            }

            if (serverConfig.hasOption(Keys.CONTEXT)) {
                serverOptions.setContextPath(serverConfig.getOptionValue(Keys.CONTEXT));
            }

            if (serverConfig.g("web").getOptionValue(Keys.HOST) != null) {
                serverConfig.put(Keys.HOST, serverConfig.g("web").getOptionValue(Keys.HOST));
            }
            if (serverConfig.hasOption(Keys.HOST)) {
                serverOptions.setHost(serverConfig.getOptionValue(Keys.HOST));
            }

            if (serverConfig.g("web").g("http").getOptionValue(Keys.PORT) != null) {
                serverConfig.put(Keys.PORT, serverConfig.g("web").g("http").getOptionValue(Keys.PORT));
            }
            if (serverConfig.hasOption(Keys.PORT)) {
                serverOptions.setPortNumber(((Number)serverConfig.getParsedOptionValue(Keys.PORT)).intValue());
            }

            if (serverConfig.g("web").g("ajp").getOptionValue(Keys.PORT) != null) {
                serverConfig.put(Keys.AJPPORT, serverConfig.g("web").g("ajp").getOptionValue(Keys.PORT));
            }
            if (serverConfig.hasOption(Keys.AJPPORT)) {
                serverOptions.setEnableHTTP(false)
                    .setEnableAJP(true).setAJPPort(((Number)serverConfig.getParsedOptionValue(Keys.AJPPORT)).intValue());
            }

            if (serverConfig.g("web").g("ssl").getOptionValue(Keys.PORT) != null) {
                serverConfig.put(Keys.SSLPORT, serverConfig.g("web").g("ssl").getOptionValue(Keys.PORT));
            }
            if (serverConfig.hasOption(Keys.SSLPORT)) {
                serverOptions.setEnableHTTP(false).setSecureCookies(true).setEnableSSL(true).setSSLPort(((Number)serverConfig.getParsedOptionValue(Keys.SSLPORT)).intValue());
            }

            if (serverConfig.g("web").g("ssl").getOptionValue("certFile") != null) {
                serverOptions.setSSLCertificate(getFile(serverConfig.g("web").g("ssl").getOptionValue("certFile")));
                if (!serverConfig.g("web").g("ssl").hasOption("keyFile") || !serverConfig.g("web").g("ssl").hasOption("keyPass")) {
                    throw new RuntimeException("Using a SSL certificate requires -sslkey /path/to/file and -sslkeypass pass**** arguments!");   
                }
            }
            if (serverConfig.hasOption(Keys.SSLCERT)) {
                serverOptions.setSSLCertificate(getFile(serverConfig.getOptionValue(Keys.SSLCERT)));
                if (!serverConfig.hasOption(Keys.SSLKEY) || !serverConfig.hasOption(Keys.SSLKEY)) {
                    throw new RuntimeException("Using a SSL certificate requires -sslkey /path/to/file and -sslkeypass pass**** arguments!");   
                }
            }

            if (serverConfig.g("web").g("ssl").getOptionValue("certFile") != null) {
                serverConfig.put(Keys.SSLKEY, serverConfig.g("web").g("ssl").getOptionValue("keyFile"));
            }
            if (serverConfig.hasOption(Keys.SSLKEY)) {
                serverOptions.setSSLKey(getFile(serverConfig.getOptionValue(Keys.SSLKEY)));
            }

            if (serverConfig.g("web").g("ssl").getOptionValue("keyPass") != null) {
                serverConfig.put(Keys.SSLKEYPASS, serverConfig.g("web").g("ssl").getOptionValue("keyPass"));
            }
            if (serverConfig.hasOption(Keys.SSLKEYPASS)) {
                serverOptions.setSSLKeyPass(serverConfig.getOptionValue(Keys.SSLKEYPASS).toCharArray());
            }

            if (serverConfig.g("web").g("ajp").hasOption("enable")) {
                serverConfig.put(Keys.AJPENABLE, serverConfig.g("web").g("ajp").getOptionValue("enable"));
            }
            if (serverConfig.hasOption(Keys.AJPENABLE)) {
                serverOptions.setEnableAJP(Boolean.valueOf(serverConfig.getOptionValue(Keys.AJPENABLE)));
            }

            if (serverConfig.g("web").g("ssl").hasOption("enable")) {
                serverConfig.put(Keys.SSLENABLE, serverConfig.g("web").g("ssl").getOptionValue("enable"));
            }
            if (serverConfig.hasOption(Keys.SSLENABLE)) {
                serverOptions.setEnableHTTP(false).setEnableSSL(Boolean.valueOf(serverConfig.getOptionValue(Keys.SSLENABLE)));
            }

            if (serverConfig.g("web").g("http").hasOption("enable")) {
                serverConfig.put(Keys.HTTPENABLE, serverConfig.g("web").g("http").getOptionValue("enable"));
            }
            if (serverConfig.hasOption(Keys.HTTPENABLE)) {
                serverOptions.setEnableHTTP(Boolean.valueOf(serverConfig.getOptionValue(Keys.HTTPENABLE)));
            }
            
            if (serverConfig.g("web").g("rewrites").hasOption(Keys.CONFIG)) {
                serverConfig.put(Keys.URLREWRITEFILE, serverConfig.g("web").g("rewrites").getOptionValue(Keys.CONFIG));
            }
            if (serverConfig.hasOption(Keys.URLREWRITEFILE)) {
                serverOptions.setURLRewriteFile(getFile(serverConfig.getOptionValue(Keys.URLREWRITEFILE)));
            }

            if (serverConfig.g("web").g("rewrites").hasOption("enable")) {
                serverConfig.put(Keys.URLREWRITEENABLE, serverConfig.g("web").g("rewrites").getOptionValue("enable"));
            }
            if (serverConfig.hasOption(Keys.URLREWRITEENABLE)) {
                serverOptions.setEnableURLRewrite(Boolean.valueOf(serverConfig.getOptionValue(Keys.URLREWRITEENABLE)));
            }

            if (serverConfig.g("web").g("rewrites").hasOption("configReloadSeconds")) {
                serverConfig.put(Keys.URLREWRITECHECK, serverConfig.g("web").g("rewrites").getOptionValue("configReloadSeconds"));
            }
            if (serverConfig.hasOption(Keys.URLREWRITECHECK) && serverConfig.getOptionValue(Keys.URLREWRITECHECK).length() > 0) {
                serverOptions.setURLRewriteCheckInterval(serverConfig.getOptionValue(Keys.URLREWRITECHECK));
            }

            if (serverConfig.g("web").g("rewrites").hasOption("statusPath")) {
                serverConfig.put(Keys.URLREWRITESTATUSPATH, serverConfig.g("web").g("rewrites").getOptionValue("statusPath"));
            }
            if (serverConfig.hasOption(Keys.URLREWRITESTATUSPATH) && serverConfig.getOptionValue(Keys.URLREWRITESTATUSPATH).length() > 0) {
                serverOptions.setURLRewriteStatusPath(serverConfig.getOptionValue(Keys.URLREWRITESTATUSPATH));
            }

            if (serverConfig.g("app").hasOption(Keys.LOGDIR)) {
                serverConfig.put(Keys.LOGDIR, serverConfig.g("app").getOptionValue(Keys.LOGDIR));
            }
            if (serverConfig.hasOption(Keys.LOGDIR) || serverConfig.g("app").getOptionValue(Keys.LOGDIR) != null) {
                if(serverConfig.hasOption(Keys.LOGDIR)) {
                    serverOptions.setLogDir(serverConfig.getOptionValue(Keys.LOGDIR));
                } else {
                    serverOptions.setLogDir(serverConfig.g("app").getOptionValue(Keys.LOGDIR));
                }
            } else {
                if(serverOptions.getWarFile() != null){
                    File warFile = serverOptions.getWarFile();
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
                    serverOptions.setLogDir(logDir);
                }
            }
            if(serverOptions.getWarFile() != null){
                serverOptions.setCfmlDirs(serverOptions.getWarFile().getAbsolutePath());
            }
            if (serverConfig.g("web").get("aliases") != null) {
                final StringBuilder dirs = new StringBuilder();
                serverConfig.g("web").get("aliases").forEach( (alias,path)-> 
                    dirs.append(alias + "=" + path + "," ) 
                );
                serverOptions.setCfmlDirs(dirs.toString().replaceAll(",$", ""));
            }
            if (serverConfig.hasOption(Keys.DIRS)) {
                serverOptions.setCfmlDirs(serverConfig.getOptionValue(Keys.DIRS));
            }
            if (serverConfig.hasOption(Keys.LOGREQUESTSBASENAME)) {
                serverOptions.logRequestsEnable(true);
                serverOptions.setLogRequestsBaseFileName(serverConfig.getOptionValue(Keys.LOGREQUESTSBASENAME));
            }
            if (serverConfig.hasOption(Keys.LOGREQUESTSDIR)) {
                serverOptions.logRequestsEnable(true);
                serverOptions.setLogRequestsDir(getFile(serverConfig.getOptionValue(Keys.LOGREQUESTSDIR)));
            }
            if (serverConfig.hasOption(Keys.LOGREQUESTS)) {
                serverOptions.logRequestsEnable(Boolean.valueOf(serverConfig.getOptionValue(Keys.LOGREQUESTS)));
            }
            if (serverConfig.hasOption(Keys.LOGACCESSBASENAME)) {
                serverOptions.logAccessEnable(true);
                serverOptions.setLogAccessBaseFileName(serverConfig.getOptionValue(Keys.LOGACCESSBASENAME));
            }
            if (serverConfig.hasOption(Keys.LOGACCESSDIR)) {
                serverOptions.logAccessEnable(true);
                serverOptions.setLogAccessDir(getFile(serverConfig.getOptionValue(Keys.LOGACCESSDIR)));
            }
            if (serverConfig.hasOption(Keys.LOGACCESS)) {
                serverOptions.logAccessEnable(Boolean.valueOf(serverConfig.getOptionValue(Keys.LOGACCESS)));
            }
            
            if (serverConfig.hasOption("openBrowser")) {
                serverConfig.put(Keys.OPENBROWSER, serverConfig.getOptionValue("openBrowser"));
            }
            if (serverConfig.hasOption(Keys.OPENBROWSER)) {
                serverOptions.setOpenbrowser(Boolean.valueOf(serverConfig.getOptionValue(Keys.OPENBROWSER)));
            }
            if (serverConfig.hasOption("openBrowserURL")) {
                serverConfig.put(Keys.OPENURL, serverConfig.getOptionValue("openBrowserURL"));
            }
            if (serverConfig.hasOption(Keys.OPENURL)) {
                serverOptions.setOpenbrowserURL(serverConfig.getOptionValue(Keys.OPENURL));
            }

            if (serverConfig.hasOption(Keys.PIDFILE)) {
                serverOptions.setPidFile(serverConfig.getOptionValue(Keys.PIDFILE));
            }

            if (serverConfig.hasOption(Keys.PROCESSNAME)) {
                serverOptions.setProcessName(serverConfig.getOptionValue(Keys.PROCESSNAME));
            }

            if (serverConfig.hasOption(Keys.TRAY)) {
                serverOptions.setTrayEnabled(Boolean.valueOf(serverConfig.getOptionValue(Keys.TRAY)));
            }

            if (serverConfig.hasOption("trayicon")) {
                serverConfig.put(Keys.ICON, serverConfig.getOptionValue("trayicon"));
            }
            if (serverConfig.hasOption(Keys.ICON)) {
                serverOptions.setIconImage(serverConfig.getOptionValue(Keys.ICON));
            }
            
            if (serverConfig.hasOption("trayOptions")) {
                serverOptions.setTrayConfig(serverConfig.getJSONArray("trayOptions"));
            }

            if (serverConfig.hasOption(Keys.TRAYCONFIG)) {
                serverOptions.setTrayConfig(getFile(serverConfig.getOptionValue(Keys.TRAYCONFIG)));
            }

            if (serverConfig.hasOption(Keys.STATUSFILE)) {
                serverOptions.setStatusFile(getFile(serverConfig.getOptionValue(Keys.STATUSFILE)));
            }
            

            if (serverConfig.hasOption(Keys.CFENGINE)) {
            	serverOptions.setCFEngineName(serverConfig.getOptionValue(Keys.CFENGINE));
            }
            if (serverConfig.hasOption(Keys.CFSERVERCONF)) {
                serverOptions.setCFMLServletConfigServerDir(serverConfig.getOptionValue(Keys.CFSERVERCONF));
            }
            if (serverConfig.hasOption(Keys.CFWEBCONF)) {
                serverOptions.setCFMLServletConfigWebDir(serverConfig.getOptionValue(Keys.CFWEBCONF));
            }
            if (serverConfig.hasOption(Keys.DIRECTORYINDEX)) {
                serverOptions.setDirectoryListingEnabled(Boolean.valueOf(serverConfig.getOptionValue(Keys.DIRECTORYINDEX)));
            }
            if (serverConfig.hasOption(Keys.CACHE)) {
                serverOptions.setCacheEnabled(Boolean.valueOf(serverConfig.getOptionValue(Keys.CACHE)));
            }
            if (serverConfig.hasOption(Keys.CUSTOMSTATUS)) {
                serverOptions.setCustomHTTPStatusEnabled(Boolean.valueOf(serverConfig.getOptionValue(Keys.CUSTOMSTATUS)));
            }
            if (serverConfig.hasOption(Keys.TRANSFERMINSIZE)) {
                serverOptions.setTransferMinSize(Long.valueOf(serverConfig.getOptionValue(Keys.TRANSFERMINSIZE)));
            }
            if (serverConfig.hasOption(Keys.SENDFILE)) {
                serverOptions.setSendfileEnabled(Boolean.valueOf(serverConfig.getOptionValue(Keys.SENDFILE)));
            }
            if (serverConfig.hasOption(Keys.GZIP)) {
                serverOptions.setGzipEnabled(Boolean.valueOf(serverConfig.getOptionValue(Keys.GZIP)));
            }
            if (serverConfig.hasOption(Keys.MARIADB4J)) {
                serverOptions.setMariaDB4jEnabled(Boolean.valueOf(serverConfig.getOptionValue(Keys.MARIADB4J)));
            }
            if (serverConfig.hasOption(Keys.MARIADB4JPORT) && serverConfig.getOptionValue(Keys.MARIADB4JPORT).length() > 0) {
                serverOptions.setMariaDB4jPort(Integer.valueOf(serverConfig.getOptionValue(Keys.MARIADB4JPORT)));
            }
            if (serverConfig.hasOption(Keys.MARIADB4JBASEDIR) && serverConfig.getOptionValue(Keys.MARIADB4JBASEDIR).length() > 0) {
                serverOptions.setMariaDB4jBaseDir(new File(serverConfig.getOptionValue(Keys.MARIADB4JBASEDIR)));
            }
            if (serverConfig.hasOption(Keys.MARIADB4JDATADIR) && serverConfig.getOptionValue(Keys.MARIADB4JDATADIR).length() > 0) {
                serverOptions.setMariaDB4jDataDir(new File(serverConfig.getOptionValue(Keys.MARIADB4JDATADIR)));
            }
            if (serverConfig.hasOption(Keys.MARIADB4JIMPORT) && serverConfig.getOptionValue(Keys.MARIADB4JIMPORT).length() > 0) {
                serverOptions.setMariaDB4jImportSQLFile(new File(serverConfig.getOptionValue(Keys.MARIADB4JIMPORT)));
            }
            if (serverConfig.hasOption(Keys.JVMARGS) && serverConfig.getOptionValue(Keys.JVMARGS).length() > 0) {
                List<String> jvmArgs = new ArrayList<String>();
                String[] jvmArgArray = serverConfig.getOptionValue(Keys.JVMARGS).split("(?<!\\\\);");
                for(String arg : jvmArgArray) {
                    jvmArgs.add(arg.replaceAll("\\\\;", ";"));
                }
                serverOptions.setJVMArgs(jvmArgs);
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
                    serverOptions.setErrorPages(serverConfig.getOptionValue(Keys.ERRORPAGES));
                } catch (Exception e) {
                    log.error("Could not parse errorPages:" + serverConfig.getOptionValue(Keys.ERRORPAGES));
                }
            }

            if (serverConfig.hasOption(Keys.SERVLETREST) && serverConfig.getOptionValue(Keys.SERVLETREST).length() > 0) {
                serverOptions.setServletRestEnabled(Boolean.valueOf(serverConfig.getOptionValue(Keys.SERVLETREST)));
            }

            if (serverConfig.g("app").hasOption("restMappings")) {
                serverConfig.put(Keys.SERVLETRESTMAPPINGS, serverConfig.g("app").getOptionValue("restMappings"));
            }
            if (serverConfig.hasOption(Keys.SERVLETRESTMAPPINGS) && serverConfig.getOptionValue(Keys.SERVLETRESTMAPPINGS).length() > 0) {
                serverOptions.setServletRestMappings(serverConfig.getOptionValue(Keys.SERVLETRESTMAPPINGS));
            }

            if (serverConfig.hasOption(Keys.FILTERPATHINFO) && serverConfig.getOptionValue(Keys.FILTERPATHINFO).length() > 0) {
                serverOptions.setFilterPathInfoEnabled(Boolean.valueOf(serverConfig.getOptionValue(Keys.FILTERPATHINFO)));
            }

            if (serverConfig.hasOption(Keys.SSLADDCERTS) && serverConfig.getOptionValue(Keys.SSLADDCERTS).length() > 0) {
                serverOptions.setSSLAddCerts(serverConfig.getOptionValue(Keys.SSLADDCERTS));
            }

            if (serverConfig.g("web").g(Keys.BASICAUTHENABLE).hasOption("enable")) {
                serverConfig.put(Keys.BASICAUTHENABLE,serverConfig.g("web").g(Keys.BASICAUTHENABLE).getOptionValue("enable"));
            }

            if (serverConfig.hasOption(Keys.BASICAUTHENABLE)) {
                serverOptions.setEnableBasicAuth((Boolean.valueOf(serverConfig.getOptionValue(Keys.BASICAUTHENABLE))));
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
                    serverOptions.setEnableBasicAuth(true);
                }
                serverOptions.setBasicAuth(serverConfig.getOptionValue(Keys.BASICAUTHENABLE));
            }
            if (serverConfig.hasOption(Keys.BUFFERSIZE) && serverConfig.getOptionValue(Keys.BUFFERSIZE).length() > 0) {
                serverOptions.setBufferSize(Integer.valueOf(serverConfig.getOptionValue(Keys.BUFFERSIZE)));
            }
            if (serverConfig.hasOption(Keys.IOTHREADS) && serverConfig.getOptionValue(Keys.IOTHREADS).length() > 0) {
                serverOptions.setIoThreads(Integer.valueOf(serverConfig.getOptionValue(Keys.IOTHREADS)));
            }
            if (serverConfig.hasOption(Keys.WORKERTHREADS) && serverConfig.getOptionValue(Keys.WORKERTHREADS).length() > 0) {
                serverOptions.setWorkerThreads(Integer.valueOf(serverConfig.getOptionValue(Keys.WORKERTHREADS)));
            }
            if (serverConfig.hasOption(Keys.DIRECTBUFFERS)) {
                serverOptions.setDirectBuffers(Boolean.valueOf(serverConfig.getOptionValue(Keys.DIRECTBUFFERS)));
            }
            if (serverConfig.hasOption(Keys.LOADBALANCE) && serverConfig.getOptionValue(Keys.LOADBALANCE).length() > 0) {
                serverOptions.setLoadBalance(serverConfig.getOptionValue(Keys.LOADBALANCE));
            }
            if (serverConfig.hasOption(Keys.DIRECTORYREFRESH) && serverConfig.getOptionValue(Keys.DIRECTORYREFRESH).length() > 0) {
                serverOptions.setDirectoryListingRefreshEnabled(Boolean.valueOf(serverConfig.getOptionValue(Keys.DIRECTORYREFRESH)));
            }
            if (serverConfig.hasOption(Keys.PROXYPEERADDRESS) && serverConfig.getOptionValue(Keys.PROXYPEERADDRESS).length() > 0) {
                serverOptions.setProxyPeerAddressEnabled(Boolean.valueOf(serverConfig.getOptionValue(Keys.PROXYPEERADDRESS)));
            }
            if (serverConfig.hasOption(Keys.HTTP2) && serverConfig.getOptionValue(Keys.HTTP2).length() > 0) {
                if(!serverConfig.hasOption(Keys.SECURECOOKIES)) {
                    serverOptions.setSecureCookies(true);
                }
                serverOptions.setHTTP2Enabled(Boolean.valueOf(serverConfig.getOptionValue(Keys.HTTP2)));
            }

            if (serverConfig.hasOption(Keys.SECURECOOKIES) && serverConfig.getOptionValue(Keys.SECURECOOKIES).length() > 0) {
                serverOptions.setSecureCookies(Boolean.valueOf(serverConfig.getOptionValue(Keys.SECURECOOKIES)));
            }

            
            if(serverOptions.getLoglevel().equals(Keys.DEBUG)) {
                Iterator<String> optionsIterator = serverConfig.getOptions().iterator();
                while(optionsIterator.hasNext()) {
                    log.debug(optionsIterator.next());
                }
            }
        }
        return serverOptions;
    }
    
    private void printUsage(String string, int exitCode) {
//        CommandLineHandler.printUsage(string, exitCode);
    }

    private File getFile(String filePath) {
        return CommandLineHandler.getFile(serverOptions.getConfigFile().getParentFile().getAbsolutePath() + '/' + filePath);
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
            return jsonConfig.containsKey(key) && jsonConfig.get(key).toString().length() > 0;
        }
    }

}
