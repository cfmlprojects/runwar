package runwar.undertow;

import static org.joox.JOOX.$;
import static runwar.logging.RunwarLogger.LOG;

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

import org.joox.Match;
import org.w3c.dom.Document;

import io.undertow.server.session.SessionCookieConfig;
import io.undertow.servlet.api.DeploymentInfo;
import io.undertow.servlet.api.ErrorPage;
import io.undertow.servlet.api.FilterInfo;
import io.undertow.servlet.api.ListenerInfo;
import io.undertow.servlet.api.MimeMapping;
import io.undertow.servlet.api.ServletInfo;

public class WebXMLParser {

    private static Pattern SPECIAL_REGEX_CHARS = Pattern.compile("[{}()\\[\\].+*?^$\\\\|]");
	/**
	 * Parses the web.xml and configures the context.
	 *
	 * @param webxml
	 * @param info
	 */
	@SuppressWarnings("unchecked")
	public static void parseWebXml(File webxml, File webinf, DeploymentInfo info, SessionCookieConfig sessionConfig, boolean ignoreWelcomePages, boolean ignoreRestMappings) {
    if (!webxml.exists() || !webxml.canRead()) {
	        LOG.error("Error reading web.xml! exists:"+webxml.exists()+"readable:"+webxml.canRead());
	    }
	    Map<String, ServletInfo> servletMap = new HashMap<String, ServletInfo>();
	    Map<String, FilterInfo> filterMap = new HashMap<String, FilterInfo>();
		try {
			final String webinfPath;
			if (File.separatorChar=='\\') {
				webinfPath  = webinf.getCanonicalPath().replace("\\", "\\\\");
			} else {
			    webinfPath = webinf.getCanonicalPath();
			}
			trace("parsing %s",webxml.getCanonicalPath());
			DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();

			// disable validation, so we don't incur network calls
			docBuilderFactory.setValidating(false);
			docBuilderFactory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
			DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();

			// parse and normalize text representation
			Document doc = docBuilder.parse(webxml);
			doc.getDocumentElement().normalize();

			trace("Root element of the doc is %s", doc.getDocumentElement().getNodeName());

			String displayName = $(doc).find("context-param").text();
			if(displayName != null) {
			    info.setDisplayName(displayName);
			}

			$(doc).find("context-param").each(ctx -> {
                String pName = $(ctx).find("param-name").text();
                String pValue = $(ctx).find("param-value").text();
                info.addServletContextAttribute(pName, pValue);
                info.addInitParameter(pName, pValue);
                LOG.tracef("context param: %s = %s", pName, pValue);
            });
			trace("Total no of context-params: %s", info.getServletContextAttributes().size());

            Match listeners = $(doc).find("listener");
            trace("Total no of listeners: %s", listeners.size());
            listeners.each(ctx -> {
                String pName = $(ctx).find("listener-class").text();
                LOG.tracef("Listener: %s", pName);
                ListenerInfo listener;
                try {
                    listener = new ListenerInfo( (Class<? extends EventListener>) info.getClassLoader().loadClass(pName));
                    info.addListener(listener);
                } catch (ClassNotFoundException e) {
                    LOG.error(e);
                }
            });

            // do filters
            Match filters = $(doc).find("filter");
            trace("Total no of filters: %s", filters.size());
            filters.each(ctx -> {
                String filterName = $(ctx).find("filter-name").text();
                String className = $(ctx).find("filter-class").text();
                LOG.tracef("filter-name: %s, filter-class: %s", filterName, className);
                try {
                    FilterInfo filter = new FilterInfo(filterName, (Class<? extends Filter>) info.getClassLoader()
                            .loadClass(className));
                    Match initParams = $(ctx).find("init-param");
                    LOG.debugf("Total no of %s init-params: %s", filterName, initParams.size());
                    initParams.each(cctx -> {
                        String pName = $(cctx).find("param-name").text();
                        String pValue = $(cctx).find("param-value").text();
                        filter.addInitParam(pName, pValue);
                        LOG.tracef("%s init-param: param-name: %s  param-value: %s", filterName, pName, pValue);
                    });
                    if($(ctx).find("async-supported").size() > 0) {
                        trace("Async supported: %s", $(ctx).find("async-supported").text());
                        filter.setAsyncSupported(Boolean.valueOf($(ctx).find("async-supported").text()));
                    }
                    filterMap.put(filterName, filter);
                } catch (ClassNotFoundException e) {
                    LOG.error(e);
                }
            });
            info.addFilters(filterMap.values());

            Match filterMappings = $(doc).find("filter-mapping");
            trace("Total no of filters-mappings: %s", filterMappings.size());
            filterMappings.each(ctx -> {
                String filterName = $(ctx).find("filter-name").text();
                String className = $(ctx).find("filter-class").text();
                LOG.tracef("filter-name: %s, filter-class: %s", filterName, className);
                FilterInfo filter = filterMap.get(filterName);
                if(filter == null) {
                    LOG.errorf("No filter found for filter-mapping: %s",filterName);
                } else {
                    String urlPattern = $(ctx).find("url-pattern").text();
                    Match dispatchers = $(ctx).find("dispatcher");
                    if(dispatchers == null) {
                        LOG.debugf("filter-name: %s url-pattern: %s dispatcher: REQUEST", filterName,urlPattern);
                        info.addFilterUrlMapping( filterName, urlPattern, DispatcherType.valueOf( "REQUEST") );
                    } else {
                        dispatchers.each(dCtx ->{
                            String dispatcher = $(dCtx).text();
                            LOG.debugf("filter-name: %s url-pattern: %s dispatcher: %s", filterName,urlPattern, dispatcher);
                            info.addFilterUrlMapping( filterName, $(dCtx).text(), DispatcherType.valueOf( dispatcher ) );
                        });
                    }
                    String servletName = $(ctx).find("servlet-name").text();
                    if(servletName != null) {
                        LOG.debugf("Adding servlet mapping: %s", servletName);
                        info.addFilterServletNameMapping(filterName, servletName, DispatcherType.valueOf("REQUEST"));
                    }
                }
            });


            Match servlets = $(doc).find("servlet");
            trace("Total no of servlets: %s", servlets.size());
            servlets.each(ctx -> {
                String servletName = $(ctx).find("servlet-name").text();
                String servletClassName = $(ctx).find("servlet-class").text();
                String loadOnStartup = $(ctx).find("load-on-startup").text();
                LOG.tracef("servlet-name: %s, servlet-class: %s", servletName, servletClassName);
                LOG.tracef("Adding servlet to undertow: ************* %s: %s *************", servletName, servletClassName);
                Class<?> servletClass;
                try{
                    servletClass = info.getClassLoader().loadClass(servletClassName);
                } catch (Exception e) {
                    String msg = "Could not load servlet class: " + servletClassName;
                    LOG.error(msg);
                    throw new RuntimeException(msg);
                }
                ServletInfo servlet = new ServletInfo(servletName, (Class<? extends Servlet>) servletClass);
                servlet.setRequireWelcomeFileMapping(true);
                if(loadOnStartup != null) {
                    trace("Load on startup: %s", loadOnStartup);
                    servlet.setLoadOnStartup(Integer.valueOf(loadOnStartup));
                }
                Match initParams = $(ctx).find("init-param");
                LOG.debugf("Total no of %s init-params: %s", servletName, initParams.size());
                initParams.each(cctx -> {
                    String pName = $(cctx).find("param-name").text();
                    String pValue = $(cctx).find("param-value").text();
                    pValue = pValue.replaceAll(".?/WEB-INF", SPECIAL_REGEX_CHARS.matcher(webinfPath).replaceAll("\\\\$0"));
                    LOG.tracef("%s init-param: param-name: %s  param-value: %s", servletName, pName, pValue);
                    servlet.addInitParam(pName, pValue);
                });
                servletMap.put(servlet.getName(), servlet);
            });


            Match servletMappings = $(doc).find("servlet-mapping");
            trace("Total no of servlet-mappings: %s", servletMappings.size());
            servletMappings.each(ctx -> {
                String servletName = $(ctx).find("servlet-name").text();
                ServletInfo servlet = servletMap.get(servletName);
                if(servlet == null) {
                    LOG.errorf("No servlet found for servlet-mapping: %s",servletName);
                } else {
                    Match urlPatterns = $(ctx).find("url-pattern");
                    urlPatterns.each( urlPatternElement -> {
                        String urlPattern = $(urlPatternElement).text();
                        if(ignoreRestMappings && (servletName.toLowerCase().equals("restservlet") || servletName.toLowerCase().equals("cfrestservlet")) ) {
                            LOG.tracef("Skipping mapping servlet-name:%s, url-partern: %s", servletName, urlPattern);
                        } else {
                            LOG.tracef("mapping servlet-name:%s, url-pattern: %s", servletName, urlPattern);
                            servlet.addMapping(urlPattern);
                        }
                    });
                }
            });

            // add servlets to deploy info
            info.addServlets(servletMap.values());

            // do welcome files
            if (ignoreWelcomePages) {
                LOG.info("Ignoring any welcome pages in web.xml");
            } else {
                Match welcomeFileList = $(doc).find("welcome-file-list");
                trace("Total no of welcome files: %s", welcomeFileList.find("welcome-file").size());
                welcomeFileList.find("welcome-file").each(welcomeFileElement -> {
                    String welcomeFile = $(welcomeFileElement).text();
                    LOG.debugf("welcome-file: %s", welcomeFile);
                    info.addWelcomePage(welcomeFile);
                });
            }
            

            Match mimeMappings = $(doc).find("mime-mapping");
            trace("Total no of mime-mappings: %s", mimeMappings.size());
            mimeMappings.each(ctx -> {
                String extension = $(ctx).find("extension").text();
                String mimeType = $(ctx).find("mime-type").text();
                LOG.tracef("filter-name: %s, filter-class: %s", extension, mimeType);
                info.addMimeMapping(new MimeMapping(extension,mimeType));
            });

            Match errorPages = $(doc).find("error-page");
            trace("Total no of error-pages: %s", errorPages.size());
            errorPages.each(ctx -> {
                String location = $(ctx).find("location").text();
                String errorCode = $(ctx).find("error-code").text();
                String exceptionType = $(ctx).find("exception-type").text();
                if(errorCode != null && exceptionType != null) {
                    LOG.errorf("Cannot specify both error-code and exception-type, using exception-type: %s", exceptionType);
                    errorCode = null;
                }
                if(errorCode == null && exceptionType == null) {
                    LOG.tracef("default error-page location: %s", location);
                    info.addErrorPage( new ErrorPage(location));                        
                } else if(errorCode != null) {
                    LOG.tracef("error-code: %s - location: %s", location, errorCode);
                    info.addErrorPage( new ErrorPage(location, Integer.parseInt(errorCode)));
                } else {
                    LOG.tracef("exception-type: %s - location: %s", location, errorCode);
                    try {
                        info.addErrorPage( new ErrorPage(location, (Class<? extends Throwable>) info.getClassLoader().loadClass(exceptionType)));
                    } catch (ClassNotFoundException e) {
                        LOG.error(e);
                    }
                }
            });

            Match sessionConfigElement = $(doc).find("session-config");
            trace("Total no of cookie config elements: %s", sessionConfigElement.find("cookie-config").size());
            sessionConfigElement.find("cookie-config").each(welcomeFileElement -> {
                String httpOnly = $(welcomeFileElement).find("http-only").text();
                String secure = $(welcomeFileElement).find("secure").text();
                sessionConfig.setHttpOnly(Boolean.valueOf(httpOnly));
                sessionConfig.setSecure(Boolean.valueOf(secure));
                LOG.debugf("http-only: %s", Boolean.valueOf(httpOnly).toString());
                LOG.debugf("secure: %s", Boolean.valueOf(secure).toString());
            });
		} catch (Exception e) {
			LOG.error("Error reading web.xml", e);
			throw new RuntimeException(e);
		}
	}

	private static void trace(String string, Object elements) {
		LOG.tracef(string,elements);
//		System.out.printf(string,elements);
//		System.out.println();
	}
	
}
