package testutils.mock;

import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import java.util.Enumeration;

public class MockFilterConfig implements FilterConfig {
    private ServletContext servletContext;

    public String getFilterName() {
        return null;
    }

    public ServletContext getServletContext() {
        return servletContext;
    }

    public String getInitParameter(String string) {
        return null;
    }

    public Enumeration<String> getInitParameterNames() {
        return null;
    }

    public void setServletContext(ServletContext servletContext) {
        this.servletContext = servletContext;
    }
}
