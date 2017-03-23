package runwar.options;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ServerOptions {
	private String serverName = "default", processName = "RunWAR", loglevel = "WARN";
    private String host = "127.0.0.1", contextPath = "/";
    private int portNumber = 8088, ajpPort = 8009, sslPort = 443, socketNumber = 8779;
    private boolean enableAJP = false, enableSSL = false, enableHTTP = true, enableURLRewrite = false;
    private boolean debug = false, isBackground = true, keepRequestLog = false, openbrowser = false;
    private String pidFile, openbrowserURL, cfmlDirs, libDirs = null;
    private int launchTimeout = 50 * 1000; // 50 secs
    private URL jarURL = null;
    private File warFile, webXmlFile, logDir, urlRewriteFile, trayConfig, statusFile = null;
    private String iconImage = null;
    private String cfmlServletConfigWebDir = null, cfmlServletConfigServerDir = null;
    private boolean directoryListingEnabled = true;
    private boolean directoryListingRefreshEnabled = false;
    private boolean cacheEnabled = false;
    private String[] welcomeFiles;
	private File sslCertificate, sslKey, configFile;
	private char[] sslKeyPass = null;
	private char[] stopPassword = "klaatuBaradaNikto".toCharArray();
	private String action;
	private String cfengineName = "lucee";
	private boolean customHTTPStatusEnabled = true;
	private boolean gzipEnabled = false;
	private Long transferMinSize = (long) 100;
	private boolean mariadb4jEnabled = false;
	private int mariadb4jPort = 13306;
	private File mariadb4jBaseDir, mariadb4jDataDir, mariadb4jImportSQLFile = null;
	private List<String> jvmArgs = null;
	private Map<Integer, String> errorPages = null;
    private boolean servletRestEnabled = true;
    private String[] servletRestMappings = { "/rest" };
    private boolean filterPathInfoEnabled = true;
    private String[] sslAddCerts = null;
    private String[] cmdlineArgs = null;
    private String[] loadBalance = null;
    private static Map<String,String> userPasswordList;
    private boolean enableBasicAuth = false;
    private boolean directBuffers = true;
    int bufferSize,ioThreads,workerThreads = 0;

    static {
        userPasswordList = new HashMap<String, String>();
        userPasswordList.put("bob", "12345");
        userPasswordList.put("alice", "secret");
    }

    public ServerOptions setCommandLineArgs(String[] args) {
        this.cmdlineArgs = args;
        return this;
    }

    public String[] getCommandLineArgs() {
        // TODO: totally refactor argument handling so we can serialize and not muck around like this.
        List<String> argarray = new ArrayList<String>();
        for (String arg : cmdlineArgs) {
            if (arg.contains("background") || arg.startsWith("-b") || arg.contains("balance") 
                    || arg.startsWith("--port") || arg.startsWith("-p")
                    || arg.startsWith("--stop-port") || arg.contains("stopsocket")) {
                continue;
            } else {
                argarray.add(arg);
            }
        }
        argarray.add("--background");
        argarray.add(Boolean.toString(isBackground()));
        argarray.add("--port");
        argarray.add(Integer.toString(getPortNumber()));
        argarray.add("--stop-port");
        argarray.add(Integer.toString(getSocketNumber()));
        return argarray.toArray(new String[argarray.size()]);
    }

    public String getServerName() {
        return serverName;
    }
    public ServerOptions setServerName(String serverName) {
        this.serverName = serverName;
        return this;
    }
    public String getLoglevel() {
        return loglevel;
    }
    public ServerOptions setLoglevel(String loglevel) {
        this.loglevel = loglevel.toUpperCase();
        return this;
    }
    public String getContextPath() {
        return contextPath;
    }
    public File getConfigFile() {
        return configFile;
    }
    public ServerOptions setConfigFile(File file) {
        this.configFile = file;
        return this;
    }
    public ServerOptions setContextPath(String contextPath) {
        this.contextPath = contextPath;
        return this;
    }
    public String getHost() {
        return host;
    }
    public ServerOptions setHost(String host) {
        this.host = host;
        return this;
    }
    public int getPortNumber() {
        return portNumber;
    }
    public ServerOptions setPortNumber(int portNumber) {
        this.portNumber = portNumber;
        return this;
    }
    public int getAJPPort() {
        return ajpPort;
    }
    public ServerOptions setAJPPort(int ajpPort) {
        this.ajpPort = ajpPort;
        return this;
    }
    public int getSSLPort() {
    	return sslPort;
    }
    public ServerOptions setSSLPort(int sslPort) {
    	this.sslPort = sslPort;
        return this;
    }
    public boolean isEnableSSL() {
        return enableSSL;
    }
    public ServerOptions setEnableSSL(boolean enableSSL) {
        this.enableSSL = enableSSL;
        return this;
    }
    public boolean isEnableHTTP() {
    	return enableHTTP;
    }
    public ServerOptions setEnableHTTP(boolean bool) {
    	this.enableHTTP = bool;
        return this;
    }
    public boolean isURLRewriteApacheFormat() {
        return getURLRewriteFile() == null ? false : getURLRewriteFile().getPath().endsWith(".htaccess");
    }
    public boolean isEnableURLRewrite() {
        return enableURLRewrite;
    }
    public ServerOptions setEnableURLRewrite(boolean bool) {
        this.enableURLRewrite = bool;
        return this;
    }
    public ServerOptions setURLRewriteFile(File file) {
        this.urlRewriteFile = file;
        return this;
    }
    public File getURLRewriteFile() {
        return this.urlRewriteFile;
    }
    public int getSocketNumber() {
        return socketNumber;
    }
    public ServerOptions setSocketNumber(int socketNumber) {
        this.socketNumber = socketNumber;
        return this;
    }
    public File getLogDir() {
        return logDir;
    }
    public ServerOptions setLogDir(String logDir) {
        if(logDir!= null && logDir.length() > 0)
        this.logDir = new File(logDir);
        return this;
    }
    public ServerOptions setLogDir(File logDir) {
        this.logDir = logDir;
        return this;
    }
    public String getCfmlDirs() {
        return cfmlDirs;
    }
    public ServerOptions setCfmlDirs(String cfmlDirs) {
        this.cfmlDirs = cfmlDirs;
        return this;
    }
    public boolean isBackground() {
        return isBackground;
    }
    public ServerOptions setBackground(boolean isBackground) {
        this.isBackground = isBackground;
        return this;
    }
    public boolean isKeepRequestLog() {
        return keepRequestLog;
    }
    public ServerOptions setKeepRequestLog(boolean keepRequestLog) {
        this.keepRequestLog = keepRequestLog;
        return this;
    }
    public boolean isOpenbrowser() {
        return openbrowser;
    }
    public ServerOptions setOpenbrowser(boolean openbrowser) {
        this.openbrowser = openbrowser;
        return this;
    }
    public String getOpenbrowserURL() {
        return openbrowserURL;
    }
    public ServerOptions setOpenbrowserURL(String openbrowserURL) {
        this.openbrowserURL = openbrowserURL;
        return this;
    }
    public String getPidFile() {
        return pidFile;
    }
    public ServerOptions setPidFile(String pidFile) {
        this.pidFile = pidFile;
        return this;
    }
    public boolean isEnableAJP() {
        return enableAJP;
    }
    public ServerOptions setEnableAJP(boolean enableAJP) {
        this.enableAJP = enableAJP;
        return this;
    }
    public int getLaunchTimeout() {
        return launchTimeout;
    }
    public ServerOptions setLaunchTimeout(int launchTimeout) {
        this.launchTimeout = launchTimeout;
        return this;
    }
    public String getProcessName() {
        return processName;
    }
    public ServerOptions setProcessName(String processName) {
        this.processName = processName;
        return this;
    }
    public String getLibDirs() {
        return libDirs;
    }
    public ServerOptions setLibDirs(String libDirs) {
        this.libDirs = libDirs;
        return this;
    }
    public URL getJarURL() {
        return jarURL;
    }
    public ServerOptions setJarURL(URL jarURL) {
        this.jarURL = jarURL;
        return this;
    }
    public boolean isDebug() {
        return debug;
    }
    public ServerOptions setDebug(boolean debug) {
        this.debug = debug;
        return this;
    }
    public File getWarFile() {
        return warFile;
    }
    public ServerOptions setWarFile(File warFile) {
        this.warFile = warFile;
        return this;
    }
    public File getWebXmlFile() {
        return webXmlFile;
    }
    public String getWebXmlPath() throws MalformedURLException {
        return webXmlFile.toURI().toURL().toString();
    }
    public ServerOptions setWebXmlFile(File webXmlFile) {
        this.webXmlFile = webXmlFile;
        return this;
    }
    public String getIconImage() {
        return iconImage;
    }
    public ServerOptions setIconImage(String iconImage) {
        this.iconImage = iconImage;
        return this;
    }
    public File getTrayConfig() {
        return trayConfig;
    }
    public ServerOptions setTrayConfig(File trayConfig) {
        this.trayConfig = trayConfig;
        return this;
    }
    public File getStatusFile() {
        return statusFile;
    }
    public ServerOptions setStatusFile(File statusFile) {
        this.statusFile = statusFile;
        return this;
    }
    public String getCFMLServletConfigWebDir() {
        return cfmlServletConfigWebDir;
    }
    public ServerOptions setCFMLServletConfigWebDir(String cfmlServletConfigWebDir) {
        this.cfmlServletConfigWebDir = cfmlServletConfigWebDir;
        return this;
    }
    public String getCFMLServletConfigServerDir() {
        return cfmlServletConfigServerDir;
    }
    public ServerOptions setCFMLServletConfigServerDir(String cfmlServletConfigServerDir) {
        this.cfmlServletConfigServerDir = cfmlServletConfigServerDir;
        return this;
    }
    public boolean isCacheEnabled() {
    	return cacheEnabled;
    }
    public ServerOptions setCacheEnabled(boolean cacheEnabled) {
    	this.cacheEnabled = cacheEnabled;
    	return this;
    }
    public boolean isDirectoryListingEnabled() {
        return directoryListingEnabled;
    }
    public ServerOptions setDirectoryListingEnabled(boolean directoryListingEnabled) {
        this.directoryListingEnabled = directoryListingEnabled;
        return this;
    }
    public boolean isDirectoryListingRefreshEnabled() {
        return directoryListingRefreshEnabled;
    }
    public ServerOptions setDirectoryListingRefreshEnabled(boolean directoryListingRefreshEnabled) {
        this.directoryListingRefreshEnabled = directoryListingRefreshEnabled;
        return this;
    }
    public String[] getWelcomeFiles() {
        return welcomeFiles;
    }
    public ServerOptions setWelcomeFiles(String[] welcomeFiles) {
        this.welcomeFiles = welcomeFiles;
        return this;
    }
    public String getWarPath() throws MalformedURLException {
        return getWarFile().toURI().toURL().toString();
    }
	public ServerOptions setSSLCertificate(File file) {
		this.sslCertificate = file;
        return this;
	}
	public File getSSLCertificate() {
		return this.sslCertificate;
	}
	public ServerOptions setSSLKey(File file) {
		this.sslKey = file;
        return this;
	}
	public File getSSLKey() {
		return this.sslKey;
	}
	public ServerOptions setSSLKeyPass(char[] pass) {
		this.sslKeyPass = pass;
        return this;
	}
	public char[] getSSLKeyPass() {
		return this.sslKeyPass;
	}
	public ServerOptions setStopPassword(char[] password) {
		this.stopPassword = password;
		return this;
	}
	public char[] getStopPassword() {
		return this.stopPassword;
	}
	public ServerOptions setAction(String action) {
		this.action = action;
		return this;
	}
	public String getAction() {
		return this.action;
	}
	public ServerOptions setCFEngineName(String cfengineName) {
	    if(cfengineName.toLowerCase().equals("lucee") 
	            || cfengineName.toLowerCase().equals("adobe")
	            || cfengineName.toLowerCase().equals("railo")
	            || cfengineName.toLowerCase().equals("")) {
	        this.cfengineName = cfengineName.toLowerCase();
	    } else {
	        throw new RuntimeException("Unknown engine type: " + cfengineName + ", must be one of: lucee, adobe, railo");
	    }
		return this;
	}
	public String getCFEngineName() {
		return this.cfengineName ;
	}
 
	public ServerOptions setCustomHTTPStatusEnabled(boolean enabled) {
		this.customHTTPStatusEnabled = enabled;
		return this;
	}
	public boolean isCustomHTTPStatusEnabled() {
		return this.customHTTPStatusEnabled;
	}

    public ServerOptions setSendfileEnabled(boolean enabled) {
        if(!enabled) {
            this.transferMinSize = Long.MAX_VALUE;
        }
        return this;
    }
    public ServerOptions setTransferMinSize(Long minSize) {
        this.transferMinSize = minSize;
        return this;
    }

    public Long getTransferMinSize() {
        return this.transferMinSize;
    }

    public ServerOptions setGzipEnabled(boolean enabled) {
        this.gzipEnabled = enabled;
        return this;
    }
    public boolean isGzipEnabled() {
        return this.gzipEnabled;
    }

    public ServerOptions setMariaDB4jEnabled(boolean enabled) {
        this.mariadb4jEnabled = enabled;
        return this;
    }
    public boolean isMariaDB4jEnabled() {
        return this.mariadb4jEnabled;
    }
    public ServerOptions setMariaDB4jPort(int port) {
        this.mariadb4jPort = port;
        return this;
    }
    public int getMariaDB4jPort() {
        return this.mariadb4jPort;
    }
    public ServerOptions setMariaDB4jBaseDir(File dir) {
        this.mariadb4jBaseDir= dir;
        return this;
    }
    public File getMariaDB4jBaseDir() {
        return this.mariadb4jBaseDir;
    }
    public ServerOptions setMariaDB4jDataDir(File dir) {
        this.mariadb4jDataDir= dir;
        return this;
    }
    public File getMariaDB4jDataDir() {
        return this.mariadb4jDataDir;
    }
    public ServerOptions setMariaDB4jImportSQLFile(File file) {
        this.mariadb4jImportSQLFile= file;
        return this;
    }
    public File getMariaDB4jImportSQLFile() {
        return this.mariadb4jImportSQLFile;
    }

    public ServerOptions setJVMArgs(List<String> args) {
        this.jvmArgs = args;
        return this;
    }
    public List<String> getJVMArgs() {
        return this.jvmArgs;
    }

    public ServerOptions setErrorPages(String errorpages) {
        this.errorPages = new HashMap<Integer, String>();
        String[] pageList = errorpages.split(",");
        for (int x = 0; x < pageList.length; x++) {
            String[] splitted = pageList[x].split("=");
            String location = "";
            int errorCode = 1;
            if (splitted.length == 1) {
                location = pageList[x].trim();
            } else {
                errorCode = Integer.parseInt(splitted[0].trim());
                location = splitted[1].trim();
            }
            //TODO: verify we don't need to do anything different if the WAR context is something other than "/".
            location = location.startsWith("/") ? location : "/" + location;
            errorPages.put(errorCode,location);
        }
        return this;
    }
    public ServerOptions setErrorPages(Map<Integer, String> errorpages) {
        this.errorPages = errorpages;
        return this;
    }
    public Map<Integer, String> getErrorPages() {
        return this.errorPages;
    }

    public ServerOptions setServletRestEnabled(boolean enabled) {
        this.servletRestEnabled = enabled;
        return this;
    }
    public boolean getServletRestEnabled() {
        return this.servletRestEnabled;
    }

    public ServerOptions setServletRestMappings(String mappings) {
        return setServletRestMappings(mappings.split(","));
    }
    public ServerOptions setServletRestMappings(String[] mappings) {
        this.servletRestMappings = mappings;
        return this;
    }
    public String[] getServletRestMappings() {
        return this.servletRestMappings;
    }

    
    public ServerOptions setFilterPathInfoEnabled(boolean enabled) {
        this.filterPathInfoEnabled = enabled;
        return this;
    }
    public boolean isFilterPathInfoEnabled() {
        return this.filterPathInfoEnabled;
    }

    public ServerOptions setEnableBasicAuth(boolean enable) {
        this.enableBasicAuth = enable;
        return this;
    }
    public boolean isEnableBasicAuth() {
        return this.enableBasicAuth;
    }
    public ServerOptions setBasicAuth(String userPasswordList) {
        HashMap<String,String> ups = new HashMap<String,String>();
        for(String up : userPasswordList.split("(?<!\\\\),")) {
            up = up.replace("\\,", ",");
            String u = up.split("(?<!\\\\)=")[0].replace("\\=", "=");
            String p = up.split("(?<!\\\\)=")[1].replace("\\=", "=");
            ups.put(u, p);
        }
        return setBasicAuth(ups);
    }
    public ServerOptions setBasicAuth(Map<String,String> userPassList) {
        userPasswordList = userPassList;
        return this;
    }
    public Map<String,String> getBasicAuth() {
        return userPasswordList;
    }

    public ServerOptions setSSLAddCerts(String sslCerts) {
        return setSSLAddCerts(sslCerts.split("(?<!\\\\),"));
    }
    public ServerOptions setSSLAddCerts(String[] sslCerts) {
        this.sslAddCerts = sslCerts;
        return this;
    }
    public String[] getSSLAddCerts() {
        return this.sslAddCerts;
    }
    
    public int getBufferSize() {
        return bufferSize;
    }
    public ServerOptions setBufferSize(int bufferSize) {
        this.bufferSize = bufferSize;
        return this;
    }
    public int getIoThreads() {
        return ioThreads;
    }
    public ServerOptions setIoThreads(int ioThreads) {
        this.ioThreads = ioThreads;
        return this;
    }
    public int getWorkerThreads() {
        return workerThreads;
    }
    public ServerOptions setWorkerThreads(int workerThreads) {
        this.workerThreads = workerThreads;
        return this;
    }
    public ServerOptions setDirectBuffers(boolean enable) {
        this.directBuffers = enable;
        return this;
    }
    public boolean isDirectBuffers() {
        return this.directBuffers;
    }

    public ServerOptions setLoadBalance(String hosts) {
        return setLoadBalance(hosts.split("(?<!\\\\),"));
    }
    public ServerOptions setLoadBalance(String[] hosts) {
        this.loadBalance = hosts;
        return this;
    }
    public String[] getLoadBalance() {
        return this.loadBalance;
    }


}