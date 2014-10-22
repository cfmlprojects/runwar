package runwar.options;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

public class ServerOptions {
    private String loglevel = "WARN";
    private String contextPath = "/";
    private String host = "127.0.0.1";
    private int portNumber = 8088;
    private int ajpPort = 8009;
    private int socketNumber = 8779;
    private File logDir;
    private String cfmlDirs;
    private boolean isBackground = true;
    private boolean keepRequestLog = false;
    private boolean openbrowser = false;
    private String openbrowserURL;
    private String pidFile;
    private boolean enableAJP;
    private int launchTimeout = 50 * 1000; // 50 secs
    private String processName = "RunWAR";
    private String libDirs = null;
    private URL jarURL = null;
    private boolean debug = false;
    private File warFile;
    private File webXmlFile;
    private String iconImage = null;
    private String railoConfigWebDir = null;
    private String railoConfigServerDir = null;
    private boolean directoryListingEnabled = true;
    private String[] welcomeFiles = new String[] { "index.cfm", "index.cfml", "default.cfm", "index.html", "index.htm",
            "default.html", "default.htm" };

    public String getLoglevel() {
        return loglevel;
    }
    public void setLoglevel(String loglevel) {
        this.loglevel = loglevel.toUpperCase();
    }
    public String getContextPath() {
        return contextPath;
    }
    public void setContextPath(String contextPath) {
        this.contextPath = contextPath;
    }
    public String getHost() {
        return host;
    }
    public void setHost(String host) {
        this.host = host;
    }
    public int getPortNumber() {
        return portNumber;
    }
    public void setPortNumber(int portNumber) {
        this.portNumber = portNumber;
    }
    public int getAjpPort() {
        return ajpPort;
    }
    public void setAjpPort(int ajpPort) {
        this.ajpPort = ajpPort;
    }
    public int getSocketNumber() {
        return socketNumber;
    }
    public void setSocketNumber(int socketNumber) {
        this.socketNumber = socketNumber;
    }
    public File getLogDir() {
        return logDir;
    }
    public void setLogDir(String logDir) {
        if(logDir!= null && logDir.length() > 0)
        this.logDir = new File(logDir);
    }
    public void setLogDir(File logDir) {
        this.logDir = logDir;
    }
    public String getCfmlDirs() {
        return cfmlDirs;
    }
    public void setCfmlDirs(String cfmlDirs) {
        this.cfmlDirs = cfmlDirs;
    }
    public boolean isBackground() {
        return isBackground;
    }
    public void setBackground(boolean isBackground) {
        this.isBackground = isBackground;
    }
    public boolean isKeepRequestLog() {
        return keepRequestLog;
    }
    public void setKeepRequestLog(boolean keepRequestLog) {
        this.keepRequestLog = keepRequestLog;
    }
    public boolean isOpenbrowser() {
        return openbrowser;
    }
    public void setOpenbrowser(boolean openbrowser) {
        this.openbrowser = openbrowser;
    }
    public String getOpenbrowserURL() {
        return openbrowserURL;
    }
    public void setOpenbrowserURL(String openbrowserURL) {
        this.openbrowserURL = openbrowserURL;
    }
    public String getPidFile() {
        return pidFile;
    }
    public void setPidFile(String pidFile) {
        this.pidFile = pidFile;
    }
    public boolean isEnableAJP() {
        return enableAJP;
    }
    public void setEnableAJP(boolean enableAJP) {
        this.enableAJP = enableAJP;
    }
    public int getLaunchTimeout() {
        return launchTimeout;
    }
    public void setLaunchTimeout(int launchTimeout) {
        this.launchTimeout = launchTimeout;
    }
    public String getProcessName() {
        return processName;
    }
    public void setProcessName(String processName) {
        this.processName = processName;
    }
    public String getLibDirs() {
        return libDirs;
    }
    public void setLibDirs(String libDirs) {
        this.libDirs = libDirs;
    }
    public URL getJarURL() {
        return jarURL;
    }
    public void setJarURL(URL jarURL) {
        this.jarURL = jarURL;
    }
    public boolean isDebug() {
        return debug;
    }
    public void setDebug(boolean debug) {
        this.debug = debug;
    }
    public File getWarFile() {
        return warFile;
    }
    public void setWarFile(File warFile) {
        this.warFile = warFile;
    }
    public File getWebXmlFile() {
        return webXmlFile;
    }
    public String getWebXmlPath() throws MalformedURLException {
        return webXmlFile.toURI().toURL().toString();
    }
    public void setWebXmlFile(File webXmlFile) {
        this.webXmlFile = webXmlFile;
    }
    public String getIconImage() {
        return iconImage;
    }
    public void setIconImage(String iconImage) {
        this.iconImage = iconImage;
    }
    public String getRailoConfigWebDir() {
        return railoConfigWebDir;
    }
    public void setRailoConfigWebDir(String railoConfigWebDir) {
        this.railoConfigWebDir = railoConfigWebDir;
    }
    public String getRailoConfigServerDir() {
        return railoConfigServerDir;
    }
    public void setRailoConfigServerDir(String railoConfigServerDir) {
        this.railoConfigServerDir = railoConfigServerDir;
    }
    public boolean isDirectoryListingEnabled() {
        return directoryListingEnabled;
    }
    public void setDirectoryListingEnabled(boolean directoryListingEnabled) {
        this.directoryListingEnabled = directoryListingEnabled;
    }
    public String[] getWelcomeFiles() {
        return welcomeFiles;
    }
    public void setWelcomeFiles(String[] welcomeFiles) {
        this.welcomeFiles = welcomeFiles;
    }
    public String getWarPath() throws MalformedURLException {
        return getWarFile().toURI().toURL().toString();
    }

}
