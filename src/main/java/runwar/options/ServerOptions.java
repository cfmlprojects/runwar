package runwar.options;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.minidev.json.JSONArray;
import org.xnio.OptionMap;

public interface ServerOptions {
    
    public static final class Keys {
        final static String CONFIG = "config";
        final static String DIRS = "dirs";
        final static String WORKINGDIR = "workingdir";
        final static String CONTENTDIRS = "contentdirs";
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
        final static String LOGLEVEL = "logLevel";
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
        final static String WEBXMLOVERRIDEPATH = "webxmloverridepath";
        final static String WEBXMLOVERRIDEFORCE = "webxmloverrideforce";
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
        final static String DOCK = "dock";
        final static String SHELL = "shell";
        final static String TRAYCONFIG = "trayconfig";
        final static String PREDICATEFILE = "predicateFile";
        final static String ICON = "icon";
        final static String URLREWRITEFILE = "urlrewritefile";
        final static String URLREWRITELOG = "urlrewritelog";
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
        final static String GZIP_PREDICATE = "gzipPredicate";
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
        final static String BUFFEREnable = "bufferenable";
        final static String SSLECCDISABLE = "SSLECCDISABLE";
        final static String SSLSELFSIGN = "sslselfsign";
        final static String SERVICE = "service";
        final static String UNDERTOWOPTIONS = "undertowOptions";
        final static String XNIOOPTIONS = "xnioOptions";
        final static String BROWSER = "browser";
        final static String DEFAULTSERVLETALLOWEDEXT = "defaultServletAllowedExt";
        final static String CASESENSITIVEWEBSERVER="caseSensitiveWebServer";
        final static String RESOURCEMANAGERLOGGING="resourceManagerLogging";
        final static String CACHESERVLETPATHS="cacheServletPaths";
        
    }
    
    String defaultShell();

    ServerOptions commandLineArgs(String[] args);

    String[] commandLineArgs();

    String serverName();

    ServerOptions serverName(String serverName);

    String logLevel();

    ServerOptions logLevel(String level);

    String contextPath();

    File configFile();

    ServerOptions configFile(File file);

    ServerOptions contextPath(String contextPath);

    String host();

    ServerOptions host(String host);

    int httpPort();

    ServerOptions httpPort(int portNumber);

    int ajpPort();

    ServerOptions ajpPort(int ajpPort);

    int sslPort();

    ServerOptions sslPort(int sslPort);

    boolean sslEnable();

    ServerOptions sslEnable(boolean enableSSL);

    boolean httpEnable();

    ServerOptions httpEnable(boolean bool);

    boolean urlRewriteApacheFormat();

    boolean urlRewriteEnable();

    ServerOptions urlRewriteEnable(boolean bool);

    ServerOptions urlRewriteFile(File file);

    File urlRewriteFile();

    ServerOptions urlRewriteLog(File file);

    File urlRewriteLog();

    ServerOptions urlRewriteCheckInterval(String interval);

    String urlRewriteCheckInterval();

    ServerOptions urlRewriteStatusPath(String path);

    String urlRewriteStatusPath();

    int stopPort();

    ServerOptions stopPort(int socketNumber);

    File logDir();

    boolean hasLogDir();

    ServerOptions logPattern(String logPattern);

    String logPattern();

    ServerOptions logDir(String logDir);

    ServerOptions logDir(File logDir);

    ServerOptions logFileName(String name);

    ServerOptions logSuffix(String suffix);

    String logSuffix();

    String logFileName();

    String contentDirs();

    Set<String> contentDirectories();

    ServerOptions contentDirectories(List<String> dirs);

    ServerOptions contentDirectories(Set<String> dirs);

    ServerOptions contentDirs(String dirs);

    public Map<String,String> aliases();

    ServerOptions aliases(Map<String,String> aliases);

    boolean background();

    ServerOptions background(boolean isBackground);

    boolean logRequestsEnable();

    ServerOptions logRequestsEnable(boolean enable);

    boolean logAccessEnable();

    ServerOptions logAccessEnable(boolean enable);

    ServerOptions logAccessDir(File logDir);
    ServerOptions logAccessDir(String logDir);

    File logAccessDir();

    ServerOptions logAccessBaseFileName(String name);

    public String logAccessBaseFileName();

    ServerOptions logRequestsDir(File logDir);
    ServerOptions logRequestsDir(String logDir);

    File logRequestsDir();

    ServerOptions logRequestsBaseFileName(String name);

    public String logRequestsBaseFileName();

    boolean openbrowser();

    ServerOptions openbrowser(boolean openbrowser);

    String openbrowserURL();

    ServerOptions openbrowserURL(String openbrowserURL);

    String pidFile();

    ServerOptions pidFile(String pidFile);

    boolean ajpEnable();

    ServerOptions ajpEnable(boolean enableAJP);

    int launchTimeout();

    ServerOptions launchTimeout(int launchTimeout);

    String processName();

    ServerOptions processName(String processName);

    String libDirs();

    ServerOptions libDirs(String libDirs);

    URL jarURL();

    ServerOptions jarURL(URL jarURL);

    boolean debug();
    boolean testing();

    ServerOptions debug(boolean debug);
    ServerOptions testing(boolean testing);

    File workingDir();

    ServerOptions workingDir(File workingDir);

    File warFile();

    ServerOptions warFile(File warFile);

    File webInfDir();

    ServerOptions webInfDir(File WebInfDir);

    File webXmlFile();

    String webXmlPath() throws MalformedURLException;

    ServerOptions webXmlFile(File webXmlFile);

    File webXmlOverrideFile();
    
    ServerOptions webXmlOverrideFile(File webXmlOverrideFile);
    
    boolean webXmlOverrideForce();

    ServerOptions webXmlOverrideForce(boolean enable);

    String iconImage();

    ServerOptions iconImage(String iconImage);

    File trayConfig();

    JSONArray trayConfigJSON();

    ServerOptions trayConfig(File trayConfig);

    ServerOptions trayConfig(JSONArray trayConfig);

    boolean trayEnable();
    
    boolean dockEnable();

    ServerOptions trayEnable(boolean enable);
    
    ServerOptions dockEnable(boolean enable);

    File statusFile();

    ServerOptions statusFile(File statusFile);

    String cfmlServletConfigWebDir();

    ServerOptions cfmlServletConfigWebDir(String cfmlServletConfigWebDir);

    String cfmlServletConfigServerDir();

    ServerOptions cfmlServletConfigServerDir(String cfmlServletConfigServerDir);

    boolean cacheEnable();

    ServerOptions cacheEnable(boolean cacheEnable);

    boolean directoryListingEnable();

    ServerOptions directoryListingEnable(boolean directoryListingEnable);

    boolean directoryListingRefreshEnable();

    ServerOptions directoryListingRefreshEnable(boolean directoryListingRefreshEnable);

    String[] welcomeFiles();

    ServerOptions welcomeFiles(String[] welcomeFiles);

    String warUriString();
    
    String browser();
    
    ServerOptions browser(String browser);
    
    String defaultServletAllowedExt();
    
    ServerOptions defaultServletAllowedExt(String defaultServletAllowedExt);

    Boolean caseSensitiveWebServer();
    
    ServerOptions caseSensitiveWebServer(Boolean caseSensitiveWebServer);

    Boolean resourceManagerLogging();
    
    Boolean cacheServletPaths();

    ServerOptions resourceManagerLogging(Boolean resourceManagerLogging);
    
    ServerOptions cacheServletPaths(Boolean cacheServletPaths);
        
    ServerOptions sslCertificate(File file);

    File sslCertificate();

    ServerOptions sslKey(File file);

    File sslKey();

    ServerOptions sslKeyPass(char[] pass);

    char[] sslKeyPass();

    ServerOptions stopPassword(char[] password);

    char[] stopPassword();

    ServerOptions action(String action);

    String action();

    ServerOptions cfEngineName(String cfengineName);

    String cfEngineName();

    ServerOptions customHTTPStatusEnable(boolean enable);

    boolean customHTTPStatusEnable();

    ServerOptions sendfileEnable(boolean enable);

    ServerOptions transferMinSize(Long minSize);

    Long transferMinSize();

    ServerOptions gzipEnable(boolean enable);

    boolean gzipEnable();
    
    ServerOptions gzipPredicate(String enable);

    String gzipPredicate();

    ServerOptions mariaDB4jEnable(boolean enable);

    boolean mariaDB4jEnable();

    ServerOptions mariaDB4jPort(int port);

    int mariaDB4jPort();

    ServerOptions mariaDB4jBaseDir(File dir);

    File mariaDB4jBaseDir();

    ServerOptions mariaDB4jDataDir(File dir);

    File mariaDB4jDataDir();

    ServerOptions mariaDB4jImportSQLFile(File file);

    File mariaDB4jImportSQLFile();

    ServerOptions jvmArgs(List<String> args);

    List<String> jvmArgs();

    ServerOptions errorPages(String errorpages);

    ServerOptions errorPages(Map<Integer, String> errorpages);

    Map<Integer, String> errorPages();

    ServerOptions servletRestEnable(boolean enable);

    boolean servletRestEnable();

    ServerOptions servletRestMappings(String mappings);

    ServerOptions servletRestMappings(String[] mappings);

    String[] servletRestMappings();

    ServerOptions filterPathInfoEnable(boolean enable);

    boolean filterPathInfoEnable();

    ServerOptions basicAuthEnable(boolean enable);

    boolean basicAuthEnable();

    ServerOptions basicAuth(String userPasswordList);

    ServerOptions basicAuth(Map<String, String> userPassList);

    Map<String, String> basicAuth();

    ServerOptions sslAddCerts(String sslCerts);

    ServerOptions sslAddCerts(String[] sslCerts);

    String[] sslAddCerts();

    boolean sslEccDisable();

    ServerOptions sslEccDisable(boolean enable);

    boolean sslSelfSign();

    ServerOptions sslSelfSign(boolean enable);

    int bufferSize();

    ServerOptions bufferSize(int bufferSize);

    int ioThreads();

    ServerOptions ioThreads(int ioThreads);

    int workerThreads();

    ServerOptions workerThreads(int workerThreads);

    ServerOptions directBuffers(boolean enable);

    boolean directBuffers();

    ServerOptions loadBalance(String hosts);

    ServerOptions loadBalance(String[] hosts);

    String[] loadBalance();

    ServerOptions proxyPeerAddressEnable(boolean enable);

    boolean proxyPeerAddressEnable();

    ServerOptions http2Enable(boolean enable);

    boolean secureCookies();

    ServerOptions secureCookies(boolean enable);

    boolean cookieHttpOnly();
    
    ServerOptions cookieHttpOnly(boolean enable);
    
    boolean cookieSecure();
    
    ServerOptions cookieSecure(boolean enable);
    
    boolean http2Enable();
    
    String serverMode();

    boolean bufferEnable();
    
    ServerOptions bufferEnable(boolean enable);

    boolean startedFromCommandLine();

    ServerOptions startedFromCommandLine(boolean enable);

    boolean ignoreWebXmlWelcomePages();

    boolean ignoreWebXmlRestMappings();

    boolean service();

    ServerOptions service(boolean enable);

    OptionMap.Builder xnioOptions();

    ServerOptions xnioOptions(String options);

    ServerOptions xnioOptions(OptionMap.Builder options);

    OptionMap.Builder undertowOptions();

    ServerOptions undertowOptions(String options);

    ServerOptions undertowOptions(OptionMap.Builder options);
    
    File predicateFile();
    
    ServerOptions predicateFile(File predicateFile);
    String getLogPattern();
}