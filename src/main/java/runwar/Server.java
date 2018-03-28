package runwar;

import static io.undertow.Handlers.predicate;
import static io.undertow.predicate.Predicates.secure;
import static io.undertow.servlet.Servlets.defaultContainer;
import static io.undertow.servlet.Servlets.deployment;
import static io.undertow.servlet.Servlets.servlet;

import java.awt.Image;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Method;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Timer;
import java.util.TimerTask;

import javax.net.ssl.SSLContext;
import javax.servlet.DispatcherType;
import javax.servlet.Filter;
import javax.servlet.Servlet;

import org.xnio.BufferAllocator;
import org.xnio.OptionMap;
import org.xnio.Options;
import org.xnio.Xnio;
import org.xnio.XnioWorker;

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
import io.undertow.server.handlers.ResponseCodeHandler;
import io.undertow.server.handlers.SSLHeaderHandler;
import io.undertow.server.handlers.accesslog.AccessLogHandler;
import io.undertow.server.handlers.accesslog.DefaultAccessLogReceiver;
import io.undertow.server.handlers.cache.CacheHandler;
import io.undertow.server.handlers.cache.DirectBufferCache;
import io.undertow.server.handlers.encoding.ContentEncodingRepository;
import io.undertow.server.handlers.encoding.EncodingHandler;
import io.undertow.server.handlers.encoding.GzipEncodingProvider;
import io.undertow.server.handlers.proxy.LoadBalancingProxyClient;
import io.undertow.server.handlers.proxy.ProxyHandler;
import io.undertow.server.handlers.resource.CachingResourceManager;
import io.undertow.server.handlers.resource.ResourceHandler;
import io.undertow.server.handlers.resource.ResourceManager;
import io.undertow.server.protocol.http2.Http2UpgradeHandler;
import io.undertow.server.session.InMemorySessionManager;
import io.undertow.server.session.SessionAttachmentHandler;
import io.undertow.server.session.SessionCookieConfig;
import io.undertow.servlet.api.DeploymentInfo;
import io.undertow.servlet.api.DeploymentManager;
import io.undertow.servlet.api.ErrorPage;
import io.undertow.servlet.api.FilterInfo;
import io.undertow.servlet.api.MimeMapping;
import io.undertow.servlet.api.ServletInfo;
import io.undertow.servlet.handlers.DefaultServlet;
import io.undertow.util.Headers;
import io.undertow.util.MimeMappings;
import io.undertow.util.StatusCodes;
import io.undertow.websockets.jsr.WebSocketDeploymentInfo;
import runwar.logging.LoggerFactory;
import runwar.mariadb4j.MariaDB4jManager;
import runwar.options.CommandLineHandler;
import runwar.options.ServerOptions;
import runwar.options.ServerOptionsImpl;
import runwar.security.SSLUtil;
import runwar.security.SecurityManager;
import runwar.tray.Tray;
import runwar.undertow.MappedResourceManager;
import runwar.undertow.RequestDebugHandler;
import runwar.undertow.WebXMLParser;
import runwar.util.ClassLoaderUtils;
import runwar.util.RequestDumper;
import runwar.util.TeeOutputStream;
import static runwar.logging.RunwarLogger.LOG;

public class Server {

    private TeeOutputStream sysOutTee, sysErrTee;
    private volatile static ServerOptionsImpl serverOptions;
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
    private SecurityManager securityManager;
    private String serverMode;

    private static final int METADATA_MAX_AGE = 2000;
    private static final Thread mainThread = Thread.currentThread();
    
    private static XnioWorker worker;
    private static Xnio xnio;

    public Server() {
    }

    // for openBrowser 
    public Server(int seconds) {
        Timer timer = new Timer();
        timer.schedule(this.new OpenBrowserTask(), seconds * 1000);
    }

    protected void initClassLoader(List<URL> _classpath) {
        if (_classLoader == null) {
            LOG.debug("Initializing classloader with "+ _classpath.size() + " libraries");
            if( _classpath != null && _classpath.size() > 0) {
                LOG.tracef("classpath: %s",_classpath);
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
    
    public synchronized void startServer(String[] args, URLClassLoader classLoader) throws Exception {
        setClassLoader(classLoader);
        startServer(args);
    }
    
    public void ensureJavaVersion() {
        Class<?> nio;
        LOG.debug("Checking that we're running on > java7");
        try{
            nio = Server.class.getClassLoader().loadClass("java.nio.charset.StandardCharsets");
            nio.getClass().getName();
        } catch (java.lang.ClassNotFoundException e) {
            throw new RuntimeException("Could not load NIO!  Are we running on Java 7 or greater?  Sorry, exiting...");
        }
    }
    
    public synchronized void startServer(final String[] args) throws Exception {
        startServer(CommandLineHandler.parseArguments(args));
    }

    public synchronized void restartServer() throws Exception {
        restartServer(getServerOptions());
    }
    public synchronized void restartServer(final ServerOptions options) throws Exception {
        LaunchUtil.displayMessage("Info", "Restarting server...");
        System.out.println(bar);
        System.out.println("***  Restarting server");
        System.out.println(bar);
        stopServer();
        LaunchUtil.restartApplication(new Runnable(){
            @Override
            public void run() {
                LOG.debug("About to restart... (but probably we'll just die here-- this is neigh impossible.)");
//                stopServer();
//                serverWentDown();
//                if(monitor != null) {
//                    monitor.stopListening();
//                }
//                monitor = null;
            }});
    }
    
    public synchronized void startServer(final ServerOptions options) throws Exception {
        serverOptions = (ServerOptionsImpl) options;
        LoggerFactory.init(serverOptions);
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
        char[] stoppassword = serverOptions.getStopPassword();
        boolean ignoreWelcomePages = false;
        boolean ignoreRestMappings = false;
        LOG.info("Starting RunWAR " + getVersion());
        // unset this so thing that reconfigure will find theirs
        System.setProperty("log4j.configuration", "");
        ensureJavaVersion();

        securityManager = new SecurityManager();

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
                LOG.debug("Exploding compressed WAR to " + warFile.getAbsolutePath());
                LaunchUtil.unzipResource(zipResource, warFile, false);
            } else {
                LOG.debug("Using already exploded WAR in " + warFile.getAbsolutePath());
            }
            warPath = warFile.getAbsolutePath();
            if(serverOptions.getWarFile().getAbsolutePath().equals(serverOptions.getCfmlDirs())) {
                serverOptions.setCfmlDirs(warFile.getAbsolutePath());
            }
        }
        if (!warFile.exists()) {
            throw new RuntimeException("war does not exist: " + warFile.getAbsolutePath());
        }

        File webinf = serverOptions.getWebInfDir();
        File webXmlFile = serverOptions.getWebXmlFile();

        String libDirs = serverOptions.getLibDirs();
        URL jarURL = serverOptions.getJarURL();
        // If this folder is a proper war, add its WEB-INF/lib folder to the passed libDirs
        if (warFile.isDirectory() && webXmlFile != null && webXmlFile.exists()) {
            if (libDirs == null) {
                libDirs = "";
            } else if( libDirs.length() > 0 ) {
                libDirs = libDirs + ","; 
            }
            libDirs = libDirs + webinf.getAbsolutePath() + "/lib";
            LOG.info("Adding additional lib dir of: " + webinf.getAbsolutePath() + "/lib");
        }

        List<URL> cp = new ArrayList<URL>();
//        cp.add(Server.class.getProtectionDomain().getCodeSource().getLocation());
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

        serverMode = Mode.WAR;
        if(!webinf.exists()) {
            serverMode = Mode.DEFAULT;
            if(getCFMLServletClass(cfengine) != null) {
                serverMode = Mode.SERVLET;
            }
        }
        LOG.debugf("Server Mode: %s",serverMode);

        sysOutTee = null;
        sysErrTee = null;

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
            Image dockIcon = Tray.getIconImage(dockIconPath);
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
                LOG.warn("error setting dock icon image",e);
            }
        }
        LOG.info(bar);
        LOG.info("Starting - port:" + portNumber + " stop-port:" + socketNumber + " warpath:" + warPath);
        LOG.info("context: " + contextPath + "  -  version: " + getVersion());
        String cfmlDirs = serverOptions.getCfmlDirs();
        if (cfmlDirs.length() > 0) {
            LOG.info("web-dirs: " + cfmlDirs);
        }
        LOG.info("Log Directory: " + serverOptions.getLogDir().getAbsolutePath());
        LOG.info(bar);
        addShutDownHook();
        portNumber = getPortOrErrorOut(portNumber, host);
        socketNumber = getPortOrErrorOut(socketNumber, host);
        
        LOG.info("Adding mariadb manager");
        mariadb4jManager = new MariaDB4jManager(_classLoader);
        
        if(serverOptions.getWelcomeFiles() != null && serverOptions.getWelcomeFiles().length > 0) {
            ignoreWelcomePages = true;
        } else {
            serverOptions.setWelcomeFiles(defaultWelcomeFiles);
        }
        if(serverOptions.getServletRestMappings() != null && serverOptions.getServletRestMappings().length > 0) {
            ignoreRestMappings = true;
        }

        LOG.debug("Transfer Min Size: " + serverOptions.getTransferMinSize());

        xnio = Xnio.getInstance("nio", Server.class.getClassLoader());
        worker = xnio.createWorker(OptionMap.builder()
                .set(Options.WORKER_IO_THREADS, 8)
                .set(Options.CONNECTION_HIGH_WATER, 1000000)
                .set(Options.CONNECTION_LOW_WATER, 1000000)
                .set(Options.WORKER_TASK_CORE_THREADS, 30)
                .set(Options.WORKER_TASK_MAX_THREADS, 30)
                .set(Options.TCP_NODELAY, true)
                .set(Options.CORK, true)
                .getMap());
        
        final DeploymentInfo servletBuilder = deployment()
                .setContextPath(contextPath.equals("/") ? "" : contextPath)
                .setTempDir(new File(System.getProperty("java.io.tmpdir")))
                .setDeploymentName(warPath)
                .setServerName( "WildFly / Undertow" );

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
        LOG.trace("java.library.path:" + System.getProperty("java.library.path"));

        final SessionCookieConfig sessionConfig = new SessionCookieConfig();
        final SessionAttachmentHandler sessionAttachmentHandler = new SessionAttachmentHandler(new InMemorySessionManager("", 1, true), sessionConfig);

        configureServerResourceHandler(servletBuilder,sessionConfig,warFile,webinf,webXmlFile,cfmlDirs,cfengine,ignoreWelcomePages,ignoreRestMappings);
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
                LOG.debug("ADOBE - coldfusion.compiler.outputDir set to " + cfclassesDir);
                if( !cfclassesDirFile.exists() ) {
                	cfclassesDirFile.mkdir();
                }
                servletBuilder.addServletContextAttribute("coldfusion.compiler.outputDir",cfclassesDir);
            }
        }
        

        if(serverOptions.isEnableBasicAuth()) {
            securityManager.configureAuth(servletBuilder, serverOptions);
        }

        configureURLRewrite(servletBuilder, webinf);
        configurePathInfoFilter(servletBuilder);

        if (serverOptions.isCacheEnabled()) {
            addCacheHandler(servletBuilder);
        } else {
            LOG.debug("File cache is disabled");
        }

        if (serverOptions.isCustomHTTPStatusEnabled()) {
            servletBuilder.setSendCustomReasonPhraseOnError(true);
        }

        if(serverOptions.getErrorPages() != null){
            for(Integer errorCode : serverOptions.getErrorPages().keySet()) {
                String location = serverOptions.getErrorPages().get(errorCode);
                if(errorCode == 1) {
                    servletBuilder.addErrorPage( new ErrorPage(location));
                    LOG.debug("Adding default error location: " + location);
                } else {
                    servletBuilder.addErrorPage( new ErrorPage(location, errorCode));
                    LOG.debug("Adding "+errorCode+" error code location: " + location);
                }
            }
        }

        //someday we may wanna listen for changes
        /*
        servletBuilder.getResourceManager().registerResourceChangeListener(new ResourceChangeListener() {
            @Override
            public void handleChanges(Collection<ResourceChangeEvent> changes) {
                for(ResourceChangeEvent change : changes) {
                    RunwarLogger.ROOT_LOGGER.info("CHANGE");
                    RunwarLogger.ROOT_LOGGER.info(change.getResource());
                    RunwarLogger.ROOT_LOGGER.info(change.getType().name());
                    manager.getDeployment().getServletPaths().invalidate();
                }
            }
        });
        */

        // this prevents us from having to use our own ResourceHandler (directory listing, welcome files, see below) and error handler for now
        servletBuilder.addServlet(new ServletInfo(io.undertow.servlet.handlers.ServletPathMatches.DEFAULT_SERVLET_NAME, DefaultServlet.class)
            .addInitParam("directory-listing", Boolean.toString(serverOptions.isDirectoryListingEnabled())));

//        servletBuilder.setExceptionHandler(LoggingExceptionHandler.DEFAULT);


        List<?> welcomePages =  servletBuilder.getWelcomePages();
        if(ignoreWelcomePages) {
            LOG.debug("Ignoring web.xml welcome file, so adding server options welcome files to deployment manager.");
            servletBuilder.addWelcomePages(serverOptions.getWelcomeFiles());
        } else if(welcomePages.size() == 0){
            LOG.debug("No welcome pages set yet, so adding defaults to deployment manager.");
            servletBuilder.addWelcomePages(defaultWelcomeFiles);
        }
        LOG.info("welcome pages in deployment manager: " + servletBuilder.getWelcomePages());

        if(ignoreRestMappings) {
            LOG.info("Overriding web.xml rest mappings with " + Arrays.toString( serverOptions.getServletRestMappings() ) );
            Iterator<Entry<String, ServletInfo>> it = servletBuilder.getServlets().entrySet().iterator();
            while (it.hasNext()) {
                ServletInfo restServlet = it.next().getValue();
                LOG.trace( "Checking servelet named: " + restServlet.getName() + "to see if it's a REST servlet." );
                if( restServlet.getName().toLowerCase().equals("restservlet") || restServlet.getName().toLowerCase().equals("cfrestservlet") ) {
                    for(String path : serverOptions.getServletRestMappings()) {
                        restServlet.addMapping(path);
                        LOG.info("Added rest mapping: " + path + " to " + restServlet.getName() );
                    }
                }
            }
        }
        
        // TODO: probably best to create a new worker for websockets, if we want fastness, but for now we share
        // TODO: add buffer pool size (maybe-- direct is best at 16k), enable/disable be good I reckon tho
        servletBuilder.addServletContextAttribute(WebSocketDeploymentInfo.ATTRIBUTE_NAME,
          new WebSocketDeploymentInfo().setBuffers(new DefaultByteBufferPool(true, 1024 * 16)).setWorker(worker));
        LOG.debug("Added websocket context");
        
        manager = defaultContainer().addDeployment(servletBuilder);
       
        
        manager.deploy();
        HttpHandler servletHandler = manager.start();
        LOG.debug("started servlet deployment manager");

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
            LOG.info("Enabling HTTP2 protocol");
            LaunchUtil.assertJavaVersion8();
            if(!serverOptions.isEnableSSL()) {
                LOG.warn("SSL is required for HTTP2.  Enabling default SSL server.");
                serverOptions.setEnableSSL(true);
            }
            serverOptions.setSSLPort(serverOptions.getSSLPort()+1);
            serverBuilder.setServerOption(UndertowOptions.ENABLE_HTTP2, true);
            //serverBuilder.setSocketOption(Options.REUSE_ADDRESSES, true);
        }

        if (serverOptions.isEnableSSL()) {
            int sslPort = serverOptions.getSSLPort();
            serverOptions.setDirectBuffers(true);
            LOG.info("Enabling SSL protocol on port " + sslPort);
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
                LOG.error("Unable to start SSL:" + e.getMessage());
                e.printStackTrace();
                System.exit(1);
            }
        }
        
        if (serverOptions.isEnableAJP()) {
            LOG.info("Enabling AJP protocol on port " + serverOptions.getAJPPort());
            serverBuilder.addAjpListener(serverOptions.getAJPPort(), host);
        }
        
        if(serverOptions.getBufferSize() != 0) {
            LOG.info("Buffer Size: " + serverOptions.getBufferSize());
            serverBuilder.setBufferSize(serverOptions.getBufferSize());
        }
        if(serverOptions.getIoThreads() != 0) {
            LOG.info("IO Threads: " + serverOptions.getIoThreads());
            serverBuilder.setIoThreads(serverOptions.getIoThreads());
        }
        if(serverOptions.getWorkerThreads() != 0) {
            LOG.info("Worker threads: " + serverOptions.getWorkerThreads());
            serverBuilder.setWorkerThreads(serverOptions.getWorkerThreads());
        }
        LOG.info("Direct Buffers: " + serverOptions.isDirectBuffers());
        serverBuilder.setDirectBuffers(serverOptions.isDirectBuffers());

        final PathHandler pathHandler = new PathHandler(Handlers.redirect(contextPath)) {
            @Override
            public void handleRequest(final HttpServerExchange exchange) throws Exception {
//                sessionConfig.setSessionId(exchange, ""); // TODO: see if this suppresses jsessionid
                if (exchange.getRequestPath().endsWith(".svgz")) {
                    exchange.getResponseHeaders().put(Headers.CONTENT_ENCODING, "gzip");
                }
                //RunwarLogger.ROOT_LOGGER.trace("pathhandler path:" + exchange.getRequestPath() + " querystring:" +exchange.getQueryString());
                // clear any welcome-file info cached after initial request *NOT THREAD SAFE*
                if (serverOptions.isDirectoryListingRefreshEnabled() && exchange.getRequestPath().endsWith("/")) {
                    //RunwarLogger.ROOT_LOGGER.trace("*** Resetting servlet path info");
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

        if(serverOptions.isSecureCookies()) {
                sessionConfig.setHttpOnly(true);
                sessionConfig.setSecure(true);
        }
        sessionAttachmentHandler.setNext(pathHandler);

        HttpHandler errPageHandler;
        
        if (serverOptions.isGzipEnabled()) {
            final EncodingHandler handler = new EncodingHandler(new ContentEncodingRepository().addEncodingHandler(
                    "gzip", new GzipEncodingProvider(), 50, Predicates.parse("max-content-size[5]")))
                    .setNext(pathHandler);
            errPageHandler = new ErrorHandler(handler);
        } else {
            errPageHandler = new ErrorHandler(pathHandler);
        }

        if (serverOptions.logAccessEnable()) {
//            final String PATTERN = "cs-uri cs(test-header) x-O(aa) x-H(secure)";
            DefaultAccessLogReceiver accessLogReceiver = DefaultAccessLogReceiver.builder().setLogWriteExecutor(worker)
                .setOutputDirectory(options.getLogAccessDir().toPath())
                .setLogBaseName(options.getLogAccessBaseFileName())
//                .setLogFileHeaderGenerator(new ExtendedAccessLogParser.ExtendedAccessLogHeaderGenerator(PATTERN))
                .build();
            LOG.info("Logging combined access to " + options.getLogAccessDir());
//            errPageHandler = new AccessLogHandler(errPageHandler, logReceiver, PATTERN, new ExtendedAccessLogParser( Server.class.getClassLoader()).parse(PATTERN));
//            errPageHandler = new AccessLogHandler(errPageHandler, logReceiver,"common", Server.class.getClassLoader());
            errPageHandler = new AccessLogHandler(errPageHandler, accessLogReceiver,"combined", Server.class.getClassLoader());
        }


        if (serverOptions.logRequestsEnable()) {
            LOG.error("Request log output currently goes to server.log");
            LOG.debug("Enabling request dumper");
            DefaultAccessLogReceiver requestsLogReceiver = DefaultAccessLogReceiver.builder().setLogWriteExecutor(worker)
                    .setOutputDirectory(options.getLogRequestsDir().toPath())
                    .setLogBaseName(options.getLogRequestsBaseFileName())
                    .build();
            errPageHandler = new RequestDebugHandler(errPageHandler, requestsLogReceiver);
        }

        if (serverOptions.isProxyPeerAddressEnabled()) {
            LOG.debug("Enabling Proxy Peer Address handling");
            errPageHandler = new SSLHeaderHandler(new ProxyPeerAddressHandler(errPageHandler));
        }

        Undertow reverseProxy = null;
        if (serverOptions.isHTTP2Enabled()) {
            LOG.debug("Enabling HTTP2 Upgrade and LearningPushHandler");
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
            ProxyHandler proxyHandler = ProxyHandler.builder().setProxyClient(proxy).setMaxRequestTime(30000).setNext(ResponseCodeHandler.HANDLE_404).build();
            reverseProxy = Undertow.builder()
                    .setServerOption(UndertowOptions.ENABLE_HTTP2, true)
                    .addHttpsListener(serverOptions.getSSLPort()-1, serverOptions.getHost(), sslContext)
                    .setHandler(proxyHandler)
                    .build();
        }

        if(serverOptions.isEnableBasicAuth()) {
            securityManager.configureAuth(errPageHandler, serverBuilder, options);
//            serverBuilder.setHandler(errPageHandler);
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
            LOG.error("Unable to get PID:" + e.getMessage());
        }

        serverBuilder.setWorker(worker);
        undertow = serverBuilder.build();

        // start the stop monitor thread
        assert monitor == null;
        monitor = new MonitorThread(stoppassword);
        monitor.start();
        LOG.debug("started stop monitor");


        if(serverOptions.isTrayEnabled()) {
            try {
                Tray.hookTray(this);
                LOG.debug("hooked system tray");	
            } catch( Throwable e ) {
                LOG.error( "system tray hook failed", e );
            }
        } else {
            LOG.debug("System tray integration disabled");
        }

        if (serverOptions.isOpenbrowser()) {
            LOG.debug("Starting open browser action");
            new Server(3);
        }
        
        // if this println changes be sure to update the LaunchUtil so it will know success
        String sslInfo = serverOptions.isEnableSSL() ? " https-port:" + serverOptions.getSSLPort() : "";
        String msg = "Server is up - http-port:" + portNumber + sslInfo + " stop-port:" + socketNumber +" PID:" + PID + " version " + getVersion();
        LOG.info(msg);
        System.out.println(msg);
        if(serverOptions.isTrayEnabled()) {
            LaunchUtil.displayMessage("info", msg);
        }
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
                LOG.error("Could not start MariaDB4j", dbStartException);
                System.out.println("Error starting MariaDB4j: " + dbStartException.getMessage());
            }
        }
        try{

            undertow.start();

            if (serverOptions.isHTTP2Enabled()) {
                LOG.debug("Starting HTTP2 proxy");
                reverseProxy.start();
            }

        } 
        catch (Exception any) {
            if(any.getCause() instanceof java.net.SocketException && any.getCause().getMessage().equals("Permission denied") ) {
            	System.err.println("You need to be root or Administrator to bind to a port below 1024!");                
            } else {
                any.printStackTrace();
            }
            System.exit(1);
        }
    }

    private void configureServerResourceHandler(DeploymentInfo servletBuilder, SessionCookieConfig sessionConfig, File warFile, File webinf, File webXmlFile, String cfmlDirs, String cfengine, Boolean ignoreWelcomePages, Boolean ignoreRestMappings) {
        if(serverMode.equals(Mode.SERVLET)) {
            configureServerServlet(servletBuilder, sessionConfig, warFile, webinf, webXmlFile, cfmlDirs, cfengine, ignoreWelcomePages, ignoreRestMappings);
        } 
        else if(webinf.exists() || serverMode.equals(Mode.SERVLET)) {
            configureServerWar(servletBuilder, sessionConfig, warFile, webinf, webXmlFile, cfmlDirs, cfengine, ignoreWelcomePages, ignoreRestMappings);
        }
        else {
            if (_classLoader == null) {
                throw new RuntimeException("FATAL: Could not load any libs for war: " + warFile.getAbsolutePath());
            }
            servletBuilder.setClassLoader(_classLoader);
            LOG.debug("Running default web server" + warFile.getAbsolutePath());
        }
    }

    private void configureServerWar(DeploymentInfo servletBuilder, SessionCookieConfig sessionConfig, File warFile, File webinf, File webXmlFile, String cfmlDirs, String cfengine, Boolean ignoreWelcomePages, Boolean ignoreRestMappings) {
        Long transferMinSize= serverOptions.getTransferMinSize();
        LOG.debug("found WEB-INF: " + webinf.getAbsolutePath());
        if (_classLoader == null) {
            throw new RuntimeException("FATAL: Could not load any libs for war: " + warFile.getAbsolutePath());
        }
        servletBuilder.setClassLoader(_classLoader);
        servletBuilder.setResourceManager(getResourceManager(warFile, transferMinSize, cfmlDirs, webinf));
//        LogSubverter.subvertJDKLoggers(loglevel);
        WebXMLParser.parseWebXml(new File(webinf, "/web.xml"), webinf, servletBuilder, sessionConfig, ignoreWelcomePages, ignoreRestMappings);
    }
    
    private void configureServerServlet(DeploymentInfo servletBuilder, SessionCookieConfig sessionConfig, File warFile, File webinf, File webXmlFile, String cfmlDirs, String cfengine, Boolean ignoreWelcomePages, Boolean ignoreRestMappings) {
        String cfmlServletConfigWebDir = serverOptions.getCFMLServletConfigWebDir();
        String cfmlServletConfigServerDir = serverOptions.getCFMLServletConfigServerDir();
        Long transferMinSize = serverOptions.getTransferMinSize();
        if (System.getProperty("coldfusion.home") == null) {
            String cfusionDir = new File(webinf,"cfusion").getAbsolutePath();
            if (webXmlFile != null) {
                cfusionDir = new File(webXmlFile.getParentFile(),"cfusion").getAbsolutePath();
            }
            LOG.debug("Setting coldfusion home:" + cfusionDir);
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

        if (_classLoader == null) {
            throw new RuntimeException("FATAL: Could not load any libs for war: " + warFile.getAbsolutePath());
        }
        if (cfmlServletConfigWebDir == null) {
            File webConfigDirFile = new File(getThisJarLocation().getParentFile(), "engine/cfml/server/cfml-web/");
            cfmlServletConfigWebDir = webConfigDirFile.getPath() + "/" + serverName;
        }
        LOG.debug("cfml.web.config.dir: " + cfmlServletConfigWebDir);
        if (cfmlServletConfigServerDir == null || cfmlServletConfigServerDir.length() == 0) {
            File serverConfigDirFile = new File(getThisJarLocation().getParentFile(), "engine/cfml/server/");
            cfmlServletConfigServerDir = serverConfigDirFile.getAbsolutePath();
        }
        LOG.debug("cfml.server.config.dir: " + cfmlServletConfigServerDir);
        String webinfDir = System.getProperty("cfml.webinf");
        if (webinfDir == null) {
            webinf = new File(cfmlServletConfigWebDir, "WEB-INF/");
        } else {
            webinf = new File(webinfDir);
        }
        LOG.debug("cfml.webinf: " + webinf.getPath());

        // servletBuilder.setResourceManager(new CFMLResourceManager(new
        // File(homeDir,"server/"), transferMinSize, cfmlDirs));
        File internalCFMLServerRoot = webinf;
        internalCFMLServerRoot.mkdirs();
        servletBuilder.setResourceManager(getResourceManager(warFile, transferMinSize, cfmlDirs, internalCFMLServerRoot));

        Class<Servlet> cfmlServlet = getCFMLServletClass(cfengine);
        if (webXmlFile != null) {
            LOG.debug("using specified web.xml : " + webXmlFile.getAbsolutePath());
            servletBuilder.setClassLoader(_classLoader);
            WebXMLParser.parseWebXml(webXmlFile, webinf, servletBuilder, sessionConfig, ignoreWelcomePages, ignoreRestMappings);
        } else {
            servletBuilder.setClassLoader(_classLoader);
            Class<Servlet> restServletClass = getRestServletClass(cfengine);
            LOG.debug("loaded servlet classes");
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
                LOG.debug("Adding REST servlet");
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
    }

    private void addShutDownHook() {
        if(shutDownThread == null) {
            shutDownThread = new Thread() {
                public void run() {
                    LOG.debug("Running shutdown hook");
                    try {
                        if(!getServerState().equals(ServerState.STOPPING) && !getServerState().equals(ServerState.STOPPED)) {
                            LOG.debug("shutdown hook:stopServer()");
                            stopServer();
                        }
//                    if(tempWarDir != null) {
//                        LaunchUtil.deleteRecursive(tempWarDir);
//                    }
                        if(mainThread.isAlive()) {
                            LOG.debug("shutdown hook joining main thread");
                            mainThread.interrupt();
                            mainThread.join();
                        }
                    } catch ( Exception e) {
                        e.printStackTrace();
                    }
                    LOG.debug("Shutdown hook finished");
                }
            };
            Runtime.getRuntime().addShutdownHook(shutDownThread);
            LOG.debug("Added shutdown hook");
        }
    }

    public synchronized void stopServer() {
        int exitCode = 0;
        if(shutDownThread != null && Thread.currentThread() != shutDownThread) {
            LOG.debug("Removed shutdown hook");
            Runtime.getRuntime().removeShutdownHook(shutDownThread);
        }
        if(getServerState() == ServerState.STOPPING) {
            LOG.warn("Stop server called, however the server is already stopping.");
        } else if(getServerState() == ServerState.STOPPED) {
            LOG.warn("Stop server called, however the server has already stopped.");
        } else {
            try{
                setServerState(ServerState.STOPPING);
                LOG.info(bar);
                LOG.info("*** stopping server");
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
                    LOG.error("*** server did not appear to be running");
                }
                LOG.info(bar);
                setServerState(ServerState.STOPPED);
                
            } catch (Exception e) {
                e.printStackTrace();
                setServerState(ServerState.UNKNOWN);
                LOG.error("Errserver", e);
                exitCode = 1;
            }
            try {
                if (sysOutTee != null) {
                    sysOutTee.flush();
                    sysOutTee.closeBranch();
                }
                if (sysErrTee != null) {
                    sysErrTee.flush();
                    sysErrTee.closeBranch();
                }
            } catch (Exception e) {
                LOG.error("Redirect:  Unable to close this log file!");
            }
            
            if(exitCode != 0) {
                System.exit(exitCode);
            }
            if(System.getProperty("runwar.classlist") != null && Boolean.parseBoolean(System.getProperty("runwar.classlist"))) {
                ClassLoaderUtils.listAllClasses(serverOptions.getLogDir() + "/classlist.txt");
            }
            if(monitor != null) {
                MonitorThread monitorThread = monitor;
                monitor = null;
                monitorThread.stopListening(false);
                monitorThread.interrupt();
            }


        }

    }

    @SuppressWarnings("unchecked")
    private void configureURLRewrite(DeploymentInfo servletBuilder, File webInfDir) throws ClassNotFoundException, IOException {
        if(serverOptions.isEnableURLRewrite()) {
            LOG.debug("enabling URL rewriting");
            Class<Filter> rewriteFilter;
            String urlRewriteFile = "runwar/urlrewrite.xml";
            try{
                rewriteFilter = (Class<Filter>) _classLoader.loadClass("org.tuckey.web.filters.urlrewrite.UrlRewriteFilter");
            } catch (java.lang.ClassNotFoundException e) {
                rewriteFilter = (Class<Filter>) Server.class.getClassLoader().loadClass("org.tuckey.web.filters.urlrewrite.UrlRewriteFilter");
            }
            if(serverOptions.getURLRewriteFile() != null) {
                if(!serverOptions.getURLRewriteFile().isFile()) {
                    LOG.error("The URL rewrite file " + urlRewriteFile + " does not exist!");
                } else {
                    String rewriteFileName = "urlrewrite";
                    rewriteFileName += serverOptions.isURLRewriteApacheFormat() ? ".htaccess" : ".xml";
                    LaunchUtil.copyFile(serverOptions.getURLRewriteFile(), new File(webInfDir, rewriteFileName));
                    LOG.debug("Copying URL rewrite file " + serverOptions.getURLRewriteFile().getPath() + " to WEB-INF: " + webInfDir.getPath() + "/"+rewriteFileName);
                    urlRewriteFile = "/WEB-INF/"+rewriteFileName;
                }
            }
            
            String rewriteformat = serverOptions.isURLRewriteApacheFormat() ? "modRewrite-style" : "XML";
            LOG.debug(rewriteformat + " rewrite config file: " + urlRewriteFile);
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
            LOG.debug("URL rewriting is disabled");
        }
    }

    @SuppressWarnings({ "unchecked" })
    private void configurePathInfoFilter(DeploymentInfo servletBuilder) throws ClassNotFoundException, IOException {
        if(serverOptions.isFilterPathInfoEnabled()) {
            LOG.debug("enabling path_info filter");
            Class<Filter> regexPathInfoFilter;
            try{
                regexPathInfoFilter = (Class<Filter>) _classLoader.loadClass("org.cfmlprojects.regexpathinfofilter.RegexPathInfoFilter");
            } catch (java.lang.ClassNotFoundException e) {
                regexPathInfoFilter = (Class<Filter>) Server.class.getClassLoader().loadClass("org.cfmlprojects.regexpathinfofilter.RegexPathInfoFilter");
            }
            servletBuilder.addFilter(new FilterInfo("RegexPathInfoFilter", regexPathInfoFilter));
            servletBuilder.addFilterUrlMapping("RegexPathInfoFilter", "/*", DispatcherType.REQUEST);
            servletBuilder.addFilterUrlMapping("RegexPathInfoFilter", "/*", DispatcherType.FORWARD);
        } else {
            LOG.debug("path_info filter is disabled");
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
                    LOG.debug("Adding mime-type: " + mime.getExtension() + " - " + mime.getMimeType());
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
                LOG.debug("lib: Skipping non-existent: " + file.getAbsolutePath());
                continue;
            }
            for (File item : file.listFiles()) {
                String fileName = item.getAbsolutePath().toLowerCase();
                if (!item.isDirectory()) {
                    if (fileName.endsWith(".jar") || fileName.endsWith(".zip")) {
                        if(fileName.contains("slf4j")) {
                            LOG.debug("lib: Skipping slf4j jar: " + item.getAbsolutePath());
                        } else {
                            URL url = item.toURI().toURL();
                            classpath.add(url);
                            LOG.trace("lib: added to classpath: " + item.getAbsolutePath());
                        }
                    }
                }
            }
        }
        return classpath;
    }

    @SuppressWarnings("unchecked")
    private static Class<Servlet> getCFMLServletClass(String cfengine) {
        Class<Servlet> cfmlServlet = null;
        try {
            cfmlServlet = (Class<Servlet>) _classLoader.loadClass(cfengine + ".loader.servlet.CFMLServlet");
            LOG.debug("dynamically loaded CFML servlet from runwar child classloader");
        } catch (java.lang.ClassNotFoundException devnul) {
            try {
                cfmlServlet = (Class<Servlet>) Server.class.getClassLoader().loadClass(cfengine + ".loader.servlet.CFMLServlet");
                LOG.debug("dynamically loaded CFML servlet from runwar classloader");
            } catch(java.lang.ClassNotFoundException e) {
            }
        }
        return cfmlServlet;
    }
    
    @SuppressWarnings("unchecked")
    private static Class<Servlet> getRestServletClass(String cfengine) {
        Class<Servlet> restServletClass = null;
        try {
            restServletClass = (Class<Servlet>) _classLoader.loadClass(cfengine + ".loader.servlet.RestServlet");
        } catch (java.lang.ClassNotFoundException e) {
            try {
                restServletClass = (Class<Servlet>) Server.class.getClassLoader().loadClass(cfengine + ".loader.servlet.RestServlet");
            } catch (ClassNotFoundException e1) {
                e1.printStackTrace();
            }
        }
        return restServletClass;
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
            LOG.debug("WEB-INF classes directory (" + classesDir.getAbsolutePath() + ") does not exist");
        }
        return classpath;
    }

    public static void printVersion() {
        System.out.println(LaunchUtil.getResourceAsString("runwar/version.properties"));
        System.out.println(LaunchUtil.getResourceAsString("io/undertow/version.properties"));
    }

    public static String getVersion() {
        String[] version = LaunchUtil.getResourceAsString("runwar/version.properties").split("=");
        return version[version.length - 1].trim();
    }

    private class MonitorThread extends Thread {

        private char[] stoppassword;
        private volatile boolean listening = false;
        private volatile boolean systemExitOnStop = true;
        private ServerSocket serverSocket;

        public MonitorThread(char[] stoppassword) {
            this.stoppassword = stoppassword;
            setDaemon(true);
            setName("StopMonitor");
        }

        @Override
        public void run() {
            // Executor exe = Executors.newCachedThreadPool();
            int exitCode = 0;
            serverSocket = null;
            try {
                serverSocket = new ServerSocket(socketNumber, 1, InetAddress.getByName(serverOptions.getHost()));
                listening = true;
                LOG.info(bar);
                LOG.info("*** starting 'stop' listener thread - Host: " + serverOptions.getHost()
                        + " - Socket: " + socketNumber);
                LOG.info(bar);
                while (listening) {
                    LOG.debug("StopMonitor listening for password");
                    if(serverState == ServerState.STOPPED || serverState == ServerState.STOPPING){
                        listening = false;
                    }
                    final Socket clientSocket = serverSocket.accept();
                    int r, i = 0;
                    BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                    try {
                        while (listening && (r = reader.read()) != -1) {
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
                            if(listening) {
                                LOG.warn("Incorrect password used when trying to stop server.");
                            } else {
                                LOG.debug("stopped listening for stop password.");
                            }
                                
                        }
                    } catch (java.net.SocketException e) {
                        // reset
                    }
                    try {
                        clientSocket.close();
                    } catch (IOException e) {
                        LOG.error(e);
                    }
                }
            } catch (Exception e) {
                LOG.error(e);
                exitCode = 1;
                e.printStackTrace();
            } finally {
                LOG.debug("Closing server socket");
                try {
                    serverSocket.close();
                    serverSocket = null;
                } catch (IOException e) {
                    LOG.error(e);
                    e.printStackTrace();
                }
                try {
                    if (mainThread.isAlive()) {
                        LOG.debug("monitor joining main thread");
                        mainThread.interrupt();
                        try{
                            mainThread.join();
                        } catch (InterruptedException ie){
                            // expected
                        }
                    }
                } catch (Exception e) {
                    LOG.error(e);
                    e.printStackTrace();
                }
            }
            if(systemExitOnStop)
                System.exit(exitCode); // this will call our shutdown hook
//            Thread.currentThread().interrupt();
            return;
        }
        
        public void stopListening(boolean systemExitOnStop) {
            this.systemExitOnStop = systemExitOnStop;
            listening = false;
            // send a char to the reader so it will stop waiting
            Socket s;
            try {
                s = new Socket(InetAddress.getByName(serverOptions.getHost()), socketNumber);
                OutputStream out = s.getOutputStream();
                out.write('s');
                out.flush();
                out.close();
                s.close();
            } catch (IOException e) {
                // expected if already stopping
            }

        }
        
    }

    public boolean serverWentDown() {
        try {
            return serverWentDown(serverOptions.getLaunchTimeout(), 3000, InetAddress.getByName(serverOptions.getHost()), serverOptions.getPortNumber());
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
            sock = new Socket();
            InetSocketAddress sa = new InetSocketAddress(server, port);
            sock.connect(sa, 500);
            return true;
        } catch (ConnectException e) {
            LOG.debug("Error while connecting. " + e.getMessage());
        } catch (SocketTimeoutException e) {
            LOG.debug("Connection: " + e.getMessage() + ".");
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (sock != null) {
                try {
                    sock.close();
                } catch (IOException e) {
                    // don't care
                }
            }
        }
        return false;
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
                LOG.error(e.getMessage());
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
                LOG.error(e.getMessage());
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

    public static class Mode {
        public static final String WAR = "war";
        public static final String SERVLET = "servlet";
        public static final String DEFAULT = "default";
    }

}
