package testutils.mock;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import java.io.IOException;

public class MockRequestDispatcher implements RequestDispatcher {

    private static MockRequestDispatcher current;

    private String url;
    private boolean forwarded = false;
    private boolean included = false;
    private long includeLastCalled = 0;

    public MockRequestDispatcher(String url) {
        this.url = url;
        current = this;
    }

    public void forward(ServletRequest servletRequest, ServletResponse servletResponse) throws ServletException, IOException {
        forwarded = true;
    }

    public void include(ServletRequest servletRequest, ServletResponse servletResponse) throws ServletException, IOException {
        includeLastCalled = System.currentTimeMillis();
        included = true;
    }

    public String getUrl() {
        return url;
    }

    public static MockRequestDispatcher getCurrent() {
        return current;
    }

    public boolean isForwarded() {
        return forwarded;
    }

    public boolean isIncluded() {
        return included;
    }

    public long getIncludeLastCalled() {
        return includeLastCalled;
    }
}
