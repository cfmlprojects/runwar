package runwar;

import java.io.File;
import java.util.HashMap;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;

import io.undertow.servlet.api.DeploymentInfo;
import io.undertow.servlet.api.FilterInfo;
import io.undertow.servlet.api.ListenerInfo;
import io.undertow.servlet.api.ServletInfo;

import java.util.EventListener;
import java.util.Map;

import org.jboss.logging.Logger;

import javax.servlet.Filter;
import javax.servlet.Servlet;
import javax.servlet.DispatcherType;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class UndertowWebXMLParser {

	private static Logger log = Logger.getLogger("io.undertow.UndertowLogger");

	/**
	 * Parses the web.xml and configures the context.
	 *
	 * @param webxml
	 * @param info
	 */
	@SuppressWarnings("unchecked")
	public static void parseWebXml(File webxml, DeploymentInfo info) {
		if (!webxml.exists() || !webxml.canRead()) {
			log.error("Error reading web.xml! exists:"+webxml.exists()+"readable:"+webxml.canRead());
		}
		try {
			DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
			Document doc = docBuilder.parse(webxml);
			// normalize text representation
			doc.getDocumentElement().normalize();

			trace("Root element of the doc is %s", doc.getDocumentElement().getNodeName());
			// to hold our servlets
			Map<String, ServletInfo> servletMap = new HashMap<String, ServletInfo>();
			// to hold our filters
			Map<String, FilterInfo> filterMap = new HashMap<String, FilterInfo>();
			// do context-param - available to the entire scope of the web
			// application
			NodeList listOfElements = doc.getElementsByTagName("context-param");
			int totalElements = listOfElements.getLength();

			trace("Total no of context-params: %s", totalElements);
			for (int s = 0; s < totalElements; s++) {
				Node fstNode = listOfElements.item(s);
				if (fstNode.getNodeType() == Node.ELEMENT_NODE) {
					Element fstElmnt = (Element) fstNode;
					NodeList fstNmElmntLst = fstElmnt.getElementsByTagName("param-name");
					Element fstNmElmnt = (Element) fstNmElmntLst.item(0);
					NodeList fstNm = fstNmElmnt.getChildNodes();
					String pName = (fstNm.item(0)).getNodeValue().trim();
					trace("context param name: %s", pName);
					NodeList lstNmElmntLst = fstElmnt.getElementsByTagName("param-value");
					Element lstNmElmnt = (Element) lstNmElmntLst.item(0);
					NodeList lstNm = lstNmElmnt.getChildNodes();
					String pValue = (lstNm.item(0)).getNodeValue().trim();
					trace("context param value: %s", pValue);
					info.addServletContextAttribute(pName, pValue);
					info.addInitParameter(pName, pValue);
				}
			}
			// do listener
			listOfElements = doc.getElementsByTagName("listener");
			totalElements = listOfElements.getLength();
			trace("Total no of listeners: %s", totalElements);
			for (int s = 0; s < totalElements; s++) {
				Node fstNode = listOfElements.item(s);
				if (fstNode.getNodeType() == Node.ELEMENT_NODE) {
					Element fstElmnt = (Element) fstNode;
					NodeList fstNmElmntLst = fstElmnt.getElementsByTagName("listener-class");
					Element fstNmElmnt = (Element) fstNmElmntLst.item(0);
					NodeList fstNm = fstNmElmnt.getChildNodes();
					String pName = (fstNm.item(0)).getNodeValue().trim();
					trace("Param name: %s", pName);
					ListenerInfo listener = new ListenerInfo((Class<? extends EventListener>) info.getClassLoader()
							.loadClass(pName));
					info.addListener(listener);
				}
			}
			// do filter
			listOfElements = doc.getElementsByTagName("filter");
			totalElements = listOfElements.getLength();
			trace("Total no of filters: %s", totalElements);
			for (int s = 0; s < totalElements; s++) {
				Node fstNode = listOfElements.item(s);
				if (fstNode.getNodeType() == Node.ELEMENT_NODE) {
					Element fstElmnt = (Element) fstNode;
					NodeList fstNmElmntLst = fstElmnt.getElementsByTagName("filter-name");
					Element fstNmElmnt = (Element) fstNmElmntLst.item(0);
					NodeList fstNm = fstNmElmnt.getChildNodes();
					String pName = (fstNm.item(0)).getNodeValue().trim();
					trace("Filter name: %s", pName);
					NodeList lstNmElmntLst = fstElmnt.getElementsByTagName("filter-class");
					Element lstNmElmnt = (Element) lstNmElmntLst.item(0);
					NodeList lstNm = lstNmElmnt.getChildNodes();
					String pValue = (lstNm.item(0)).getNodeValue().trim();
					trace("Filter class: %s", pValue);
					// create the filter
					FilterInfo filter = new FilterInfo(pName, (Class<? extends Filter>) info.getClassLoader()
							.loadClass(pValue));
					// do init-param - available in the context of a servlet
					// or filter in the web application
					NodeList listOfInitParams = fstElmnt.getElementsByTagName("init-param");
					int totalInitParams = listOfInitParams.getLength();
					trace("Total no of init-params: %s", totalInitParams);
					for (int i = 0; i < totalInitParams; i++) {
						Node inNode = listOfInitParams.item(i);
						if (inNode.getNodeType() == Node.ELEMENT_NODE) {
							Element inElmnt = (Element) inNode;
							NodeList inNmElmntLst = inElmnt.getElementsByTagName("param-name");
							Element inNmElmnt = (Element) inNmElmntLst.item(0);
							NodeList inNm = inNmElmnt.getChildNodes();
							String inName = (inNm.item(0)).getNodeValue().trim();
							trace("Param name: %s", inName);
							NodeList inValElmntLst = inElmnt.getElementsByTagName("param-value");
							Element inValElmnt = (Element) inValElmntLst.item(0);
							NodeList inVal = inValElmnt.getChildNodes();
							String inValue = (inVal.item(0)).getNodeValue().trim();
							trace("Param value: %s", inValue);
							// add the param
							filter.addInitParam(inName, inValue);
						}
					}
					// do async-supported
					NodeList ldElmntLst = fstElmnt.getElementsByTagName("async-supported");
					if (ldElmntLst != null && ldElmntLst.getLength()>0) {
						Element ldElmnt = (Element) ldElmntLst.item(0);
						NodeList ldNm = ldElmnt.getChildNodes();
						String pAsync = (ldNm.item(0)).getNodeValue().trim();
						trace("Async supported: %s", pAsync);
						filter.setAsyncSupported(Boolean.valueOf(pAsync));
					}
					// add to map
					filterMap.put(pName, filter);
				}
				// add filters
				info.addFilters(filterMap.values());
			}
			// do filter mappings
			if (!filterMap.isEmpty()) {
				listOfElements = doc.getElementsByTagName("filter-mapping");
				totalElements = listOfElements.getLength();
				trace("Total no of filter-mappings: %s", totalElements);
				for (int s = 0; s < totalElements; s++) {
					Node fstNode = listOfElements.item(s); 
					if (fstNode.getNodeType() == Node.ELEMENT_NODE) {
						Element fstElmnt = (Element) fstNode;
						NodeList fstNmElmntLst = fstElmnt.getElementsByTagName("filter-name");
						Element fstNmElmnt = (Element) fstNmElmntLst.item(0);
						NodeList fstNm = fstNmElmnt.getChildNodes();
						String pName = (fstNm.item(0)).getNodeValue().trim();
						trace("Param name: %s", pName);
						// lookup the filter info
						FilterInfo filter = filterMap.get(pName);
						// add the mapping
						if (filter != null) {
							NodeList lstNmElmntLst = fstElmnt.getElementsByTagName("url-pattern");
							if(lstNmElmntLst != null &&  lstNmElmntLst.item(0) != null) {
								Element lstNmElmnt = (Element) lstNmElmntLst.item(0);
								NodeList lstNm = lstNmElmnt.getChildNodes();
								String pValue = (lstNm.item(0)).getNodeValue().trim();
								NodeList dstNmElmntLst = fstElmnt.getElementsByTagName("dispatcher");
								
								if ( dstNmElmntLst == null || dstNmElmntLst.getLength() == 0 ){
									info.addFilterUrlMapping( pName, pValue, DispatcherType.valueOf( "REQUEST") );
								} else {
									int totalDispatchers = dstNmElmntLst.getLength();
									for(int i = 0; i < totalDispatchers; i++){
										Element dstNmElmnt = (Element) dstNmElmntLst.item(i);
										NodeList dstNm = dstNmElmnt.getChildNodes();
										String dValue = (dstNm.item(0)).getNodeValue().trim();
										info.addFilterUrlMapping( pName, pValue, DispatcherType.valueOf( dValue ) );
									}
								}							
							}
						} else {
							log.warnf("No servlet found for %s", pName);
						}
					}
				}
			}
			// do servlet
			NodeList listOfServlets = doc.getElementsByTagName("servlet");
			totalElements = listOfServlets.getLength();
			trace("Total no of servlets: %s", totalElements);
			for (int s = 0; s < listOfServlets.getLength(); s++) {
				Node fstNode = listOfServlets.item(s);
				if (fstNode.getNodeType() == Node.ELEMENT_NODE) {
					Element fstElmnt = (Element) fstNode;
					NodeList fstNmElmntLst = fstElmnt.getElementsByTagName("servlet-name");
					Element fstNmElmnt = (Element) fstNmElmntLst.item(0);
					NodeList fstNm = fstNmElmnt.getChildNodes();
					String pName = (fstNm.item(0)).getNodeValue().trim();
					trace("Adding servlet to undertow: ************* %s *************", pName);
					trace("Param name: %s", pName);
					NodeList lstNmElmntLst = fstElmnt.getElementsByTagName("servlet-class");
					Element lstNmElmnt = (Element) lstNmElmntLst.item(0);
					NodeList lstNm = lstNmElmnt.getChildNodes();
					String pValue = (lstNm.item(0)).getNodeValue().trim();
					trace("Param value: %s", pValue);
					// create the servlet
					ServletInfo servlet = new ServletInfo(pName, (Class<? extends Servlet>) info.getClassLoader()
							.loadClass(pValue));
					// parse load on startup
					NodeList ldElmntLst = fstElmnt.getElementsByTagName("load-on-startup");
					if (ldElmntLst != null) {
						Element ldElmnt = (Element) ldElmntLst.item(0);
						if(ldElmnt != null) {
							NodeList ldNm = ldElmnt.getChildNodes();
							String pLoad = (ldNm.item(0)).getNodeValue().trim();
							trace("Load on startup: %s", pLoad);
							servlet.setLoadOnStartup(Integer.valueOf(pLoad));
						}
					}
					// do init-param - available in the context of a servlet
					// or filter in the web application
					listOfElements = fstElmnt.getElementsByTagName("init-param");
					totalElements = listOfElements.getLength();
					trace("Total no of init-params: %s", totalElements);
					for (int i = 0; i < totalElements; i++) {
						Node inNode = listOfElements.item(i);
						if (inNode.getNodeType() == Node.ELEMENT_NODE) {
							Element inElmnt = (Element) inNode;
							NodeList inNmElmntLst = inElmnt.getElementsByTagName("param-name");
							Element inNmElmnt = (Element) inNmElmntLst.item(0);
							NodeList inNm = inNmElmnt.getChildNodes();
							String inName = (inNm.item(0)).getNodeValue().trim();
							trace("Param name: %s", inName);
							NodeList inValElmntLst = inElmnt.getElementsByTagName("param-value");
							Element inValElmnt = (Element) inValElmntLst.item(0);
							NodeList inVal = inValElmnt.getChildNodes();
							String inValue = (inVal.item(0)).getNodeValue().trim();
							trace("Param value: %s", inValue);
							// add the param
							servlet.addInitParam(inName, inValue);
						}
					}
					// add to the map
					servletMap.put(servlet.getName(), servlet);
				}
			}
			// do servlet-mapping
			if (!servletMap.isEmpty()) {
				listOfElements = doc.getElementsByTagName("servlet-mapping");
				totalElements = listOfElements.getLength();
				log.warnf("Total no of servlet-mappings: %2", totalElements);
				for (int s = 0; s < totalElements; s++) {
					Node fstNode = listOfElements.item(s);
					if (fstNode.getNodeType() == Node.ELEMENT_NODE) {
						Element fstElmnt = (Element) fstNode;
						NodeList fstNmElmntLst = fstElmnt.getElementsByTagName("servlet-name");
						Element fstNmElmnt = (Element) fstNmElmntLst.item(0);
						NodeList fstNm = fstNmElmnt.getChildNodes();
						String pName = (fstNm.item(0)).getNodeValue().trim();
						trace("Param name: %s", pName);
						// lookup the servlet info
						ServletInfo servlet = servletMap.get(pName);
						// add the mapping
						if (servlet != null) {
							NodeList lstNmElmntLst = fstElmnt.getElementsByTagName("url-pattern");
							for (int p = 0; p < lstNmElmntLst.getLength(); p++) {
								Element lstNmElmnt = (Element) lstNmElmntLst.item(p);
								NodeList lstNm = lstNmElmnt.getChildNodes();
								String pValue = (lstNm.item(0)).getNodeValue().trim();
								trace("Param value: %s", pValue);
								servlet.addMapping(pValue);

							}
						} else {
							log.warnf("No servlet found for %s", pName);
						}
					}
				}
				// add servlets to deploy info
				info.addServlets(servletMap.values());
			}
			// do welcome files
			listOfElements = doc.getElementsByTagName("welcome-file-list");
			totalElements = listOfElements.getLength();
			trace("Total no of welcome-files: %s", totalElements);
			for (int s = 0; s < totalElements; s++) {
				Node fstNode = listOfElements.item(s);
				if (fstNode.getNodeType() == Node.ELEMENT_NODE) {
					Element fstElmnt = (Element) fstNode;
					NodeList fstNmElmntLst = fstElmnt.getElementsByTagName("welcome-file");
					int totalWelcomeFiles = fstNmElmntLst.getLength();

					log.debug( "Adding welcome pages:" );
					for(int i=0; i < totalWelcomeFiles; i++){
						Element fstNmElmnt = (Element) fstNmElmntLst.item(i);
						NodeList fstNm = fstNmElmnt.getChildNodes();
						String pName = (fstNm.item(0)).getNodeValue().trim();
						trace("Param name: %s", pName);
						log.debug( "welcome page:" + pName);
						// add welcome page
						info.addWelcomePage(pName);
					}
				}
			}
			// do display name
			NodeList dNmElmntLst = doc.getElementsByTagName("display-name");
			if (dNmElmntLst.getLength() == 1) {
				Node dNmNode = dNmElmntLst.item(0);
				if (dNmNode.getNodeType() == Node.TEXT_NODE) {
					String dName = dNmNode.getNodeValue().trim();
					trace("Display name: %s", dName);
					info.setDisplayName(dName);
				}
			}
			// TODO add security stuff
		} catch (Exception e) {
			e.printStackTrace();
			log.error("Error reading web.xml", e);
		}
	}

	private static void trace(String string, Object elements) {
		log.tracef(string,elements);
//		System.out.printf(string,elements);
//		System.out.println();
	}
	
}
