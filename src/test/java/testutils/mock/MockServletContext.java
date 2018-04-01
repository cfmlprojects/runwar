package testutils.mock;

import org.tuckey.web.filters.urlrewrite.utils.Log;

import runwar.undertow.UrlRewriteFilterTest;

import javax.servlet.Filter;
import javax.servlet.FilterRegistration;
import javax.servlet.FilterRegistration.Dynamic;
import javax.servlet.RequestDispatcher;
import javax.servlet.Servlet;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRegistration;
import javax.servlet.SessionCookieConfig;
import javax.servlet.SessionTrackingMode;
import javax.servlet.descriptor.JspConfigDescriptor;

import java.io.File;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Enumeration;
import java.util.EventListener;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;

public class MockServletContext implements ServletContext {

    private static Log log = Log.getLog(MockServletContext.class);
    private final Hashtable<String, Object> attributes = new Hashtable<String, Object>();

    public ServletContext getContext(String s) {
        return new MockServletContext();
    }

    public Object getAttribute(String name) {
        return this.attributes.get(name);
    }

    public int getMajorVersion() {
        return 0;
    }

    public int getMinorVersion() {
        return 0;
    }

    public String getMimeType(String s) {
        return null;
    }

    public Set<String> getResourcePaths(String s) {
        return null;
    }

    public URL getResource(String s) throws MalformedURLException {
        return null;
    }

    public InputStream getResourceAsStream(String s) {
        return null;
    }

    public RequestDispatcher getRequestDispatcher(String s) {
        return null;
    }

    public RequestDispatcher getNamedDispatcher(String s) {
        return null;
    }

    public Servlet getServlet(String s) throws ServletException {
        return null;
    }

    public Enumeration<Servlet> getServlets() {
        return null;
    }

    public Enumeration<String> getServletNames() {
        return null;
    }

    public void log(String s) {
    }

    public void log(Exception e, String s) {
    }

    public void log(String s, Throwable throwable) {
    }

    public String getRealPath(String s) {
        URL url = null;
        try {
            url = new File("tests/resource").toURI().toURL();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        if ( url == null ) {
            log.error("could not get base path for comparison");
            return null;
        }   else {
            String basePath = url.getFile();
            log.debug("TEST ONLY using base path of " + basePath);
            if (basePath.endsWith("urlrewrite.conf")) basePath = basePath.substring(0, basePath.length() - "urlrewrite.conf".length());
            if (basePath.endsWith("/")) basePath = basePath.substring(0, basePath.length() - 1);
            return basePath + (s == null ? "" : s);
        }
    }

    public String getServerInfo() {
        return null;
    }

    public String getInitParameter(String s) {
        return null;
    }

    public Enumeration<String> getInitParameterNames() {
        return null;
    }

    public Enumeration<String> getAttributeNames() {
        return null;
    }

    public void setAttribute(String name, Object value) {
        if (value != null) {
            this.attributes.put(name, value);
        } else {
            this.attributes.remove(name);
        }
    }

    public void removeAttribute(String s) {
    }

    public String getServletContextName() {
        return null;
    }

    @Override
    public Dynamic addFilter(String arg0, String arg1) {
        return null;
    }

    @Override
    public Dynamic addFilter(String arg0, Filter arg1) {
        return null;
    }

    @Override
    public Dynamic addFilter(String arg0, Class<? extends Filter> arg1) {
        return null;
    }

    @Override
    public void addListener(String arg0) {
    }

    @Override
    public <T extends EventListener> void addListener(T arg0) {
    }

    @Override
    public void addListener(Class<? extends EventListener> arg0) {
    }

    @Override
    public javax.servlet.ServletRegistration.Dynamic addServlet(String arg0, String arg1) {
        return null;
    }

    @Override
    public javax.servlet.ServletRegistration.Dynamic addServlet(String arg0, Servlet arg1) {
        return null;
    }

    @Override
    public javax.servlet.ServletRegistration.Dynamic addServlet(String arg0, Class<? extends Servlet> arg1) {

        return null;
    }

    @Override
    public <T extends Filter> T createFilter(Class<T> arg0) throws ServletException {
        return null;
    }

    @Override
    public <T extends EventListener> T createListener(Class<T> arg0) throws ServletException {
        return null;
    }

    @Override
    public <T extends Servlet> T createServlet(Class<T> arg0) throws ServletException {
        return null;
    }

    @Override
    public void declareRoles(String... arg0) {
    }

    @Override
    public ClassLoader getClassLoader() {

        return null;
    }

    @Override
    public String getContextPath() {
        return null;
    }

    @Override
    public Set<SessionTrackingMode> getDefaultSessionTrackingModes() {
        return null;
    }

    @Override
    public int getEffectiveMajorVersion() {
        return 0;
    }

    @Override
    public int getEffectiveMinorVersion() {
        return 0;
    }

    @Override
    public Set<SessionTrackingMode> getEffectiveSessionTrackingModes() {
        return null;
    }

    @Override
    public FilterRegistration getFilterRegistration(String arg0) {
        return null;
    }

    @Override
    public Map<String, ? extends FilterRegistration> getFilterRegistrations() {
        return null;
    }

    @Override
    public JspConfigDescriptor getJspConfigDescriptor() {
        return null;
    }

    @Override
    public ServletRegistration getServletRegistration(String arg0) {
        return null;
    }

    @Override
    public Map<String, ? extends ServletRegistration> getServletRegistrations() {
        return null;
    }

    @Override
    public SessionCookieConfig getSessionCookieConfig() {
        return null;
    }

    @Override
    public String getVirtualServerName() {
        return null;
    }

    @Override
    public boolean setInitParameter(String arg0, String arg1) {
        return false;
    }

    @Override
    public void setSessionTrackingModes(Set<SessionTrackingMode> arg0) {
    }
}
