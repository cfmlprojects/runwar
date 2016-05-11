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
import java.net.URL;
import java.net.URLClassLoader;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.awt.Image;

import javax.net.SocketFactory;
import javax.servlet.DispatcherType;

import runwar.logging.Logger;
import runwar.logging.LogSubverter;
import runwar.mariadb4j.MariaDB4jManager;
import runwar.options.CommandLineHandler;
import runwar.options.ServerOptions;
import runwar.undertow.MappedResourceManager;
import runwar.undertow.WebXMLParser;
import runwar.util.TeeOutputStream;
import io.undertow.Handlers;
import io.undertow.Undertow;
import io.undertow.Undertow.Builder;
import io.undertow.predicate.Predicates;
import io.undertow.server.HandlerWrapper;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.PathHandler;
import io.undertow.server.handlers.PredicateHandler;
import io.undertow.server.handlers.cache.CacheHandler;
import io.undertow.server.handlers.cache.DirectBufferCache;
import io.undertow.server.handlers.encoding.ContentEncodingRepository;
import io.undertow.server.handlers.encoding.EncodingHandler;
import io.undertow.server.handlers.encoding.GzipEncodingProvider;
import io.undertow.server.handlers.resource.ResourceHandler;
import io.undertow.servlet.api.DeploymentInfo;
import io.undertow.servlet.api.DeploymentManager;
import io.undertow.servlet.api.FilterInfo;
import io.undertow.servlet.api.MimeMapping;
import io.undertow.servlet.api.ServletInfo;
import io.undertow.servlet.handlers.DefaultServlet;
import io.undertow.util.Headers;
import io.undertow.util.MimeMappings;
import static io.undertow.servlet.Servlets.defaultContainer;
import static io.undertow.servlet.Servlets.deployment;
import static io.undertow.servlet.Servlets.servlet;

public class Server {

    private static Logger log = Logger.getLogger("RunwarLogger");
    private TeeOutputStream tee;
    private static ServerOptions serverOptions;
    private static MariaDB4jManager mariadb4jManager;
    static volatile boolean listening;
    int portNumber;
    int socketNumber;
    private DeploymentManager manager;
    private Undertow undertow;

    private String PID;
    private String serverState = ServerState.STOPPED;

    private static URLClassLoader _classLoader;

    private String serverName = "default";
    private File statusFile = null;
    public static final String bar = "******************************************************************************";
    
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
            _classpath.add(getClass().getResource("/bcpkix-jdk15on.jar"));
            _classpath.add(getClass().getResource("/bcprov-jdk15on.jar"));
            log.debugf("classpath: %s",_classpath);
//          _classLoader = new URLClassLoader(_classpath.toArray(new URL[_classpath.size()]),Thread.currentThread().getContextClassLoader());
//          _classLoader = new URLClassLoader(_classpath.toArray(new URL[_classpath.size()]),ClassLoader.getSystemClassLoader());
//          _classLoader = new URLClassLoader(_classpath.toArray(new URL[_classpath.size()]));
            _classLoader = new URLClassLoader(_classpath.toArray(new URL[_classpath.size()]));
//          _classLoader = new XercesFriendlyURLClassLoader(_classpath.toArray(new URL[_classpath.size()]),ClassLoader.getSystemClassLoader());
//          Thread.currentThread().setContextClassLoader(_classLoader);
        }
    }
    
    protected void setClassLoader(URLClassLoader classLoader){
        _classLoader = classLoader;
    }
    
    public static URLClassLoader getClassLoader(){
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
    
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void startServer(final String[] args) throws Exception {
        ensureJavaVersion();
        serverState = ServerState.STARTING;
        serverOptions = CommandLineHandler.parseArguments(args);
        if(serverOptions.getAction().equals("stop")){
            Stop.stopServer(args,true);
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

        if (serverOptions.isBackground()) {
            setServerState(ServerState.STARTING_BACKGROUND);
            // this will eventually system.exit();
            List<String> argarray = new ArrayList<String>();
            for (String arg : args) {
                if (arg.contains("background") || arg.startsWith("-b")) {
                    continue;
                } else {
                    argarray.add(arg);
                }
            }
            argarray.add("--background");
            argarray.add("false");
            int launchTimeout = serverOptions.getLaunchTimeout();
            LaunchUtil.relaunchAsBackgroundProcess(launchTimeout, argarray.toArray(new String[argarray.size()]),
                    processName);
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
            if (logDirectory.exists()) {
                log.info("Logging to " + logDirectory + "/server.out.txt");
                tee = new TeeOutputStream(System.out, new FileOutputStream(logDirectory + "/server.out.txt"));
                PrintStream newOut = new PrintStream(tee, true);
                System.setOut(newOut);
                System.setErr(newOut);
            } else {
                log.error("Could not create log: " + logDirectory + "/server.out.txt");
            }
        }
        
        new AgentInitialization().loadAgentFromLocalJarFile(new File(warFile, "/WEB-INF/lib/"));

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
        if (warFile.isDirectory() && webinf.exists()) {
            libDirs = webinf.getAbsolutePath() + "/lib";
            log.info("Using existing WEB-INF/lib of: " + libDirs);
        }

        List<URL> cp = new ArrayList<URL>();
        if (libDirs != null || jarURL != null) {
            if (libDirs != null)
                cp.addAll(getJarList(libDirs));
            if (jarURL != null)
                cp.add(jarURL);
        }
        if(serverOptions.getMariaDB4jImportSQLFile() != null){
            System.out.println("ADDN"+serverOptions.getMariaDB4jImportSQLFile().toURI().toURL());
            cp.add(serverOptions.getMariaDB4jImportSQLFile().toURI().toURL());
        }
        cp.addAll(getClassesList(new File(webinf, "/classes")));
        initClassLoader(cp);
        
        mariadb4jManager = new MariaDB4jManager(_classLoader);

        log.debug("Transfer Min Size: " + serverOptions.getTransferMinSize());

        final DeploymentInfo servletBuilder = deployment()
                .setContextPath(contextPath.equals("/") ? "" : contextPath)
                .setTempDir(new File(System.getProperty("java.io.tmpdir")))
                .setDeploymentName(warPath);

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
                    getThisJarLocation().getPath() + ":" + System.getProperty("java.library.path"));
        }
        log.debug("java.library.path:" + System.getProperty("java.library.path"));

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

        if(warFile.isDirectory() && !webinf.exists()) {
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
            servletBuilder.setResourceManager(new MappedResourceManager(warFile, transferMinSize, cfmlDirs, internalCFMLServerRoot));

            if (webXmlFile != null) {
                log.debug("using specified web.xml : " + webXmlFile.getAbsolutePath());
                servletBuilder.setClassLoader(_classLoader);
                WebXMLParser.parseWebXml(webXmlFile, servletBuilder);
            } else {
                if (_classLoader == null) {
                    throw new RuntimeException("FATAL: Could not load any libs for war: " + warFile.getAbsolutePath());
                }
                servletBuilder.setClassLoader(_classLoader);
                Class cfmlServlet;
                Class restServlet;
                try {
                    cfmlServlet = _classLoader.loadClass(cfengine + ".loader.servlet.CFMLServlet");
                    log.debug("dynamically loaded CFML servlet from runwar child classloader");
                } catch (java.lang.ClassNotFoundException e) {
                    cfmlServlet = Server.class.getClassLoader().loadClass(cfengine + ".loader.servlet.CFMLServlet");
                    log.debug("dynamically loaded CFML servlet from runwar classloader");
                }
                try {
                    restServlet = _classLoader.loadClass(cfengine + ".loader.servlet.RestServlet");
                } catch (java.lang.ClassNotFoundException e) {
                    restServlet = Server.class.getClassLoader().loadClass(cfengine + ".loader.servlet.RestServlet");
                }
                log.debug("loaded servlet classes");
                servletBuilder
                    .addWelcomePages(serverOptions.getWelcomeFiles())
                    .addServlets(
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
                                ,
                        servlet("RESTServlet", restServlet)
                                .setRequireWelcomeFileMapping(true)
                                .addInitParam(cfengine+"-web-directory",cfmlServletConfigWebDir)
                                .addMapping("/rest/*")
                                .setLoadOnStartup(2));
            }
        } else if(webinf.exists()) {
            log.debug("found WEB-INF: " + webinf.getAbsolutePath());
            if (_classLoader == null) {
                throw new RuntimeException("FATAL: Could not load any libs for war: " + warFile.getAbsolutePath());
            }
            servletBuilder.setClassLoader(_classLoader);
            servletBuilder.setResourceManager(new MappedResourceManager(warFile, transferMinSize, cfmlDirs, webinf));
            LogSubverter.subvertJDKLoggers(loglevel);
            WebXMLParser.parseWebXml(new File(webinf, "/web.xml"), servletBuilder);
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

        configureURLRewrite(servletBuilder, webinf);

        if (serverOptions.isCacheEnabled()) {
            addCacheHandler(servletBuilder);
        }

        if (serverOptions.isCustomHTTPStatusEnabled()) {
            servletBuilder.setSendCustomReasonPhraseOnError(true);
        }

        // this prevents us from having to use our own ResourceHandler (directory listing, welcome files, see below) and error handler for now
        servletBuilder.addServlet(new ServletInfo(io.undertow.servlet.handlers.ServletPathMatches.DEFAULT_SERVLET_NAME, DefaultServlet.class)
            .addInitParam("directory-listing", Boolean.toString(serverOptions.isDirectoryListingEnabled())));

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

        if (serverOptions.isEnableSSL()) {
            int sslPort = serverOptions.getSSLPort();
            serverBuilder.setDirectBuffers(true);
            log.info("Enabling SSL protocol on port " + sslPort);
            if (serverOptions.getSSLCertificate() != null) {
                File certfile = serverOptions.getSSLCertificate();
                File keyfile = serverOptions.getSSLKey();
                char[] keypass = serverOptions.getSSLKeyPass();
                serverBuilder.addHttpsListener(sslPort, host, SSLUtil.createSSLContext(certfile, keyfile, keypass));
                Arrays.fill(keypass, '*');
            } else {
                serverBuilder.addHttpsListener(sslPort, host, SSLUtil.createSSLContext());
            }
        }
        
        if (serverOptions.isEnableAJP()) {
            log.info("Enabling AJP protocol on port " + serverOptions.getAJPPort());
            serverBuilder.addAjpListener(serverOptions.getAJPPort(), host);
        }

//        final PathHandler pathHandler = Handlers.path(Handlers.redirect(contextPath))
//                .addPrefixPath(contextPath, servletHandler);

        final PathHandler pathHandler = new PathHandler(Handlers.redirect(contextPath)) {
            @Override
            public void handleRequest(final HttpServerExchange exchange) throws Exception {
                if (exchange.getRequestPath().endsWith(".svgz")) {
                    exchange.getResponseHeaders().put(Headers.CONTENT_ENCODING, "gzip");
                }
                super.handleRequest(exchange);
            }
        };
        pathHandler.addPrefixPath(contextPath, servletHandler);

        if (serverOptions.isGzipEnabled()) {
            final EncodingHandler handler = new EncodingHandler(new ContentEncodingRepository().addEncodingHandler(
                    "gzip", new GzipEncodingProvider(), 50, Predicates.parse("max-content-size[5]")))
                    .setNext(pathHandler);
            serverBuilder.setHandler(handler);
        } else {
            serverBuilder.setHandler(pathHandler);
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
        Thread monitor = new MonitorThread(stoppassword);
        monitor.start();
        log.debug("started stop monitor");
        LaunchUtil.hookTray(this);
        log.debug("hooked system tray");

        if (serverOptions.isOpenbrowser()) {
            new Server(3);
        }
        
        // if this println changes be sure to update the LaunchUtil so it will know success
        String msg = "Server is up - http-port:" + portNumber + " stop-port:" + socketNumber +" PID:" + PID + " version " + getVersion();
        log.debug(msg);
        System.out.println(msg);
        setServerState(ServerState.STARTED);

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

        undertow.start();
    }

    private void addShutDownHook() {
        final Thread mainThread = Thread.currentThread();
        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                try {
                    stopServer();
//                    if(tempWarDir != null) {
//                        LaunchUtil.deleteRecursive(tempWarDir);
//                    }
                    mainThread.join();
                } catch ( Exception e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        });
        log.debug("Added shutdown hook");
    }

    public void stopServer() {
        int exitCode = 0;
        try{
            System.out.println();
            System.out.println(bar);
            System.out.println("*** stopping server");
            if (serverOptions.isMariaDB4jEnabled()) {
                mariadb4jManager.stop();
            }
            try {
                manager.undeploy();
                undertow.stop();
                Thread.sleep(1000);
            } catch (Exception notRunning) {
                System.out.println("*** server did not appear to be running");
            }
            System.out.println(bar);
            setServerState(ServerState.STOPPED);
        } catch (Exception e) {
            e.printStackTrace();
            setServerState(ServerState.UNKNOWN);
            log.error(e);
            exitCode = 1;
        }
        try {
            if (tee != null)
                tee.close();
        } catch (Exception e) {
            System.out.println("Redirect:  Unable to close this log file!");
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
            log.debug("URL rewriting config file: " + urlRewriteFile);
            servletBuilder.addFilter(new FilterInfo("UrlRewriteFilter", rewriteFilter)
                .addInitParam("confPath", urlRewriteFile)
                .addInitParam("statusEnabled", Boolean.toString(serverOptions.isDebug()))
                .addInitParam("modRewriteConf", "false"));
            servletBuilder.addFilterUrlMapping("UrlRewriteFilter", "/*", DispatcherType.REQUEST);
        } else {
            log.debug("URL rewriting is disabled");            
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
                final PredicateHandler predicateHandler = new PredicateHandler(Predicates.suffixes(suffixes), cacheHandler, handler);
                return predicateHandler;
            }
        });
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
            for (File item : file.listFiles()) {
                String fileName = item.getAbsolutePath();
                if (!item.isDirectory()) {
                    if (fileName.toLowerCase().endsWith(".jar") || fileName.toLowerCase().endsWith(".zip")) {
                        URL url = item.toURI().toURL();
                        classpath.add(url);
                        log.debug("lib: added to classpath: " + fileName);
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
            listening = true;
            try {
                serverSocket = new ServerSocket(socketNumber, 1, InetAddress.getByName(serverOptions.getHost()));
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
            System.exit(exitCode);
//            Thread.currentThread().interrupt();
            return;
        }
    }

    public static boolean serverWentDown(int timeout, long sleepTime, InetAddress server, int port) {
        long start = System.currentTimeMillis();
        while ((System.currentTimeMillis() - start) < timeout) {
            if (checkServerIsUp(server, port)) {
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

    private void setServerState(String state) {
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
