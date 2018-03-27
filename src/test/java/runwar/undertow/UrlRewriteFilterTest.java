package runwar.undertow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

import javax.servlet.ServletException;

import org.junit.jupiter.api.Test;
import org.tuckey.web.filters.urlrewrite.Conf;
import org.tuckey.web.filters.urlrewrite.NormalRewrittenUrl;
import org.tuckey.web.filters.urlrewrite.UrlRewriter;
import org.tuckey.web.filters.urlrewrite.utils.Log;

import lucee.loader.servlet.CFMLServlet;
import testutils.mock.MockFilterChain;
import testutils.mock.MockRequest;
import testutils.mock.MockResponse;
import testutils.mock.MockServletContext;

public class UrlRewriteFilterTest {
    CFMLServlet servlet;

    MockResponse response;
    MockRequest request;
    testutils.mock.MockServletContext servletContext;
    MockFilterChain chain;

    public UrlRewriteFilterTest() {
        Log.setLevel("DEBUG");
        response = new MockResponse();
        request = new MockRequest("/");
        servletContext = new MockServletContext();
        chain = new MockFilterChain();
    }

    @Test
    public void simpleApacheHConf() throws IOException, ServletException, InvocationTargetException {
        Conf conf = new Conf(servletContext, new FileInputStream(new File("src/test/resources/urlrewrite.htaccess")), "urlrewrite.htaccess", null, true);
        assertTrue(conf.isEngineEnabled());
        assertTrue(conf.isOk());
        assertTrue(conf.getRules().size() > 0);
        NormalRewrittenUrl rewrittenRequest;
        MockRequest request;

        UrlRewriter urlRewriter = new UrlRewriter(conf);

        request = new MockRequest("/notthere");
        rewrittenRequest = (NormalRewrittenUrl) urlRewriter.processRequest(request, response);
        assertNotNull(rewrittenRequest);
        assertEquals("index.cfm/notthere", rewrittenRequest.getTarget());

    }

    class MockNormalRewrittenUrl extends NormalRewrittenUrl {
        protected MockNormalRewrittenUrl(String target) {
            super(target);
        }

    }
}
