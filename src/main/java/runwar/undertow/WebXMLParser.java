package runwar.undertow;

import static org.joox.JOOX.$;
import static runwar.logging.RunwarLogger.CONF_LOG;

import java.io.File;
import java.util.EventListener;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import javax.servlet.DispatcherType;
import javax.servlet.Filter;
import javax.servlet.Servlet;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import io.undertow.servlet.api.*;
import org.joox.Context;
import org.joox.Match;
import org.w3c.dom.Document;

public class WebXMLParser {

    private static Pattern SPECIAL_REGEX_CHARS = Pattern.compile("[{}()\\[\\].+*?^$\\\\|]");

    /**
     * Parses the web.xml and configures the context.
     * @param webxml web.xml file
     * @param info DeploymentInfo
     * @param ignoreRestMappings rest mappings
     * @param ignoreWelcomePages ignore welcome pages or not
     */
    @SuppressWarnings("unchecked")
    public static void parseWebXml(File webxml, DeploymentInfo info,
            boolean ignoreWelcomePages, boolean ignoreRestMappings, boolean overrideEnabled) {
        CONF_LOG.infof("Parsing '%s'", webxml.getPath());
        CONF_LOG.debugf("Overriding previous web.xml '%s'", overrideEnabled?"True":"False");

        if (!webxml.exists() || !webxml.canRead()) {
            CONF_LOG.error("Error reading web.xml! exists:" + webxml.exists() + "readable:" + webxml.canRead());
        }
        try {
            DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();

            // disable validation, so we don't incur network calls
            docBuilderFactory.setValidating(false);
            docBuilderFactory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();

            // parse and normalize text representation
            Document doc = docBuilder.parse(webxml);
            doc.getDocumentElement().normalize();

            trace("Root element of the doc is %s", doc.getDocumentElement().getNodeName());

            String displayName = $(doc).find("display-name").text();
            if (displayName != null) {
                info.setDisplayName(displayName);
            }

            $(doc).find("context-param").each(ctx -> {
                String pName = getRequired(ctx,"param-name");
                String pValue = getRequired(ctx,"param-value");
                info.addServletContextAttribute(pName, pValue);
                info.addInitParameter(pName, pValue);
                CONF_LOG.tracef("context param: '%s' = '%s'", pName, pValue);
            });
            trace("Total No. of context-params: %s", info.getServletContextAttributes().size());

            //do listeners
            Match listeners = $(doc).find("listener");
            int sizeListeners = listeners.size();
            trace("Total No. of listeners: %s", sizeListeners);
            if ( overrideEnabled && sizeListeners > 0 ) {
                trace("Removing %s listener(s) from deployment due to web.xml override", info.getListeners().size());
                info.getListeners().clear();
            }              
            listeners.each(ctx -> {
                String pName = getRequired(ctx,"listener-class");
                CONF_LOG.tracef("Listener: %s", pName);
                ListenerInfo listener;
                try {
                    listener = new ListenerInfo(
                            (Class<? extends EventListener>) info.getClassLoader().loadClass(pName));
                    info.addListener(listener);
                } catch (ClassNotFoundException e) {
                    CONF_LOG.error(e);
                }
            });

            //do servlets
            Match servlets = $(doc).find("servlet");
            int sizeServlets = servlets.size();
            trace("Total No. of servlets: %s", sizeServlets);  
            if ( overrideEnabled && sizeServlets > 0 ) {
                trace("Removing %s servlet(s) from deployment due to web.xml override", info.getServlets().size());
                info.getServlets().clear();
            }           
            servlets.each(servletElement -> {
                String servletName = getRequired(servletElement, "servlet-name");
                String servletClassName = getRequired(servletElement, "servlet-class");
                String loadOnStartup = get(servletElement, "load-on-startup");
                CONF_LOG.tracef("servlet-name: %s, servlet-class: %s", servletName, servletClassName);
                CONF_LOG.tracef("Adding servlet: ***** %s: %s *****", servletName,
                        servletClassName);
                Class<?> servletClass;
                try {
                    servletClass = info.getClassLoader().loadClass(servletClassName);
                } catch (Exception e) {
                    String msg = "Could not load servlet class: " + servletClassName;
                    CONF_LOG.error(msg);
                    throw new RuntimeException(msg);
                }
                ServletInfo servlet = new ServletInfo(servletName, (Class<? extends Servlet>) servletClass);
                servlet.setRequireWelcomeFileMapping(true);
                if (loadOnStartup != null) {
                    trace("Load on startup: %s", loadOnStartup);
                    servlet.setLoadOnStartup(Integer.valueOf(loadOnStartup));
                }
                Match initParams = $(servletElement).find("init-param");
                CONF_LOG.debugf("Total No. of %s init-params: %s", servletName, initParams.size());
                initParams.each(initParamElement -> {
                    String pName = getRequired(initParamElement, "param-name");
                    String pValue = getRequired(initParamElement, "param-value");
                    CONF_LOG.tracef("%s init-param: param-name: '%s'  param-value: '%s'", servletName, pName, pValue);
                    servlet.addInitParam(pName, pValue);
                });
                info.addServlet(servlet);
            });

            Match servletMappings = $(doc).find("servlet-mapping");
            trace("Total No. of servlet-mappings: %s", servletMappings.size());        
            servletMappings.each(mappingElement -> {
                String servletName = getRequired(mappingElement, "servlet-name");
                ServletInfo servlet = info.getServlets().get(servletName);
                if (servlet == null) {
                    CONF_LOG.errorf("No servlet found for servlet-mapping: %s", servletName);
                } else {
                    Match urlPatterns = $(mappingElement).find("url-pattern");
                    urlPatterns.each(urlPatternElement -> {
                        String urlPattern = $(urlPatternElement).text();
                        if (ignoreRestMappings && (servletName.toLowerCase().equals("restservlet")
                                || servletName.toLowerCase().equals("cfrestservlet"))) {
                            CONF_LOG.tracef("Skipping mapping servlet-name: %s, url-partern: %s", servletName,
                                    urlPattern);
                        } else {
                            CONF_LOG.tracef("mapping servlet-name: %s, url-pattern: %s", servletName, urlPattern);
                            servlet.addMapping(urlPattern);
                        }
                    });
                }
            });

            // do filters
            Match filters = $(doc).find("filter");
            int sizeFilters = filters.size();
            trace("Total No. of filters: %s", sizeFilters);
            if ( overrideEnabled && sizeFilters > 0 ) {
                trace("Removing %s filter(s) from deployment due to web.xml override", info.getFilters().size());
                info.getFilters().clear();
            }
            filters.each(ctx -> {
                String filterName = $(ctx).find("filter-name").text();
                String className = $(ctx).find("filter-class").text();
                CONF_LOG.tracef("filter-name: %s, filter-class: %s", filterName, className);
                try {
                    FilterInfo filter = new FilterInfo(filterName,
                            (Class<? extends Filter>) info.getClassLoader().loadClass(className));
                    Match initParams = $(ctx).find("init-param");
                    CONF_LOG.debugf("Total No. of %s init-params: %s", filterName, initParams.size());
                    initParams.each(cctx -> {
                        String pName = $(cctx).find("param-name").text();
                        String pValue = $(cctx).find("param-value").text();
                        filter.addInitParam(pName, pValue);
                        CONF_LOG.tracef("%s init-param: param-name: '%s'  param-value: '%s'", filterName, pName, pValue);
                    });
                    if ($(ctx).find("async-supported").size() > 0) {
                        trace("Async supported: %s", $(ctx).find("async-supported").text());
                        filter.setAsyncSupported(Boolean.valueOf($(ctx).find("async-supported").text()));
                    }
                    info.addFilter(filter);
                } catch (ClassNotFoundException e) {
                    CONF_LOG.error(e);
                }
            });

            // do filter mappings
            Match filterMappings = $(doc).find("filter-mapping");
            int sizeFilterMappings = filterMappings.size();
            trace("Total No. of filters-mappings: %s", sizeFilterMappings);
            //TODO: waiting for UNDERTOW-1930: https://github.com/undertow-io/undertow/pull/1212
            /* if ( overrideEnabled && sizeFilterMappings > 0 ) {
                trace("Removing %s filter mapping(s) from deployment due to web.xml override", info.getFilterMappings().size());
                info.getFilterMappings().clear();
            } */
            filterMappings.each(ctx -> {
                String filterName = $(ctx).find("filter-name").text();
                String urlPattern = $(ctx).find("url-pattern").text();
                if( urlPattern != null ) {
                    Match dispatchers = $(ctx).find("dispatcher");
                    if (dispatchers == null) {
                        CONF_LOG.debugf("filter-name: %s url-pattern: %s dispatcher: REQUEST", filterName, urlPattern);
                        info.addFilterUrlMapping(filterName, urlPattern, DispatcherType.valueOf("REQUEST"));
                    } else {
                        dispatchers.each(dCtx -> {
                            String dispatcher = $(dCtx).text();
                            CONF_LOG.debugf("filter-name: %s url-pattern: %s dispatcher: %s", filterName, urlPattern,
                                    dispatcher);
                            info.addFilterUrlMapping(filterName, urlPattern, DispatcherType.valueOf(dispatcher));
                        });
                    }	
                }
                String servletName = $(ctx).find("servlet-name").text();
                if (servletName != null) {
                    CONF_LOG.debugf("Adding servlet mapping: %s", servletName);
                    info.addFilterServletNameMapping(filterName, servletName, DispatcherType.valueOf("REQUEST"));
                }
            });

            // do welcome files
            if (ignoreWelcomePages) {
                CONF_LOG.info("Ignoring any welcome pages in web.xml");
            } else {
                Match welcomeFileList = $(doc).find("welcome-file-list");
                int sizeWelcomeFiles = welcomeFileList.find("welcome-file").size();
                trace("Total No. of welcome files: %s", sizeWelcomeFiles);
                if ( overrideEnabled && sizeWelcomeFiles > 0 ) {
                    trace("Removing %s welcome page(s) from deployment due to web.xml override", info.getWelcomePages().size());
                    info.getWelcomePages().clear();
                }
                welcomeFileList.find("welcome-file").each(welcomeFileElement -> {
                    String welcomeFile = $(welcomeFileElement).text();
                    CONF_LOG.debugf("welcome-file: %s", welcomeFile);
                    info.addWelcomePage(welcomeFile);
                });
            }

            // do mime mappings
            Match mimeMappings = $(doc).find("mime-mapping");
            int sizeMimeMapping = mimeMappings.size();
            trace("Total No. of mime-mappings: %s", sizeMimeMapping);            
            if ( overrideEnabled && sizeMimeMapping > 0 ) {
                trace("Removing %s mime mapping(s) from deployment due to web.xml override", info.getMimeMappings().size());
                info.getMimeMappings().clear();
            }            
            mimeMappings.each(ctx -> {
                String extension = $(ctx).find("extension").text();
                String mimeType = $(ctx).find("mime-type").text();
                CONF_LOG.tracef("filter-name: %s, filter-class: %s", extension, mimeType);
                info.addMimeMapping(new MimeMapping(extension, mimeType));
            });

            // do error pages
            Match errorPages = $(doc).find("error-page");
            int sizeErrorPages = errorPages.size();
            trace("Total No. of error-pages: %s", sizeErrorPages);
            if ( overrideEnabled && sizeErrorPages > 0 ) {
                trace("Removing %s error page(s) from deployment due to web.xml override", info.getErrorPages().size());
                info.getErrorPages().clear();
            }              
            errorPages.each(ctx -> {
                String location = $(ctx).find("location").text();
                String errorCode = $(ctx).find("error-code").text();
                String exceptionType = $(ctx).find("exception-type").text();
                if (errorCode != null && exceptionType != null) {
                    CONF_LOG.errorf("Cannot specify both error-code and exception-type, using exception-type: %s",
                            exceptionType);
                    errorCode = null;
                }
                if (errorCode == null && exceptionType == null) {
                    CONF_LOG.tracef("default error-page location: %s", location);
                    info.addErrorPage(new ErrorPage(location));
                } else if (errorCode != null) {
                    CONF_LOG.tracef("error-code: %s - location: %s", location, errorCode);
                    info.addErrorPage(new ErrorPage(location, Integer.parseInt(errorCode)));
                } else {
                    CONF_LOG.tracef("exception-type: %s - location: %s", location, exceptionType);
                    try {
                        info.addErrorPage(new ErrorPage(location,
                                (Class<? extends Throwable>) info.getClassLoader().loadClass(exceptionType)));
                    } catch (ClassNotFoundException e) {
                        CONF_LOG.error(e);
                    }
                }
            });

            // do session configuration
            Match sessionConfigElement = $(doc).find("session-config");
            trace("Total No. of cookie config elements: %s", sessionConfigElement.find("cookie-config").size());
            sessionConfigElement.find("cookie-config").each(welcomeFileElement -> {
                String httpOnly = $(welcomeFileElement).find("http-only").text();
                String secure = $(welcomeFileElement).find("secure").text();
                info.getServletSessionConfig().setHttpOnly(Boolean.valueOf(httpOnly));
                info.getServletSessionConfig().setSecure(Boolean.valueOf(secure));
                CONF_LOG.debugf("http-only: %s", Boolean.valueOf(httpOnly).toString());
                CONF_LOG.debugf("secure: %s", Boolean.valueOf(secure).toString());
            });

        } catch (Exception e) {
            CONF_LOG.error("Error reading web.xml", e);
            throw new RuntimeException(e);
        }
    }

    private static String getRequired(Context ctx, String param) {
        final String result = $(ctx).find(param).text();
        if(result == null) {
            String msg = "Missing required parameter: '" + param + "'";
            CONF_LOG.error(msg);
            throw new RuntimeException(msg);
        }
        return result.trim();
    }

    private static String get(Context ctx, String param) {
        final String result = $(ctx).find(param).text();
        if(result == null) {
            return null;
        }
        return result.trim();
    }

    private static void trace(String string, Object elements) {
        CONF_LOG.tracef(string, elements);
    }
    
}
