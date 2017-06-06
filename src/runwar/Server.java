package runwar;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Timer;
import java.util.TimerTask;
import java.awt.Image;

import javax.net.SocketFactory;
import javax.net.ssl.SSLContext;
import javax.servlet.DispatcherType;
import javax.websocket.server.ServerApplicationConfig;

import org.xnio.BufferAllocator;
import org.xnio.OptionMap;
import org.xnio.Options;
import org.xnio.Xnio;

import runwar.logging.Logger;
import runwar.logging.LogSubverter;
import runwar.mariadb4j.MariaDB4jManager;
import runwar.options.CommandLineHandler;
import runwar.options.ServerOptions;
import runwar.undertow.MappedResourceManager;
import runwar.undertow.WebXMLParser;
import runwar.util.RequestDumper;
import runwar.util.SSLUtil;
import runwar.util.TeeOutputStream;
import runwar.security.SecurityManager;
import io.undertow.Handlers;
import io.undertow.Undertow;
import io.undertow.Undertow.Builder;
import io.undertow.UndertowOptions;
import io.undertow.attribute.ExchangeAttributes;
import io.undertow.predicate.Predicates;
import io.undertow.protocols.ssl.UndertowXnioSsl;
import io.undertow.server.DefaultByteBufferPool;
import io.undertow.server.HandlerWrapper;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.LearningPushHandler;
import io.undertow.server.handlers.PathHandler;
import io.undertow.server.handlers.PredicateHandler;
import io.undertow.server.handlers.ProxyPeerAddressHandler;
import io.undertow.server.handlers.RequestDumpingHandler;
import io.undertow.server.handlers.ResponseCodeHandler;
import io.undertow.server.handlers.SSLHeaderHandler;
import io.undertow.server.handlers.cache.CacheHandler;
import io.undertow.server.handlers.cache.DirectBufferCache;
import io.undertow.server.handlers.encoding.ContentEncodingRepository;
import io.undertow.server.handlers.encoding.EncodingHandler;
import io.undertow.server.handlers.encoding.GzipEncodingProvider;
import io.undertow.server.handlers.proxy.LoadBalancingProxyClient;
import io.undertow.server.handlers.proxy.ProxyHandler;
import io.undertow.server.handlers.resource.CachingResourceManager;
import io.undertow.server.handlers.resource.PathResourceManager;
import io.undertow.server.handlers.resource.ResourceHandler;
import io.undertow.server.handlers.resource.ResourceManager;
import io.undertow.server.protocol.http2.Http2UpgradeHandler;
import io.undertow.server.session.InMemorySessionManager;
import io.undertow.server.session.SessionAttachmentHandler;
import io.undertow.server.session.SessionCookieConfig;
import io.undertow.servlet.api.DeploymentInfo;
import io.undertow.servlet.api.DeploymentManager;
import io.undertow.servlet.api.DeploymentManager.State;
import io.undertow.servlet.api.ErrorPage;
import io.undertow.servlet.api.FilterInfo;
import io.undertow.servlet.api.MimeMapping;
import io.undertow.servlet.api.ServletInfo;
import io.undertow.servlet.handlers.DefaultServlet;
import io.undertow.util.Headers;
import io.undertow.util.MimeMappings;
import io.undertow.util.StatusCodes;
import io.undertow.websockets.jsr.WebSocketDeploymentInfo;

import static io.undertow.Handlers.predicate;
import static io.undertow.Handlers.resource;
import static io.undertow.predicate.Predicates.secure;

import static io.undertow.servlet.Servlets.defaultContainer;
import static io.undertow.servlet.Servlets.deployment;
import static io.undertow.servlet.Servlets.servlet;
import static java.nio.file.StandardCopyOption.*;

public class Server {

    private static Logger log = Logger.getLogger("RunwarLogger");
    private TeeOutputStream tee;
    private volatile static ServerOptions serverOptions;
    private static MariaDB4jManager mariadb4jManager;
    int portNumber;
    int socketNumber;
    private DeploymentManager manager;
    private Undertow undertow;
    private MonitorThread monitor;

    private String PID;
    private volatile String serverState = ServerState.STOPPED;

    private static ClassLoader _classLoader;

    private String serverName = "default";
    private File statusFile = null;
    public static final String bar = "******************************************************************************";
    private String[] defaultWelcomeFiles = new String[] { "index.cfm", "index.cfml", "default.cfm", "index.html", "index.htm",
            "default.html", "default.htm" };
    private SSLContext sslContext;
    private Thread shutDownThread;

    private static final int METADATA_MAX_AGE = 2000;
    private static final Thread mainThread = Thread.currentThread();

    public Server() {
    }

    // for openBrowser 
    public Server(int seconds) {
        Timer timer = new Timer();
        timer.schedule(this.new OpenBrowserTask(), seconds * 1000);
    }

    protected void initClassLoader(List<URL> _classpath) {
        if (_classLoader == null) {
            log.debug("Loading classes from lib dir");
            if( _classpath != null && _classpath.size() > 0) {
                log.tracef("classpath: %s",_classpath);
                _classLoader = new URLClassLoader(_classpath.toArray(new URL[_classpath.size()]));
    //          _classLoader = new URLClassLoader(_classpath.toArray(new URL[_classpath.size()]),Thread.currentThread().getContextClassLoader());
    //          _classLoader = new URLClassLoader(_classpath.toArray(new URL[_classpath.size()]),ClassLoader.getSystemClassLoader());
    //          _classLoader = new XercesFriendlyURLClassLoader(_classpath.toArray(new URL[_classpath.size()]),ClassLoader.getSystemClassLoader());
    //          Thread.currentThread().setContextClassLoader(_classLoader);
            } else {
                _classLoader = Thread.currentThread().getContextClassLoader();
            }
        }
    }

    protected void setClassLoader(URLClassLoader classLoader){
        _classLoader = classLoader;
    }
    
    public static ClassLoader getClassLoader(){
        return _classLoader;
    }
    
    public void startServer(String[] args, URLClassLoader classLoader) throws Exception {
        setClassLoader(classLoader);
        startServer(args);
    }
    
    public void ensureJavaVersion() {
        Class<?> nio;
        log.debug("Checking that we're running on > java7");
        try{
            nio = Server.class.getClassLoader().loadClass("java.nio.charset.StandardCharsets");
            nio.getClass().getName();
        } catch (java.lang.ClassNotFoundException e) {
            throw new RuntimeException("Could not load NIO!  Are we running on Java 7 or greater?  Sorry, exiting...");
        }
    }
    
    public void startServer(final String[] args) throws Exception {
        startServer(CommandLineHandler.parseArguments(args));
    }

    public void restartServer(final ServerOptions options) throws Exception {
        LaunchUtil.displayMessage("info", "Restarting server...");
        System.out.println(bar);
        System.out.println("***  Restarting server");
        System.out.println(bar);
        stopServer();
        serverWentDown();
        startServer(options);
    }
    
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void startServer(final ServerOptions options) throws Exception {
        ensureJavaVersion();
        serverOptions = options;
        serverState = ServerState.STARTING;
        if(serverOptions.getAction().equals("stop")){
            Stop.stopServer(serverOptions,true);
        }
        serverName = serverOptions.getServerName();
        portNumber = serverOptions.getPortNumber();
        socketNumber = serverOptions.getSocketNumber();
        String cfengine = serverOptions.getCFEngineName();
        String processName = serverOptions.getProcessName();
        String contextPath = serverOptions.getContextPath();
        String host = serverOptions.getHost();
        File warFile = serverOptions.getWarFile();
        if (serverOptions.getStatusFile() != null) {
            statusFile = serverOptions.getStatusFile();
        }
        String warPath = serverOptions.getWarPath();
        String loglevel = serverOptions.getLoglevel();
        char[] stoppassword = serverOptions.getStopPassword();
        Long transferMinSize= serverOptions.getTransferMinSize();
        boolean ignoreWelcomePages = false;
        boolean ignoreRestMappings = false;

        if (serverOptions.isBackground()) {
            setServerState(ServerState.STARTING_BACKGROUND);
            // this will eventually system.exit();
            LaunchUtil.relaunchAsBackgroundProcess(serverOptions.setBackground(false),true);
            setServerState(ServerState.STARTED_BACKGROUND);
            // just in case
            Thread.sleep(200);
            System.exit(0);
        }
        
        // if the war is archived, unpack it to system temp
        if(warFile.exists() && !warFile.isDirectory()) {
            URL zipResource = warFile.toURI().toURL();
            String warDir = warFile.getName().toLowerCase().replace(".war", "");
            warFile = new File(warFile.getParentFile(), warDir);
            if(!warFile.exists()) {
                warFile.mkdir();
                log.debug("Exploding compressed WAR to " + warFile.getAbsolutePath());
                LaunchUtil.unzipResource(zipResource, warFile, false);
            } else {
                log.debug("Using already exploded WAR in " + warFile.getAbsolutePath());
            }
            warPath = warFile.getAbsolutePath();
            if(serverOptions.getWarFile().getAbsolutePath().equals(serverOptions.getCfmlDirs())) {
                serverOptions.setCfmlDirs(warFile.getAbsolutePath());
            }
        }

        tee = null;
        if (serverOptions.getLogDir() != null) {
            File logDirectory = serverOptions.getLogDir();
            logDirectory.mkdir();
            File outLog = new File(logDirectory,"server.out.txt");
            if (logDirectory.exists()) {
                if(outLog.exists()) {
                    if(Files.size(Paths.get(outLog.getPath())) > 10 * 1024 * 1024) {
                        log.info("Log is over 10MB, moving " + outLog.getPath() + " to " + outLog.getPath() + ".bak");
                        Files.move(Paths.get(outLog.getPath()), Paths.get(outLog.getPath()+".bak"), REPLACE_EXISTING);
                    }
                }
                log.info("Logging to " + outLog.getPath());
                tee = new TeeOutputStream(System.out, new FileOutputStream(outLog.getPath(), outLog.exists()));
                PrintStream newOut = new PrintStream(tee, true);
                System.setOut(newOut);
                System.setErr(newOut);
            } else {
                log.error("Could not create log: " + outLog.getPath());
            }
        }
        
        String osName = System.getProperties().getProperty("os.name");
        String iconPNG = System.getProperty("cfml.server.trayicon");
        if( iconPNG != null && iconPNG.length() > 0) {
            serverOptions.setIconImage(iconPNG);
        }
        String dockIconPath = System.getProperty("cfml.server.dockicon");
        if( dockIconPath == null || dockIconPath.length() == 0) {
            dockIconPath = serverOptions.getIconImage();
        }

        if (osName != null && osName.startsWith("Mac OS X")) {
            Image dockIcon = LaunchUtil.getIconImage(dockIconPath);
            System.setProperty("com.apple.mrj.application.apple.menu.about.name", processName);
            System.setProperty("com.apple.mrj.application.growbox.intrudes", "false");
            System.setProperty("apple.laf.useScreenMenuBar", "true");
            System.setProperty("-Xdock:name", processName);
            try {
                Class<?> appClass = Class.forName("com.apple.eawt.Application");
                Method getAppMethod = appClass.getMethod("getApplication");
                Object appInstance = getAppMethod.invoke(null);
                Method dockMethod = appInstance.getClass().getMethod("setDockIconImage", java.awt.Image.class);
                dockMethod.invoke(appInstance, dockIcon);
            } catch (Exception e) {
                log.warn(e);
            }
        }
        String startingtext = "Starting - port:" + portNumber + " stop-port:" + socketNumber + " warpath:" + warPath;
        startingtext += "\ncontext: " + contextPath + "  -  version: " + getVersion();
        String cfmlDirs = serverOptions.getCfmlDirs();
        if (cfmlDirs.length() > 0) {
            startingtext += "\nweb-dirs: " + cfmlDirs;
        }
        startingtext += "\nLog Directory: " + serverOptions.getLogDir().getAbsolutePath();
        System.out.println(bar);
        System.out.println(startingtext);
        //System.out.println("background: " + background);
        System.out.println(bar);
        addShutDownHook();
        portNumber = getPortOrErrorOut(portNumber, host);
        socketNumber = getPortOrErrorOut(socketNumber, host);
        String cfmlServletConfigWebDir = serverOptions.getCFMLServletConfigWebDir();
        String cfmlServletConfigServerDir = serverOptions.getCFMLServletConfigServerDir();
        File webXmlFile = serverOptions.getWebXmlFile();
        File webinf = new File(warFile, "WEB-INF");
        if (webXmlFile != null && new File(webXmlFile.getParentFile(), "lib").exists()) {
            webinf = webXmlFile.getParentFile();
        }
        String libDirs = serverOptions.getLibDirs();
        URL jarURL = serverOptions.getJarURL();
        // If this folder is a proper war, add its WEB-INF/lib folder to the passed libDirs
        if (warFile.isDirectory() && new File(webinf, "/web.xml").exists()) {
        	if (libDirs == null) {
        		libDirs = "";
        	} else if( libDirs.length() > 0 ) {
        		libDirs = libDirs + ","; 
        	}
    		libDirs = libDirs + webinf.getAbsolutePath() + "/lib";
            log.info("Adding additional lib dir of: " + webinf.getAbsolutePath() + "/lib");
        }

        List<URL> cp = new ArrayList<URL>();
        if (libDirs != null)
            cp.addAll(getJarList(libDirs));
        if (jarURL != null)
            cp.add(jarURL);
        
        if(serverOptions.getMariaDB4jImportSQLFile() != null){
            System.out.println("ADDN"+serverOptions.getMariaDB4jImportSQLFile().toURI().toURL());
            cp.add(serverOptions.getMariaDB4jImportSQLFile().toURI().toURL());
        }
        cp.addAll(getClassesList(new File(webinf, "/classes")));
        initClassLoader(cp);
        
        mariadb4jManager = new MariaDB4jManager(_classLoader);
        
        if(serverOptions.getWelcomeFiles() != null && serverOptions.getWelcomeFiles().length > 0) {
            ignoreWelcomePages = true;
        } else {
            serverOptions.setWelcomeFiles(defaultWelcomeFiles);
        }
        if(serverOptions.getServletRestMappings() != null && serverOptions.getServletRestMappings().length > 0) {
            ignoreRestMappings = true;
        }

        log.debug("Transfer Min Size: " + serverOptions.getTransferMinSize());

        final DeploymentInfo servletBuilder = deployment()
                .setContextPath(contextPath.equals("/") ? "" : contextPath)
                .setTempDir(new File(System.getProperty("java.io.tmpdir")))
                .setDeploymentName(warPath)
                .setServerName( "WildFly / Undertow" );

        if (!warFile.exists()) {
            throw new RuntimeException("war does not exist: " + warFile.getAbsolutePath());
        }

        // hack to prevent . being picked up as the system path (jacob.x.dll)
        if (System.getProperty("java.library.path") == null) {
            if (webXmlFile != null) {
                System.setProperty("java.library.path", getThisJarLocation().getPath() 
                        + ':' + new File(webXmlFile.getParentFile(), "lib").getPath());
            } else {
                System.setProperty("java.library.path", getThisJarLocation().getPath() 
                        + ':' + new File(warFile, "/WEB-INF/lib/").getPath());
            }
        } else {
            System.setProperty("java.library.path",
                    getThisJarLocation().getPath() + System.getProperty("path.separator") + System.getProperty("java.library.path"));
        }
        log.trace("java.library.path:" + System.getProperty("java.library.path"));

        if (System.getProperty("coldfusion.home") == null) {
            String cfusionDir = new File(webinf,"cfusion").getAbsolutePath();
            if (webXmlFile != null) {
                cfusionDir = new File(webXmlFile.getParentFile(),"cfusion").getAbsolutePath();
            }
            log.debug("Setting coldfusion home:" + cfusionDir);
            System.setProperty("coldfusion.home", cfusionDir);
            System.setProperty("coldfusion.rootDir", cfusionDir);
//            System.setProperty("javax.servlet.context.tempdir", cfusionDir + "/../cfclasses");
            System.setProperty("coldfusion.libPath", cfusionDir + "/lib");
            System.setProperty("flex.dir", new File(webinf,"cfform").getAbsolutePath());
            System.setProperty("coldfusion.jsafe.defaultalgo", "FIPS186Random");
            System.setProperty("coldfusion.classPath", cfusionDir + "/lib/updates/," + cfusionDir + "/lib/,"
                    + cfusionDir + "/lib/axis2,"+ cfusionDir + "/gateway/lib/,"+ cfusionDir + "/../cfform/jars,"
                    + cfusionDir + "/../flex/jars,"+ cfusionDir + "/lib/oosdk/lib,"+ cfusionDir + "/lib/oosdk/classes");
            System.setProperty("java.security.policy", cfusionDir + "/lib/coldfusion.policy");
            System.setProperty("java.security.auth.policy", cfusionDir + "/lib/neo_jaas.policy");
            System.setProperty("java.nixlibrary.path", cfusionDir + "/lib");
            System.setProperty("java.library.path", cfusionDir + "/lib");
        }

        if(warFile.isDirectory() && !new File(webinf, "/web.xml").exists()) {
            if (cfmlServletConfigWebDir == null) {
                File webConfigDirFile = new File(getThisJarLocation().getParentFile(), "engine/cfml/server/cfml-web/");
                cfmlServletConfigWebDir = webConfigDirFile.getPath() + "/" + serverName;
            }
            log.debug("cfml.web.config.dir: " + cfmlServletConfigWebDir);
            if (cfmlServletConfigServerDir == null || cfmlServletConfigServerDir.length() == 0) {
                File serverConfigDirFile = new File(getThisJarLocation().getParentFile(), "engine/cfml/server/");
                cfmlServletConfigServerDir = serverConfigDirFile.getAbsolutePath();
            }
            log.debug("cfml.server.config.dir: " + cfmlServletConfigServerDir);
            String webinfDir = System.getProperty("cfml.webinf");
            if (webinfDir == null) {
                webinf = new File(cfmlServletConfigWebDir, "WEB-INF/");
            } else {
                webinf = new File(webinfDir);
            }
            log.debug("cfml.webinf: " + webinf.getPath());

            // servletBuilder.setResourceManager(new CFMLResourceManager(new
            // File(homeDir,"server/"), transferMinSize, cfmlDirs));
            File internalCFMLServerRoot = webinf;
            internalCFMLServerRoot.mkdirs();
            servletBuilder.setResourceManager(getResourceManager(warFile, transferMinSize, cfmlDirs, internalCFMLServerRoot));

            if (webXmlFile != null) {
                log.debug("using specified web.xml : " + webXmlFile.getAbsolutePath());
                servletBuilder.setClassLoader(_classLoader);
                WebXMLParser.parseWebXml(webXmlFile, servletBuilder, ignoreWelcomePages, ignoreRestMappings);
            } else {
                if (_classLoader == null) {
                    throw new RuntimeException("FATAL: Could not load any libs for war: " + warFile.getAbsolutePath());
                }
                servletBuilder.setClassLoader(_classLoader);
                Class cfmlServlet;
                Class restServletClass;
                try {
                    cfmlServlet = _classLoader.loadClass(cfengine + ".loader.servlet.CFMLServlet");
                    log.debug("dynamically loaded CFML servlet from runwar child classloader");
                } catch (java.lang.ClassNotFoundException e) {
                    cfmlServlet = Server.class.getClassLoader().loadClass(cfengine + ".loader.servlet.CFMLServlet");
                    log.debug("dynamically loaded CFML servlet from runwar classloader");
                }
                try {
                    restServletClass = _classLoader.loadClass(cfengine + ".loader.servlet.RestServlet");
                } catch (java.lang.ClassNotFoundException e) {
                    restServletClass = Server.class.getClassLoader().loadClass(cfengine + ".loader.servlet.RestServlet");
                }
                log.debug("loaded servlet classes");
                servletBuilder.addServlet(
                    servlet("CFMLServlet", cfmlServlet)
                    .setRequireWelcomeFileMapping(true)
                    .addInitParam("configuration",cfmlServletConfigWebDir)
                    .addInitParam(cfengine+"-server-root",cfmlServletConfigServerDir)
                    .addMapping("*.cfm")
                    .addMapping("*.cfc")
                    .addMapping("/index.cfc/*")
                    .addMapping("/index.cfm/*")
                    .addMapping("/index.cfml/*")
                    .setLoadOnStartup(1)
                    );
                if(serverOptions.getServletRestEnabled()) {
                    log.debug("Adding REST servlet");
                    ServletInfo restServlet = servlet("RESTServlet", restServletClass)
                        .setRequireWelcomeFileMapping(true)
                        .addInitParam(cfengine+"-web-directory",cfmlServletConfigWebDir)
                        .setLoadOnStartup(2);
                    for(String path : serverOptions.getServletRestMappings()) {
                        restServlet.addMapping(path);
                    }
                    servletBuilder.addServlet(restServlet);
                }
            }
        } else if(webinf.exists()) {
            log.debug("found WEB-INF: " + webinf.getAbsolutePath());
            if (_classLoader == null) {
                throw new RuntimeException("FATAL: Could not load any libs for war: " + warFile.getAbsolutePath());
            }
            servletBuilder.setClassLoader(_classLoader);
            servletBuilder.setResourceManager(getResourceManager(warFile, transferMinSize, cfmlDirs, webinf));
            LogSubverter.subvertJDKLoggers(loglevel);
            WebXMLParser.parseWebXml(new File(webinf, "/web.xml"), servletBuilder, ignoreWelcomePages, ignoreRestMappings);
        } else {
            throw new RuntimeException("Didn't know how to handle war:"+warFile.getAbsolutePath());
        }
        /*      
        servletBuilder.addInitialHandlerChainWrapper(new HandlerWrapper() {
            @Override
            public HttpHandler wrap(final HttpHandler handler) {
                return resource(new FileResourceManager(new File(libDir,"server/WEB-INF"), transferMinSize))
                        .setDirectoryListingEnabled(true);
            }
        });
        */

        if(cfengine.equals("adobe")){
            String cfclassesDir = (String) servletBuilder.getServletContextAttributes().get("coldfusion.compiler.outputDir");
            if(cfclassesDir == null || cfclassesDir.startsWith("/WEB-INF")){
                // TODO: figure out why adobe needs the absolute path, vs. /WEB-INF/cfclasses
                File cfclassesDirFile = new File(webinf, "/cfclasses");
                cfclassesDir = cfclassesDirFile.getAbsolutePath();
                log.debug("ADOBE - coldfusion.compiler.outputDir set to " + cfclassesDir);
                if( !cfclassesDirFile.exists() ) {
                	cfclassesDirFile.mkdir();
                }
                servletBuilder.addServletContextAttribute("coldfusion.compiler.outputDir",cfclassesDir);
            }
        }

        configureURLRewrite(servletBuilder, webinf);
        configurePathInfoFilter(servletBuilder);

        if (serverOptions.isCacheEnabled()) {
            addCacheHandler(servletBuilder);
        } else {
            log.debug("File cache is disabled");
        }

        if (serverOptions.isCustomHTTPStatusEnabled()) {
            servletBuilder.setSendCustomReasonPhraseOnError(true);
        }

        if(serverOptions.getErrorPages() != null){
            for(Integer errorCode : serverOptions.getErrorPages().keySet()) {
                String location = serverOptions.getErrorPages().get(errorCode);
                if(errorCode == 1) {
                    servletBuilder.addErrorPage( new ErrorPage(location));
                    log.debug("Adding default error location: " + location);
                } else {
                    servletBuilder.addErrorPage( new ErrorPage(location, errorCode));
                    log.debug("Adding "+errorCode+" error code location: " + location);
                }
            }
        }

        //someday we may wanna listen for changes
        /*
        servletBuilder.getResourceManager().registerResourceChangeListener(new ResourceChangeListener() {
            @Override
            public void handleChanges(Collection<ResourceChangeEvent> changes) {
                for(ResourceChangeEvent change : changes) {
                    log.info("CHANGE");
                    log.info(change.getResource());
                    log.info(change.getType().name());
                    manager.getDeployment().getServletPaths().invalidate();
                }
            }
        });
        */

        // this prevents us from having to use our own ResourceHandler (directory listing, welcome files, see below) and error handler for now
        servletBuilder.addServlet(new ServletInfo(io.undertow.servlet.handlers.ServletPathMatches.DEFAULT_SERVLET_NAME, DefaultServlet.class)
            .addInitParam("directory-listing", Boolean.toString(serverOptions.isDirectoryListingEnabled())));

//        servletBuilder.setExceptionHandler(LoggingExceptionHandler.DEFAULT);


        List welcomePages =  servletBuilder.getWelcomePages();
        if(ignoreWelcomePages) {
            log.debug("Ignoring web.xml welcome file, so adding server options welcome files to deployment manager.");
            servletBuilder.addWelcomePages(serverOptions.getWelcomeFiles());
        } else if(welcomePages.size() == 0){
            log.debug("No welcome pages set yet, so adding defaults to deployment manager.");
            servletBuilder.addWelcomePages(defaultWelcomeFiles);
        }
        log.info("welcome pages in deployment manager: " + servletBuilder.getWelcomePages());

        if(ignoreRestMappings) {
            log.info("Overriding web.xml rest mappings with " + Arrays.toString( serverOptions.getServletRestMappings() ) );
            Iterator<Entry<String, ServletInfo>> it = servletBuilder.getServlets().entrySet().iterator();
            while (it.hasNext()) {
                ServletInfo restServlet = it.next().getValue();
                log.trace( "Checking servelet named: " + restServlet.getName() + "to see if it's a REST servlet." );
                if( restServlet.getName().toLowerCase().equals("restservlet") || restServlet.getName().toLowerCase().equals("cfrestservlet") ) {
                    for(String path : serverOptions.getServletRestMappings()) {
                        restServlet.addMapping(path);
                        log.info("Added rest mapping: " + path + " to " + restServlet.getName() );
                    }
                }
            }
        }
        
        // TODO: add buffer pool size (maybe-- direct is best at 16k), enable/disable be good I reckon tho
        servletBuilder.addServletContextAttribute(WebSocketDeploymentInfo.ATTRIBUTE_NAME,
          new WebSocketDeploymentInfo().setBuffers(new DefaultByteBufferPool(true, 1024 * 16)));
        log.debug("Added websocket context");
        
        manager = defaultContainer().addDeployment(servletBuilder);
       
        
        manager.deploy();
        HttpHandler servletHandler = manager.start();
        log.debug("started servlet deployment manager");

        /*
        List welcomePages =  manager.getDeployment().getDeploymentInfo().getWelcomePages();
        CFMLResourceHandler resourceHandler = new CFMLResourceHandler(servletBuilder.getResourceManager(), servletHandler, welcomePages);
        resourceHandler.setDirectoryListingEnabled(directoryListingEnabled);
        PathHandler pathHandler = Handlers.path(Handlers.redirect(contextPath))
                .addPrefixPath(contextPath, resourceHandler);
        HttpHandler errPageHandler = new SimpleErrorPageHandler(pathHandler);
        Builder serverBuilder = Undertow.builder().addHttpListener(portNumber, host).setHandler(errPageHandler);
*/
        
        Builder serverBuilder = Undertow.builder();

        if(serverOptions.isEnableHTTP()) {
            serverBuilder.addHttpListener(portNumber, host);
        }

        if(serverOptions.isHTTP2Enabled()) {
            log.info("Enabling HTTP2 protocol");
            LaunchUtil.assertJavaVersion8();
            if(!serverOptions.isEnableSSL()) {
                log.warn("SSL is required for HTTP2.  Enabling default SSL server.");
                serverOptions.setEnableSSL(true);
            }
            serverOptions.setSSLPort(serverOptions.getSSLPort()+1);
            serverBuilder.setServerOption(UndertowOptions.ENABLE_HTTP2, true);
            //serverBuilder.setSocketOption(Options.REUSE_ADDRESSES, true);
        }

        if (serverOptions.isEnableSSL()) {
            int sslPort = serverOptions.getSSLPort();
            serverOptions.setDirectBuffers(true);
            log.info("Enabling SSL protocol on port " + sslPort);
            try {
                if (serverOptions.getSSLCertificate() != null) {
                    File certfile = serverOptions.getSSLCertificate();
                    File keyfile = serverOptions.getSSLKey();
                    char[] keypass = serverOptions.getSSLKeyPass();
                    String[] sslAddCerts = serverOptions.getSSLAddCerts();
                    sslContext = SSLUtil.createSSLContext(certfile, keyfile, keypass, sslAddCerts);
                    if(keypass != null) 
                        Arrays.fill(keypass, '*');
                } else {
                    sslContext = SSLUtil.createSSLContext();
                }
                serverBuilder.addHttpsListener(sslPort, host, sslContext);
            } catch (Exception e) {
                log.error("Unable to start SSL:" + e.getMessage());
                e.printStackTrace();
                System.exit(1);
            }
        }
        
        if (serverOptions.isEnableAJP()) {
            log.info("Enabling AJP protocol on port " + serverOptions.getAJPPort());
            serverBuilder.addAjpListener(serverOptions.getAJPPort(), host);
        }
        
        if(serverOptions.getBufferSize() != 0) {
            log.info("Buffer Size: " + serverOptions.getBufferSize());
            serverBuilder.setBufferSize(serverOptions.getBufferSize());
        }
        if(serverOptions.getIoThreads() != 0) {
            log.info("IO Threads: " + serverOptions.getIoThreads());
            serverBuilder.setIoThreads(serverOptions.getIoThreads());
        }
        if(serverOptions.getWorkerThreads() != 0) {
            log.info("Worker threads: " + serverOptions.getWorkerThreads());
            serverBuilder.setWorkerThreads(serverOptions.getWorkerThreads());
        }
        log.info("Direct Buffers: " + serverOptions.isDirectBuffers());
        serverBuilder.setDirectBuffers(serverOptions.isDirectBuffers());

        final PathHandler pathHandler = new PathHandler(Handlers.redirect(contextPath)) {
            @Override
            public void handleRequest(final HttpServerExchange exchange) throws Exception {
                if (exchange.getRequestPath().endsWith(".svgz")) {
                    exchange.getResponseHeaders().put(Headers.CONTENT_ENCODING, "gzip");
                }
                //log.trace("pathhandler path:" + exchange.getRequestPath() + " querystring:" +exchange.getQueryString());
                // clear any welcome-file info cached after initial request *NOT THREAD SAFE*
                if (serverOptions.isDirectoryListingRefreshEnabled() && exchange.getRequestPath().endsWith("/")) {
                    //log.trace("*** Resetting servlet path info");
                    manager.getDeployment().getServletPaths().invalidate();
                }
                if(serverOptions.isDebug() && exchange.getRequestPath().endsWith("/dumprunwarrequest")) {
                    new RequestDumper().handleRequest(exchange);
                } else {
                    super.handleRequest(exchange);
                }
            }
        };
        pathHandler.addPrefixPath(contextPath, servletHandler);

//        SessionManager sessionManager = new InMemorySessionManager("SESSION_MANAGER");
//        SessionCookieConfig sessionConfig = new SessionCookieConfig();
//        SessionAttachmentHandler sessionAttachmentHandler = new SessionAttachmentHandler(sessionManager, sessionConfig);
//        // set as next handler your root handler
//        sessionAttachmentHandler.setNext(pathHandler);

        HttpHandler errPageHandler;
        
        if (serverOptions.isGzipEnabled()) {
            final EncodingHandler handler = new EncodingHandler(new ContentEncodingRepository().addEncodingHandler(
                    "gzip", new GzipEncodingProvider(), 50, Predicates.parse("max-content-size[5]")))
                    .setNext(pathHandler);
            errPageHandler = new ErrorHandler(handler);
        } else {
            errPageHandler = new ErrorHandler(pathHandler);
        }

//        if (serverOptions.isDebug()) {
//            log.debug("Enabling request dumper");
//            errPageHandler = new RequestDumpingHandler(errPageHandler);
//        }

        if (serverOptions.isProxyPeerAddressEnabled()) {
            log.debug("Enabling Proxy Peer Address handling");
            errPageHandler = new SSLHeaderHandler(new ProxyPeerAddressHandler(errPageHandler));
        }

        Undertow reverseProxy = null;
        if (serverOptions.isHTTP2Enabled()) {
            log.debug("Enabling HTTP2 Upgrade and LearningPushHandler");
            /**
             * To not be dependent on java9 or crazy requirements, we set up a proxy to enable http2, and swap it with the actual SSL server (thus the port++/port--)
             */
            errPageHandler = new Http2UpgradeHandler(errPageHandler);
            errPageHandler = Handlers.header(predicate(secure(),errPageHandler,new HttpHandler() {
                @Override
                public void handleRequest(HttpServerExchange exchange) throws Exception {
                    exchange.getResponseHeaders().add(Headers.LOCATION, "https://" + exchange.getHostName()
                            + ":" + (serverOptions.getSSLPort()-1) + exchange.getRelativePath());
                    exchange.setStatusCode(StatusCodes.TEMPORARY_REDIRECT);
                }
            }), "x-undertow-transport", ExchangeAttributes.transportProtocol());
            errPageHandler = new SessionAttachmentHandler(new LearningPushHandler(100, -1, errPageHandler),new InMemorySessionManager("runwarsessions"), new SessionCookieConfig());
            LoadBalancingProxyClient proxy = new LoadBalancingProxyClient()
                    .addHost(new URI("https://localhost:"+serverOptions.getSSLPort()), null, new UndertowXnioSsl(Xnio.getInstance(), OptionMap.EMPTY, SSLUtil.createClientSSLContext()), OptionMap.create(UndertowOptions.ENABLE_HTTP2, true))
                    .setConnectionsPerThread(20);
            reverseProxy = Undertow.builder()
                    .setServerOption(UndertowOptions.ENABLE_HTTP2, true)
                    .addHttpsListener(serverOptions.getSSLPort()-1, serverOptions.getHost(), sslContext)
                    .setHandler(new ProxyHandler(proxy, 30000, ResponseCodeHandler.HANDLE_404))
                    .build();
        }

        if(serverOptions.isEnableBasicAuth()) {
            String realm = serverName + " Realm";
            log.debug("Enabling Basic Auth: " + realm);
            final Map<String, char[]> users = new HashMap<>(2);
            for(Entry<String,String> userNpass : serverOptions.getBasicAuth().entrySet()) {
                users.put(userNpass.getKey(), userNpass.getValue().toCharArray());
                log.debug(String.format("User:%s password:%s",userNpass.getKey(),userNpass.getValue()));
            }
            serverBuilder.setHandler(SecurityManager.addSecurity(errPageHandler, users, realm));
        } else {
            serverBuilder.setHandler(errPageHandler);
        }

        
        try {
            PID = ManagementFactory.getRuntimeMXBean().getName().split("@")[0];
            String pidFile = serverOptions.getPidFile();
            if (pidFile != null && pidFile.length() > 0) {
                File file = new File(pidFile);
                file.deleteOnExit();
                PrintWriter writer = new PrintWriter(file);
                writer.print(PID);
                writer.close();
            }
        } catch (Exception e) {
            log.error("Unable to get PID:" + e.getMessage());
        }
        if (serverOptions.isKeepRequestLog()) {
            log.error("request log currently unsupported");
        }
        // start the stop monitor thread
        undertow = serverBuilder.build();
        monitor = new MonitorThread(stoppassword);
        monitor.start();
        log.debug("started stop monitor");

        if(serverOptions.isTrayEnabled()) {
            try {
                LaunchUtil.hookTray(this);
                log.debug("hooked system tray");	
            } catch( Throwable e ) {
                log.debug("system tray hook failed.");
                log.error( e );
            }
        } else {
            log.debug("System tray integration disabled");
        }

        if (serverOptions.isOpenbrowser()) {
            new Server(3);
        }
        
        // if this println changes be sure to update the LaunchUtil so it will know success
        String sslInfo = serverOptions.isEnableSSL() ? " https-port:" + serverOptions.getSSLPort() : "";
        String msg = "Server is up - http-port:" + portNumber + sslInfo + " stop-port:" + socketNumber +" PID:" + PID + " version " + getVersion();
        log.debug(msg);
        LaunchUtil.displayMessage("info", msg);
        System.out.println(msg);
        setServerState(ServerState.STARTED);
//        ConfigWebAdmin admin = new ConfigWebAdmin(lucee.runtime.engine.ThreadLocalPageContext.getConfig(), null);
//        admin._updateMapping(virtual, physical, archive, primary, inspect, toplevel);
//        admin._store();

//        Class<?> appClass = _classLoader.loadClass("lucee.loader.engine.CFMLEngineFactory");
//        Method getAppMethod = appClass.getMethod("getInstance");
//        Object appInstance = getAppMethod.invoke(null);
//        Object webs = appInstance.getClass().getMethod("getCFMLEngineFactory").invoke(appInstance, null);
//        Object ef = webs.getClass().getMethod("getInstance").invoke(webs, null);
//
//        System.out.println(appInstance.toString());

        if (serverOptions.isMariaDB4jEnabled()) {
            try {
                mariadb4jManager.start(serverOptions.getMariaDB4jPort(), serverOptions.getMariaDB4jBaseDir(),
                        serverOptions.getMariaDB4jDataDir(), serverOptions.getMariaDB4jImportSQLFile());
            } catch (Exception dbStartException) {
                log.error("Could not start MariaDB4j");
                log.error(dbStartException);
                System.out.println("Error starting MariaDB4j: " + dbStartException.getMessage());
            }
        }
        try{

            undertow.start();

            if (serverOptions.isHTTP2Enabled()) {
                log.debug("Starting HTTP2 proxy");
                reverseProxy.start();
            }

        } 
        catch (Exception any) {
            if(any.getCause() instanceof java.net.SocketException) {
                if(any.getCause().getMessage().equals("Permission denied")) {
                    System.err.println("You need to be root or Administrator to bind to a port below 1024!");
                }
            } else {
                any.printStackTrace();
            }
            System.exit(1);
        }
    }

    private void addShutDownHook() {
        if(shutDownThread == null) {
            shutDownThread = new Thread() {
                public void run() {
                    log.debug("Running shutdown hook");
                    try {
                        if(!getServerState().equals(ServerState.STOPPING)) {
                            log.debug("shutdown hook:stopServer()");
                            stopServer();
                        }
//                    if(tempWarDir != null) {
//                        LaunchUtil.deleteRecursive(tempWarDir);
//                    }
                        if(mainThread.isAlive()) {
                            log.debug("shutdown hook joining main thread");
                            mainThread.interrupt();
                            mainThread.join();
                        }
                        log.debug("shutdown hook finished");
                    } catch ( Exception e) {
                        e.printStackTrace();
                    }
                    log.debug("Ran shutdown hook");
                }
            };
            Runtime.getRuntime().addShutdownHook(shutDownThread);
            log.debug("Added shutdown hook");
        }
    }

    public void stopServer() {
        int exitCode = 0;
        try{
            setServerState(ServerState.STOPPING);
            System.out.println();
            System.out.println(bar);
            System.out.println("*** stopping server");
            if (serverOptions.isMariaDB4jEnabled()) {
                mariadb4jManager.stop();
            }
            try {
                switch(manager.getState()) {
                    case UNDEPLOYED:
                        break;
                    default:
                        manager.undeploy();
                }
                undertow.stop();
//                Thread.sleep(1000);
            } catch (Exception notRunning) {
                System.out.println("*** server did not appear to be running");
            }
            System.out.println(bar);
            if(monitor != null) {
                monitor.stopListening();
            }
            setServerState(ServerState.STOPPED);
        } catch (Exception e) {
            e.printStackTrace();
            setServerState(ServerState.UNKNOWN);
            log.error(e);
            exitCode = 1;
        }
        try {
            if (tee != null) {
                tee.flush();
                tee.closeBranch();
            }
        } catch (Exception e) {
            System.err.println("Redirect:  Unable to close this log file!");
        }
        if(exitCode != 0) {
            System.exit(exitCode);
        }

    }

    
    @SuppressWarnings({ "rawtypes", "unchecked" })
    private void configureURLRewrite(DeploymentInfo servletBuilder, File webInfDir) throws ClassNotFoundException, IOException {
        if(serverOptions.isEnableURLRewrite()) {
            log.debug("enabling URL rewriting");
            Class rewriteFilter;
            String urlRewriteFile = "runwar/urlrewrite.xml";
            try{
                rewriteFilter = _classLoader.loadClass("org.tuckey.web.filters.urlrewrite.UrlRewriteFilter");
            } catch (java.lang.ClassNotFoundException e) {
                rewriteFilter = Server.class.getClassLoader().loadClass("org.tuckey.web.filters.urlrewrite.UrlRewriteFilter");
            }
            if(serverOptions.getURLRewriteFile() != null) {
                if(!serverOptions.getURLRewriteFile().isFile()) {
                    log.error("The URL rewrite file " + urlRewriteFile + " does not exist!");
                } else {
                    String rewriteFileName = "urlrewrite.xml";
                    LaunchUtil.copyFile(serverOptions.getURLRewriteFile(), new File(webInfDir, rewriteFileName));
                    log.debug("Copying URL rewrite file " + serverOptions.getURLRewriteFile().getPath() + " to WEB-INF: " + webInfDir.getPath() + "/"+rewriteFileName);
                    urlRewriteFile = "/WEB-INF/"+rewriteFileName;
                }
            }
            
            String rewriteformat = serverOptions.isURLRewriteApacheFormat() ? "modRewrite-style" : "XML";
            log.debug(rewriteformat + " rewrite config file: " + urlRewriteFile);
            FilterInfo rewriteFilterInfo = new FilterInfo("UrlRewriteFilter", rewriteFilter)
                    .addInitParam("confPath", urlRewriteFile)
                    .addInitParam("statusEnabled", Boolean.toString(serverOptions.isDebug()))
                    .addInitParam("modRewriteConf", Boolean.toString(serverOptions.isURLRewriteApacheFormat()));
            if(serverOptions.getURLRewriteCheckInterval() != null) {
                rewriteFilterInfo.addInitParam("confReloadCheckInterval", serverOptions.getURLRewriteCheckInterval());
            }
            if(serverOptions.getURLRewriteStatusPath() != null && serverOptions.getURLRewriteStatusPath().length() != 0) {
                rewriteFilterInfo.addInitParam("statusPath", serverOptions.getURLRewriteStatusPath());
            }
            if(serverOptions.getLoglevel() != "WARN") {
                rewriteFilterInfo.addInitParam("logLevel", serverOptions.getLoglevel());
            }
            servletBuilder.addFilter(rewriteFilterInfo);
            servletBuilder.addFilterUrlMapping("UrlRewriteFilter", "/*", DispatcherType.REQUEST);
        } else {
            log.debug("URL rewriting is disabled");
        }
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private void configurePathInfoFilter(DeploymentInfo servletBuilder) throws ClassNotFoundException, IOException {
        if(serverOptions.isFilterPathInfoEnabled()) {
            log.debug("enabling path_info filter");
            Class regexPathInfoFilter;
            try{
                regexPathInfoFilter = _classLoader.loadClass("org.cfmlprojects.regexpathinfofilter.RegexPathInfoFilter");
            } catch (java.lang.ClassNotFoundException e) {
                regexPathInfoFilter = Server.class.getClassLoader().loadClass("org.cfmlprojects.regexpathinfofilter.RegexPathInfoFilter");
            }
            servletBuilder.addFilter(new FilterInfo("RegexPathInfoFilter", regexPathInfoFilter));
            servletBuilder.addFilterUrlMapping("RegexPathInfoFilter", "/*", DispatcherType.REQUEST);
            servletBuilder.addFilterUrlMapping("RegexPathInfoFilter", "/*", DispatcherType.FORWARD);
        } else {
            log.debug("path_info filter is disabled");
        }
    }
    
    private void addCacheHandler(final DeploymentInfo servletBuilder) {
        // this handles mime types and adds a simple cache for static files
        servletBuilder.addInitialHandlerChainWrapper(new HandlerWrapper() {
            @Override
            public HttpHandler wrap(final HttpHandler handler) {
              final ResourceHandler resourceHandler = new ResourceHandler(servletBuilder.getResourceManager());
                io.undertow.util.MimeMappings.Builder mimes = MimeMappings.builder();
                List<String> suffixList = new ArrayList<String>();
                // add font mime types not included by default
                mimes.addMapping("eot", "application/vnd.ms-fontobject");
                mimes.addMapping("otf", "font/opentype");
                mimes.addMapping("ttf", "application/x-font-ttf");
                mimes.addMapping("woff", "application/x-font-woff");
                suffixList.addAll(Arrays.asList(".eot",".otf",".ttf",".woff"));
                // add the default types and any added in web.xml files
                for(MimeMapping mime : servletBuilder.getMimeMappings()) {
                    log.debug("Adding mime-type: " + mime.getExtension() + " - " + mime.getMimeType());
                    mimes.addMapping(mime.getExtension(), mime.getMimeType());
                    suffixList.add("."+mime.getExtension());
                }
                resourceHandler.setMimeMappings(mimes.build());
                String[] suffixes = new String[suffixList.size()];
                suffixes = suffixList.toArray(suffixes);
                // simple cacheHandler, someday maybe make this configurable
                final CacheHandler cacheHandler = new CacheHandler(new DirectBufferCache(1024, 10, 10480), resourceHandler);
                final PredicateHandler predicateHandler = predicate(Predicates.suffixes(suffixes), cacheHandler, handler);
                return predicateHandler;
            }
        });
    }
    
    private ResourceManager getResourceManager(File warFile, Long transferMinSize, String cfmlDirs, File internalCFMLServerRoot) {
        MappedResourceManager mappedResourceManager = new MappedResourceManager(warFile, transferMinSize, cfmlDirs, internalCFMLServerRoot);
        if(serverOptions.isDirectoryListingRefreshEnabled()) return mappedResourceManager;
        final DirectBufferCache dataCache = new DirectBufferCache(1000, 10, 1000 * 10 * 1000, BufferAllocator.DIRECT_BYTE_BUFFER_ALLOCATOR, METADATA_MAX_AGE);
        final int metadataCacheSize = 100;
        final long maxFileSize = 10000;
        return new CachingResourceManager(metadataCacheSize,maxFileSize, dataCache, mappedResourceManager, METADATA_MAX_AGE);
    }

    public static File getThisJarLocation() {
        return LaunchUtil.getJarDir(Server.class);
    }

    public String getPID() {
        return PID;
    }

    private int getPortOrErrorOut(int portNumber, String host) {
        try {
            ServerSocket nextAvail = new ServerSocket(portNumber, 1, InetAddress.getByName(host));
            portNumber = nextAvail.getLocalPort();
            nextAvail.close();
            return portNumber;
        } catch (java.net.BindException e) {
            throw new RuntimeException("Error getting port " + portNumber + "!  Cannot start.  " + e.getMessage());
        } catch (UnknownHostException e) {
            throw new RuntimeException("Unknown host (" + host + ")");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private List<URL> getJarList(String libDirs) throws IOException {
        List<URL> classpath = new ArrayList<URL>();
        String[] list = libDirs.split(",");
        if (list == null)
            return classpath;

        for (String path : list) {
            if (".".equals(path) || "..".equals(path))
                continue;

            File file = new File(path); 
            // Ignore non-existent dirs
            if( !file.exists() ) {
                continue;
            }
            for (File item : file.listFiles()) {
                String fileName = item.getAbsolutePath();
                if (!item.isDirectory()) {
                    if (fileName.toLowerCase().endsWith(".jar") || fileName.toLowerCase().endsWith(".zip")) {
                        URL url = item.toURI().toURL();
                        classpath.add(url);
                        log.trace("lib: added to classpath: " + fileName);
                    }
                }
            }
        }
        return classpath;
    }

    private List<URL> getClassesList(File classesDir) throws IOException {
        List<URL> classpath = new ArrayList<URL>();
        if (classesDir == null)
            return classpath;
        if (classesDir.exists() && classesDir.isDirectory()) {
            URL url = classesDir.toURI().toURL();
            classpath.add(url);
            for (File item : classesDir.listFiles()) {
                if (item.isDirectory()) {
                    classpath.addAll(getClassesList(item));
                }
            }
        } else {
            log.debug("WEB-INF classes directory (" + classesDir.getAbsolutePath() + ") does not exist");
        }
        return classpath;
    }

    public static void printVersion() {
        System.out.println(LaunchUtil.getResourceAsString("runwar/version.properties"));
        System.out.println(LaunchUtil.getResourceAsString("io/undertow/version.properties"));
    }

    private static String getVersion() {
        String[] version = LaunchUtil.getResourceAsString("runwar/version.properties").split("=");
        return version[version.length - 1].trim();
    }

    private class MonitorThread extends Thread {

        private char[] stoppassword;
        private volatile boolean listening = false;

        public MonitorThread(char[] stoppassword) {
            this.stoppassword = stoppassword;
            setDaemon(true);
            setName("StopMonitor");
        }

        @Override
        public void run() {
            // Executor exe = Executors.newCachedThreadPool();
            ServerSocket serverSocket = null;
            int exitCode = 0;
            try {
                serverSocket = new ServerSocket(socketNumber, 1, InetAddress.getByName(serverOptions.getHost()));
                listening = true;
                System.out.println(bar);
                System.out.println("*** starting 'stop' listener thread - Host: " + serverOptions.getHost()
                        + " - Socket: " + socketNumber);
                System.out.println(bar);
                while (listening) {
                    final Socket clientSocket = serverSocket.accept();
                    int r, i = 0;
                    BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                    try {
                        while ((r = reader.read()) != -1) {
                            char ch = (char) r;
                            if (stoppassword.length > i && ch == stoppassword[i]) {
                                i++;
                            } else {
                                i = 0; // prevent prefix only matches
                            }
                        }
                        if (i == stoppassword.length) {
                            listening = false;
                        } else {
                            log.warn("Incorrect password used when trying to stop server.");
                        }
                    } catch (java.net.SocketException e) {
                        // reset
                    }
                    try {
                        clientSocket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                serverSocket.close();
            } catch (Exception e) {
                exitCode = 1;
                e.printStackTrace();
            }
//            stopServer();
//            System.exit(exitCode);
//            Thread.currentThread().interrupt();
            return;
        }
        
        public void stopListening() {
            listening = false;
        }
    }

    public boolean serverWentDown() {
        try {
            return serverWentDown(serverOptions.getLaunchTimeout(), 3000, InetAddress.getByName(serverOptions.getHost()), serverOptions.getSocketNumber());
        } catch (UnknownHostException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return false;
    }
    
    public static boolean serverWentDown(int timeout, long sleepTime, InetAddress server, int port) {
        long start = System.currentTimeMillis();
        long elapsed = (System.currentTimeMillis() - start);
        while (elapsed < timeout) {
            if (checkServerIsUp(server, port)) {
                try {
                    Thread.sleep(sleepTime);
                    elapsed = (System.currentTimeMillis() - start);
                } catch (InterruptedException e) {
                }
            } else {
                return true;
            }
        }
        return false;
    }

    public static boolean serverCameUp(int timeout, long sleepTime, InetAddress server, int port) {
        long start = System.currentTimeMillis();
        while ((System.currentTimeMillis() - start) < timeout) {
            if (!checkServerIsUp(server, port)) {
                try {
                    Thread.sleep(sleepTime);
                } catch (InterruptedException e) {
                    return false;
                }
            } else {
                return true;
            }
        }
        return false;
    }

    public static boolean checkServerIsUp(InetAddress server, int port) {
        Socket sock = null;
        try {
            sock = SocketFactory.getDefault().createSocket(server, port);
            sock.setSoLinger(true, 0);
            return true;
        } catch (IOException e) {
            return false;
        } finally {
            if (sock != null) {
                try {
                    sock.close();
                } catch (IOException e) {
                    // don't care
                }
            }
        }
    }

    class OpenBrowserTask extends TimerTask {
        public void run() {
            int portNumber = serverOptions.getPortNumber();
            String protocol = "http";
            String host = serverOptions.getHost();
            String openbrowserURL = serverOptions.getOpenbrowserURL();
            int timeout = serverOptions.getLaunchTimeout();
            if (openbrowserURL == null || openbrowserURL.length() == 0) {
                openbrowserURL = "http://" + host + ":" + portNumber;
            }
            if(serverOptions.isEnableSSL()) {
                portNumber = serverOptions.getSSLPort();
                protocol = "https";
                openbrowserURL = openbrowserURL.replace("http:", "https:");
            }
            if (!openbrowserURL.startsWith("http")) {
                openbrowserURL = (!openbrowserURL.startsWith("/")) ? "/" + openbrowserURL : openbrowserURL;
                openbrowserURL = protocol + "://" + host + ":" + portNumber + openbrowserURL;
            }
            System.out.println("Waiting up to " + (timeout/1000) + " seconds for " + host + ":" + portNumber + "...");
            try {
                if (serverCameUp(timeout, 3000, InetAddress.getByName(host), portNumber)) {
                    System.out.println("Opening browser to..." + openbrowserURL);
                    BrowserOpener.openURL(openbrowserURL.trim());
                } else {
                    System.out.println("could not open browser to..." + openbrowserURL + "... timeout...");
                }
            } catch (Exception e) {
                log.error(e.getMessage());
            }
            return;
        }
    }

    public static ServerOptions getServerOptions() {
        return serverOptions;
    }

    synchronized void setServerState(String state) {
        serverState = state;
        if (statusFile != null) {
            try {
                PrintWriter writer;
                writer = new PrintWriter(statusFile);
                writer.print(state);
                writer.close();
            } catch (FileNotFoundException e) {
                log.error(e.getMessage());
            }
        }
    }

    public String getServerState() {
        return serverState;
    }
    
    public static class ServerState {
        public static final String STARTING = "STARTING";
        public static final String STARTED = "STARTED";
        public static final String STARTING_BACKGROUND = "STARTING_BACKGROUND";
        public static final String STARTED_BACKGROUND = "STARTED_BACKGROUND";
        public static final String STOPPING = "STOPPING";
        public static final String STOPPED = "STOPPED";
        public static final String UNKNOWN = "UNKNOWN";
    }

}
