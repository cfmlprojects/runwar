package runwar.options;

import io.undertow.UndertowOptions;
import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import org.xnio.OptionMap;
import org.xnio.Options;
import runwar.Server;
import runwar.Server.Mode;

import java.io.File;
import java.lang.reflect.Modifier;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Stream;

import static runwar.util.Reflection.setOptionMapValue;

public class ServerOptionsImpl implements ServerOptions {
    
    private String serverName = null, processName = "RunWAR", logLevel = "INFO";
    
    private String host = "127.0.0.1", contextPath = "/";
    
    private int portNumber = 8088, ajpPort = 8009, sslPort = 1443, socketNumber = 8779, http2ProxySSLPort = 1444;
    
    private boolean enableAJP = false, enableSSL = false, enableHTTP = true, enableURLRewrite = false;
    
    private boolean debug = false, isBackground = true, logAccessEnable = false, logRequestsEnable = false, openbrowser = false, startedFromCommandline = false;
    
    private String pidFile, openbrowserURL, cfmlDirs, logFileBaseName="server", logRequestBaseFileName="requests", logAccessBaseFileName="access", logSuffix="txt", libDirs = null;
    
    private int launchTimeout = 50 * 1000; // 50 secs
    
    private URL jarURL = null;
    
    private File workingDir, warFile, webInfDir, webXmlFile, logDir, logRequestsDir, logAccessDir, urlRewriteFile, urlRewriteLog, trayConfig, statusFile = null, predicateFile;
    
    private String iconImage = null;
    
    private String urlRewriteCheckInterval = null, urlRewriteStatusPath = null;
    
    private String cfmlServletConfigWebDir = null, cfmlServletConfigServerDir = null;
    
    private String defaultShell = "";
    
    private boolean trayEnable = true;
    
    private boolean dockEnable = true; // for mac users
    
    private boolean directoryListingEnable = true;
    
    private boolean directoryListingRefreshEnable = false;
    
    private boolean cacheEnable = false;
    
    private String[] welcomeFiles;
    
    private File sslCertificate, sslKey, configFile;
    
    private char[] sslKeyPass = null;
    
    private char[] stopPassword = "klaatuBaradaNikto".toCharArray();
    
    private String action = "start";

    private String browser = "";
    
    private String cfengineName = "";
    
    private boolean customHTTPStatusEnable = true;
    
    private boolean gzipEnable = false;

    private String gzipPredicate = "request-larger-than(1500)";

    private Long transferMinSize = (long) 100;
    
    private boolean mariadb4jEnable = false;
    
    private int mariadb4jPort = 13306;
    
    private File mariadb4jBaseDir, mariadb4jDataDir, mariadb4jImportSQLFile = null;
    
    private List<String> jvmArgs = null;
    
    private Map<Integer, String> errorPages = null;
    
    private boolean servletRestEnable = false;
    
    private String[] servletRestMappings = { "/rest" };
    
    private boolean filterPathInfoEnable = true;
    
    private String[] sslAddCerts = null;
    
    private String[] cmdlineArgs = null;
    
    private String[] loadBalance = null;
    
    private static Map<String, String> userPasswordList;
    
    private boolean enableBasicAuth = false;
    
    private boolean directBuffers = true;
    
    int bufferSize, ioThreads, workerThreads = 0;
    
    private boolean proxyPeerAddressEnable = false;
    
    private boolean http2enable = false;
    
    private boolean secureCookies = false, cookieHttpOnly = false, cookieSecure = false;
    
    private JSONArray trayConfigJSON;
    
    private boolean bufferEnable = false;
    
    private boolean sslEccDisable = true;
    
    private boolean sslSelfSign = false;
    
    private boolean service = false;
    
    public String logPattern = "[%-5p] %c: %m%n";
    
    private String defaultServletAllowedExt = "";

    private Boolean caseSensitiveWebServer= null;
    
    private Boolean resourceManagerLogging= false;
    
    
    private final Map<String, String> aliases = new HashMap<>();
    
    private Set<String> contentDirectories = new HashSet<>();

    public String getLogPattern() {
        return logPattern;
    }
    
    private OptionMap.Builder serverXnioOptions = OptionMap.builder()
            .set(Options.WORKER_IO_THREADS, 8)
            .set(Options.CONNECTION_HIGH_WATER, 1000000)
            .set(Options.CONNECTION_LOW_WATER, 1000000)
            .set(Options.WORKER_TASK_CORE_THREADS, 30)
            .set(Options.WORKER_TASK_MAX_THREADS, 30)
            .set(Options.TCP_NODELAY, true)
            .set(Options.CORK, true);
    
    private boolean testing = false;
    private OptionMap.Builder undertowOptions = OptionMap.builder();

    static {
        userPasswordList = new HashMap<>();
        userPasswordList.put("bob", "12345");
        userPasswordList.put("alice", "secret");
    }


//    public String toJson(){
//        final Gson gson=new GsonBuilder().excludeFieldsWithoutExposeAnnotation()
//            .excludeFieldsWithModifiers(Modifier.TRANSIENT).create();
//        return gson.toJson(this);
//    }

    public String toJson(Set<String> set){
        JSONArray jsonArray = new JSONArray();
        jsonArray.addAll(set);
        return jsonArray.toJSONString();
    }

    public String toJson(Map<?,?> map){
        final Map<String,String> finalMap = new HashMap<>();
        map.forEach((o, o2) -> {
            if(o instanceof Integer){
                finalMap.put(Integer.toString((Integer) o),(String)o2);
            } else {
                finalMap.put((String)o,(String)o2);
            }
        });
        return new JSONObject(finalMap).toJSONString();
    }

    /** 
     * @see runwar.options.ServerOptions#commandLineArgs(java.lang.String[])
     */
    @Override
    public ServerOptions commandLineArgs(String[] args) {
        this.cmdlineArgs = args;
        return this;
    }

    /** 
     * @see runwar.options.ServerOptions#commandLineArgs()
     */
    @Override
    public String[] commandLineArgs() {
        // TODO: totally refactor argument handling so we can serialize and not
        // muck around like this.
        List<String> argarray = new ArrayList<String>();
        if (cmdlineArgs == null) {
            cmdlineArgs = "".split("");
            argarray.add("-war");
            argarray.add(warFile() != null ? warFile().getAbsolutePath() : new File(".").getAbsolutePath());
        }
        Boolean skipNext = false;
        for (String arg : cmdlineArgs) {
            arg = arg.trim();
            if (arg.contains("background") || arg.equalsIgnoreCase("-b") || arg.contains("balance")
                    || arg.startsWith("--port") || arg.equalsIgnoreCase("-p") || arg.startsWith("--stop-port")
                    || arg.contains("stopsocket") || arg.contains("--host")) {
                skipNext = true;
            } else {
                if(skipNext) {
                    skipNext = false;
                } else {
                    argarray.add(arg);
                }
            }
        }
        argarray.add("--host");
        argarray.add(String.valueOf(host()));
        argarray.add("--background");
        argarray.add(Boolean.toString(background()));
        argarray.add("--port");
        argarray.add(Integer.toString(httpPort()));
        argarray.add("--stop-port");
        argarray.add(Integer.toString(stopPort()));
        return argarray.toArray(new String[argarray.size()]);
    }

    /** 
     * @see runwar.options.ServerOptions#serverName()
     */
    @Override
    public String serverName() {
        if(serverName == null){
            serverName = Paths.get(".").toFile().getAbsoluteFile().getParentFile().getName();
            assert serverName.length() > 3;
        }
        return serverName;
    }

    /** 
     * @see runwar.options.ServerOptions#serverName(java.lang.String)
     */
    @Override
    public ServerOptions serverName(String serverName) {
        this.serverName = serverName;
        return this;
    }

    /** 
     * @see runwar.options.ServerOptions#logLevel()
     */
    @Override
    public String logLevel() {
        return logLevel;
    }

    /** 
     * @see runwar.options.ServerOptions#logLevel(java.lang.String)
     */
    @Override
    public ServerOptions logLevel(String level) {
        this.logLevel = level.toUpperCase();
        return this;
    }

    /** 
     * @see runwar.options.ServerOptions#contextPath()
     */
    @Override
    public String contextPath() {
        return contextPath;
    }

    /** 
     * @see runwar.options.ServerOptions#configFile()
     */
    @Override
    public File configFile() {
        return configFile;
    }

    /** 
     * @see runwar.options.ServerOptions#configFile(java.io.File)
     */
    @Override
    public ServerOptions configFile(File file) {
        this.configFile = file;
        return this;
    }

    /** 
     * @see runwar.options.ServerOptions#contextPath(java.lang.String)
     */
    @Override
    public ServerOptions contextPath(String contextPath) {
        this.contextPath = contextPath;
        return this;
    }

    /** 
     * @see runwar.options.ServerOptions#host()
     */
    @Override
    public String host() {
        return host;
    }

    /** 
     * @see runwar.options.ServerOptions#host(java.lang.String)
     */
    @Override
    public ServerOptions host(String host) {
        this.host = host;
        return this;
    }

    /** 
     * @see runwar.options.ServerOptions#httpPort()
     */
    @Override
    public int httpPort() {
        return portNumber;
    }

    /** 
     * @see runwar.options.ServerOptions#httpPort(int)
     */
    @Override
    public ServerOptions httpPort(int portNumber) {
        this.portNumber = portNumber;
        return this;
    }

    /** 
     * @see runwar.options.ServerOptions#ajpPort()
     */
    @Override
    public int ajpPort() {
        return ajpPort;
    }

    /** 
     * @see runwar.options.ServerOptions#ajpPort(int)
     */
    @Override
    public ServerOptions ajpPort(int ajpPort) {
        this.ajpPort = ajpPort;
        return this;
    }

    /** 
     * @see runwar.options.ServerOptions#sslPort()
     */
    @Override
    public int sslPort() {
        return sslPort;
    }

    /** 
     * @see runwar.options.ServerOptions#sslPort(int)
     */
    @Override
    public ServerOptions sslPort(int sslPort) {
        this.sslPort = sslPort;
        return this;
    }

    /** 
     * @see runwar.options.ServerOptions#sslEnable()
     */
    @Override
    public boolean sslEnable() {
        return enableSSL;
    }

    /** 
     * @see runwar.options.ServerOptions#sslEnable(boolean)
     */
    @Override
    public ServerOptions sslEnable(boolean enableSSL) {
        this.enableSSL = enableSSL;
        return this;
    }

    /** 
     * @see runwar.options.ServerOptions#httpEnable()
     */
    @Override
    public boolean httpEnable() {
        return enableHTTP;
    }

    /** 
     * @see runwar.options.ServerOptions#httpEnable(boolean)
     */
    @Override
    public ServerOptions httpEnable(boolean bool) {
        this.enableHTTP = bool;
        return this;
    }

    /** 
     * @see runwar.options.ServerOptions#urlRewriteApacheFormat()
     */
    @Override
    public boolean urlRewriteApacheFormat() {
        return urlRewriteFile() == null ? false : urlRewriteFile().getPath().endsWith(".htaccess") || urlRewriteFile().getPath().endsWith(".conf");
    }

    /** 
     * @see runwar.options.ServerOptions#urlRewriteEnable()
     */
    @Override
    public boolean urlRewriteEnable() {
        return enableURLRewrite;
    }

    /** 
     * @see runwar.options.ServerOptions#urlRewriteEnable(boolean)
     */
    @Override
    public ServerOptions urlRewriteEnable(boolean bool) {
        this.enableURLRewrite = bool;
        return this;
    }

    /** 
     * @see runwar.options.ServerOptions#urlRewriteFile(java.io.File)
     */
    @Override
    public ServerOptions urlRewriteFile(File file) {
        this.urlRewriteFile = file;
        return this;
    }

    /** 
     * @see runwar.options.ServerOptions#urlRewriteFile()
     */
    @Override
    public File urlRewriteFile() {
        return this.urlRewriteFile;
    }
    
    /** 
     * @see runwar.options.ServerOptions#urlRewriteLog(java.io.File)
     */
    @Override
    public ServerOptions urlRewriteLog(File file) {
        this.urlRewriteLog = file;
        return this;
    }
    
    /** 
     * @see runwar.options.ServerOptions#urlRewriteLog()
     */
    @Override
    public File urlRewriteLog() {
        return this.urlRewriteLog;
    }

    /** 
     * @see
     * runwar.options.ServerOptions#urlRewriteCheckInterval(java.lang.String)
     */
    @Override
    public ServerOptions urlRewriteCheckInterval(String interval) {
        this.urlRewriteCheckInterval = interval;
        return this;
    }

    /** 
     * @see runwar.options.ServerOptions#urlRewriteCheckInterval()
     */
    @Override
    public String urlRewriteCheckInterval() {
        return this.urlRewriteCheckInterval;
    }

    /** 
     * @see
     * runwar.options.ServerOptions#urlRewriteStatusPath(java.lang.String)
     */
    @Override
    public ServerOptions urlRewriteStatusPath(String path) {
        if (!path.startsWith("/")) {
            path = '/' + path;
        }
        this.urlRewriteStatusPath = path;
        return this;
    }

    /** 
     * @see runwar.options.ServerOptions#urlRewriteStatusPath()
     */
    @Override
    public String urlRewriteStatusPath() {
        return this.urlRewriteStatusPath;
    }

    /** 
     * @see runwar.options.ServerOptions#stopPort()
     */
    @Override
    public int stopPort() {
        return socketNumber;
    }

    /** 
     * @see runwar.options.ServerOptions#stopPort(int)
     */
    @Override
    public ServerOptions stopPort(int socketNumber) {
        this.socketNumber = socketNumber;
        return this;
    }

    /** 
     * @see runwar.options.ServerOptions#logPattern(java.lang.String)
     */
    @Override
    public ServerOptions logPattern(String pattern) {
        if (pattern != null && pattern.length() > 0)
            logPattern = pattern;
        return this;
    }

    /** 
     * @see runwar.options.ServerOptions#logPattern()
     */
    @Override
    public String logPattern() {
        return logPattern;
    }

    /** 
     * @see runwar.options.ServerOptions#hasLogDir()
     */
    @Override
    public boolean hasLogDir() {
        return logDir != null;
    }

    /** 
     * @see runwar.options.ServerOptions#logDir()
     */
    @Override
    public File logDir() {
        if (logDir == null) {
            String defaultLogDir = new File(Server.getThisJarLocation().getParentFile(), "./.logs/")
                    .getAbsolutePath();
            logDir = new File(defaultLogDir);
            if (warFile() != null) {
                File warFile = warFile();
                if (warFile.isDirectory() && new File(warFile, "WEB-INF").exists()) {
                    defaultLogDir = warFile.getPath() + "/WEB-INF/logs/";
                } else if (cfEngineName().length() != 0) {
                    String serverConfigDir = cfmlServletConfigServerDir();
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

    /** 
     * @see runwar.options.ServerOptions#logDir(java.lang.String)
     */
    @Override
    public ServerOptions logDir(String logDir) {
        if (logDir != null && logDir.length() > 0)
            this.logDir = new File(logDir);
        return this;
    }

    /** 
     * @see runwar.options.ServerOptions#logDir(java.io.File)
     */
    @Override
    public ServerOptions logDir(File logDir) {
        this.logDir = logDir;
        return this;
    }

    /** 
     * @see runwar.options.ServerOptions#logFileName(java.lang.String)
     */
    @Override
    public ServerOptions logFileName(String name) {
        this.logFileBaseName = name;
        return this;
    }

    /** 
     * @see runwar.options.ServerOptions#logFileName()
     */
    @Override
    public String logFileName() {
        this.logFileBaseName = (this.logFileBaseName == null) ? "server." : this.logFileBaseName;
        return this.logFileBaseName;
    }

    /** 
     * @see runwar.options.ServerOptions#logFileName(java.lang.String)
     */
    @Override
    public ServerOptions logSuffix(String suffix) {
        this.logSuffix = suffix;
        return this;
    }

    /** 
     * @see runwar.options.ServerOptions#logFileName()
     */
    @Override
    public String logSuffix() {
        return this.logSuffix;
    }

    /** 
     * @see runwar.options.ServerOptions#contentDirs()
     */
    @Override
    public String contentDirs() {
        if (cfmlDirs == null && warFile() != null) {
            contentDirs(warFile().getAbsolutePath());
        }
        return cfmlDirs;
    }

    @Override
    public Set<String> contentDirectories() {
        if(contentDirs() != null){
            Stream.of(contentDirs().split(",")).forEach(aDirList -> {
                String dir;
                String[] directoryAndAliasList = aDirList.trim().split("=");
                if (directoryAndAliasList.length == 1) {
                    dir = directoryAndAliasList[0].trim();
                    if(dir.length() > 0)
                        contentDirectories.add(dir);
                }
            });
        }
        return contentDirectories;
    }

    @Override
    public ServerOptions contentDirectories(List<String> dirs) {
        contentDirectories.addAll(dirs);  // a set so we can always safely add
        return this;
    }

    @Override
    public ServerOptions contentDirectories(Set<String> dirs) {
        contentDirectories = dirs;
        return this;
    }

    /**
     * @see runwar.options.ServerOptions#contentDirs(java.lang.String)
     */
    @Override
    public ServerOptions contentDirs(String dirs) {
        this.cfmlDirs = dirs;
        return this;
    }

    /**
     * @see runwar.options.ServerOptions#aliases()
     */
    @Override
    public Map<String,String> aliases() {
        if(contentDirs() == null && aliases.size() == 0){
            return new HashMap<>();
        }
        Stream.of(contentDirs().split(",")).forEach(aDirList -> {
            Path path;
            String dir = "";
            String virtual = "";
            String[] directoryAndAliasList = aDirList.trim().split("=");
            if (directoryAndAliasList.length == 1) {
                dir = directoryAndAliasList[0].trim();
                if(dir.length() > 0)
                    contentDirectories.add(dir); // a set so we can always safely add
            } else {
                dir = directoryAndAliasList[1].trim();
                virtual = directoryAndAliasList[0].trim();
            }
            dir = dir.endsWith("/") ? dir : dir + '/';
            path = Paths.get(dir).normalize().toAbsolutePath();
            if(virtual.length() != 0){
                virtual = virtual.startsWith("/") ? virtual : "/" + virtual;
                virtual = virtual.endsWith("/") ? virtual.substring(0, virtual.length() - 1) : virtual;
                aliases.put(virtual.toLowerCase(), path.toString());
            }
        });
        return aliases;
    }

    /** 
     * @see runwar.options.ServerOptions#contentDirs(java.lang.String)
     */
    @Override
    public ServerOptions aliases(Map<String,String> aliases) {
        this.aliases.putAll(aliases);
        return this;
    }

    /** 
     * @see runwar.options.ServerOptions#background()
     */
    @Override
    public boolean background() {
        return isBackground;
    }

    /** 
     * @see runwar.options.ServerOptions#background(boolean)
     */
    @Override
    public ServerOptions background(boolean isBackground) {
        this.isBackground = isBackground;
        return this;
    }

    /** 
     * @see runwar.options.ServerOptions#logRequestsEnable()
     */
    @Override
    public boolean logRequestsEnable() {
        return logRequestsEnable;
    }

    /** 
     * @see runwar.options.ServerOptions#logRequestsEnable(boolean)
     */
    @Override
    public ServerOptions logRequestsEnable(boolean enable) {
        this.logRequestsEnable = enable;
        return this;
    }

    /** 
     * @see runwar.options.ServerOptions#logRequestsEnable()
     */
    @Override
    public boolean logAccessEnable() {
        return logAccessEnable;
    }
    
    /** 
     * @see runwar.options.ServerOptions#logRequestsEnable(boolean)
     */
    @Override
    public ServerOptions logAccessEnable(boolean enable) {
        this.logAccessEnable = enable;
        return this;
    }

    /** 
     * @see runwar.options.ServerOptions#logRequestsDir(java.io.File)
     */
    @Override
    public ServerOptions logRequestsDir(File logDir) {
        this.logRequestsDir = logDir;
        return this;
    }
    @Override
    public ServerOptions logRequestsDir(String logDir) {
        this.logRequestsDir = new File(logDir);
        return this;
    }
    @Override
    public File logRequestsDir() {
        if(this.logRequestsDir == null)
            return logDir();
        return this.logRequestsDir;
    }

    /** 
     * @see runwar.options.ServerOptions#logRequestsBaseFileName(java.lang.String)
     */
    @Override
    public ServerOptions logRequestsBaseFileName(String name) {
        this.logRequestBaseFileName= name;
        return this;
    }

    /** 
     * @see runwar.options.ServerOptions#logRequestsBaseFileName()
     */
    @Override
    public String logRequestsBaseFileName() {
        return this.logRequestBaseFileName;
    }

    /** 
     * @see runwar.options.ServerOptions#logAccessDir(java.io.File)
     */
    @Override
    public ServerOptions logAccessDir(File logDir) {
        this.logAccessDir = logDir;
        return this;
    }
    @Override
    public ServerOptions logAccessDir(String logDir) {
        this.logAccessDir = new File(logDir);
        return this;
    }
    @Override
    public File logAccessDir() {
        if(this.logAccessDir == null)
            return logDir();
        return this.logAccessDir;
    }

    /** 
     * @see runwar.options.ServerOptions#logAccessBaseFileName(java.lang.String)
     */
    @Override
    public ServerOptions logAccessBaseFileName(String name) {
        this.logAccessBaseFileName = name;
        return this;
    }

    /** 
     * @see runwar.options.ServerOptions#logAccessBaseFileName()
     */
    @Override
    public String logAccessBaseFileName() {
        return this.logAccessBaseFileName;
    }
    /** 
     * @see runwar.options.ServerOptions#openbrowser()
     */
    @Override
    public boolean openbrowser() {
        return openbrowser;
    }

    /** 
     * @see runwar.options.ServerOptions#openbrowser(boolean)
     */
    @Override
    public ServerOptions openbrowser(boolean openbrowser) {
        this.openbrowser = openbrowser;
        return this;
    }

    /** 
     * @see runwar.options.ServerOptions#openbrowserURL()
     */
    @Override
    public String openbrowserURL() {
        return openbrowserURL;
    }

    /** 
     * @see runwar.options.ServerOptions#openbrowserURL(java.lang.String)
     */
    @Override
    public ServerOptions openbrowserURL(String openbrowserURL) {
        this.openbrowserURL = openbrowserURL;
        return this;
    }

    /** 
     * @see runwar.options.ServerOptions#pidFile()
     */
    @Override
    public String pidFile() {
        return pidFile;
    }

    /** 
     * @see runwar.options.ServerOptions#pidFile(java.lang.String)
     */
    @Override
    public ServerOptions pidFile(String pidFile) {
        this.pidFile = pidFile;
        return this;
    }

    /** 
     * @see runwar.options.ServerOptions#ajpEnable()
     */
    @Override
    public boolean ajpEnable() {
        return enableAJP;
    }

    /** 
     * @see runwar.options.ServerOptions#ajpEnable(boolean)
     */
    @Override
    public ServerOptions ajpEnable(boolean enableAJP) {
        this.enableAJP = enableAJP;
        return this;
    }

    /** 
     * @see runwar.options.ServerOptions#launchTimeout()
     */
    @Override
    public int launchTimeout() {
        return launchTimeout;
    }

    /** 
     * @see runwar.options.ServerOptions#launchTimeout(int)
     */
    @Override
    public ServerOptions launchTimeout(int launchTimeout) {
        this.launchTimeout = launchTimeout;
        return this;
    }

    /** 
     * @see runwar.options.ServerOptions#processName()
     */
    @Override
    public String processName() {
        return processName;
    }

    /** 
     * @see runwar.options.ServerOptions#processName(java.lang.String)
     */
    @Override
    public ServerOptions processName(String processName) {
        this.processName = processName;
        return this;
    }

    /** 
     * @see runwar.options.ServerOptions#libDirs()
     */
    @Override
    public String libDirs() {
        return libDirs;
    }

    /** 
     * @see runwar.options.ServerOptions#libDirs(java.lang.String)
     */
    @Override
    public ServerOptions libDirs(String libDirs) {
        this.libDirs = libDirs;
        return this;
    }

    /** 
     * @see runwar.options.ServerOptions#jarURL()
     */
    @Override
    public URL jarURL() {
        return jarURL;
    }

    /** 
     * @see runwar.options.ServerOptions#jarURL(java.net.URL)
     */
    @Override
    public ServerOptions jarURL(URL jarURL) {
        this.jarURL = jarURL;
        return this;
    }

    /** 
     * @see runwar.options.ServerOptions#debug()
     */
    @Override
    public boolean debug() {
        return debug;
    }

    /** 
     * @see runwar.options.ServerOptions#debug(boolean)
     */
    @Override
    public ServerOptions debug(boolean debug) {
        this.debug = debug;
        if (debug && logLevel == "WARN") {
            logLevel = "DEBUG";
        }
        return this;
    }
    
      @Override
    public ServerOptions testing(boolean testing) {
        this.testing = testing;
        if (testing && logLevel == "WARN") {
            logLevel = "DEBUG";
        }
        return this;
    }

    /** 
     * @see runwar.options.ServerOptions#workingDir()
     */
    @Override
    public File workingDir() {
        return workingDir != null ? workingDir: Paths.get(".").toFile().getAbsoluteFile();
    }

    /** 
     * @see runwar.options.ServerOptions#workingDir(java.io.File)
     */
    @Override
    public ServerOptions workingDir(File workingDir) {
        this.workingDir = workingDir;
        return this;
    }

    /**
     * @see runwar.options.ServerOptions#warFile()
     */
    @Override
    public File warFile() {
        return warFile;
    }

    /**
     * @see runwar.options.ServerOptions#warFile(java.io.File)
     */
    @Override
    public ServerOptions warFile(File warFile) {
        this.warFile = warFile;
        return this;
    }

    /**
     * @see runwar.options.ServerOptions#webInfDir()
     */
    @Override
    public File webInfDir() {
        if(webInfDir == null) {
            if (webXmlFile != null && (webXmlFile.getParentFile().getName().equalsIgnoreCase("WEB-INF") || new File(webXmlFile.getParentFile(), "lib").exists())) {
                webInfDir = webXmlFile.getParentFile();
            } else if(warFile() != null && warFile.exists() && warFile.isDirectory()) {
                webInfDir = new File(warFile, "WEB-INF");
            }
        }
        return webInfDir;
    }

    /** 
     * @see runwar.options.ServerOptions#webInfDir(java.io.File)
     */
    @Override
    public ServerOptions webInfDir(File webInfDir) {
        this.webInfDir = webInfDir;
        return this;
    }

    /** 
     * @see runwar.options.ServerOptions#webXmlFile()
     */
    @Override
    public File webXmlFile() {
        if(webXmlFile == null && webInfDir() != null) {
            webXmlFile(new File(webInfDir(),"web.xml"));
        }
        return webXmlFile;
    }

    /** 
     * @see runwar.options.ServerOptions#webXmlPath()
     */
    @Override
    public String webXmlPath() throws MalformedURLException {
        return webXmlFile.toURI().toURL().toString();
    }

    /** 
     * @see runwar.options.ServerOptions#webXmlFile(java.io.File)
     */
    @Override
    public ServerOptions webXmlFile(File webXmlFile) {
        this.webXmlFile = webXmlFile;
        return this;
    }

    /** 
     * @see runwar.options.ServerOptions#iconImage()
     */
    @Override
    public String iconImage() {
        return iconImage;
    }

    /** 
     * @see runwar.options.ServerOptions#iconImage(java.lang.String)
     */
    @Override
    public ServerOptions iconImage(String iconImage) {
        this.iconImage = iconImage;
        return this;
    }

    /** 
     * @see runwar.options.ServerOptions#trayConfig()
     */
    @Override
    public File trayConfig() {
        return trayConfig;
    }
    
    /** 
     * @see runwar.options.ServerOptions#trayConfig(java.io.File)
     */
    @Override
    public ServerOptions trayConfig(File trayConfig) {
        this.trayConfig = trayConfig;
        return this;
    }
    
        /** 
     * @see runwar.options.ServerOptions#predicateFile()
     */
    @Override
    public File predicateFile() {
        return predicateFile;
    }
    
    /** 
     * @see runwar.options.ServerOptions#predicateFile(java.io.File)
     */
    @Override
    public ServerOptions predicateFile(File predicateFile) {
        this.predicateFile = predicateFile;
        return this;
    }

    /** 
     * @see runwar.options.ServerOptions#trayConfigJSON()
     */
    @Override
    public JSONArray trayConfigJSON() {
        return trayConfigJSON;
    }

    /** 
     * @see
     * runwar.options.ServerOptions#trayConfig(net.minidev.json.JSONArray)
     */
    @Override
    public ServerOptions trayConfig(JSONArray trayConfig) {
        this.trayConfigJSON = trayConfig;
        return this;
    }

    /** 
     * @see runwar.options.ServerOptions#trayEnable()
     */
    @Override
    public boolean trayEnable() {
        return trayEnable;
    }

    /** 
     * @see runwar.options.ServerOptions#trayEnable(boolean)
     */
    @Override
    public ServerOptions trayEnable(boolean enable) {
        this.trayEnable = enable;
        return this;
    }

    
    @Override
    public String defaultShell() {
        return defaultShell;
    }
    
    @Override
    public String browser() {
        return browser;
    }
    
    public ServerOptions browser(String browser){
        this.browser = browser;
        return this;
    }

    public ServerOptions defaultShell(String defaultShell) {
        this.defaultShell = defaultShell;
        return this;
    }
    
    
    
     /** 
     * @see runwar.options.ServerOptions#dockEnable()
     */
    @Override
    public boolean dockEnable() {
        return dockEnable;
    }

    /** 
     * @see runwar.options.ServerOptions#dockEnable(boolean)
     */
    @Override
    public ServerOptions dockEnable(boolean enable) {
        this.dockEnable = enable;
        return this;
    }

    /** 
     * @see runwar.options.ServerOptions#statusFile()
     */
    @Override
    public File statusFile() {
        return statusFile;
    }

    /** 
     * @see runwar.options.ServerOptions#statusFile(java.io.File)
     */
    @Override
    public ServerOptions statusFile(File statusFile) {
        this.statusFile = statusFile;
        return this;
    }

    /** 
     * @see runwar.options.ServerOptions#cfmlServletConfigWebDir()
     */
    @Override
    public String cfmlServletConfigWebDir() {
        return cfmlServletConfigWebDir;
    }

    /** 
     * @see
     * runwar.options.ServerOptions#cfmlServletConfigWebDir(java.lang.String)
     */
    @Override
    public ServerOptions cfmlServletConfigWebDir(String cfmlServletConfigWebDir) {
        this.cfmlServletConfigWebDir = cfmlServletConfigWebDir;
        return this;
    }

    /** 
     * @see runwar.options.ServerOptions#cfmlServletConfigServerDir()
     */
    @Override
    public String cfmlServletConfigServerDir() {
        if (cfmlServletConfigServerDir == null)
            cfmlServletConfigServerDir = System.getProperty("cfml.server.config.dir");
        return cfmlServletConfigServerDir;
    }

    /** 
     * @see
     * runwar.options.ServerOptions#cfmlServletConfigServerDir(java.lang.String)
     */
    @Override
    public ServerOptions cfmlServletConfigServerDir(String cfmlServletConfigServerDir) {
        this.cfmlServletConfigServerDir = cfmlServletConfigServerDir;
        return this;
    }

    /** 
     * @see runwar.options.ServerOptions#cacheEnable()
     */
    @Override
    public boolean cacheEnable() {
        return cacheEnable;
    }

    /** 
     * @see runwar.options.ServerOptions#cacheEnable(boolean)
     */
    @Override
    public ServerOptions cacheEnable(boolean cacheEnable) {
        this.cacheEnable = cacheEnable;
        return this;
    }

    /** 
     * @see runwar.options.ServerOptions#directoryListingEnable()
     */
    @Override
    public boolean directoryListingEnable() {
        return directoryListingEnable;
    }

    /** 
     * @see runwar.options.ServerOptions#directoryListingEnable(boolean)
     */
    @Override
    public ServerOptions directoryListingEnable(boolean directoryListingEnable) {
        this.directoryListingEnable = directoryListingEnable;
        return this;
    }

    /** 
     * @see runwar.options.ServerOptions#directoryListingRefreshEnable()
     */
    @Override
    public boolean directoryListingRefreshEnable() {
        return directoryListingRefreshEnable;
    }

    /** 
     * @see
     * runwar.options.ServerOptions#directoryListingRefreshEnable(boolean)
     */
    @Override
    public ServerOptions directoryListingRefreshEnable(boolean directoryListingRefreshEnable) {
        this.directoryListingRefreshEnable = directoryListingRefreshEnable;
        return this;
    }

    /** 
     * @see runwar.options.ServerOptions#welcomeFiles()
     */
    @Override
    public String[] welcomeFiles() {
        return welcomeFiles;
    }

    /** 
     * @see runwar.options.ServerOptions#welcomeFiles(java.lang.String[])
     */
    @Override
    public ServerOptions welcomeFiles(String[] welcomeFiles) {
        this.welcomeFiles = welcomeFiles;
        return this;
    }

    /**
     * @see runwar.options.ServerOptions#warUriString()
     */
    @Override
    public String warUriString() {
        if(warFile() == null)
            return "";
        try {
            return warFile().toURI().toURL().toString();
        } catch (MalformedURLException e) {
            e.printStackTrace();
            return "";
        }
    }

    /** 
     * @see runwar.options.ServerOptions#sslCertificate(java.io.File)
     */
    @Override
    public ServerOptions sslCertificate(File file) {
        this.sslCertificate = file;
        return this;
    }

    /** 
     * @see runwar.options.ServerOptions#sslCertificate()
     */
    @Override
    public File sslCertificate() {
        if(sslCertificate != null && !sslCertificate.exists() && !sslSelfSign){
            throw new IllegalArgumentException("Certificate file does not exist: " + sslCertificate.getAbsolutePath());
        }
        return this.sslCertificate;
    }

    /** 
     * @see runwar.options.ServerOptions#sslKey(java.io.File)
     */
    @Override
    public ServerOptions sslKey(File file) {
        this.sslKey = file;
        return this;
    }

    /** 
     * @see runwar.options.ServerOptions#sslKey()
     */
    @Override
    public File sslKey() {
        return this.sslKey;
    }

    /** 
     * @see runwar.options.ServerOptions#sslKeyPass(char[])
     */
    @Override
    public ServerOptions sslKeyPass(char[] pass) {
        this.sslKeyPass = pass;
        return this;
    }

    /** 
     * @see runwar.options.ServerOptions#sslKeyPass()
     */
    @Override
    public char[] sslKeyPass() {
        return this.sslKeyPass;
    }

    /** 
     * @see runwar.options.ServerOptions#stopPassword(char[])
     */
    @Override
    public ServerOptions stopPassword(char[] password) {
        this.stopPassword = password;
        return this;
    }

    /** 
     * @see runwar.options.ServerOptions#stopPassword()
     */
    @Override
    public char[] stopPassword() {
        return this.stopPassword;
    }

    /** 
     * @see runwar.options.ServerOptions#action(java.lang.String)
     */
    @Override
    public ServerOptions action(String action) {
        this.action = action;
        return this;
    }

    /** 
     * @see runwar.options.ServerOptions#action()
     */
    @Override
    public String action() {
        return this.action;
    }

    /** 
     * @see runwar.options.ServerOptions#cfEngineName(java.lang.String)
     */
    @Override
    public ServerOptions cfEngineName(String cfengineName) {
        if (cfengineName.toLowerCase().equals("lucee") || cfengineName.toLowerCase().equals("adobe")
                || cfengineName.toLowerCase().equals("railo") || cfengineName.toLowerCase().equals("")) {
            this.cfengineName = cfengineName.toLowerCase();
        } else {
            throw new RuntimeException(
                    "Unknown engine type: " + cfengineName + ", must be one of: lucee, adobe, railo");
        }
        return this;
    }

    /** 
     * @see runwar.options.ServerOptions#cfEngineName()
     */
    @Override
    public String cfEngineName() {
        if(cfengineName.isEmpty() && webInfDir() != null && webInfDir().exists() && new File(webInfDir(),"cfusion").exists()) {
            cfengineName = "adobe";
        } else if(cfengineName.isEmpty() && webInfDir() != null && webInfDir().exists() && new File(webInfDir(),"lucee").exists()) {
            cfengineName = "lucee";
        }
        return cfengineName;
    }

    /** 
     * @see runwar.options.ServerOptions#customHTTPStatusEnable(boolean)
     */
    @Override
    public ServerOptions customHTTPStatusEnable(boolean enable) {
        this.customHTTPStatusEnable = enable;
        return this;
    }

    /** 
     * @see runwar.options.ServerOptions#customHTTPStatusEnable()
     */
    @Override
    public boolean customHTTPStatusEnable() {
        return this.customHTTPStatusEnable;
    }

    /** 
     * @see runwar.options.ServerOptions#sendfileEnable(boolean)
     */
    @Override
    public ServerOptions sendfileEnable(boolean enable) {
        if (!enable) {
            this.transferMinSize = Long.MAX_VALUE;
        }
        return this;
    }

    /** 
     * @see runwar.options.ServerOptions#transferMinSize(java.lang.Long)
     */
    @Override
    public ServerOptions transferMinSize(Long minSize) {
        this.transferMinSize = minSize;
        return this;
    }

    /** 
     * @see runwar.options.ServerOptions#transferMinSize()
     */
    @Override
    public Long transferMinSize() {
        return this.transferMinSize;
    }

    /** 
     * @see runwar.options.ServerOptions#gzipEnable(boolean)
     */
    @Override
    public ServerOptions gzipEnable(boolean enable) {
        this.gzipEnable = enable;
        return this;
    }

    /** 
     * @see runwar.options.ServerOptions#gzipEnable()
     */
    @Override
    public boolean gzipEnable() {
        return this.gzipEnable;
    }
    
    
    @Override
    public ServerOptions gzipPredicate(String predicate) {
        this.gzipPredicate = predicate;
        return this;
    }

    @Override
    public String gzipPredicate() {
        return this.gzipPredicate;
    }

    /** 
     * @see runwar.options.ServerOptions#mariaDB4jEnable(boolean)
     */
    @Override
    public ServerOptions mariaDB4jEnable(boolean enable) {
        this.mariadb4jEnable = enable;
        return this;
    }

    /** 
     * @see runwar.options.ServerOptions#mariaDB4jEnable()
     */
    @Override
    public boolean mariaDB4jEnable() {
        return this.mariadb4jEnable;
    }

    /** 
     * @see runwar.options.ServerOptions#mariaDB4jPort(int)
     */
    @Override
    public ServerOptions mariaDB4jPort(int port) {
        this.mariadb4jPort = port;
        return this;
    }

    /** 
     * @see runwar.options.ServerOptions#mariaDB4jPort()
     */
    @Override
    public int mariaDB4jPort() {
        return this.mariadb4jPort;
    }

    /** 
     * @see runwar.options.ServerOptions#mariaDB4jBaseDir(java.io.File)
     */
    @Override
    public ServerOptions mariaDB4jBaseDir(File dir) {
        this.mariadb4jBaseDir = dir;
        return this;
    }

    /** 
     * @see runwar.options.ServerOptions#mariaDB4jBaseDir()
     */
    @Override
    public File mariaDB4jBaseDir() {
        return this.mariadb4jBaseDir;
    }

    /** 
     * @see runwar.options.ServerOptions#mariaDB4jDataDir(java.io.File)
     */
    @Override
    public ServerOptions mariaDB4jDataDir(File dir) {
        this.mariadb4jDataDir = dir;
        return this;
    }

    /** 
     * @see runwar.options.ServerOptions#mariaDB4jDataDir()
     */
    @Override
    public File mariaDB4jDataDir() {
        return this.mariadb4jDataDir;
    }

    /** 
     * @see runwar.options.ServerOptions#mariaDB4jImportSQLFile(java.io.File)
     */
    @Override
    public ServerOptions mariaDB4jImportSQLFile(File file) {
        this.mariadb4jImportSQLFile = file;
        return this;
    }

    /** 
     * @see runwar.options.ServerOptions#mariaDB4jImportSQLFile()
     */
    @Override
    public File mariaDB4jImportSQLFile() {
        return this.mariadb4jImportSQLFile;
    }

    /** 
     * @see runwar.options.ServerOptions#jvmArgs(java.util.List)
     */
    @Override
    public ServerOptions jvmArgs(List<String> args) {
        this.jvmArgs = args;
        return this;
    }

    /** 
     * @see runwar.options.ServerOptions#jvmArgs()
     */
    @Override
    public List<String> jvmArgs() {
        return this.jvmArgs;
    }

    /** 
     * @see runwar.options.ServerOptions#errorPages(java.lang.String)
     */
    @Override
    public ServerOptions errorPages(String errorpages) {
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

    /** 
     * @see runwar.options.ServerOptions#errorPages(java.util.Map)
     */
    @Override
    public ServerOptions errorPages(Map<Integer, String> errorpages) {
        this.errorPages = errorpages;
        return this;
    }

    /** 
     * @see runwar.options.ServerOptions#errorPages()
     */
    @Override
    public Map<Integer, String> errorPages() {
        return this.errorPages;
    }

    /** 
     * @see runwar.options.ServerOptions#servletRestEnable(boolean)
     */
    @Override
    public ServerOptions servletRestEnable(boolean enable) {
        this.servletRestEnable = enable;
        return this;
    }

    /** 
     * @see runwar.options.ServerOptions#servletRestEnable()
     */
    @Override
    public boolean servletRestEnable() {
        return this.servletRestEnable;
    }

    /** 
     * @see
     * runwar.options.ServerOptions#servletRestMappings(java.lang.String)
     */
    @Override
    public ServerOptions servletRestMappings(String mappings) {
        return servletRestMappings(mappings.split(","));
    }

    /** 
     * @see
     * runwar.options.ServerOptions#servletRestMappings(java.lang.String[])
     */
    @Override
    public ServerOptions servletRestMappings(String[] mappings) {
        this.servletRestMappings = mappings;
        return this;
    }

    /** 
     * @see runwar.options.ServerOptions#servletRestMappings()
     */
    @Override
    public String[] servletRestMappings() {
        return this.servletRestMappings;
    }

    /** 
     * @see runwar.options.ServerOptions#filterPathInfoEnable(boolean)
     */
    @Override
    public ServerOptions filterPathInfoEnable(boolean enable) {
        this.filterPathInfoEnable = enable;
        return this;
    }

    /** 
     * @see runwar.options.ServerOptions#filterPathInfoEnable()
     */
    @Override
    public boolean filterPathInfoEnable() {
        return this.filterPathInfoEnable;
    }

    /** 
     * @see runwar.options.ServerOptions#basicAuthEnable(boolean)
     */
    @Override
    public ServerOptions basicAuthEnable(boolean enable) {
        this.enableBasicAuth = enable;
        return this;
    }

    /** 
     * @see runwar.options.ServerOptions#basicAuthEnable()
     */
    @Override
    public boolean basicAuthEnable() {
        return this.enableBasicAuth;
    }

    /** 
     * @see runwar.options.ServerOptions#basicAuth(java.lang.String)
     */
    @Override
    public ServerOptions basicAuth(String userPasswordList) {
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
        return basicAuth(ups);
    }

    /** 
     * @see runwar.options.ServerOptions#basicAuth(java.util.Map)
     */
    @Override
    public ServerOptions basicAuth(Map<String, String> userPassList) {
        userPasswordList = userPassList;
        return this;
    }

    /** 
     * @see runwar.options.ServerOptions#basicAuth()
     */
    @Override
    public Map<String, String> basicAuth() {
        return userPasswordList;
    }

    /** 
     * @see runwar.options.ServerOptions#sslAddCerts(java.lang.String)
     */
    @Override
    public ServerOptions sslAddCerts(String sslCerts) {
        return sslAddCerts(sslCerts.split("(?<!\\\\),"));
    }

    /** 
     * @see runwar.options.ServerOptions#sslAddCerts(java.lang.String[])
     */
    @Override
    public ServerOptions sslAddCerts(String[] sslCerts) {
        this.sslAddCerts = sslCerts;
        return this;
    }

    /** 
     * @see runwar.options.ServerOptions#sslAddCerts()
     */
    @Override
    public String[] sslAddCerts() {
        return this.sslAddCerts;
    }

    /** 
     * @see runwar.options.ServerOptions#bufferSize()
     */
    @Override
    public int bufferSize() {
        return bufferSize;
    }

    /** 
     * @see runwar.options.ServerOptions#bufferSize(int)
     */
    @Override
    public ServerOptions bufferSize(int bufferSize) {
        this.bufferSize = bufferSize;
        return this;
    }

    /** 
     * @see runwar.options.ServerOptions#ioThreads()
     */
    @Override
    public int ioThreads() {
        return ioThreads;
    }

    /** 
     * @see runwar.options.ServerOptions#ioThreads(int)
     */
    @Override
    public ServerOptions ioThreads(int ioThreads) {
        this.ioThreads = ioThreads;
        return this;
    }

    /** 
     * @see runwar.options.ServerOptions#workerThreads()
     */
    @Override
    public int workerThreads() {
        return workerThreads;
    }

    /** 
     * @see runwar.options.ServerOptions#workerThreads(int)
     */
    @Override
    public ServerOptions workerThreads(int workerThreads) {
        this.workerThreads = workerThreads;
        return this;
    }

    /** 
     * @see runwar.options.ServerOptions#directBuffers(boolean)
     */
    @Override
    public ServerOptions directBuffers(boolean enable) {
        this.directBuffers = enable;
        return this;
    }

    /** 
     * @see runwar.options.ServerOptions#directBuffers()
     */
    @Override
    public boolean directBuffers() {
        return this.directBuffers;
    }

    /** 
     * @see runwar.options.ServerOptions#loadBalance(java.lang.String)
     */
    @Override
    public ServerOptions loadBalance(String hosts) {
        return loadBalance(hosts.split("(?<!\\\\),"));
    }

    /** 
     * @see runwar.options.ServerOptions#loadBalance(java.lang.String[])
     */
    @Override
    public ServerOptions loadBalance(String[] hosts) {
        this.loadBalance = hosts;
        return this;
    }

    /** 
     * @see runwar.options.ServerOptions#loadBalance()
     */
    @Override
    public String[] loadBalance() {
        return this.loadBalance;
    }

    /** 
     * @see runwar.options.ServerOptions#proxyPeerAddressEnable(boolean)
     */
    @Override
    public ServerOptions proxyPeerAddressEnable(boolean enable) {
        this.proxyPeerAddressEnable = enable;
        return this;
    }

    /** 
     * @see runwar.options.ServerOptions#proxyPeerAddressEnable()
     */
    @Override
    public boolean proxyPeerAddressEnable() {
        return this.proxyPeerAddressEnable;
    }

    /** 
     * @see runwar.options.ServerOptions#http2Enable(boolean)
     */
    @Override
    public ServerOptions http2Enable(boolean enable) {
        this.http2enable = enable;
        return this;
    }

    /** 
     * @see runwar.options.ServerOptions#http2Enable()
     */
    @Override
    public boolean http2Enable() {
        return this.http2enable;
    }

    /** 
     * @see runwar.options.ServerOptions#secureCookies(boolean)
     */
    @Override
    public ServerOptions secureCookies(boolean enable) {
        this.secureCookies = enable;
        this.cookieHttpOnly = enable;
        this.cookieSecure = enable;
        return this;
    }

    /*
     * @see runwar.options.ServerOptions#secureCookies()
     */
    @Override
    public boolean secureCookies() {
        return this.secureCookies;
    }

    /** 
     * @see runwar.options.ServerOptions#cookieHttpOnly(boolean)
     */
    @Override
    public ServerOptions cookieHttpOnly(boolean enable) {
        this.cookieHttpOnly= enable;
        return this;
    }

    /*
     * @see runwar.options.ServerOptions#cookieHttpOnly()
     */
    @Override
    public boolean cookieHttpOnly() {
        return this.cookieHttpOnly;
    }

    /** 
     * @see runwar.options.ServerOptions#cookieSecure(boolean)
     */
    @Override
    public ServerOptions cookieSecure(boolean enable) {
        this.cookieSecure = enable;
        return this;
    }

    /*
     * @see runwar.options.ServerOptions#cookieSecure()
     */
    @Override
    public boolean cookieSecure() {
        return this.cookieSecure;
    }
    
    /*
     * @see runwar.options.ServerOptions#secureCookies()
     */
    @Override
    public String serverMode() {
        if(webInfDir() != null && webInfDir().exists()) {
            return Mode.WAR;
        } else if( new File(warFile(), "WEB-INF").exists()) {
            return Mode.WAR;
        }
        return Mode.DEFAULT;
    }


    /*
     * @see runwar.options.ServerOptions#bufferEnable(boolean)
     */
    @Override
    public ServerOptions bufferEnable(boolean enable) {
        this.bufferEnable = enable;
        return this;
    }

    /*
     * @see runwar.options.ServerOptions#bufferEnable()
     */
    @Override
    public boolean bufferEnable() {
        return this.bufferEnable ;
    }
    
    /*
     * @see runwar.options.ServerOptions#startedFromCommandLine(boolean)
     */
    @Override
    public ServerOptions startedFromCommandLine(boolean enable) {
        this.startedFromCommandline = enable;
        return this;
    }

    /*
     * @see runwar.options.ServerOptions#startedFromCommandLine()
     */
    @Override
    public boolean startedFromCommandLine() {
        return this.startedFromCommandline;
    }

    /*
     * @see runwar.options.ServerOptions#http2ProxySSLPort()
     */
    @Override
    public int http2ProxySSLPort() {
        return http2ProxySSLPort;
    }

    /*
     * @see runwar.options.ServerOptions#setsetHttp2ProxySSLPort(int)
     */
    @Override
    public ServerOptions http2ProxySSLPort(int portNumber) {
        http2ProxySSLPort = portNumber;
        return this;
    }

    /*
     * @see runwar.options.ServerOptions#SSLECCDISABLE(boolean)
     */
    @Override
    public ServerOptions sslEccDisable(boolean enable) {
        this.sslEccDisable = enable;
        return this;
    }

    /*
     * @see runwar.options.ServerOptions#SSLECCDISABLE()
     */
    @Override
    public boolean sslEccDisable() {
        return this.sslEccDisable;
    }

    /*
     * @see runwar.options.ServerOptions#sslSelfSign(boolean)
     */
    @Override
    public ServerOptions sslSelfSign(boolean enable) {
        this.sslSelfSign = enable;
        return this;
    }

    /*
     * @see runwar.options.ServerOptions#sslSelfSign()
     */
    @Override
    public boolean sslSelfSign() { return this.sslSelfSign; }

    /*
     * @see runwar.options.ServerOptions#ignoreWebXmlWelcomePages()
     */
    @Override
    public boolean ignoreWebXmlWelcomePages() {
        return welcomeFiles() != null && welcomeFiles().length > 0;
    }

    /*
     * @see runwar.options.ServerOptions#ignoreWebXmlWelcomePages()
     */
    @Override
    public boolean ignoreWebXmlRestMappings() {
        return servletRestMappings() != null && servletRestMappings().length > 0;
    }

    /*
     * @see runwar.options.ServerOptions#service()
     */
    @Override
    public boolean service() {
        return service;
    }

    /*
     * @see runwar.options.ServerOptions#service(boolean)
     */
    @Override
    public ServerOptions service(boolean enable) {
        service = enable;
        return this;
    }

    /*
     * @see runwar.options.ServerOptions#defaultServletAllowedExt()
     */
    @Override
    public String defaultServletAllowedExt() {
        return defaultServletAllowedExt;
    }

    /*
     * @see runwar.options.ServerOptions#defaultServletAllowedExt(String)
     */
    @Override
    public ServerOptions defaultServletAllowedExt(String defaultServletAllowedExt) {
    	this.defaultServletAllowedExt = defaultServletAllowedExt;
        return this;
    }

    /*
     * @see runwar.options.ServerOptions#resourceManagerLogging()
     */
    @Override
    public Boolean resourceManagerLogging() {
        return resourceManagerLogging;
    }

    /*
     * @see runwar.options.ServerOptions#resourceManagerLogging(boolean)
     */
    @Override
    public ServerOptions resourceManagerLogging(Boolean resourceManagerLogging) {
    	this.resourceManagerLogging = resourceManagerLogging;
        return this;
    }    

    /*
     * @see runwar.options.ServerOptions#caseSensitiveWebServer()
     */
    @Override
    public Boolean caseSensitiveWebServer() {
        return caseSensitiveWebServer;
    }

    /*
     * @see runwar.options.ServerOptions#caseSensitiveWebServer(boolean)
     */
    @Override
    public ServerOptions caseSensitiveWebServer(Boolean caseSensitiveWebServer) {
    	this.caseSensitiveWebServer = caseSensitiveWebServer;
        return this;
    }
    
    /**
     * @see runwar.options.ServerOptions#xnioOptions(java.lang.String)
     */
    @Override
    public ServerOptions xnioOptions(String options) {
        String[] optionList = options.split(",");
        for (int x = 0; x < optionList.length; x++) {
            String[] splitted = optionList[x].split("=");
            setOptionMapValue(serverXnioOptions, Options.class, splitted[0].trim(), splitted[1].trim());
        }
        return this;
    }

    /**
     * @see runwar.options.ServerOptions#xnioOptions(OptionMap.Builder)
     */
    @Override
    public ServerOptions xnioOptions(OptionMap.Builder options) {
        this.serverXnioOptions = options;
        return this;
    }

    /**
     * @see runwar.options.ServerOptions#xnioOptions()
     */
    @Override
    public OptionMap.Builder xnioOptions() {
        return this.serverXnioOptions;
    }


    /**
     * @see runwar.options.ServerOptions#xnioOptions(java.lang.String)
     */
    @Override
    public ServerOptions undertowOptions(String options) {
        String[] optionList = options.split(",");
        for (int x = 0; x < optionList.length; x++) {
            String[] splitted = optionList[x].split("=");
            String optionName = splitted[0].trim().toUpperCase();
            String optionValue = splitted[1].trim();
            setOptionMapValue(undertowOptions, UndertowOptions.class, optionName, optionValue);
        }
        return this;
    }

    /**
     * @see runwar.options.ServerOptions#undertowOptions(OptionMap.Builder)
     */
    @Override
    public ServerOptions undertowOptions(OptionMap.Builder options) {
        this.undertowOptions = options;
        return this;
    }

    /**
     * @see runwar.options.ServerOptions#xnioOptions()
     */
    @Override
    public OptionMap.Builder undertowOptions() {
        return this.undertowOptions;
    }

    @Override
    public boolean testing() {
        return testing;
    }

}