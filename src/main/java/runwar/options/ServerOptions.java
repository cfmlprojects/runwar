package runwar.options;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Map;

import net.minidev.json.JSONArray;
import runwar.Server.Mode;

public interface ServerOptions {
    
    public static final class Keys {
        final static String CONFIG = "config";
        final static String DIRS = "dirs";
        final static String WAR = "war";
        final static String WEBINF = "webinf";
        final static String NAME = "name";
        final static String CONTEXT = "context";
        final static String HOST = "host";
        final static String HELP = "help";
        final static String PORT = "port";
        final static String STOPSOCKET = "stopsocket";
        final static String DEBUG = "debug";
        final static String PASSWORD = "password";
        final static String STOP = "stop";
        final static String HTTPENABLE = "httpenable";
        final static String AJPENABLE = "ajpenable";
        final static String URLREWRITEENABLE = "urlrewriteenable";
        final static String LOGLEVEL = "loglevel";
        final static String LOGBASENAME = "logbasename";
        final static String LOGDIR = "logDir";
        final static String LOGREQUESTSBASENAME = "logrequestsbasename";
        final static String LOGREQUESTSDIR = "logrequestsdir";
        final static String LOGREQUESTS = "logrequests";
        final static String LOGACCESSBASENAME = "logaccessbasename";
        final static String LOGACCESSDIR = "logaccessdir";
        final static String LOGACCESS = "logaccess";
        final static String TRACE = "trace";
        final static String BACKGROUND = "background";
        final static String LIBDIRS = "libDirs";
        final static String LIBS = "libs";
        final static String WELCOMEFILES = "welcomefiles";
        final static String JAR = "jar";
        final static String STARTTIMEOUT = "startTimeout";
        final static String TIMEOUT = "timeout";
        final static String WEBXMLPATH = "webxmlpath";
        final static String AJPPORT = "ajpport";
        final static String REQUESTLOG = "requestlog";
        final static String OPENBROWSER = "open-browser";
        final static String OPENURL = "open-url";
        final static String PIDFILE = "pidfile";
        final static String PROCESSNAME = "processname";
        final static String SSLPORT = "sslport";
        final static String SSLCERT = "sslcert";
        final static String SSLKEY = "sslkey";
        final static String SSLKEYPASS = "sslkeypass";
        final static String SSLENABLE = "sslenable";
        final static String TRAY = "tray";
        final static String TRAYCONFIG = "trayconfig";
        final static String ICON = "icon";
        final static String URLREWRITEFILE = "urlrewritefile";
        final static String URLREWRITECHECK = "urlrewritecheck";
        final static String URLREWRITESTATUSPATH = "urlrewritestatuspath";
        final static String STATUSFILE = "statusfile";
        final static String CFENGINE = "cfengine";
        final static String CFSERVERCONF = "cfserverconf";
        final static String CFWEBCONF = "cfwebconf";
        final static String DIRECTORYINDEX = "directoryindex";
        final static String CACHE = "cache";
        final static String CUSTOMSTATUS = "customstatus";
        final static String TRANSFERMINSIZE = "transferminsize";
        final static String SENDFILE = "sendfile";
        final static String GZIP = "gzip";
        final static String MARIADB4J = "mariadb4j";
        final static String MARIADB4JPORT = "mariadb4jport";
        final static String MARIADB4JBASEDIR = "mariadb4jbasedir";
        final static String MARIADB4JDATADIR = "mariadb4jdatadir";
        final static String MARIADB4JIMPORT = "mariadb4jimport";
        final static String JVMARGS = "jvmargs";
        final static String ERRORPAGES = "errorpages";
        final static String SERVLETREST = "servletrest";
        final static String SERVLETRESTMAPPINGS = "servletrestmappings";
        final static String FILTERPATHINFO = "filterpathinfo";
        final static String SSLADDCERTS = "ssladdcerts";
        final static String BASICAUTHENABLE = "basicauth";
        final static String BUFFERSIZE = "buffersize";
        final static String IOTHREADS = "iothreads";
        final static String WORKERTHREADS = "workerthreads";
        final static String DIRECTBUFFERS = "directbuffers";
        final static String LOADBALANCE = "loadbalance";
        final static String DIRECTORYREFRESH = "directoryrefresh";
        final static String PROXYPEERADDRESS = "proxypeeraddress";
        final static String HTTP2 = "http2";
        final static String SECURECOOKIES = "securecookies";
        final static String COOKIEHTTPONLY = "cookiehttponly";
        final static String COOKIESECURE = "cookiesecure";
        final static String SERVERMODE = "mode";
    }

    ServerOptions setCommandLineArgs(String[] args);

    String[] getCommandLineArgs();

    String getServerName();

    ServerOptions setServerName(String serverName);

    String getLoglevel();

    ServerOptions setLoglevel(String loglevel);

    String getContextPath();

    File getConfigFile();

    ServerOptions setConfigFile(File file);

    ServerOptions setContextPath(String contextPath);

    String getHost();

    ServerOptions setHost(String host);

    int getPortNumber();

    ServerOptions setPortNumber(int portNumber);

    int getAJPPort();

    ServerOptions setAJPPort(int ajpPort);

    int getSSLPort();

    ServerOptions setSSLPort(int sslPort);

    boolean isEnableSSL();

    ServerOptions setEnableSSL(boolean enableSSL);

    boolean isEnableHTTP();

    ServerOptions setEnableHTTP(boolean bool);

    boolean isURLRewriteApacheFormat();

    boolean isEnableURLRewrite();

    ServerOptions setEnableURLRewrite(boolean bool);

    ServerOptions setURLRewriteFile(File file);

    File getURLRewriteFile();

    ServerOptions setURLRewriteCheckInterval(String interval);

    String getURLRewriteCheckInterval();

    ServerOptions setURLRewriteStatusPath(String path);

    String getURLRewriteStatusPath();

    int getSocketNumber();

    ServerOptions setSocketNumber(int socketNumber);

    File getLogDir();

    boolean hasLogDir();

    ServerOptions setLogDir(String logDir);

    ServerOptions setLogDir(File logDir);

    ServerOptions setLogFileName(String name);

    ServerOptions setLogSuffix(String suffix);

    String getLogSuffix();

    String getLogFileName();

    String getCfmlDirs();

    ServerOptions setCfmlDirs(String cfmlDirs);

    boolean isBackground();

    ServerOptions setBackground(boolean isBackground);

    /**
     * Will be removed eventually.  Use logRequestsEnabled() instead.
     * @return boolean
     */
    @Deprecated
    boolean isKeepRequestLog();

    /**
     * Will be removed eventually.  Use logRequestsEnabled() instead.
     * @return boolean
     */
    @Deprecated
    ServerOptions setKeepRequestLog(boolean keepRequestLog);

    boolean logRequestsEnable();

    ServerOptions logRequestsEnable(boolean enable);

    boolean logAccessEnable();

    ServerOptions logAccessEnable(boolean enable);

    ServerOptions setLogAccessDir(File logDir);
    ServerOptions setLogAccessDir(String logDir);

    File getLogAccessDir();

    ServerOptions setLogAccessBaseFileName(String name);

    public String getLogAccessBaseFileName();

    ServerOptions setLogRequestsDir(File logDir);
    ServerOptions setLogRequestsDir(String logDir);

    File getLogRequestsDir();

    ServerOptions setLogRequestsBaseFileName(String name);

    public String getLogRequestsBaseFileName();

    boolean isOpenbrowser();

    ServerOptions setOpenbrowser(boolean openbrowser);

    String getOpenbrowserURL();

    ServerOptions setOpenbrowserURL(String openbrowserURL);

    String getPidFile();

    ServerOptions setPidFile(String pidFile);

    boolean isEnableAJP();

    ServerOptions setEnableAJP(boolean enableAJP);

    int getLaunchTimeout();

    ServerOptions setLaunchTimeout(int launchTimeout);

    String getProcessName();

    ServerOptions setProcessName(String processName);

    String getLibDirs();

    ServerOptions setLibDirs(String libDirs);

    URL getJarURL();

    ServerOptions setJarURL(URL jarURL);

    boolean isDebug();

    ServerOptions setDebug(boolean debug);

    File getWarFile();

    ServerOptions setWarFile(File warFile);

    File getWebInfDir();

    ServerOptions setWebInfDir(File WebInfDir);

    File getWebXmlFile();

    String getWebXmlPath() throws MalformedURLException;

    ServerOptions setWebXmlFile(File webXmlFile);

    String getIconImage();

    ServerOptions setIconImage(String iconImage);

    File getTrayConfig();

    JSONArray getTrayConfigJSON();

    ServerOptions setTrayConfig(File trayConfig);

    ServerOptions setTrayConfig(JSONArray trayConfig);

    boolean isTrayEnabled();

    ServerOptions setTrayEnabled(boolean enabled);

    File getStatusFile();

    ServerOptions setStatusFile(File statusFile);

    String getCFMLServletConfigWebDir();

    ServerOptions setCFMLServletConfigWebDir(String cfmlServletConfigWebDir);

    String getCFMLServletConfigServerDir();

    ServerOptions setCFMLServletConfigServerDir(String cfmlServletConfigServerDir);

    boolean isCacheEnabled();

    ServerOptions setCacheEnabled(boolean cacheEnabled);

    boolean isDirectoryListingEnabled();

    ServerOptions setDirectoryListingEnabled(boolean directoryListingEnabled);

    boolean isDirectoryListingRefreshEnabled();

    ServerOptions setDirectoryListingRefreshEnabled(boolean directoryListingRefreshEnabled);

    String[] getWelcomeFiles();

    ServerOptions setWelcomeFiles(String[] welcomeFiles);

    String getWarPath();

    ServerOptions setSSLCertificate(File file);

    File getSSLCertificate();

    ServerOptions setSSLKey(File file);

    File getSSLKey();

    ServerOptions setSSLKeyPass(char[] pass);

    char[] getSSLKeyPass();

    ServerOptions setStopPassword(char[] password);

    char[] getStopPassword();

    ServerOptions setAction(String action);

    String getAction();

    ServerOptions setCFEngineName(String cfengineName);

    String getCFEngineName();

    ServerOptions setCustomHTTPStatusEnabled(boolean enabled);

    boolean isCustomHTTPStatusEnabled();

    ServerOptions setSendfileEnabled(boolean enabled);

    ServerOptions setTransferMinSize(Long minSize);

    Long getTransferMinSize();

    ServerOptions setGzipEnabled(boolean enabled);

    boolean isGzipEnabled();

    ServerOptions setMariaDB4jEnabled(boolean enabled);

    boolean isMariaDB4jEnabled();

    ServerOptions setMariaDB4jPort(int port);

    int getMariaDB4jPort();

    ServerOptions setMariaDB4jBaseDir(File dir);

    File getMariaDB4jBaseDir();

    ServerOptions setMariaDB4jDataDir(File dir);

    File getMariaDB4jDataDir();

    ServerOptions setMariaDB4jImportSQLFile(File file);

    File getMariaDB4jImportSQLFile();

    ServerOptions setJVMArgs(List<String> args);

    List<String> getJVMArgs();

    ServerOptions setErrorPages(String errorpages);

    ServerOptions setErrorPages(Map<Integer, String> errorpages);

    Map<Integer, String> getErrorPages();

    ServerOptions setServletRestEnabled(boolean enabled);

    boolean getServletRestEnabled();

    ServerOptions setServletRestMappings(String mappings);

    ServerOptions setServletRestMappings(String[] mappings);

    String[] getServletRestMappings();

    ServerOptions setFilterPathInfoEnabled(boolean enabled);

    boolean isFilterPathInfoEnabled();

    ServerOptions setEnableBasicAuth(boolean enable);

    boolean isEnableBasicAuth();

    ServerOptions setBasicAuth(String userPasswordList);

    ServerOptions setBasicAuth(Map<String, String> userPassList);

    Map<String, String> getBasicAuth();

    ServerOptions setSSLAddCerts(String sslCerts);

    ServerOptions setSSLAddCerts(String[] sslCerts);

    String[] getSSLAddCerts();

    int getBufferSize();

    ServerOptions setBufferSize(int bufferSize);

    int getIoThreads();

    ServerOptions setIoThreads(int ioThreads);

    int getWorkerThreads();

    ServerOptions setWorkerThreads(int workerThreads);

    ServerOptions setDirectBuffers(boolean enable);

    boolean isDirectBuffers();

    ServerOptions setLoadBalance(String hosts);

    ServerOptions setLoadBalance(String[] hosts);

    String[] getLoadBalance();

    ServerOptions setProxyPeerAddressEnabled(boolean enable);

    boolean isProxyPeerAddressEnabled();

    ServerOptions setHTTP2Enabled(boolean enable);

    boolean isSecureCookies();

    ServerOptions setSecureCookies(boolean enable);

    boolean isCookieHttpOnly();
    
    ServerOptions setCookieHttpOnly(boolean enable);
    
    boolean isCookieSecure();
    
    ServerOptions setCookieSecure(boolean enable);
    
    boolean isHTTP2Enabled();
    
    String getServerMode();
}