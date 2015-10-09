package runwar.options;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

public class ServerOptions {
	private String serverName = "default", processName = "RunWAR", loglevel = "WARN";
    private String host = "127.0.0.1", contextPath = "/";
    private int portNumber = 8088, ajpPort = 8009, sslPort = 443, socketNumber = 8779;
    private boolean enableAJP = false, enableSSL = false, enableHTTP = true, enableURLRewrite = false;
    private boolean debug = false, isBackground = true, keepRequestLog = false, openbrowser = false;
    private String pidFile, openbrowserURL, cfmlDirs, libDirs = null;
    private int launchTimeout = 50 * 1000; // 50 secs
    private URL jarURL = null;
    private File warFile, webXmlFile, logDir, urlRewriteFile, trayConfig = null;
    private String iconImage = null;
    private String cfmlServletConfigWebDir = null, cfmlServletConfigServerDir = null;
    private boolean directoryListingEnabled = true;
    private boolean cacheEnabled = false;
    private String[] welcomeFiles = new String[] { "index.cfm", "index.cfml", "default.cfm", "index.html", "index.htm",
            "default.html", "default.htm" };
	private File sslCertificate, sslKey, configFile;
	private char[] sslKeyPass;
	private char[] stopPassword = "klaatuBaradaNikto".toCharArray();
	private String action;
	private String cfengineName = "lucee";
	private boolean customHTTPStatusEnabled = true;

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
		this.cfengineName = cfengineName;
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

}
