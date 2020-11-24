package testutils.mock;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpSession;
import java.util.Enumeration;
import java.util.Hashtable;

@SuppressWarnings("deprecation")
public class MockSession implements HttpSession {

    MockServletContext servletContext = new MockServletContext();
    Hashtable<String, Object> attrs = new Hashtable<String, Object>();
    private boolean sessionNew;

    public long getCreationTime() {
        return 0;
    }

    public String getId() {
        return null;
    }

    public long getLastAccessedTime() {
        return 0;
    }

    public ServletContext getServletContext() {
        return servletContext;
    }

    public void setServletContext(ServletContext sc) {
        servletContext = (MockServletContext) sc;
    }

    public int getMaxInactiveInterval() {
        return 0;
    }

    public void setMaxInactiveInterval(int i) {

    }

    @Deprecated
    public javax.servlet.http.HttpSessionContext getSessionContext() {
        return null;
    }

    public Object getAttribute(String s) {
        return attrs.get(s);
    }

    @Deprecated
    public Object getValue(String s) {
        return null;
    }

    public Enumeration<String> getAttributeNames() {
        return null;
    }

    @Deprecated
    public String[] getValueNames() {
        return new String[0];
    }

    public void setAttribute(String s, Object o) {
        attrs.put(s, o);
    }

    @Deprecated
    public void putValue(String s, Object o) {

    }

    public void removeAttribute(String s) {
        attrs.remove(s);
    }

    @Deprecated
    public void removeValue(String s) {

    }

    public void invalidate() {

    }

    public boolean isNew() {
        return sessionNew;
    }

    public void setNew(boolean sessionNew) {
        this.sessionNew = sessionNew;
    }
}
