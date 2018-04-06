package runwar.options;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minidev.json.JSONArray;
import runwar.Server;
import runwar.Server.Mode;

public class ServerOptionsImpl implements ServerOptions {
    private String serverName = "default", processName = "RunWAR", loglevel = "INFO";
    private String host = "127.0.0.1", contextPath = "/";
    private int portNumber = 8088, ajpPort = 8009, sslPort = 1443, socketNumber = 8779;
    private boolean enableAJP = false, enableSSL = false, enableHTTP = true, enableURLRewrite = false;
    private boolean debug = false, isBackground = true, logAccessEnable = false, logRequestsEnable = false, openbrowser = false;
    private String pidFile, openbrowserURL, cfmlDirs, logFileBaseName="server", logRequestBaseFileName="requests", logAccessBaseFileName="access", logSuffix=".txt", libDirs = null;
    private int launchTimeout = 50 * 1000; // 50 secs
    private URL jarURL = null;
    private File warFile, webInfDir, webXmlFile, logDir, logRequestsDir, logAccessDir, urlRewriteFile, urlRewriteLog, trayConfig, statusFile = null;
    private String iconImage = null;
    private String urlRewriteCheckInterval = null, urlRewriteStatusPath = null;
    private String cfmlServletConfigWebDir = null, cfmlServletConfigServerDir = null;
    private boolean trayEnabled = true;
    private boolean directoryListingEnabled = true;
    private boolean directoryListingRefreshEnabled = false;
    private boolean cacheEnabled = false;
    private String[] welcomeFiles;
    private File sslCertificate, sslKey, configFile;
    private char[] sslKeyPass = null;
    private char[] stopPassword = "klaatuBaradaNikto".toCharArray();
    private String action = "start";
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
    private static Map<String, String> userPasswordList;
    private boolean enableBasicAuth = false;
    private boolean directBuffers = true;
    int bufferSize, ioThreads, workerThreads = 0;
    private boolean proxyPeerAddressEnabled = false;
    private boolean http2enabled = false;
    private boolean secureCookies = false, cookieHttpOnly = false, cookieSecure = false;
    private JSONArray trayConfigJSON;

    static {
        userPasswordList = new HashMap<String, String>();
        userPasswordList.put("bob", "12345");
        userPasswordList.put("alice", "secret");
    }

    /*
     * (non-Javadoc)
     * 
     * @see runwar.options.ServerOptions#setCommandLineArgs(java.lang.String[])
     */
    @Override
    public ServerOptions setCommandLineArgs(String[] args) {
        this.cmdlineArgs = args;
        return this;
    }

    /*
     * (non-Javadoc)
     * 
     * @see runwar.options.ServerOptions#getCommandLineArgs()
     */
    @Override
    public String[] getCommandLineArgs() {
        // TODO: totally refactor argument handling so we can serialize and not
        // muck around like this.
        List<String> argarray = new ArrayList<String>();
        if (cmdlineArgs == null) {
            cmdlineArgs = "".split("");
            argarray.add("-war");
            argarray.add(getWarFile().getAbsolutePath());
        }
        for (String arg : cmdlineArgs) {
            if (arg.contains("background") || arg.startsWith("-b") || arg.contains("balance")
                    || arg.startsWith("--port") || arg.startsWith("-p") || arg.startsWith("--stop-port")
                    || arg.contains("stopsocket")) {
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

    /*
     * (non-Javadoc)
     * 
     * @see runwar.options.ServerOptions#getServerName()
     */
    @Override
    public String getServerName() {
        return serverName;
    }

    /*
     * (non-Javadoc)
     * 
     * @see runwar.options.ServerOptions#setServerName(java.lang.String)
     */
    @Override
    public ServerOptions setServerName(String serverName) {
        this.serverName = serverName;
        return this;
    }

    /*
     * (non-Javadoc)
     * 
     * @see runwar.options.ServerOptions#getLoglevel()
     */
    @Override
    public String getLoglevel() {
        return loglevel;
    }

    /*
     * (non-Javadoc)
     * 
     * @see runwar.options.ServerOptions#setLoglevel(java.lang.String)
     */
    @Override
    public ServerOptions setLoglevel(String loglevel) {
        this.loglevel = loglevel.toUpperCase();
        return this;
    }

    /*
     * (non-Javadoc)
     * 
     * @see runwar.options.ServerOptions#getContextPath()
     */
    @Override
    public String getContextPath() {
        return contextPath;
    }

    /*
     * (non-Javadoc)
     * 
     * @see runwar.options.ServerOptions#getConfigFile()
     */
    @Override
    public File getConfigFile() {
        return configFile;
    }

    /*
     * (non-Javadoc)
     * 
     * @see runwar.options.ServerOptions#setConfigFile(java.io.File)
     */
    @Override
    public ServerOptions setConfigFile(File file) {
        this.configFile = file;
        return this;
    }

    /*
     * (non-Javadoc)
     * 
     * @see runwar.options.ServerOptions#setContextPath(java.lang.String)
     */
    @Override
    public ServerOptions setContextPath(String contextPath) {
        this.contextPath = contextPath;
        return this;
    }

    /*
     * (non-Javadoc)
     * 
     * @see runwar.options.ServerOptions#getHost()
     */
    @Override
    public String getHost() {
        return host;
    }

    /*
     * (non-Javadoc)
     * 
     * @see runwar.options.ServerOptions#setHost(java.lang.String)
     */
    @Override
    public ServerOptions setHost(String host) {
        this.host = host;
        return this;
    }

    /*
     * (non-Javadoc)
     * 
     * @see runwar.options.ServerOptions#getPortNumber()
     */
    @Override
    public int getPortNumber() {
        return portNumber;
    }

    /*
     * (non-Javadoc)
     * 
     * @see runwar.options.ServerOptions#setPortNumber(int)
     */
    @Override
    public ServerOptions setPortNumber(int portNumber) {
        this.portNumber = portNumber;
        return this;
    }

    /*
     * (non-Javadoc)
     * 
     * @see runwar.options.ServerOptions#getAJPPort()
     */
    @Override
    public int getAJPPort() {
        return ajpPort;
    }

    /*
     * (non-Javadoc)
     * 
     * @see runwar.options.ServerOptions#setAJPPort(int)
     */
    @Override
    public ServerOptions setAJPPort(int ajpPort) {
        this.ajpPort = ajpPort;
        return this;
    }

    /*
     * (non-Javadoc)
     * 
     * @see runwar.options.ServerOptions#getSSLPort()
     */
    @Override
    public int getSSLPort() {
        return sslPort;
    }

    /*
     * (non-Javadoc)
     * 
     * @see runwar.options.ServerOptions#setSSLPort(int)
     */
    @Override
    public ServerOptions setSSLPort(int sslPort) {
        this.sslPort = sslPort;
        return this;
    }

    /*
     * (non-Javadoc)
     * 
     * @see runwar.options.ServerOptions#isEnableSSL()
     */
    @Override
    public boolean isEnableSSL() {
        return enableSSL;
    }

    /*
     * (non-Javadoc)
     * 
     * @see runwar.options.ServerOptions#setEnableSSL(boolean)
     */
    @Override
    public ServerOptions setEnableSSL(boolean enableSSL) {
        this.enableSSL = enableSSL;
        return this;
    }

    /*
     * (non-Javadoc)
     * 
     * @see runwar.options.ServerOptions#isEnableHTTP()
     */
    @Override
    public boolean isEnableHTTP() {
        return enableHTTP;
    }

    /*
     * (non-Javadoc)
     * 
     * @see runwar.options.ServerOptions#setEnableHTTP(boolean)
     */
    @Override
    public ServerOptions setEnableHTTP(boolean bool) {
        this.enableHTTP = bool;
        return this;
    }

    /*
     * (non-Javadoc)
     * 
     * @see runwar.options.ServerOptions#isURLRewriteApacheFormat()
     */
    @Override
    public boolean isURLRewriteApacheFormat() {
        return getURLRewriteFile() == null ? false : getURLRewriteFile().getPath().endsWith(".htaccess") || getURLRewriteFile().getPath().endsWith(".conf");
    }

    /*
     * (non-Javadoc)
     * 
     * @see runwar.options.ServerOptions#isEnableURLRewrite()
     */
    @Override
    public boolean isEnableURLRewrite() {
        return enableURLRewrite;
    }

    /*
     * (non-Javadoc)
     * 
     * @see runwar.options.ServerOptions#setEnableURLRewrite(boolean)
     */
    @Override
    public ServerOptions setEnableURLRewrite(boolean bool) {
        this.enableURLRewrite = bool;
        return this;
    }

    /*
     * (non-Javadoc)
     * 
     * @see runwar.options.ServerOptions#setURLRewriteFile(java.io.File)
     */
    @Override
    public ServerOptions setURLRewriteFile(File file) {
        this.urlRewriteFile = file;
        return this;
    }

    /*
     * (non-Javadoc)
     * 
     * @see runwar.options.ServerOptions#getURLRewriteFile()
     */
    @Override
    public File getURLRewriteFile() {
        return this.urlRewriteFile;
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see runwar.options.ServerOptions#setURLRewriteLog(java.io.File)
     */
    @Override
    public ServerOptions setURLRewriteLog(File file) {
        this.urlRewriteLog = file;
        return this;
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see runwar.options.ServerOptions#getURLRewriteLog()
     */
    @Override
    public File getURLRewriteLog() {
        return this.urlRewriteLog;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * runwar.options.ServerOptions#setURLRewriteCheckInterval(java.lang.String)
     */
    @Override
    public ServerOptions setURLRewriteCheckInterval(String interval) {
        this.urlRewriteCheckInterval = interval;
        return this;
    }

    /*
     * (non-Javadoc)
     * 
     * @see runwar.options.ServerOptions#getURLRewriteCheckInterval()
     */
    @Override
    public String getURLRewriteCheckInterval() {
        return this.urlRewriteCheckInterval;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * runwar.options.ServerOptions#setURLRewriteStatusPath(java.lang.String)
     */
    @Override
    public ServerOptions setURLRewriteStatusPath(String path) {
        if (!path.startsWith("/")) {
            path = '/' + path;
        }
        this.urlRewriteStatusPath = path;
        return this;
    }

    /*
     * (non-Javadoc)
     * 
     * @see runwar.options.ServerOptions#getURLRewriteStatusPath()
     */
    @Override
    public String getURLRewriteStatusPath() {
        return this.urlRewriteStatusPath;
    }

    /*
     * (non-Javadoc)
     * 
     * @see runwar.options.ServerOptions#getSocketNumber()
     */
    @Override
    public int getSocketNumber() {
        return socketNumber;
    }

    /*
     * (non-Javadoc)
     * 
     * @see runwar.options.ServerOptions#setSocketNumber(int)
     */
    @Override
    public ServerOptions setSocketNumber(int socketNumber) {
        this.socketNumber = socketNumber;
        return this;
    }

    /*
     * (non-Javadoc)
     * 
     * @see runwar.options.ServerOptions#hasLogDir()
     */
    @Override
    public boolean hasLogDir() {
        return logDir != null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see runwar.options.ServerOptions#getLogDir()
     */
    @Override
    public File getLogDir() {
        if (logDir == null) {
            String defaultLogDir = new File(Server.getThisJarLocation().getParentFile(), "./.logs/")
                    .getAbsolutePath();
            logDir = new File(defaultLogDir);
            if (getWarFile() != null) {
                File warFile = getWarFile();
                if (warFile.isDirectory() && new File(warFile, "WEB-INF").exists()) {
                    defaultLogDir = warFile.getPath() + "/WEB-INF/logs/";
                } else if (getCFEngineName().length() != 0) {
                    String serverConfigDir = getCFMLServletConfigServerDir();
                    if (serverConfigDir != null) {
                        defaultLogDir = new File(serverConfigDir, "log/").getAbsolutePath();
                    }
                }
                logDir = new File(defaultLogDir);
            }
        }
        assert logDir != null;
        return logDir;
    }

    /*
     * (non-Javadoc)
     * 
     * @see runwar.options.ServerOptions#setLogDir(java.lang.String)
     */
    @Override
    public ServerOptions setLogDir(String logDir) {
        if (logDir != null && logDir.length() > 0)
            this.logDir = new File(logDir);
        return this;
    }

    /*
     * (non-Javadoc)
     * 
     * @see runwar.options.ServerOptions#setLogDir(java.io.File)
     */
    @Override
    public ServerOptions setLogDir(File logDir) {
        this.logDir = logDir;
        return this;
    }

    /*
     * (non-Javadoc)
     * 
     * @see runwar.options.ServerOptions#setLogFileName(java.lang.String)
     */
    @Override
    public ServerOptions setLogFileName(String name) {
        this.logFileBaseName = name;
        return this;
    }

    /*
     * (non-Javadoc)
     * 
     * @see runwar.options.ServerOptions#setLogFileName()
     */
    @Override
    public String getLogFileName() {
        this.logFileBaseName = (this.logFileBaseName == null) ? "server." : this.logFileBaseName;
        return this.logFileBaseName;
    }

    /*
     * (non-Javadoc)
     * 
     * @see runwar.options.ServerOptions#setLogFileName(java.lang.String)
     */
    @Override
    public ServerOptions setLogSuffix(String suffix) {
        this.logSuffix = suffix;
        return this;
    }

    /*
     * (non-Javadoc)
     * 
     * @see runwar.options.ServerOptions#setLogFileName()
     */
    @Override
    public String getLogSuffix() {
        return this.logSuffix;
    }

    /*
     * (non-Javadoc)
     * 
     * @see runwar.options.ServerOptions#getCfmlDirs()
     */
    @Override
    public String getCfmlDirs() {
        if (cfmlDirs == null && getWarFile() != null) {
            setCfmlDirs(getWarFile().getAbsolutePath());
        }
        return cfmlDirs;
    }

    /*
     * (non-Javadoc)
     * 
     * @see runwar.options.ServerOptions#setCfmlDirs(java.lang.String)
     */
    @Override
    public ServerOptions setCfmlDirs(String cfmlDirs) {
        this.cfmlDirs = cfmlDirs;
        return this;
    }

    /*
     * (non-Javadoc)
     * 
     * @see runwar.options.ServerOptions#isBackground()
     */
    @Override
    public boolean isBackground() {
        return isBackground;
    }

    /*
     * (non-Javadoc)
     * 
     * @see runwar.options.ServerOptions#setBackground(boolean)
     */
    @Override
    public ServerOptions setBackground(boolean isBackground) {
        this.isBackground = isBackground;
        return this;
    }

    /*
     * (non-Javadoc)
     * 
     * @see runwar.options.ServerOptions#logRequestsEnable()
     */
    @Override
    public boolean logRequestsEnable() {
        return logRequestsEnable;
    }

    /*
     * (non-Javadoc)
     * 
     * @see runwar.options.ServerOptions#logRequestsEnable(boolean)
     */
    @Override
    public ServerOptions logRequestsEnable(boolean enable) {
        this.logRequestsEnable = enable;
        return this;
    }

    /*
     * (non-Javadoc)
     * 
     * @see runwar.options.ServerOptions#logRequestsEnable()
     */
    @Override
    public boolean logAccessEnable() {
        return logAccessEnable;
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see runwar.options.ServerOptions#logRequestsEnable(boolean)
     */
    @Override
    public ServerOptions logAccessEnable(boolean enable) {
        this.logAccessEnable = enable;
        return this;
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see runwar.options.ServerOptions#isKeepRequestLog()
     */
    @Override
    @Deprecated
    public boolean isKeepRequestLog() {
        return logRequestsEnable;
    }

    /*
     * (non-Javadoc)
     * 
     * @see runwar.options.ServerOptions#setKeepRequestLog(boolean)
     */
    @Override
    @Deprecated
    public ServerOptions setKeepRequestLog(boolean keepRequestLog) {
        this.logRequestsEnable = keepRequestLog;
        return this;
    }

    /*
     * (non-Javadoc)
     * 
     * @see runwar.options.ServerOptions#setLogRequestsDir(java.io.File)
     */
    @Override
    public ServerOptions setLogRequestsDir(File logDir) {
        this.logRequestsDir = logDir;
        return this;
    }
    @Override
    public ServerOptions setLogRequestsDir(String logDir) {
        this.logRequestsDir = new File(logDir);
        return this;
    }
    @Override
    public File getLogRequestsDir() {
        if(this.logRequestsDir == null)
            return getLogDir();
        return this.logRequestsDir;
    }

    /*
     * (non-Javadoc)
     * 
     * @see runwar.options.ServerOptions#setLogRequestsBaseFileName(java.lang.String)
     */
    @Override
    public ServerOptions setLogRequestsBaseFileName(String name) {
        this.logRequestBaseFileName= name;
        return this;
    }

    /*
     * (non-Javadoc)
     * 
     * @see runwar.options.ServerOptions#getLogRequestsBaseFileName()
     */
    @Override
    public String getLogRequestsBaseFileName() {
        return this.logRequestBaseFileName;
    }

    /*
     * (non-Javadoc)
     * 
     * @see runwar.options.ServerOptions#setLogAccessDir(java.io.File)
     */
    @Override
    public ServerOptions setLogAccessDir(File logDir) {
        this.logAccessDir = logDir;
        return this;
    }
    @Override
    public ServerOptions setLogAccessDir(String logDir) {
        this.logAccessDir = new File(logDir);
        return this;
    }
    @Override
    public File getLogAccessDir() {
        if(this.logAccessDir == null)
            return getLogDir();
        return this.logAccessDir;
    }

    /*
     * (non-Javadoc)
     * 
     * @see runwar.options.ServerOptions#setLogAccessBaseFileName(java.lang.String)
     */
    @Override
    public ServerOptions setLogAccessBaseFileName(String name) {
        this.logAccessBaseFileName = name;
        return this;
    }

    /*
     * (non-Javadoc)
     * 
     * @see runwar.options.ServerOptions#getLogAccessBaseFileName()
     */
    @Override
    public String getLogAccessBaseFileName() {
        return this.logAccessBaseFileName;
    }
    /*
     * (non-Javadoc)
     * 
     * @see runwar.options.ServerOptions#isOpenbrowser()
     */
    @Override
    public boolean isOpenbrowser() {
        return openbrowser;
    }

    /*
     * (non-Javadoc)
     * 
     * @see runwar.options.ServerOptions#setOpenbrowser(boolean)
     */
    @Override
    public ServerOptions setOpenbrowser(boolean openbrowser) {
        this.openbrowser = openbrowser;
        return this;
    }

    /*
     * (non-Javadoc)
     * 
     * @see runwar.options.ServerOptions#getOpenbrowserURL()
     */
    @Override
    public String getOpenbrowserURL() {
        return openbrowserURL;
    }

    /*
     * (non-Javadoc)
     * 
     * @see runwar.options.ServerOptions#setOpenbrowserURL(java.lang.String)
     */
    @Override
    public ServerOptions setOpenbrowserURL(String openbrowserURL) {
        this.openbrowserURL = openbrowserURL;
        return this;
    }

    /*
     * (non-Javadoc)
     * 
     * @see runwar.options.ServerOptions#getPidFile()
     */
    @Override
    public String getPidFile() {
        return pidFile;
    }

    /*
     * (non-Javadoc)
     * 
     * @see runwar.options.ServerOptions#setPidFile(java.lang.String)
     */
    @Override
    public ServerOptions setPidFile(String pidFile) {
        this.pidFile = pidFile;
        return this;
    }

    /*
     * (non-Javadoc)
     * 
     * @see runwar.options.ServerOptions#isEnableAJP()
     */
    @Override
    public boolean isEnableAJP() {
        return enableAJP;
    }

    /*
     * (non-Javadoc)
     * 
     * @see runwar.options.ServerOptions#setEnableAJP(boolean)
     */
    @Override
    public ServerOptions setEnableAJP(boolean enableAJP) {
        this.enableAJP = enableAJP;
        return this;
    }

    /*
     * (non-Javadoc)
     * 
     * @see runwar.options.ServerOptions#getLaunchTimeout()
     */
    @Override
    public int getLaunchTimeout() {
        return launchTimeout;
    }

    /*
     * (non-Javadoc)
     * 
     * @see runwar.options.ServerOptions#setLaunchTimeout(int)
     */
    @Override
    public ServerOptions setLaunchTimeout(int launchTimeout) {
        this.launchTimeout = launchTimeout;
        return this;
    }

    /*
     * (non-Javadoc)
     * 
     * @see runwar.options.ServerOptions#getProcessName()
     */
    @Override
    public String getProcessName() {
        return processName;
    }

    /*
     * (non-Javadoc)
     * 
     * @see runwar.options.ServerOptions#setProcessName(java.lang.String)
     */
    @Override
    public ServerOptions setProcessName(String processName) {
        this.processName = processName;
        return this;
    }

    /*
     * (non-Javadoc)
     * 
     * @see runwar.options.ServerOptions#getLibDirs()
     */
    @Override
    public String getLibDirs() {
        return libDirs;
    }

    /*
     * (non-Javadoc)
     * 
     * @see runwar.options.ServerOptions#setLibDirs(java.lang.String)
     */
    @Override
    public ServerOptions setLibDirs(String libDirs) {
        this.libDirs = libDirs;
        return this;
    }

    /*
     * (non-Javadoc)
     * 
     * @see runwar.options.ServerOptions#getJarURL()
     */
    @Override
    public URL getJarURL() {
        return jarURL;
    }

    /*
     * (non-Javadoc)
     * 
     * @see runwar.options.ServerOptions#setJarURL(java.net.URL)
     */
    @Override
    public ServerOptions setJarURL(URL jarURL) {
        this.jarURL = jarURL;
        return this;
    }

    /*
     * (non-Javadoc)
     * 
     * @see runwar.options.ServerOptions#isDebug()
     */
    @Override
    public boolean isDebug() {
        return debug;
    }

    /*
     * (non-Javadoc)
     * 
     * @see runwar.options.ServerOptions#setDebug(boolean)
     */
    @Override
    public ServerOptions setDebug(boolean debug) {
        this.debug = debug;
        if (debug && loglevel == "WARN") {
            loglevel = "DEBUG";
        }
        return this;
    }

    /*
     * (non-Javadoc)
     * 
     * @see runwar.options.ServerOptions#getWarFile()
     */
    @Override
    public File getWarFile() {
        return warFile;
    }

    /*
     * (non-Javadoc)
     * 
     * @see runwar.options.ServerOptions#setWarFile(java.io.File)
     */
    @Override
    public ServerOptions setWarFile(File warFile) {
        this.warFile = warFile;
        return this;
    }

    /*
     * (non-Javadoc)
     * 
     * @see runwar.options.ServerOptions#getWebInfDir()
     */
    @Override
    public File getWebInfDir() {
        if(webInfDir == null) {
            if (webXmlFile != null && (webXmlFile.getParentFile().getName().equalsIgnoreCase("WEB-INF") || new File(webXmlFile.getParentFile(), "lib").exists())) {
                webInfDir = webXmlFile.getParentFile();
            } else if(getWarFile() != null && warFile.exists() && warFile.isDirectory()) {
                webInfDir = new File(warFile, "WEB-INF");
            }
        }
        return webInfDir;
    }

    /*
     * (non-Javadoc)
     * 
     * @see runwar.options.ServerOptions#setWebInfDir(java.io.File)
     */
    @Override
    public ServerOptions setWebInfDir(File webInfDir) {
        this.webInfDir = webInfDir;
        return this;
    }

    /*
     * (non-Javadoc)
     * 
     * @see runwar.options.ServerOptions#getWebXmlFile()
     */
    @Override
    public File getWebXmlFile() {
        if(webXmlFile == null && getWebInfDir() != null) {
            setWebXmlFile(new File(getWebInfDir(),"web.xml"));
        }
        return webXmlFile;
    }

    /*
     * (non-Javadoc)
     * 
     * @see runwar.options.ServerOptions#getWebXmlPath()
     */
    @Override
    public String getWebXmlPath() throws MalformedURLException {
        return webXmlFile.toURI().toURL().toString();
    }

    /*
     * (non-Javadoc)
     * 
     * @see runwar.options.ServerOptions#setWebXmlFile(java.io.File)
     */
    @Override
    public ServerOptions setWebXmlFile(File webXmlFile) {
        this.webXmlFile = webXmlFile;
        return this;
    }

    /*
     * (non-Javadoc)
     * 
     * @see runwar.options.ServerOptions#getIconImage()
     */
    @Override
    public String getIconImage() {
        return iconImage;
    }

    /*
     * (non-Javadoc)
     * 
     * @see runwar.options.ServerOptions#setIconImage(java.lang.String)
     */
    @Override
    public ServerOptions setIconImage(String iconImage) {
        this.iconImage = iconImage;
        return this;
    }

    /*
     * (non-Javadoc)
     * 
     * @see runwar.options.ServerOptions#getTrayConfig()
     */
    @Override
    public File getTrayConfig() {
        return trayConfig;
    }

    /*
     * (non-Javadoc)
     * 
     * @see runwar.options.ServerOptions#getTrayConfigJSON()
     */
    @Override
    public JSONArray getTrayConfigJSON() {
        return trayConfigJSON;
    }

    /*
     * (non-Javadoc)
     * 
     * @see runwar.options.ServerOptions#setTrayConfig(java.io.File)
     */
    @Override
    public ServerOptions setTrayConfig(File trayConfig) {
        this.trayConfig = trayConfig;
        return this;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * runwar.options.ServerOptions#setTrayConfig(net.minidev.json.JSONArray)
     */
    @Override
    public ServerOptions setTrayConfig(JSONArray trayConfig) {
        this.trayConfigJSON = trayConfig;
        return this;
    }

    /*
     * (non-Javadoc)
     * 
     * @see runwar.options.ServerOptions#isTrayEnabled()
     */
    @Override
    public boolean isTrayEnabled() {
        return trayEnabled;
    }

    /*
     * (non-Javadoc)
     * 
     * @see runwar.options.ServerOptions#setTrayEnabled(boolean)
     */
    @Override
    public ServerOptions setTrayEnabled(boolean enabled) {
        this.trayEnabled = enabled;
        return this;
    }

    /*
     * (non-Javadoc)
     * 
     * @see runwar.options.ServerOptions#getStatusFile()
     */
    @Override
    public File getStatusFile() {
        return statusFile;
    }

    /*
     * (non-Javadoc)
     * 
     * @see runwar.options.ServerOptions#setStatusFile(java.io.File)
     */
    @Override
    public ServerOptions setStatusFile(File statusFile) {
        this.statusFile = statusFile;
        return this;
    }

    /*
     * (non-Javadoc)
     * 
     * @see runwar.options.ServerOptions#getCFMLServletConfigWebDir()
     */
    @Override
    public String getCFMLServletConfigWebDir() {
        return cfmlServletConfigWebDir;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * runwar.options.ServerOptions#setCFMLServletConfigWebDir(java.lang.String)
     */
    @Override
    public ServerOptions setCFMLServletConfigWebDir(String cfmlServletConfigWebDir) {
        this.cfmlServletConfigWebDir = cfmlServletConfigWebDir;
        return this;
    }

    /*
     * (non-Javadoc)
     * 
     * @see runwar.options.ServerOptions#getCFMLServletConfigServerDir()
     */
    @Override
    public String getCFMLServletConfigServerDir() {
        if (cfmlServletConfigServerDir == null)
            cfmlServletConfigServerDir = System.getProperty("cfml.server.config.dir");
        return cfmlServletConfigServerDir;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * runwar.options.ServerOptions#setCFMLServletConfigServerDir(java.lang.
     * String)
     */
    @Override
    public ServerOptions setCFMLServletConfigServerDir(String cfmlServletConfigServerDir) {
        this.cfmlServletConfigServerDir = cfmlServletConfigServerDir;
        return this;
    }

    /*
     * (non-Javadoc)
     * 
     * @see runwar.options.ServerOptions#isCacheEnabled()
     */
    @Override
    public boolean isCacheEnabled() {
        return cacheEnabled;
    }

    /*
     * (non-Javadoc)
     * 
     * @see runwar.options.ServerOptions#setCacheEnabled(boolean)
     */
    @Override
    public ServerOptions setCacheEnabled(boolean cacheEnabled) {
        this.cacheEnabled = cacheEnabled;
        return this;
    }

    /*
     * (non-Javadoc)
     * 
     * @see runwar.options.ServerOptions#isDirectoryListingEnabled()
     */
    @Override
    public boolean isDirectoryListingEnabled() {
        return directoryListingEnabled;
    }

    /*
     * (non-Javadoc)
     * 
     * @see runwar.options.ServerOptions#setDirectoryListingEnabled(boolean)
     */
    @Override
    public ServerOptions setDirectoryListingEnabled(boolean directoryListingEnabled) {
        this.directoryListingEnabled = directoryListingEnabled;
        return this;
    }

    /*
     * (non-Javadoc)
     * 
     * @see runwar.options.ServerOptions#isDirectoryListingRefreshEnabled()
     */
    @Override
    public boolean isDirectoryListingRefreshEnabled() {
        return directoryListingRefreshEnabled;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * runwar.options.ServerOptions#setDirectoryListingRefreshEnabled(boolean)
     */
    @Override
    public ServerOptions setDirectoryListingRefreshEnabled(boolean directoryListingRefreshEnabled) {
        this.directoryListingRefreshEnabled = directoryListingRefreshEnabled;
        return this;
    }

    /*
     * (non-Javadoc)
     * 
     * @see runwar.options.ServerOptions#getWelcomeFiles()
     */
    @Override
    public String[] getWelcomeFiles() {
        return welcomeFiles;
    }

    /*
     * (non-Javadoc)
     * 
     * @see runwar.options.ServerOptions#setWelcomeFiles(java.lang.String[])
     */
    @Override
    public ServerOptions setWelcomeFiles(String[] welcomeFiles) {
        this.welcomeFiles = welcomeFiles;
        return this;
    }

    /*
     * (non-Javadoc)
     * 
     * @see runwar.options.ServerOptions#getWarPath()
     */
    @Override
    public String getWarPath() {
        try {
            return getWarFile().toURI().toURL().toString();
        } catch (MalformedURLException e) {
            e.printStackTrace();
            return "";
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see runwar.options.ServerOptions#setSSLCertificate(java.io.File)
     */
    @Override
    public ServerOptions setSSLCertificate(File file) {
        this.sslCertificate = file;
        return this;
    }

    /*
     * (non-Javadoc)
     * 
     * @see runwar.options.ServerOptions#getSSLCertificate()
     */
    @Override
    public File getSSLCertificate() {
        return this.sslCertificate;
    }

    /*
     * (non-Javadoc)
     * 
     * @see runwar.options.ServerOptions#setSSLKey(java.io.File)
     */
    @Override
    public ServerOptions setSSLKey(File file) {
        this.sslKey = file;
        return this;
    }

    /*
     * (non-Javadoc)
     * 
     * @see runwar.options.ServerOptions#getSSLKey()
     */
    @Override
    public File getSSLKey() {
        return this.sslKey;
    }

    /*
     * (non-Javadoc)
     * 
     * @see runwar.options.ServerOptions#setSSLKeyPass(char[])
     */
    @Override
    public ServerOptions setSSLKeyPass(char[] pass) {
        this.sslKeyPass = pass;
        return this;
    }

    /*
     * (non-Javadoc)
     * 
     * @see runwar.options.ServerOptions#getSSLKeyPass()
     */
    @Override
    public char[] getSSLKeyPass() {
        return this.sslKeyPass;
    }

    /*
     * (non-Javadoc)
     * 
     * @see runwar.options.ServerOptions#setStopPassword(char[])
     */
    @Override
    public ServerOptions setStopPassword(char[] password) {
        this.stopPassword = password;
        return this;
    }

    /*
     * (non-Javadoc)
     * 
     * @see runwar.options.ServerOptions#getStopPassword()
     */
    @Override
    public char[] getStopPassword() {
        return this.stopPassword;
    }

    /*
     * (non-Javadoc)
     * 
     * @see runwar.options.ServerOptions#setAction(java.lang.String)
     */
    @Override
    public ServerOptions setAction(String action) {
        this.action = action;
        return this;
    }

    /*
     * (non-Javadoc)
     * 
     * @see runwar.options.ServerOptions#getAction()
     */
    @Override
    public String getAction() {
        return this.action;
    }

    /*
     * (non-Javadoc)
     * 
     * @see runwar.options.ServerOptions#setCFEngineName(java.lang.String)
     */
    @Override
    public ServerOptions setCFEngineName(String cfengineName) {
        if (cfengineName.toLowerCase().equals("lucee") || cfengineName.toLowerCase().equals("adobe")
                || cfengineName.toLowerCase().equals("railo") || cfengineName.toLowerCase().equals("")) {
            this.cfengineName = cfengineName.toLowerCase();
        } else {
            throw new RuntimeException(
                    "Unknown engine type: " + cfengineName + ", must be one of: lucee, adobe, railo");
        }
        return this;
    }

    /*
     * (non-Javadoc)
     * 
     * @see runwar.options.ServerOptions#getCFEngineName()
     */
    @Override
    public String getCFEngineName() {
        return this.cfengineName;
    }

    /*
     * (non-Javadoc)
     * 
     * @see runwar.options.ServerOptions#setCustomHTTPStatusEnabled(boolean)
     */
    @Override
    public ServerOptions setCustomHTTPStatusEnabled(boolean enabled) {
        this.customHTTPStatusEnabled = enabled;
        return this;
    }

    /*
     * (non-Javadoc)
     * 
     * @see runwar.options.ServerOptions#isCustomHTTPStatusEnabled()
     */
    @Override
    public boolean isCustomHTTPStatusEnabled() {
        return this.customHTTPStatusEnabled;
    }

    /*
     * (non-Javadoc)
     * 
     * @see runwar.options.ServerOptions#setSendfileEnabled(boolean)
     */
    @Override
    public ServerOptions setSendfileEnabled(boolean enabled) {
        if (!enabled) {
            this.transferMinSize = Long.MAX_VALUE;
        }
        return this;
    }

    /*
     * (non-Javadoc)
     * 
     * @see runwar.options.ServerOptions#setTransferMinSize(java.lang.Long)
     */
    @Override
    public ServerOptions setTransferMinSize(Long minSize) {
        this.transferMinSize = minSize;
        return this;
    }

    /*
     * (non-Javadoc)
     * 
     * @see runwar.options.ServerOptions#getTransferMinSize()
     */
    @Override
    public Long getTransferMinSize() {
        return this.transferMinSize;
    }

    /*
     * (non-Javadoc)
     * 
     * @see runwar.options.ServerOptions#setGzipEnabled(boolean)
     */
    @Override
    public ServerOptions setGzipEnabled(boolean enabled) {
        this.gzipEnabled = enabled;
        return this;
    }

    /*
     * (non-Javadoc)
     * 
     * @see runwar.options.ServerOptions#isGzipEnabled()
     */
    @Override
    public boolean isGzipEnabled() {
        return this.gzipEnabled;
    }

    /*
     * (non-Javadoc)
     * 
     * @see runwar.options.ServerOptions#setMariaDB4jEnabled(boolean)
     */
    @Override
    public ServerOptions setMariaDB4jEnabled(boolean enabled) {
        this.mariadb4jEnabled = enabled;
        return this;
    }

    /*
     * (non-Javadoc)
     * 
     * @see runwar.options.ServerOptions#isMariaDB4jEnabled()
     */
    @Override
    public boolean isMariaDB4jEnabled() {
        return this.mariadb4jEnabled;
    }

    /*
     * (non-Javadoc)
     * 
     * @see runwar.options.ServerOptions#setMariaDB4jPort(int)
     */
    @Override
    public ServerOptions setMariaDB4jPort(int port) {
        this.mariadb4jPort = port;
        return this;
    }

    /*
     * (non-Javadoc)
     * 
     * @see runwar.options.ServerOptions#getMariaDB4jPort()
     */
    @Override
    public int getMariaDB4jPort() {
        return this.mariadb4jPort;
    }

    /*
     * (non-Javadoc)
     * 
     * @see runwar.options.ServerOptions#setMariaDB4jBaseDir(java.io.File)
     */
    @Override
    public ServerOptions setMariaDB4jBaseDir(File dir) {
        this.mariadb4jBaseDir = dir;
        return this;
    }

    /*
     * (non-Javadoc)
     * 
     * @see runwar.options.ServerOptions#getMariaDB4jBaseDir()
     */
    @Override
    public File getMariaDB4jBaseDir() {
        return this.mariadb4jBaseDir;
    }

    /*
     * (non-Javadoc)
     * 
     * @see runwar.options.ServerOptions#setMariaDB4jDataDir(java.io.File)
     */
    @Override
    public ServerOptions setMariaDB4jDataDir(File dir) {
        this.mariadb4jDataDir = dir;
        return this;
    }

    /*
     * (non-Javadoc)
     * 
     * @see runwar.options.ServerOptions#getMariaDB4jDataDir()
     */
    @Override
    public File getMariaDB4jDataDir() {
        return this.mariadb4jDataDir;
    }

    /*
     * (non-Javadoc)
     * 
     * @see runwar.options.ServerOptions#setMariaDB4jImportSQLFile(java.io.File)
     */
    @Override
    public ServerOptions setMariaDB4jImportSQLFile(File file) {
        this.mariadb4jImportSQLFile = file;
        return this;
    }

    /*
     * (non-Javadoc)
     * 
     * @see runwar.options.ServerOptions#getMariaDB4jImportSQLFile()
     */
    @Override
    public File getMariaDB4jImportSQLFile() {
        return this.mariadb4jImportSQLFile;
    }

    /*
     * (non-Javadoc)
     * 
     * @see runwar.options.ServerOptions#setJVMArgs(java.util.List)
     */
    @Override
    public ServerOptions setJVMArgs(List<String> args) {
        this.jvmArgs = args;
        return this;
    }

    /*
     * (non-Javadoc)
     * 
     * @see runwar.options.ServerOptions#getJVMArgs()
     */
    @Override
    public List<String> getJVMArgs() {
        return this.jvmArgs;
    }

    /*
     * (non-Javadoc)
     * 
     * @see runwar.options.ServerOptions#setErrorPages(java.lang.String)
     */
    @Override
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
            // TODO: verify we don't need to do anything different if the WAR
            // context is something other than "/".
            location = location.startsWith("/") ? location : "/" + location;
            errorPages.put(errorCode, location);
        }
        return this;
    }

    /*
     * (non-Javadoc)
     * 
     * @see runwar.options.ServerOptions#setErrorPages(java.util.Map)
     */
    @Override
    public ServerOptions setErrorPages(Map<Integer, String> errorpages) {
        this.errorPages = errorpages;
        return this;
    }

    /*
     * (non-Javadoc)
     * 
     * @see runwar.options.ServerOptions#getErrorPages()
     */
    @Override
    public Map<Integer, String> getErrorPages() {
        return this.errorPages;
    }

    /*
     * (non-Javadoc)
     * 
     * @see runwar.options.ServerOptions#setServletRestEnabled(boolean)
     */
    @Override
    public ServerOptions setServletRestEnabled(boolean enabled) {
        this.servletRestEnabled = enabled;
        return this;
    }

    /*
     * (non-Javadoc)
     * 
     * @see runwar.options.ServerOptions#getServletRestEnabled()
     */
    @Override
    public boolean getServletRestEnabled() {
        return this.servletRestEnabled;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * runwar.options.ServerOptions#setServletRestMappings(java.lang.String)
     */
    @Override
    public ServerOptions setServletRestMappings(String mappings) {
        return setServletRestMappings(mappings.split(","));
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * runwar.options.ServerOptions#setServletRestMappings(java.lang.String[])
     */
    @Override
    public ServerOptions setServletRestMappings(String[] mappings) {
        this.servletRestMappings = mappings;
        return this;
    }

    /*
     * (non-Javadoc)
     * 
     * @see runwar.options.ServerOptions#getServletRestMappings()
     */
    @Override
    public String[] getServletRestMappings() {
        return this.servletRestMappings;
    }

    /*
     * (non-Javadoc)
     * 
     * @see runwar.options.ServerOptions#setFilterPathInfoEnabled(boolean)
     */
    @Override
    public ServerOptions setFilterPathInfoEnabled(boolean enabled) {
        this.filterPathInfoEnabled = enabled;
        return this;
    }

    /*
     * (non-Javadoc)
     * 
     * @see runwar.options.ServerOptions#isFilterPathInfoEnabled()
     */
    @Override
    public boolean isFilterPathInfoEnabled() {
        return this.filterPathInfoEnabled;
    }

    /*
     * (non-Javadoc)
     * 
     * @see runwar.options.ServerOptions#setEnableBasicAuth(boolean)
     */
    @Override
    public ServerOptions setEnableBasicAuth(boolean enable) {
        this.enableBasicAuth = enable;
        return this;
    }

    /*
     * (non-Javadoc)
     * 
     * @see runwar.options.ServerOptions#isEnableBasicAuth()
     */
    @Override
    public boolean isEnableBasicAuth() {
        return this.enableBasicAuth;
    }

    /*
     * (non-Javadoc)
     * 
     * @see runwar.options.ServerOptions#setBasicAuth(java.lang.String)
     */
    @Override
    public ServerOptions setBasicAuth(String userPasswordList) {
        HashMap<String, String> ups = new HashMap<String, String>();
        try {
            for (String up : userPasswordList.split("(?<!\\\\),")) {
                up = up.replace("\\,", ",");
                String u = up.split("(?<!\\\\)=")[0].replace("\\=", "=");
                String p = up.split("(?<!\\\\)=")[1].replace("\\=", "=");
                ups.put(u, p);
            }
        } catch (Exception e) {
            throw new RuntimeException("Incorrect 'users' format (user=pass,user2=pass2) : " + userPasswordList);
        }
        return setBasicAuth(ups);
    }

    /*
     * (non-Javadoc)
     * 
     * @see runwar.options.ServerOptions#setBasicAuth(java.util.Map)
     */
    @Override
    public ServerOptions setBasicAuth(Map<String, String> userPassList) {
        userPasswordList = userPassList;
        return this;
    }

    /*
     * (non-Javadoc)
     * 
     * @see runwar.options.ServerOptions#getBasicAuth()
     */
    @Override
    public Map<String, String> getBasicAuth() {
        return userPasswordList;
    }

    /*
     * (non-Javadoc)
     * 
     * @see runwar.options.ServerOptions#setSSLAddCerts(java.lang.String)
     */
    @Override
    public ServerOptions setSSLAddCerts(String sslCerts) {
        return setSSLAddCerts(sslCerts.split("(?<!\\\\),"));
    }

    /*
     * (non-Javadoc)
     * 
     * @see runwar.options.ServerOptions#setSSLAddCerts(java.lang.String[])
     */
    @Override
    public ServerOptions setSSLAddCerts(String[] sslCerts) {
        this.sslAddCerts = sslCerts;
        return this;
    }

    /*
     * (non-Javadoc)
     * 
     * @see runwar.options.ServerOptions#getSSLAddCerts()
     */
    @Override
    public String[] getSSLAddCerts() {
        return this.sslAddCerts;
    }

    /*
     * (non-Javadoc)
     * 
     * @see runwar.options.ServerOptions#getBufferSize()
     */
    @Override
    public int getBufferSize() {
        return bufferSize;
    }

    /*
     * (non-Javadoc)
     * 
     * @see runwar.options.ServerOptions#setBufferSize(int)
     */
    @Override
    public ServerOptions setBufferSize(int bufferSize) {
        this.bufferSize = bufferSize;
        return this;
    }

    /*
     * (non-Javadoc)
     * 
     * @see runwar.options.ServerOptions#getIoThreads()
     */
    @Override
    public int getIoThreads() {
        return ioThreads;
    }

    /*
     * (non-Javadoc)
     * 
     * @see runwar.options.ServerOptions#setIoThreads(int)
     */
    @Override
    public ServerOptions setIoThreads(int ioThreads) {
        this.ioThreads = ioThreads;
        return this;
    }

    /*
     * (non-Javadoc)
     * 
     * @see runwar.options.ServerOptions#getWorkerThreads()
     */
    @Override
    public int getWorkerThreads() {
        return workerThreads;
    }

    /*
     * (non-Javadoc)
     * 
     * @see runwar.options.ServerOptions#setWorkerThreads(int)
     */
    @Override
    public ServerOptions setWorkerThreads(int workerThreads) {
        this.workerThreads = workerThreads;
        return this;
    }

    /*
     * (non-Javadoc)
     * 
     * @see runwar.options.ServerOptions#setDirectBuffers(boolean)
     */
    @Override
    public ServerOptions setDirectBuffers(boolean enable) {
        this.directBuffers = enable;
        return this;
    }

    /*
     * (non-Javadoc)
     * 
     * @see runwar.options.ServerOptions#isDirectBuffers()
     */
    @Override
    public boolean isDirectBuffers() {
        return this.directBuffers;
    }

    /*
     * (non-Javadoc)
     * 
     * @see runwar.options.ServerOptions#setLoadBalance(java.lang.String)
     */
    @Override
    public ServerOptions setLoadBalance(String hosts) {
        return setLoadBalance(hosts.split("(?<!\\\\),"));
    }

    /*
     * (non-Javadoc)
     * 
     * @see runwar.options.ServerOptions#setLoadBalance(java.lang.String[])
     */
    @Override
    public ServerOptions setLoadBalance(String[] hosts) {
        this.loadBalance = hosts;
        return this;
    }

    /*
     * (non-Javadoc)
     * 
     * @see runwar.options.ServerOptions#getLoadBalance()
     */
    @Override
    public String[] getLoadBalance() {
        return this.loadBalance;
    }

    /*
     * (non-Javadoc)
     * 
     * @see runwar.options.ServerOptions#setProxyPeerAddressEnabled(boolean)
     */
    @Override
    public ServerOptions setProxyPeerAddressEnabled(boolean enable) {
        this.proxyPeerAddressEnabled = enable;
        return this;
    }

    /*
     * (non-Javadoc)
     * 
     * @see runwar.options.ServerOptions#isProxyPeerAddressEnabled()
     */
    @Override
    public boolean isProxyPeerAddressEnabled() {
        return this.proxyPeerAddressEnabled;
    }

    /*
     * (non-Javadoc)
     * 
     * @see runwar.options.ServerOptions#setHTTP2Enabled(boolean)
     */
    @Override
    public ServerOptions setHTTP2Enabled(boolean enable) {
        this.http2enabled = enable;
        return this;
    }

    /*
     * (non-Javadoc)
     * 
     * @see runwar.options.ServerOptions#isHTTP2Enabled()
     */
    @Override
    public boolean isHTTP2Enabled() {
        return this.http2enabled;
    }

    /*
     * (non-Javadoc)
     * 
     * @see runwar.options.ServerOptions#setSecureCookies(boolean)
     */
    @Override
    public ServerOptions setSecureCookies(boolean enable) {
        this.secureCookies = enable;
        this.cookieHttpOnly = enable;
        this.cookieSecure = enable;
        return this;
    }

    /*
     * @see runwar.options.ServerOptions#isSecureCookies()
     */
    @Override
    public boolean isSecureCookies() {
        return this.secureCookies;
    }

    /*
     * (non-Javadoc)
     * 
     * @see runwar.options.ServerOptions#setCookieHttpOnly(boolean)
     */
    @Override
    public ServerOptions setCookieHttpOnly(boolean enable) {
        this.cookieHttpOnly= enable;
        return this;
    }

    /*
     * @see runwar.options.ServerOptions#isCookieHttpOnly()
     */
    @Override
    public boolean isCookieHttpOnly() {
        return this.cookieHttpOnly;
    }

    /*
     * (non-Javadoc)
     * 
     * @see runwar.options.ServerOptions#setCookieSecure(boolean)
     */
    @Override
    public ServerOptions setCookieSecure(boolean enable) {
        this.cookieSecure = enable;
        return this;
    }

    /*
     * @see runwar.options.ServerOptions#isCookieSecure()
     */
    @Override
    public boolean isCookieSecure() {
        return this.cookieSecure;
    }
    
    /*
     * @see runwar.options.ServerOptions#isSecureCookies()
     */
    @Override
    public String getServerMode() {
        if(getWebInfDir() != null && getWebInfDir().exists()) {
            return Mode.WAR;
        } else if( new File(getWarFile(), "WEB-INF").exists()) {
            return Mode.WAR;
        }
        return Mode.DEFAULT;
    }
    
}