package runwar.options;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Map;

import net.minidev.json.JSONArray;

public interface ServerOptions {

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

    ServerOptions setLogDir(String logDir);

    ServerOptions setLogDir(File logDir);

    ServerOptions setLogFileName(String name);

    public String getLogFileName();

    String getCfmlDirs();

    ServerOptions setCfmlDirs(String cfmlDirs);

    boolean isBackground();

    ServerOptions setBackground(boolean isBackground);

    boolean requestLogEnable();

    ServerOptions requestLogEnable(boolean keepRequestLog);

    /**
     * Will be removed eventually.  Use requestLogEnabled() instead.
     * @return
     */
    @Deprecated
    boolean isKeepRequestLog();

    /**
     * Will be removed eventually.  Use requestLogEnabled() instead.
     * @return
     */
    @Deprecated
    ServerOptions setKeepRequestLog(boolean keepRequestLog);

    ServerOptions setRequestLogDir(File logDir);

    ServerOptions setLogRequestFileName(String name);

    public String getLogRequestFileName();

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

    boolean isHTTP2Enabled();

}