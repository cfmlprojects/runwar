package runwar;

import io.undertow.predicate.Predicates;
import io.undertow.server.handlers.cache.CacheHandler;
import io.undertow.server.handlers.cache.DirectBufferCache;
import io.undertow.server.handlers.resource.ResourceHandler;
import io.undertow.servlet.api.*;
import io.undertow.servlet.handlers.DefaultServlet;
import io.undertow.util.MimeMappings;
import runwar.options.ServerOptions;
import runwar.undertow.WebXMLParser;

import javax.servlet.DispatcherType;
import javax.servlet.Filter;
import javax.servlet.Servlet;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.*;

import static io.undertow.Handlers.predicate;
import static io.undertow.servlet.Servlets.servlet;
import static runwar.logging.RunwarLogger.LOG;

class RunwarConfigurer {

    private static ServerOptions serverOptions;
    private static ClassLoader _classLoader;
    private final String serverMode;
    private final Server server;

    RunwarConfigurer(final Server server){
        this.server = server;
        _classLoader = Server.getClassLoader();
        serverOptions = Server.getServerOptions();
        serverMode = serverOptions.getServerMode();
    }


    void configureServerResourceHandler(DeploymentInfo servletBuilder, ServletSessionConfig sessionConfig, File warFile, File webinf, File webXmlFile, String cfmlDirs, String cfengine, Boolean ignoreWelcomePages, Boolean ignoreRestMappings) {
        String cfusionDir = new File(webinf,"cfusion").getAbsolutePath().replace('\\', '/');
        String cfformDir = new File(webinf,"cfform").getAbsolutePath().replace('\\', '/');
        final String cfClasspath = "%s/lib/updates/,%s/lib/,%s/lib/axis2,%s/gateway/lib/,%s/../cfform/jars,%s/../flex/jars,%s/lib/oosdk/lib,%s/lib/oosdk/classes".replaceAll("%s", cfusionDir);
        final HashMap<String,String> cfprops = new HashMap<>();
        cfprops.put("coldfusion.home", cfusionDir);
        cfprops.put("coldfusion.rootDir", cfusionDir);
        cfprops.put("coldfusion.libPath", cfusionDir + "/lib");
        cfprops.put("flex.dir", cfformDir);
        cfprops.put("coldfusion.jsafe.defaultalgo", "FIPS186Random");
        cfprops.put("coldfusion.classPath", cfClasspath);
        // Hide error messages about MediaLib stuff
        cfprops.put("com.sun.media.jai.disableMediaLib", "true");
        // Make the embedded version of Jetty inside Adobe CF shut up since it dumps everything to the error stream
        cfprops.put("java.security.policy", cfusionDir + "/lib/coldfusion.policy");
        cfprops.put("java.security.auth.policy", cfusionDir + "/lib/neo_jaas.policy");
        cfprops.put("java.nixlibrary.path", cfusionDir + "/lib");
        cfprops.put("java.library.path", cfusionDir + "/lib");
        if (cfengine.equals("adobe") || cfengine.equals("") && new File(cfusionDir).exists()) {
            LOG.debug("Setting coldfusion.home: '" + cfusionDir + "'");
            LOG.debug("Setting coldfusion.classpath: '" + cfClasspath + "'");
            LOG.debug("Setting flex.dir (cfform): '" + cfformDir + "'");
            if(serverOptions.isEnableSSL()) {
                LOG.debug("disabling com.sun.net.ssl.enableECC");
                cfprops.put("com.sun.net.ssl.enableECC", "false");
            }
            if (System.getProperty("coldfusion.home") == null) {
                cfprops.forEach((k, v) -> {
                    System.setProperty(k, v);
                    LOG.tracef("Setting %s = '%s'", k, v);
                });
            }
            cfengine = "adobe";
        }

        if(serverMode.equals(Server.Mode.SERVLET)) {
            configureServerServlet(servletBuilder, warFile, webinf, webXmlFile, cfmlDirs, cfengine, ignoreWelcomePages, ignoreRestMappings);
        }
        else if(webinf.exists() || serverMode.equals(Server.Mode.SERVLET)) {
            configureServerWar(servletBuilder, warFile, webinf, webXmlFile, cfmlDirs, cfengine, ignoreWelcomePages, ignoreRestMappings);
        }
        else {
            if (_classLoader == null) {
                throw new RuntimeException("FATAL: Could not load any libs for war: " + warFile.getAbsolutePath());
            }
            servletBuilder.setClassLoader(_classLoader);
            LOG.debug("Running default web server '" + warFile.getAbsolutePath()+ "'");
        }
        if(cfengine.equals("adobe")) {
            String cfCompilerOutput = (String) servletBuilder.getServletContextAttributes().get("coldfusion.compiler.outputDir");
            if(cfCompilerOutput == null || cfCompilerOutput.matches("^.?WEB-INF.*?")){
                // TODO: figure out why adobe needs the absolute path, vs. /WEB-INF/cfclasses
                File cfCompilerOutputDir = new File(webinf, "/cfclasses").getAbsoluteFile();
                try {
                    cfCompilerOutputDir = new File(webinf, "/cfclasses").getAbsoluteFile().getCanonicalFile();
                } catch (IOException e) {
                    LOG.error(e);
                }
                LOG.debug("Setting coldfusion.compiler.outputDir: '" + cfCompilerOutputDir.getPath() + "'");
                if( !cfCompilerOutputDir.exists() ) {
                    if(!cfCompilerOutputDir.mkdir()){
                        LOG.error("Unable to create cfclasses dir: '" + cfCompilerOutputDir.getPath() + "'");
                    }
                }
                servletBuilder.addServletContextAttribute("coldfusion.compiler.outputDir", cfCompilerOutputDir.getPath());
                LOG.debug(servletBuilder.getServletContextAttributes().toString());
            }
        }
    }

    void configureServerWar(DeploymentInfo servletBuilder, File warFile, File webinf, File webXmlFile, String cfmlDirs, String cfengine, Boolean ignoreWelcomePages, Boolean ignoreRestMappings) {
        Long transferMinSize= serverOptions.getTransferMinSize();
        LOG.debug("found WEB-INF: '" + webinf.getAbsolutePath() + "'");
        if (_classLoader == null) {
            throw new RuntimeException("FATAL: Could not load any libs for war: " + warFile.getAbsolutePath());
        }
        servletBuilder.setClassLoader(_classLoader);
        servletBuilder.setResourceManager(server.getResourceManager(warFile, transferMinSize, cfmlDirs, webinf));
//        LogSubverter.subvertJDKLoggers(loglevel);
        WebXMLParser.parseWebXml(webXmlFile, webinf, servletBuilder, ignoreWelcomePages, ignoreRestMappings);
    }

    void configureServerServlet(DeploymentInfo servletBuilder, File warFile, File webinf, File webXmlFile, String cfmlDirs, String cfengine, Boolean ignoreWelcomePages, Boolean ignoreRestMappings) {
        String cfmlServletConfigWebDir = serverOptions.getCFMLServletConfigWebDir();
        String cfmlServletConfigServerDir = serverOptions.getCFMLServletConfigServerDir();
        Long transferMinSize = serverOptions.getTransferMinSize();

        if (_classLoader == null) {
            throw new RuntimeException("FATAL: Could not load any libs for war: " + warFile.getAbsolutePath());
        }
        if (cfmlServletConfigWebDir == null) {
            File webConfigDirFile = new File(Server.getThisJarLocation().getParentFile(), "engine/cfml/server/cfml-web/");
            cfmlServletConfigWebDir = webConfigDirFile.getPath() + "/" + serverOptions.getServerName();
        }
        LOG.debug("cfml.web.config.dir: " + cfmlServletConfigWebDir);
        if (cfmlServletConfigServerDir == null || cfmlServletConfigServerDir.length() == 0) {
            File serverConfigDirFile = new File(Server.getThisJarLocation().getParentFile(), "engine/cfml/server/");
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
        if(!internalCFMLServerRoot.mkdirs()){
            LOG.errorf("Unable to create cfml resource server root: %s", internalCFMLServerRoot.getAbsolutePath());
        }
        servletBuilder.setResourceManager(server.getResourceManager(warFile, transferMinSize, cfmlDirs, internalCFMLServerRoot));

        servletBuilder.setClassLoader(_classLoader);
        if (webXmlFile != null) {
            LOG.debug("using specified web.xml : " + webXmlFile.getAbsolutePath());
            WebXMLParser.parseWebXml(webXmlFile, webinf, servletBuilder, ignoreWelcomePages, ignoreRestMappings);
        } else {
            Class<Servlet> cfmlServlet = getCFMLServletClass(cfengine);
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


    @SuppressWarnings("unchecked")
    void configureURLRewrite(DeploymentInfo servletBuilder, File webInfDir) throws ClassNotFoundException {
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
                    String message = "The URL rewrite file " + urlRewriteFile + " does not exist!";
                    LOG.error(message);
                    throw new RuntimeException(message);
                } else {
                    String rewriteFileName = "urlrewrite";
                    rewriteFileName += serverOptions.isURLRewriteApacheFormat() ? ".htaccess" : ".xml";
                    LaunchUtil.copyFile(serverOptions.getURLRewriteFile(), new File(webInfDir, rewriteFileName));
                    LOG.debug("Copying URL rewrite file " + serverOptions.getURLRewriteFile().getPath() + " to WEB-INF: " + webInfDir.getPath() + "/"+rewriteFileName);
                    urlRewriteFile = "/WEB-INF/"+rewriteFileName;
                }
            }

            String rewriteFormat = serverOptions.isURLRewriteApacheFormat() ? "modRewrite-style" : "XML";
            LOG.debug(rewriteFormat + " rewrite config file: " + urlRewriteFile);
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
            rewriteFilterInfo.addInitParam("logLevel", "SLF4J");
            servletBuilder.addFilter(rewriteFilterInfo);
            servletBuilder.addFilterUrlMapping("UrlRewriteFilter", "/*", DispatcherType.REQUEST);
        } else {
            LOG.debug("URL rewriting is disabled");
        }
    }

    @SuppressWarnings({ "unchecked" })
    void configurePathInfoFilter(DeploymentInfo servletBuilder) throws ClassNotFoundException {
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


    @SuppressWarnings("unchecked")
    static Class<Servlet> getCFMLServletClass(String cfengine) {
        Class<Servlet> cfmlServlet = null;
        try {
            cfmlServlet = (Class<Servlet>) _classLoader.loadClass(cfengine + ".loader.servlet.CFMLServlet");
            LOG.debug("dynamically loaded CFML servlet from runwar child classloader");
        } catch (java.lang.ClassNotFoundException devnul) {
            try {
                cfmlServlet = (Class<Servlet>) Server.class.getClassLoader().loadClass(cfengine + ".loader.servlet.CFMLServlet");
                LOG.debug("dynamically loaded CFML servlet from runwar classloader");
            } catch(java.lang.ClassNotFoundException e) {
                LOG.trace("No CFML servlet found in class loader hierarchy");
            }
        }
        return cfmlServlet;
    }

    @SuppressWarnings("unchecked")
    static Class<Servlet> getRestServletClass(String cfengine) {
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


    public static List<URL> getJarList(String libDirs) throws IOException {
        List<URL> classpath = new ArrayList<>();
        String[] list = libDirs.split(",");
        for (String path : list) {
            if (".".equals(path) || "..".equals(path))
                continue;

            File file = new File(path);
            // Ignore non-existent dirs
            if( !file.exists() ) {
                LOG.debug("lib: Skipping non-existent: " + file.getAbsolutePath());
                continue;
            }
            for (File item : Objects.requireNonNull(file.listFiles())) {
                String fileName = item.getAbsolutePath().toLowerCase();
                if (!item.isDirectory()) {
                    if (fileName.endsWith(".jar") || fileName.endsWith(".zip")) {
                        if(fileName.contains("slf4j") || fileName.contains("log4j")) {
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

    void addCacheHandler(final DeploymentInfo servletBuilder) {
        // this handles mime types and adds a simple cache for static files
        servletBuilder.addInitialHandlerChainWrapper(handler -> {
            final ResourceHandler resourceHandler = new ResourceHandler(servletBuilder.getResourceManager());
            MimeMappings.Builder mimes = MimeMappings.builder();
            // add font mime types not included by default
            mimes.addMapping("eot", "application/vnd.ms-fontobject");
            mimes.addMapping("otf", "font/opentype");
            mimes.addMapping("ttf", "application/x-font-ttf");
            mimes.addMapping("woff", "application/x-font-woff");
            List<String> suffixList = new ArrayList<>(Arrays.asList(".eot", ".otf", ".ttf", ".woff"));
            // add the default types and any added in web.xml files
            for(MimeMapping mime : servletBuilder.getMimeMappings()) {
                LOG.debug("Adding mime-name: " + mime.getExtension() + " - " + mime.getMimeType());
                mimes.addMapping(mime.getExtension(), mime.getMimeType());
                suffixList.add("."+mime.getExtension());
            }
            resourceHandler.setMimeMappings(mimes.build());
            String[] suffixes = new String[suffixList.size()];
            suffixes = suffixList.toArray(suffixes);
            // simple cacheHandler, someday maybe make this configurable
            final CacheHandler cacheHandler = new CacheHandler(new DirectBufferCache(1024, 10, 10480), resourceHandler);
            return predicate(Predicates.suffixes(suffixes), cacheHandler, handler);
        });
    }

    void configureServlet(final DeploymentInfo servletBuilder, final String[] defaultWelcomeFiles, Boolean ignoreWelcomePages, Boolean ignoreRestMappings) throws ClassNotFoundException {
        File webinf = serverOptions.getWebInfDir();
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

        if(ignoreRestMappings && serverOptions.getServletRestEnabled()) {
            LOG.info("Overriding web.xml rest mappings with " + Arrays.toString( serverOptions.getServletRestMappings() ) );
            for (Map.Entry<String, ServletInfo> stringServletInfoEntry : servletBuilder.getServlets().entrySet()) {
                ServletInfo restServlet = stringServletInfoEntry.getValue();
//                LOG.trace("Checking servlet named: " + restServlet.getName() + " to see if it's a REST servlet.");
                if (restServlet.getName().toLowerCase().equals("restservlet") || restServlet.getName().toLowerCase().equals("cfrestservlet")) {
                    for (String path : serverOptions.getServletRestMappings()) {
                        restServlet.addMapping(path);
                        LOG.info("Added rest mapping: " + path + " to " + restServlet.getName());
                    }
                }
            }
        } else if (!serverOptions.getServletRestEnabled()) {
            LOG.trace("REST servlets disabled");
        }

    }

}
