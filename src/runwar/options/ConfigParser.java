package runwar.options;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import runwar.LaunchUtil;
import runwar.Server;
import runwar.logging.Logger;
import net.minidev.json.JSONObject;
import net.minidev.json.JSONValue;
import net.minidev.json.parser.ParseException;

public class ConfigParser {

    private static Logger log = Logger.getLogger("RunwarLogger");
    private ServerOptions serverOptions;

    public ConfigParser(File config){
        parseOptions(config);
        if(serverOptions != null) {
            serverOptions.setConfigFile(config);
        }
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
            serverOptions = new ServerOptions();
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
                    System.out.println("Warning:  debug overrides loglevel (both are specified, setting level to DEBUG)");
                }
            }

            if (serverConfig.hasOption("background")) {
                serverOptions.setBackground(Boolean.valueOf(serverConfig.getOptionValue("background")));
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
            
            if (serverConfig.hasOption("timeout")) {
                serverOptions.setLaunchTimeout(((Number)serverConfig.getParsedOptionValue("timeout")).intValue() * 1000);
            }
            if (serverConfig.hasOption("password")) {
                serverOptions.setStopPassword(serverConfig.getOptionValue("password").toCharArray());
            }
            if (serverConfig.hasOption("stop-port")) {
                serverOptions.setSocketNumber(((Number)serverConfig.getParsedOptionValue("stop-port")).intValue());
            }
            if (serverConfig.hasOption("war")) {
                String warPath = serverConfig.getOptionValue("war");
                serverOptions.setWarFile(getFile(warPath));
            } else if (!serverConfig.hasOption("stop") && !serverConfig.hasOption("c")) {
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
            if (serverConfig.hasOption("host")) {
                serverOptions.setHost(serverConfig.getOptionValue("host"));
            }
            if (serverConfig.hasOption("port")) {
                serverOptions.setPortNumber(((Number)serverConfig.getParsedOptionValue("port")).intValue());
            }
            if (serverConfig.hasOption("ajpport")) {
                serverOptions.setEnableHTTP(false)
                    .setEnableAJP(true).setAJPPort(((Number)serverConfig.getParsedOptionValue("ajpport")).intValue());
            }
            if (serverConfig.hasOption("sslport")) {
                serverOptions.setEnableHTTP(false).setEnableSSL(true).setSSLPort(((Number)serverConfig.getParsedOptionValue("sslport")).intValue());
            }
            if (serverConfig.hasOption("sslcert")) {
                serverOptions.setSSLCertificate(getFile(serverConfig.getOptionValue("sslcert")));
                if (!serverConfig.hasOption("sslkey") || !serverConfig.hasOption("sslkey")) {
                    throw new RuntimeException("Using a SSL certificate requires -sslkey /path/to/file and -sslkeypass pass**** arguments!");   
                }
            }
            if (serverConfig.hasOption("sslkey")) {
                serverOptions.setSSLKey(getFile(serverConfig.getOptionValue("sslkey")));
            }
            if (serverConfig.hasOption("sslkeypass")) {
                serverOptions.setSSLKeyPass(serverConfig.getOptionValue("sslkeypass").toCharArray());
            }
            if (serverConfig.hasOption("enableajp")) {
                serverOptions.setEnableAJP(Boolean.valueOf(serverConfig.getOptionValue("enableajp")));
            }
            if (serverConfig.hasOption("enablessl")) {
                serverOptions.setEnableHTTP(false).setEnableSSL(Boolean.valueOf(serverConfig.getOptionValue("enablessl")));
            }
            if (serverConfig.hasOption("enablehttp")) {
                serverOptions.setEnableHTTP(Boolean.valueOf(serverConfig.getOptionValue("enablehttp")));
            }
            if (serverConfig.hasOption("urlrewritefile")) {
                serverOptions.setURLRewriteFile(getFile(serverConfig.getOptionValue("urlrewritefile")));
            }
            if (serverConfig.hasOption("urlrewriteenable")) {
                serverOptions.setEnableURLRewrite(Boolean.valueOf(serverConfig.getOptionValue("urlrewriteenable")));
            }
            if (serverConfig.hasOption("logdir")) {
                serverOptions.setLogDir(serverConfig.getOptionValue("logdir"));
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
            if (serverConfig.hasOption("dirs")) {
                serverOptions.setCfmlDirs(serverConfig.getOptionValue("dirs"));
            }
            if (serverConfig.hasOption("requestlog")) {
                serverOptions.setKeepRequestLog(Boolean.valueOf(serverConfig.getOptionValue("requestlog")));
            }
            
            if (serverConfig.hasOption("open-browser")) {
                serverOptions.setOpenbrowser(Boolean.valueOf(serverConfig.getOptionValue("open")));
            }
            if (serverConfig.hasOption("open-url")) {
                serverOptions.setOpenbrowserURL(serverConfig.getOptionValue("open-url"));
            }

            if (serverConfig.hasOption("pidfile")) {
                serverOptions.setPidFile(serverConfig.getOptionValue("pidfile"));
            }

            if (serverConfig.hasOption("processname")) {
                serverOptions.setProcessName(serverConfig.getOptionValue("processname"));
            }

            if (serverConfig.hasOption("trayconfig")) {
                serverOptions.setTrayConfig(getFile(serverConfig.getOptionValue("trayconfig")));
            }

            if (serverConfig.hasOption("statusfile")) {
                serverOptions.setStatusFile(getFile(serverConfig.getOptionValue("statusfile")));
            }
            
            if (serverConfig.hasOption("icon")) {
                serverOptions.setIconImage(serverConfig.getOptionValue("icon"));
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
            if (serverConfig.hasOption("errorpages")) {
                serverOptions.setErrorPages(serverConfig.getOptionValue("errorpages"));
            }
            if (serverConfig.hasOption("servletrest") && serverConfig.getOptionValue("servletrest").length() > 0) {
                serverOptions.setServletRestEnabled(Boolean.valueOf(serverConfig.getOptionValue("servletrest")));
            }
            if (serverConfig.hasOption("servletrestmappings") && serverConfig.getOptionValue("servletrestmappings").length() > 0) {
                serverOptions.setServletRestMappings(serverConfig.getOptionValue("servletrestmappings"));
            }

            if (serverConfig.hasOption("filterpathinfo") && serverConfig.getOptionValue("filterpathinfo").length() > 0) {
                serverOptions.setFilterPathInfoEnabled(Boolean.valueOf(serverConfig.getOptionValue("filterpathinfo")));
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
        CommandLineHandler.printUsage(string, exitCode);
    }

    private File getFile(String filePath) {
        return CommandLineHandler.getFile(filePath);
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
            if(hasOption(key)){
              return jsonConfig.get(key).toString();
            }
            return null;
        }

        public boolean hasOption(String key) {
            return jsonConfig.containsKey(key);
        }
    }

}
