package runwar.options;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import runwar.LaunchUtil;
import runwar.Server;
import runwar.logging.Logger;
import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import net.minidev.json.JSONValue;
import net.minidev.json.parser.ParseException;

public class ConfigParser {

    private static Logger log = Logger.getLogger("RunwarLogger");
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
            if (serverConfig.hasOption("help")) {
                printUsage("Options",0);
            }
            if (serverConfig.hasOption("loglevel")) {
                serverOptions.setLoglevel(serverConfig.getOptionValue("loglevel"));
            }

            if (serverConfig.hasOption("name")) {
                serverOptions.setServerName(serverConfig.getOptionValue("name"));
            }

            if (serverConfig.hasOption("debug")) {
                Boolean debug= Boolean.valueOf(serverConfig.getOptionValue("debug"));
                serverOptions.setDebug(debug);
                if(debug)serverOptions.setLoglevel("DEBUG");
                if(serverConfig.hasOption("loglevel")) {
                    System.out.println("Warning:  debug overrides loglevel (both are specified, setting level to " + serverOptions.getLoglevel() + ")");
                }
            }

            if (serverConfig.hasOption("trace")) {
                Boolean trace = Boolean.valueOf(serverConfig.getOptionValue("trace"));
                serverOptions.setDebug(trace);
                if(trace)serverOptions.setLoglevel("TRACE");
                if(serverConfig.hasOption("loglevel")) {
                    System.out.println("Warning:  trace overrides loglevel (both are specified, setting level to " + serverOptions.getLoglevel() + ")");
                }
            }
            
            if (serverConfig.hasOption("background")) {
                serverOptions.setBackground(Boolean.valueOf(serverConfig.getOptionValue("background")));
            }
            if (serverConfig.g("app").hasOption("libDirs")) {
                serverConfig.put("libs", serverConfig.g("app").getOptionValue("libDirs"));
            }
            if (serverConfig.hasOption("libs")) {
                String[] list = serverConfig.getOptionValue("libs").split(",");
                for (String path : list) {
                    File lib = new File(path);
                    if (!lib.exists() || !lib.isDirectory())
                        printUsage("No such lib directory "+path,1);
                }               
                serverOptions.setLibDirs(serverConfig.getOptionValue("libs"));
            }

            if (serverConfig.g("web").hasOption("welcomefiles")) {
                serverConfig.put("welcomefiles",serverConfig.g("web").getOptionValue("welcomeFiles"));
            }
            if (serverConfig.hasOption("welcomefiles")) {
                serverOptions.setWelcomeFiles(serverConfig.getOptionValue("welcomefiles").split(","));
            }

            if (serverConfig.hasOption("jar")) {
                 File jar = new File(serverConfig.getOptionValue("jar"));
                    try {
                        serverOptions.setJarURL(jar.toURI().toURL());
                        if (!jar.exists() || jar.isDirectory())
                            printUsage("No such jar "+jar,1);
                    } catch (MalformedURLException e) {
                        printUsage("No such jar "+jar,1);
                        e.printStackTrace();
                    }
            }
            
            if (serverConfig.hasOption("startTimeout")) {
                serverConfig.put("timeout",serverConfig.getOptionValue("startTimeout"));
            }
            if (serverConfig.hasOption("timeout")) {
                serverOptions.setLaunchTimeout(((Number)serverConfig.getParsedOptionValue("timeout")).intValue() * 1000);
            }
            if (serverConfig.hasOption("password")) {
                serverOptions.setStopPassword(serverConfig.getOptionValue("password").toCharArray());
            }
            if (serverConfig.hasOption("stopsocket")) {
                serverConfig.put("stop-port",serverConfig.getOptionValue("stopsocket"));
            }
            if (serverConfig.hasOption("stop-port")) {
                serverOptions.setSocketNumber(((Number)serverConfig.getParsedOptionValue("stop-port")).intValue());
            }

            if(serverConfig.g("web").getOptionValue("webroot") != null) {
                serverConfig.put("war",serverConfig.g("web").getOptionValue("webroot"));
            }
            if(serverConfig.g("app").getOptionValue("WARPath") != null) {
                serverConfig.put("war",serverConfig.g("app").getOptionValue("WARPath"));
            }
            if (serverConfig.hasOption("war")) {
                String warPath = serverConfig.getOptionValue("war");
                serverOptions.setWarFile(getFile(warPath));
            } else if (!serverConfig.hasOption("stop") && !serverConfig.hasOption("c") && serverOptions.getWarFile() == null) {
                printUsage("Must specify -war path/to/war, or -stop [-stop-socket]",1);
            } 
            if(serverConfig.hasOption("D")){
                final String[] properties = serverConfig.getOptionValue("D").split(" ");
                for (int i = 0; i < properties.length; i++) {
                    log.debugf("setting system property: %s", properties[i].toString()+'='+properties[i+1].toString());
                    System.setProperty(properties[i].toString(),properties[i+1].toString());
                    i++;
                }
            }

            if (serverConfig.g("app").hasOption("webXML")) {
                serverConfig.put("webxmlpath", serverConfig.g("app").getOptionValue("webXML"));
            }
            if (serverConfig.hasOption("webxmlpath")) {
                String webXmlPath = serverConfig.getOptionValue("webxmlpath");
                File webXmlFile = new File(webXmlPath);
                if(webXmlFile.exists()) {
                    serverOptions.setWebXmlFile(webXmlFile);
                } else {
                    throw new RuntimeException("Could not find web.xml! " + webXmlPath);
                }
            }

            if (serverConfig.hasOption("stop")) {
                serverOptions.setAction("stop");
                String[] values = serverConfig.getOptionValue("stop").split(" ");
                if(values != null && values.length > 0) {
                    serverOptions.setSocketNumber(Integer.parseInt(values[0])); 
                }
                if(values != null && values.length >= 1) {
                    serverOptions.setStopPassword(values[1].toCharArray()); 
                }
            } else {
                serverOptions.setAction("start");
            }

            if (serverConfig.hasOption("context")) {
                serverOptions.setContextPath(serverConfig.getOptionValue("context"));
            }

            if (serverConfig.g("web").getOptionValue("host") != null) {
                serverConfig.put("host", serverConfig.g("web").getOptionValue("host"));
            }
            if (serverConfig.hasOption("host")) {
                serverOptions.setHost(serverConfig.getOptionValue("host"));
            }

            if (serverConfig.g("web").g("http").getOptionValue("port") != null) {
                serverConfig.put("port", serverConfig.g("web").g("http").getOptionValue("port"));
            }
            if (serverConfig.hasOption("port")) {
                serverOptions.setPortNumber(((Number)serverConfig.getParsedOptionValue("port")).intValue());
            }

            if (serverConfig.g("web").g("ajp").getOptionValue("port") != null) {
                serverConfig.put("ajpport", serverConfig.g("web").g("ajp").getOptionValue("port"));
            }
            if (serverConfig.hasOption("ajpport")) {
                serverOptions.setEnableHTTP(false)
                    .setEnableAJP(true).setAJPPort(((Number)serverConfig.getParsedOptionValue("ajpport")).intValue());
            }

            if (serverConfig.g("web").g("ssl").getOptionValue("port") != null) {
                serverConfig.put("sslport", serverConfig.g("web").g("ssl").getOptionValue("port"));
            }
            if (serverConfig.hasOption("sslport")) {
                serverOptions.setEnableHTTP(false).setEnableSSL(true).setSSLPort(((Number)serverConfig.getParsedOptionValue("sslport")).intValue());
            }

            if (serverConfig.g("web").g("ssl").getOptionValue("certFile") != null) {
                serverOptions.setSSLCertificate(getFile(serverConfig.g("web").g("ssl").getOptionValue("certFile")));
                if (!serverConfig.g("web").g("ssl").hasOption("keyFile") || !serverConfig.g("web").g("ssl").hasOption("keyPass")) {
                    throw new RuntimeException("Using a SSL certificate requires -sslkey /path/to/file and -sslkeypass pass**** arguments!");   
                }
            }
            if (serverConfig.hasOption("sslcert")) {
                serverOptions.setSSLCertificate(getFile(serverConfig.getOptionValue("sslcert")));
                if (!serverConfig.hasOption("sslkey") || !serverConfig.hasOption("sslkey")) {
                    throw new RuntimeException("Using a SSL certificate requires -sslkey /path/to/file and -sslkeypass pass**** arguments!");   
                }
            }

            if (serverConfig.g("web").g("ssl").getOptionValue("certFile") != null) {
                serverConfig.put("sslkey", serverConfig.g("web").g("ssl").getOptionValue("keyFile"));
            }
            if (serverConfig.hasOption("sslkey")) {
                serverOptions.setSSLKey(getFile(serverConfig.getOptionValue("sslkey")));
            }

            if (serverConfig.g("web").g("ssl").getOptionValue("keyPass") != null) {
                serverConfig.put("sslkeypass", serverConfig.g("web").g("ssl").getOptionValue("keyPass"));
            }
            if (serverConfig.hasOption("sslkeypass")) {
                serverOptions.setSSLKeyPass(serverConfig.getOptionValue("sslkeypass").toCharArray());
            }

            if (serverConfig.g("web").g("ajp").hasOption("enable")) {
                serverConfig.put("enableajp", serverConfig.g("web").g("ajp").getOptionValue("enable"));
            }
            if (serverConfig.hasOption("enableajp")) {
                serverOptions.setEnableAJP(Boolean.valueOf(serverConfig.getOptionValue("enableajp")));
            }

            if (serverConfig.g("web").g("ssl").hasOption("enable")) {
                serverConfig.put("enablessl", serverConfig.g("web").g("ssl").getOptionValue("enable"));
            }
            if (serverConfig.hasOption("enablessl")) {
                serverOptions.setEnableHTTP(false).setEnableSSL(Boolean.valueOf(serverConfig.getOptionValue("enablessl")));
            }

            if (serverConfig.g("web").g("http").hasOption("enable")) {
                serverConfig.put("enablehttp", serverConfig.g("web").g("http").getOptionValue("enable"));
            }
            if (serverConfig.hasOption("enablehttp")) {
                serverOptions.setEnableHTTP(Boolean.valueOf(serverConfig.getOptionValue("enablehttp")));
            }
            
            if (serverConfig.g("web").g("rewrites").hasOption("config")) {
                serverConfig.put("urlrewritefile", serverConfig.g("web").g("rewrites").getOptionValue("config"));
            }
            if (serverConfig.hasOption("urlrewritefile")) {
                serverOptions.setURLRewriteFile(getFile(serverConfig.getOptionValue("urlrewritefile")));
            }

            if (serverConfig.g("web").g("rewrites").hasOption("enable")) {
                serverConfig.put("urlrewriteenable", serverConfig.g("web").g("rewrites").getOptionValue("enable"));
            }
            if (serverConfig.hasOption("urlrewriteenable")) {
                serverOptions.setEnableURLRewrite(Boolean.valueOf(serverConfig.getOptionValue("urlrewriteenable")));
            }

            if (serverConfig.g("web").g("rewrites").hasOption("configReloadSeconds")) {
                serverConfig.put("urlrewritecheck", serverConfig.g("web").g("rewrites").getOptionValue("configReloadSeconds"));
            }
            if (serverConfig.hasOption("urlrewritecheck") && serverConfig.getOptionValue("urlrewritecheck").length() > 0) {
                serverOptions.setURLRewriteCheckInterval(serverConfig.getOptionValue("urlrewritecheck"));
            }

            if (serverConfig.g("web").g("rewrites").hasOption("statusPath")) {
                serverConfig.put("urlrewritestatuspath", serverConfig.g("web").g("rewrites").getOptionValue("statusPath"));
            }
            if (serverConfig.hasOption("urlrewritestatuspath") && serverConfig.getOptionValue("urlrewritestatuspath").length() > 0) {
                serverOptions.setURLRewriteStatusPath(serverConfig.getOptionValue("urlrewritestatuspath"));
            }

            if (serverConfig.g("app").hasOption("logDir")) {
                serverConfig.put("logdir", serverConfig.g("app").getOptionValue("logDir"));
            }
            if (serverConfig.hasOption("logdir") || serverConfig.g("app").getOptionValue("logDir") != null) {
                if(serverConfig.hasOption("logdir")) {
                    serverOptions.setLogDir(serverConfig.getOptionValue("logdir"));
                } else {
                    serverOptions.setLogDir(serverConfig.g("app").getOptionValue("logDir"));
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
            if (serverConfig.hasOption("dirs")) {
                serverOptions.setCfmlDirs(serverConfig.getOptionValue("dirs"));
            }
            if (serverConfig.hasOption("requestlog")) {
                serverOptions.setKeepRequestLog(Boolean.valueOf(serverConfig.getOptionValue("requestlog")));
            }
            
            if (serverConfig.hasOption("openBrowser")) {
                serverOptions.setOpenbrowser(Boolean.valueOf(serverConfig.getOptionValue("openBrowser")));
            }
            if (serverConfig.hasOption("open-browser")) {
                serverOptions.setOpenbrowser(Boolean.valueOf(serverConfig.getOptionValue("open-browser")));
            }
            if (serverConfig.hasOption("open-url")) {
                serverOptions.setOpenbrowserURL(serverConfig.getOptionValue("open-url"));
            }
            if (serverConfig.hasOption("openBrowserURL")) {
                serverOptions.setOpenbrowserURL(serverConfig.getOptionValue("openBrowserURL"));
            }

            if (serverConfig.hasOption("pidfile")) {
                serverOptions.setPidFile(serverConfig.getOptionValue("pidfile"));
            }

            if (serverConfig.hasOption("processname")) {
                serverOptions.setProcessName(serverConfig.getOptionValue("processname"));
            }

            if (serverConfig.hasOption("tray")) {
                serverOptions.setTrayEnabled(Boolean.valueOf(serverConfig.getOptionValue("tray")));
            }

            if (serverConfig.hasOption("trayicon")) {
                serverConfig.put("icon", serverConfig.getOptionValue("trayicon"));
            }
            if (serverConfig.hasOption("icon")) {
                serverOptions.setIconImage(serverConfig.getOptionValue("icon"));
            }

            if (serverConfig.hasOption("trayconfig")) {
                serverOptions.setTrayConfig(getFile(serverConfig.getOptionValue("trayconfig")));
            }

            if (serverConfig.hasOption("trayOptions")) {
                serverOptions.setTrayConfig(serverConfig.getJSONArray("trayOptions"));
            }

            if (serverConfig.hasOption("statusfile")) {
                serverOptions.setStatusFile(getFile(serverConfig.getOptionValue("statusfile")));
            }
            

            if (serverConfig.hasOption("cfengine")) {
            	serverOptions.setCFEngineName(serverConfig.getOptionValue("cfengine"));
            }
            if (serverConfig.hasOption("cfserverconf")) {
                serverOptions.setCFMLServletConfigServerDir(serverConfig.getOptionValue("cfserverconf"));
            }
            if (serverConfig.hasOption("cfwebconf")) {
                serverOptions.setCFMLServletConfigWebDir(serverConfig.getOptionValue("cfwebconf"));
            }
            if (serverConfig.hasOption("directoryindex")) {
                serverOptions.setDirectoryListingEnabled(Boolean.valueOf(serverConfig.getOptionValue("directoryindex")));
            }
            if (serverConfig.hasOption("cache")) {
                serverOptions.setCacheEnabled(Boolean.valueOf(serverConfig.getOptionValue("cache")));
            }
            if (serverConfig.hasOption("customstatus")) {
                serverOptions.setCustomHTTPStatusEnabled(Boolean.valueOf(serverConfig.getOptionValue("customstatus")));
            }
            if (serverConfig.hasOption("transferminsize")) {
                serverOptions.setTransferMinSize(Long.valueOf(serverConfig.getOptionValue("transferminsize")));
            }
            if (serverConfig.hasOption("sendfile")) {
                serverOptions.setSendfileEnabled(Boolean.valueOf(serverConfig.getOptionValue("sendfile")));
            }
            if (serverConfig.hasOption("gzip")) {
                serverOptions.setGzipEnabled(Boolean.valueOf(serverConfig.getOptionValue("gzip")));
            }
            if (serverConfig.hasOption("mariadb4j")) {
                serverOptions.setMariaDB4jEnabled(Boolean.valueOf(serverConfig.getOptionValue("mariadb4j")));
            }
            if (serverConfig.hasOption("mariadb4jport") && serverConfig.getOptionValue("mariadb4jport").length() > 0) {
                serverOptions.setMariaDB4jPort(Integer.valueOf(serverConfig.getOptionValue("mariadb4jport")));
            }
            if (serverConfig.hasOption("mariadb4jbasedir") && serverConfig.getOptionValue("mariadb4jbasedir").length() > 0) {
                serverOptions.setMariaDB4jBaseDir(new File(serverConfig.getOptionValue("mariadb4jbasedir")));
            }
            if (serverConfig.hasOption("mariadb4jdatadir") && serverConfig.getOptionValue("mariadb4jdatadir").length() > 0) {
                serverOptions.setMariaDB4jDataDir(new File(serverConfig.getOptionValue("mariadb4jdatadir")));
            }
            if (serverConfig.hasOption("mariadb4jimport") && serverConfig.getOptionValue("mariadb4jimport").length() > 0) {
                serverOptions.setMariaDB4jImportSQLFile(new File(serverConfig.getOptionValue("mariadb4jimport")));
            }
            if (serverConfig.hasOption("jvmargs") && serverConfig.getOptionValue("jvmargs").length() > 0) {
                List<String> jvmArgs = new ArrayList<String>();
                String[] jvmArgArray = serverConfig.getOptionValue("jvmargs").split(";");
                for(String arg : jvmArgArray) {
                    jvmArgs.add(arg);
                }
                serverOptions.setJVMArgs(jvmArgs);
            }

            if (serverConfig.g("web").get("errorPages") != null) {
                final StringBuilder pages = new StringBuilder();
                serverConfig.g("web").get("errorPages").forEach( (code,path)-> 
                pages.append(code.toLowerCase().equals("default") ? path + "," : code + "=" + path + "," ) 
                );
                String pagesStr = pages.toString().replaceAll(",$", "");
                serverConfig.put("errorpages", pagesStr);
            }
            if (serverConfig.hasOption("errorpages")) {
                try {
                    serverOptions.setErrorPages(serverConfig.getOptionValue("errorpages"));
                } catch (Exception e) {
                    log.error("Could not parse errorPages:" + serverConfig.getOptionValue("errorpages"));
                }
            }

            if (serverConfig.hasOption("servletrest") && serverConfig.getOptionValue("servletrest").length() > 0) {
                serverOptions.setServletRestEnabled(Boolean.valueOf(serverConfig.getOptionValue("servletrest")));
            }

            if (serverConfig.g("app").hasOption("restMappings")) {
                serverConfig.put("servletrestmappings", serverConfig.g("app").getOptionValue("restMappings"));
            }
            if (serverConfig.hasOption("servletrestmappings") && serverConfig.getOptionValue("servletrestmappings").length() > 0) {
                serverOptions.setServletRestMappings(serverConfig.getOptionValue("servletrestmappings"));
            }

            if (serverConfig.hasOption("filterpathinfo") && serverConfig.getOptionValue("filterpathinfo").length() > 0) {
                serverOptions.setFilterPathInfoEnabled(Boolean.valueOf(serverConfig.getOptionValue("filterpathinfo")));
            }

            if (serverConfig.hasOption("ssladdcerts") && serverConfig.getOptionValue("ssladdcerts").length() > 0) {
                serverOptions.setSSLAddCerts(serverConfig.getOptionValue("ssladdcerts"));
            }

            if (serverConfig.g("web").g("basicAuth").hasOption("enable")) {
                serverConfig.put("basicauthenable",serverConfig.g("web").g("basicAuth").getOptionValue("enable"));
            }

            if (serverConfig.hasOption("basicauthenable")) {
                serverOptions.setEnableBasicAuth((Boolean.valueOf(serverConfig.getOptionValue("basicauthenable"))));
            }

            if (serverConfig.g("web").g("basicAuth").hasOption("users")) {
                final StringBuilder dirs = new StringBuilder();
                serverConfig.g("web").g("basicAuth").get("users").forEach( (user,pass)-> 
                    dirs.append(user + "=" + pass + "," ) 
                );
                serverConfig.put("basicauth", dirs.toString().replaceAll(",$", ""));
            }
            if (serverConfig.hasOption("basicauth") && serverConfig.getOptionValue("basicauth").length() > 0) {
                if(!serverConfig.hasOption("basicauthenable") || serverConfig.hasOption("basicauthenable") && Boolean.valueOf(serverConfig.getOptionValue("basicauthenable"))) {
                    serverOptions.setEnableBasicAuth(true);
                }
                serverOptions.setBasicAuth(serverConfig.getOptionValue("basicauth"));
            }
            if (serverConfig.hasOption("buffersize") && serverConfig.getOptionValue("buffersize").length() > 0) {
                serverOptions.setBufferSize(Integer.valueOf(serverConfig.getOptionValue("buffersize")));
            }
            if (serverConfig.hasOption("iothreads") && serverConfig.getOptionValue("iothreads").length() > 0) {
                serverOptions.setIoThreads(Integer.valueOf(serverConfig.getOptionValue("iothreads")));
            }
            if (serverConfig.hasOption("workerthreads") && serverConfig.getOptionValue("workerthreads").length() > 0) {
                serverOptions.setWorkerThreads(Integer.valueOf(serverConfig.getOptionValue("workerthreads")));
            }
            if (serverConfig.hasOption("directbuffers")) {
                serverOptions.setDirectBuffers(Boolean.valueOf(serverConfig.getOptionValue("directbuffers")));
            }
            if (serverConfig.hasOption("loadbalance") && serverConfig.getOptionValue("loadbalance").length() > 0) {
                serverOptions.setLoadBalance(serverConfig.getOptionValue("loadbalance"));
            }
            if (serverConfig.hasOption("directoryrefresh") && serverConfig.getOptionValue("directoryrefresh").length() > 0) {
                serverOptions.setDirectoryListingRefreshEnabled(Boolean.valueOf(serverConfig.getOptionValue("directoryrefresh")));
            }
            if (serverConfig.hasOption("proxypeeraddress") && serverConfig.getOptionValue("proxypeeraddress").length() > 0) {
                serverOptions.setProxyPeerAddressEnabled(Boolean.valueOf(serverConfig.getOptionValue("proxypeeraddress")));
            }
            if (serverConfig.hasOption("http2") && serverConfig.getOptionValue("http2").length() > 0) {
                serverOptions.setHTTP2Enabled(Boolean.valueOf(serverConfig.getOptionValue("http2")));
            }

            
            if(serverOptions.getLoglevel().equals("DEBUG")) {
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
