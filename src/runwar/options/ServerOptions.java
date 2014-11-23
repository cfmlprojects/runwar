package runwar.options;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

public class ServerOptions {
    private String loglevel = "WARN";
    private String host = "127.0.0.1", contextPath = "/";
    private int portNumber = 8088, ajpPort = 8009, sslPort = 443, socketNumber = 8779;
    private boolean enableAJP = false, enableSSL = false, enableHTTP = true;
    private File logDir;
    private String cfmlDirs;
    private boolean isBackground = true, keepRequestLog = false, openbrowser = false;
    private String openbrowserURL;
    private String pidFile;
    private int launchTimeout = 50 * 1000; // 50 secs
    private String processName = "RunWAR";
    private String libDirs = null;
    private URL jarURL = null;
    private boolean debug = false;
    private File warFile, webXmlFile;
    private String iconImage = null;
    private String railoConfigWebDir = null, railoConfigServerDir = null;
    private boolean directoryListingEnabled = true;
    private String[] welcomeFiles = new String[] { "index.cfm", "index.cfml", "default.cfm", "index.html", "index.htm",
            "default.html", "default.htm" };
	private File sslCertificate, sslKey;
	private char[] sslKeyPass;

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
    public String getRailoConfigWebDir() {
        return railoConfigWebDir;
    }
    public ServerOptions setRailoConfigWebDir(String railoConfigWebDir) {
        this.railoConfigWebDir = railoConfigWebDir;
        return this;
    }
    public String getRailoConfigServerDir() {
        return railoConfigServerDir;
    }
    public ServerOptions setRailoConfigServerDir(String railoConfigServerDir) {
        this.railoConfigServerDir = railoConfigServerDir;
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

}
