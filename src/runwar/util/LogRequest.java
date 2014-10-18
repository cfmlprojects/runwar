package runwar.util;

import java.io.IOException;
import java.util.Enumeration;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import javax.servlet.ServletConfig;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
/**
 * A simple request logger 
  <rule enabled="true">
    <name>railoLocalOnly</name>
    <note>Only allow access to railo admin from localhost</note>
    <condition operator="notequal" type="remote-addr">127\.0\.0\.1|0:0:0:0:0:0:0:1%0</condition>
    <run class="runwar.LogRequest">
      <init-param>
        <param-name>logLevel</param-name>
        <param-value>SEVERE</param-value>
      </init-param>
      <init-param>
        <param-name>logFilePath</param-name>
        <param-value>./requests.log</param-value>
      </init-param>
    </run>
    <from casesensitive="false">^/(railo-context/admin/|tests|CFIDE/administrator/|bluedragon/administrator).*</from>
    <to last="true" type="redirect">/</to>
  </rule>
    Good examples of logging configurations:
    http://www.exampledepot.com/egs/java.util.logging/pkg.html
 */
public class LogRequest {

    private String logLevel;
	private String logFilePath;
    static final Logger log = Logger.getLogger(LogRequest.class.getName());
    
    public void run(ServletRequest request, ServletResponse response) {
        if (logLevel != null) {
        	HttpServletRequest httpRequest = (HttpServletRequest) request;
        	String remoteAddress =  request.getRemoteAddr();
            String uri = httpRequest.getRequestURI();
            String protocol = request.getProtocol();
            String logString = "*URI*:" + uri + " *REMOTEADDRESS*:" + remoteAddress + " *PROTOCOL*: " + protocol + " *METHOD*:" + httpRequest.getMethod() + " ";
            Enumeration headerNames = httpRequest.getHeaderNames();
            while(headerNames.hasMoreElements()) {
              String headerName = (String)headerNames.nextElement();
              logString += headerName.toUpperCase() + ": ";
              logString += httpRequest.getHeader(headerName) + " ";
            }
            log.log(Level.parse(logLevel),logString);
            }
    }

    public void init(ServletConfig config) {
    	this.logLevel = config.getInitParameter("logLevel");
    	this.logFilePath = config.getInitParameter("logFilePath");
    	boolean append = true;
        FileHandler handler;
		try {
			handler = new FileHandler(this.logFilePath, append);
			handler.setFormatter(new SimpleFormatter());
			log.addHandler(handler);
		} catch (SecurityException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        // Add to the desired logger
    }

    public void destroy() {
    }

}