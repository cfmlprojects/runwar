package runwar;

import io.undertow.predicate.Predicates;
import io.undertow.server.handlers.cache.CacheHandler;
import io.undertow.server.handlers.cache.DirectBufferCache;
import io.undertow.server.handlers.resource.ResourceHandler;
import io.undertow.servlet.api.*;
import io.undertow.servlet.handlers.DefaultServlet;
import io.undertow.util.MimeMappings;
import runwar.options.ServerOptions;
import runwar.security.SelfSignedCertificate;
import runwar.undertow.WebXMLParser;

import javax.servlet.DispatcherType;
import javax.servlet.Filter;
import javax.servlet.Servlet;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.util.*;

import static io.undertow.Handlers.predicate;
import static io.undertow.servlet.Servlets.servlet;
import static runwar.logging.RunwarLogger.LOG;

class RunwarConfigurer {

    private static ServerOptions serverOptions;
    private final Server server;
    private final String serverMode;
    private String[] defaultWelcomeFiles = new String[] { "index.cfm", "index.cfml", "default.cfm", "index.html", "index.htm",
            "default.html", "default.htm" };

    private static ClassLoader getClassLoader(){
        return Server.getClassLoader();

    }

    RunwarConfigurer(final Server server){
        this.server = server;
        serverOptions = server.getServerOptions();
        if(getCFMLServletClass(serverOptions.cfEngineName()) != null) {
            serverMode = Server.Mode.SERVLET;
        } else {
            serverMode = serverOptions.serverMode();
        }
        LOG.debugf("Server Mode: %s",serverMode);
    }


    void configureServerResourceHandler(DeploymentInfo servletBuilder) {
        File warFile = serverOptions.warFile();
        File webInfDir = serverOptions.webInfDir();
        String cfengine = serverOptions.cfEngineName();
        String cfusionDir = new File(webInfDir,"cfusion").getAbsolutePath().replace('\\', '/');
        String cfformDir = new File(webInfDir,"cfform").getAbsolutePath().replace('\\', '/');

        final String cfClasspath = "%s/lib/updates,%s/lib/,%s/lib/axis2,%s/gateway/lib/,%s/../cfform/jars,%s/../flex/jars,%s/lib/oosdk/lib,%s/lib/oosdk/classes".replaceAll("%s", cfusionDir);
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
            if (System.getProperty("coldfusion.home") == null) {
                cfprops.forEach((k, v) -> {
                    System.setProperty(k, v);
                    LOG.tracef("Setting %s = '%s'", k, v);
                });
            }
            cfengine = "adobe";
        }

        if(serverMode.equals(Server.Mode.SERVLET)) {
            configureServerServlet(servletBuilder);
        }
        else if(webInfDir.exists() || serverMode.equals(Server.Mode.SERVLET)) {
            configureServerWar(servletBuilder);
        }
        else {
            if (getClassLoader() == null) {
                throw new RuntimeException("FATAL: Could not load any libs for war: " + warFile.getAbsolutePath());
            }
            servletBuilder.setClassLoader(getClassLoader());
            LOG.debug("Running default web server '" + warFile.getAbsolutePath()+ "'");
        }
        if(cfengine.equals("adobe")) {
            String cfCompilerOutput = (String) servletBuilder.getServletContextAttributes().get("coldfusion.compiler.outputDir");
            if(cfCompilerOutput == null || cfCompilerOutput.matches("^.?WEB-INF.*?")){
                // TODO: figure out why adobe needs the absolute path, vs. /WEB-INF/cfclasses
                File cfCompilerOutputDir = new File(webInfDir, "/cfclasses").getAbsoluteFile();
                try {
                    cfCompilerOutputDir = new File(webInfDir, "/cfclasses").getAbsoluteFile().getCanonicalFile();
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

    private void configureServerWar(DeploymentInfo servletBuilder) {
        File warFile = serverOptions.warFile();
        File webInfDir = serverOptions.webInfDir();
        Long transferMinSize= serverOptions.transferMinSize();
        LOG.debug("found WEB-INF: '" + webInfDir.getAbsolutePath() + "'");
        if (getClassLoader() == null) {
            throw new RuntimeException("FATAL: Could not load any libs for war: " + warFile.getAbsolutePath());
        }
        servletBuilder.setClassLoader(getClassLoader());
        Set<Path> contentDirs = new HashSet<>();
        Map<String,Path> aliases = new HashMap<>();
        serverOptions.contentDirectories().forEach(s -> contentDirs.add(Paths.get(s)));
        serverOptions.aliases().forEach((s, s2) -> aliases.put(s,Paths.get(s2)));
        servletBuilder.setResourceManager(server.getResourceManager(warFile, transferMinSize, contentDirs, aliases, webInfDir));
        WebXMLParser.parseWebXml(serverOptions.webXmlFile(), webInfDir, servletBuilder, serverOptions.ignoreWebXmlWelcomePages(), serverOptions.ignoreWebXmlRestMappings());
    }

    private void configureServerServlet(DeploymentInfo servletBuilder) {
        File warFile = serverOptions.warFile();
        String cfengine = serverOptions.cfEngineName();
        String cfmlServletConfigWebDir = serverOptions.cfmlServletConfigWebDir();
        String cfmlServletConfigServerDir = serverOptions.cfmlServletConfigServerDir();
        Long transferMinSize = serverOptions.transferMinSize();
        File webXmlFile = serverOptions.webXmlFile();

        if (getClassLoader() == null) {
            throw new RuntimeException("FATAL: Could not load any libs for war: " + warFile.getAbsolutePath());
        }
        if (cfmlServletConfigWebDir == null) {
            File webConfigDirFile = new File(Server.getThisJarLocation().getParentFile(), "engine/cfml/server/cfml-web/");
            cfmlServletConfigWebDir = webConfigDirFile.getPath() + "/" + serverOptions.serverName();
        }
        LOG.debug("cfml.web.config.dir: " + cfmlServletConfigWebDir);
        if (cfmlServletConfigServerDir == null || cfmlServletConfigServerDir.length() == 0) {
            File serverConfigDirFile = new File(Server.getThisJarLocation().getParentFile(), "engine/cfml/server/");
            cfmlServletConfigServerDir = serverConfigDirFile.getAbsolutePath();
        }
        LOG.debug("cfml.server.config.dir: " + cfmlServletConfigServerDir);
        File webInfDir;
        if (System.getProperty("cfml.webinf") == null) {
            webInfDir = new File(cfmlServletConfigWebDir, "WEB-INF/");
        } else {
            webInfDir = new File(System.getProperty("cfml.webinf"));
            LOG.debug("Found cfml.webinf system property: " + webInfDir.getPath());
        }
        LOG.debug("cfml.webinf: " + webInfDir.getPath());
        serverOptions.webInfDir(webInfDir);

        // servletBuilder.setResourceManager(new CFMLResourceManager(new
        // File(homeDir,"server/"), transferMinSize, contentDirs));
        File internalCFMLServerRoot = webInfDir;
        if(!internalCFMLServerRoot.mkdirs()){
            LOG.errorf("Unable to create cfml resource server root: %s", internalCFMLServerRoot.getAbsolutePath());
        }
        Set<Path> contentDirs = new HashSet<>();
        Map<String,Path> aliases = new HashMap<>();
        serverOptions.contentDirectories().forEach(s -> contentDirs.add(Paths.get(s)));
        serverOptions.aliases().forEach((s, s2) -> aliases.put(s,Paths.get(s2)));
        servletBuilder.setResourceManager(server.getResourceManager(warFile, transferMinSize, contentDirs, aliases, internalCFMLServerRoot));

        servletBuilder.setClassLoader(getClassLoader());
        if (webXmlFile != null) {
            LOG.debug("using specified web.xml : " + webXmlFile.getAbsolutePath());
            WebXMLParser.parseWebXml(webXmlFile, webInfDir, servletBuilder, serverOptions.ignoreWebXmlWelcomePages(), serverOptions.ignoreWebXmlRestMappings());
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
            if(serverOptions.servletRestEnable()) {
                LOG.debug("Adding REST servlet");
                ServletInfo restServlet = servlet("RESTServlet", restServletClass)
                        .setRequireWelcomeFileMapping(true)
                        .addInitParam(cfengine+"-web-directory",cfmlServletConfigWebDir)
                        .setLoadOnStartup(2);
                for(String path : serverOptions.servletRestMappings()) {
                    restServlet.addMapping(path);
                }
                servletBuilder.addServlet(restServlet);
            }
        }
    }


    @SuppressWarnings("unchecked")
    private void configureURLRewrite(DeploymentInfo servletBuilder, File webInfDir) throws ClassNotFoundException {
        if(serverOptions.urlRewriteEnable()) {
            LOG.debug("enabling URL rewriting");
            Class<Filter> rewriteFilter;
            String urlRewriteFile = "runwar/urlrewrite.xml";
            if(new File(webInfDir,"urlrewrite.xml").exists() && serverOptions.urlRewriteFile() == null){
                serverOptions.urlRewriteFile(new File(webInfDir,"urlrewrite.xml"));
            }
            try{
                rewriteFilter = (Class<Filter>) getClassLoader().loadClass("runwar.util.UrlRewriteFilter");
            } catch (java.lang.ClassNotFoundException e) {
                rewriteFilter = (Class<Filter>) Server.class.getClassLoader().loadClass("runwar.util.UrlRewriteFilter");
            }
            if(serverOptions.urlRewriteFile() != null) {
                if(!serverOptions.urlRewriteFile().isFile()) {
                    String message = "The URL rewrite file " + urlRewriteFile + " does not exist!";
                    LOG.error(message);
                    throw new RuntimeException(message);
                } else {
                    urlRewriteFile = serverOptions.urlRewriteFile().getAbsolutePath();
                }
            }

            String rewriteFormat = serverOptions.urlRewriteApacheFormat() ? "modRewrite-style" : "XML";
            LOG.debug(rewriteFormat + " rewrite config file: " + urlRewriteFile);
            FilterInfo rewriteFilterInfo = new FilterInfo("UrlRewriteFilter", rewriteFilter)
                    .addInitParam("confPath", urlRewriteFile)
                    .addInitParam("statusEnabled", Boolean.toString(serverOptions.debug()))
                    .addInitParam("modRewriteConf", Boolean.toString(serverOptions.urlRewriteApacheFormat()));
            if(serverOptions.urlRewriteCheckInterval() != null) {
                rewriteFilterInfo.addInitParam("confReloadCheckInterval", serverOptions.urlRewriteCheckInterval());
            }
            if(serverOptions.urlRewriteStatusPath() != null && serverOptions.urlRewriteStatusPath().length() != 0) {
                rewriteFilterInfo.addInitParam("statusPath", serverOptions.urlRewriteStatusPath());
            }
            rewriteFilterInfo.addInitParam("logLevel", "SLF4J");
            servletBuilder.addFilter(rewriteFilterInfo);
            servletBuilder.addFilterUrlMapping("UrlRewriteFilter", "/*", DispatcherType.REQUEST);
        } else {
            LOG.debug("URL rewriting is disabled");
        }
    }

    @SuppressWarnings({ "unchecked" })
    private void configurePathInfoFilter(DeploymentInfo servletBuilder) throws ClassNotFoundException {
        if(serverOptions.filterPathInfoEnable()) {
            LOG.debug("enabling path_info filter");
            Class<Filter> regexPathInfoFilter;
            try{
                regexPathInfoFilter = (Class<Filter>) getClassLoader().loadClass("org.cfmlprojects.regexpathinfofilter.RegexPathInfoFilter");
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
    private static Class<Servlet> getCFMLServletClass(String cfengine) {
        Class<Servlet> cfmlServlet = null;
        try {
            cfmlServlet = (Class<Servlet>) getClassLoader().loadClass(cfengine + ".loader.servlet.CFMLServlet");
            LOG.debug("dynamically loaded CFML servlet from runwar child classloader");
        } catch (java.lang.ClassNotFoundException devnul) {
            try {
                cfmlServlet = (Class<Servlet>) Server.class.getClassLoader().loadClass(cfengine + ".loader.servlet.CFMLServlet");
                LOG.debug("dynamically loaded CFML servlet from runwar classloader");
            } catch(java.lang.ClassNotFoundException e) {
                LOG.trace("No CFML servlet found in class loader hierarchy");
            }
        } catch (NullPointerException ne){
            LOG.trace("No CFML servlet found");
        }
        return cfmlServlet;
    }

    @SuppressWarnings("unchecked")
    private static Class<Servlet> getRestServletClass(String cfengine) {
        Class<Servlet> restServletClass = null;
        try {
            restServletClass = (Class<Servlet>) getClassLoader().loadClass(cfengine + ".loader.servlet.RestServlet");
        } catch (java.lang.ClassNotFoundException e) {
            try {
                restServletClass = (Class<Servlet>) Server.class.getClassLoader().loadClass(cfengine + ".loader.servlet.RestServlet");
            } catch (ClassNotFoundException e1) {
                e1.printStackTrace();
            }
        }
        return restServletClass;
    }


    static List<URL> getJarList(String libDirs) throws IOException {
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

    private void addCacheHandler(final DeploymentInfo servletBuilder) {
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

    void configureServlet(final DeploymentInfo servletBuilder) throws ClassNotFoundException {
        File webInfDir = serverOptions.webInfDir();
        configureURLRewrite(servletBuilder, webInfDir);
        configurePathInfoFilter(servletBuilder);

        if (serverOptions.cacheEnable()) {
            addCacheHandler(servletBuilder);
        } else {
            LOG.debug("File cache is disabled");
        }

        if (serverOptions.customHTTPStatusEnable()) {
            servletBuilder.setSendCustomReasonPhraseOnError(true);
        }

        if(serverOptions.errorPages() != null){
            for(Integer errorCode : serverOptions.errorPages().keySet()) {
                String location = serverOptions.errorPages().get(errorCode);
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
        servletBuilder.addServlet( new ServletInfo(io.undertow.servlet.handlers.ServletPathMatches.DEFAULT_SERVLET_NAME, DefaultServlet.class)
                .addInitParam("directory-listing", Boolean.toString(serverOptions.directoryListingEnable()))
        		.addInitParam("disallowed-extensions", "CFC,cfc,Cfc,CFc,cFc,cfC,CfC,cFC,CFM,cfm,Cfm,CFm,cFm,cfM,CfM,cFM,CFML,cfmL,CfmL,CFmL,cFmL,cfML,CfML,cFML,CFMl,cfml,Cfml,CFml,cFml,cfMl,CfMl,cFMl")
        		.addInitParam("allow-post", "true") );

        List<?> welcomePages =  servletBuilder.getWelcomePages();
        if(serverOptions.ignoreWebXmlWelcomePages()) {
            LOG.debug("Ignoring web.xml welcome file, so adding server options welcome files to deployment manager.");
            servletBuilder.addWelcomePages(serverOptions.welcomeFiles());
        } else if(welcomePages.size() == 0){
            LOG.debug("No welcome pages set yet, so adding defaults to deployment manager.");
            servletBuilder.addWelcomePages(defaultWelcomeFiles);
        }
        LOG.info("welcome pages in deployment manager: " + servletBuilder.getWelcomePages());

        if(serverOptions.ignoreWebXmlRestMappings() && serverOptions.servletRestEnable()) {
            LOG.info("Overriding web.xml rest mappings with " + Arrays.toString( serverOptions.servletRestMappings() ) );
            for (Map.Entry<String, ServletInfo> stringServletInfoEntry : servletBuilder.getServlets().entrySet()) {
                ServletInfo restServlet = stringServletInfoEntry.getValue();
//                LOG.trace("Checking servlet named: " + restServlet.getName() + " to see if it's a REST servlet.");
                if (restServlet.getName().toLowerCase().equals("restservlet") || restServlet.getName().toLowerCase().equals("cfrestservlet")) {
                    for (String path : serverOptions.servletRestMappings()) {
                        restServlet.addMapping(path);
                        LOG.info("Added rest mapping: " + path + " to " + restServlet.getName());
                    }
                }
            }
        } else if (!serverOptions.servletRestEnable()) {
            LOG.trace("REST servlets disabled");
        }

    }

    void generateSelfSignedCertificate() throws GeneralSecurityException, IOException {
        Path defaultCertPath, defaultKeyPath;
        if(serverOptions.sslSelfSign()){
            if(serverOptions.webInfDir() != null && serverOptions.webInfDir().exists()) {
                defaultCertPath = new File(serverOptions.webInfDir(),"selfsign.crt").toPath();
                defaultKeyPath = new File(serverOptions.webInfDir(),"selfsign.key").toPath();
            } else {
                defaultCertPath = Paths.get("./selfsign.crt");
                defaultKeyPath = Paths.get("./selfsign.key");
            }
            Path certPath = serverOptions.sslCertificate() == null ? defaultCertPath : Paths.get(serverOptions.sslCertificate().getAbsolutePath());
            Path keyPath = serverOptions.sslKey() == null ? defaultKeyPath : Paths.get(serverOptions.sslKey().getAbsolutePath());
            SelfSignedCertificate.generateCertificate(certPath, keyPath);
            serverOptions.sslKey(keyPath.toFile());
            serverOptions.sslCertificate(certPath.toFile());
            serverOptions.sslEnable(true);
            if(serverOptions.warFile() == null)
                System.exit(0);
        }
    }

    void generateService() {
        if(serverOptions.sslSelfSign()){
        }
    }

}
