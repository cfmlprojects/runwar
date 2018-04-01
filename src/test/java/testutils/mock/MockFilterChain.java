package testutils.mock;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import java.io.IOException;

public class MockFilterChain implements FilterChain {

    private int invocationCount = 0;
    private long timeInvoked = 0;

    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse) throws IOException, ServletException {
        invocationCount++;
        timeInvoked = System.currentTimeMillis();
        // make sure we wait a little so time elapses
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            //
        }
    }

    public boolean isDoFilterRun() {
        return invocationCount > 0;
    }

    public int getInvocationCount() {
        return invocationCount;
    }

    public long getTimeInvoked() {
        return timeInvoked;
    }
}
