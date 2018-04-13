package runwar.undertow;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.tuckey.web.filters.urlrewrite.Conf;
import org.tuckey.web.filters.urlrewrite.NormalRewrittenUrl;
import org.tuckey.web.filters.urlrewrite.UrlRewriter;
import org.tuckey.web.filters.urlrewrite.utils.Log;
import testutils.mock.MockFilterChain;
import testutils.mock.MockRequest;
import testutils.mock.MockResponse;
import testutils.mock.MockServletContext;

import javax.servlet.ServletException;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.*;

public class UrlRewriteFilterTest {

    MockResponse response;
    MockRequest request;
    testutils.mock.MockServletContext servletContext;
    MockFilterChain chain;

    UrlRewriteFilterTest() {
        Log.setLevel("DEBUG");
        response = new MockResponse();
        request = new MockRequest("/");
        servletContext = new MockServletContext();
        chain = new MockFilterChain();
    }

    @Test
    @DisplayName("Should test a simple mod_rewrite style .htaccess conf")
    void simpleApacheHConf() throws IOException, ServletException, InvocationTargetException {
        Conf conf = new Conf(servletContext, new FileInputStream(new File("src/test/resources/urlrewrite/simple.htaccess")), "simple.htaccess", null, true);
        assertTrue(conf.isEngineEnabled());
        assertTrue(conf.isOk(),"conf did not parse");
        assertTrue(conf.getRules().size() > 0);
        NormalRewrittenUrl rewrittenRequest;
        MockRequest request;

        UrlRewriter urlRewriter = new UrlRewriter(conf);

        request = new MockRequest("/notthere");
        rewrittenRequest = (NormalRewrittenUrl) urlRewriter.processRequest(request, response);
        assertNotNull(rewrittenRequest);
        assertEquals("index.cfm/notthere", rewrittenRequest.getTarget());

        request = new MockRequest("/foo/not/there");
        rewrittenRequest = (NormalRewrittenUrl) urlRewriter.processRequest(request, response);
        assertNotNull(rewrittenRequest);
        assertEquals("index.cfm/foo/not/there", rewrittenRequest.getTarget());
    }

    @Test
    @DisplayName("Should test relative paths of rewrite rule")
    void relativePathTest() throws IOException, ServletException, InvocationTargetException {
        HashMap<String,String> requestTargets = new HashMap<>();
        String rule = "RewriteRule ^.*$ rewritten.html";
        requestTargets.put("/index.cfm","rewritten.html");
        requestTargets.put("/foo/index.cfm","rewritten.html");
        requestTargets.put("/bar/index.cfm","rewritten.html");
        assertRuleResult(rule, requestTargets);
        
        requestTargets = new HashMap<>();
        rule = "RewriteRule ^.*$ /rewritten.html";
        requestTargets.put("/index.cfm","/rewritten.html");
        requestTargets.put("/foo/index.cfm","/rewritten.html");
        requestTargets.put("/bar/index.cfm","/rewritten.html");
        assertRuleResult(rule, requestTargets);

        requestTargets = new HashMap<>();
        rule = "RewriteRule .* 410.html [L]";
        requestTargets.put("/index.cfm","410.html");
        assertRuleResult(rule, requestTargets);
        
    }

    private void assertRuleResult(String rule, HashMap<String,String> requestTargets) {
        Conf conf = new Conf(servletContext, new ByteArrayInputStream(rule.getBytes()), "simple.htaccess", null, true);
        assertTrue(conf.isEngineEnabled());
        assertTrue(conf.isOk(),"conf did not parse");
        assertTrue(conf.getRules().size() > 0);

        final UrlRewriter urlRewriter = new UrlRewriter(conf);

        requestTargets.forEach((path, result) -> {
            MockRequest mockRequest = new MockRequest(path);
            try {
                NormalRewrittenUrl rewrittenRequest = (NormalRewrittenUrl) urlRewriter.processRequest(mockRequest, response);
                assertEquals(result, rewrittenRequest.getTarget());
            } catch (InvocationTargetException | IOException | ServletException e) {
                e.printStackTrace();
            }
        });
    }

    class MockNormalRewrittenUrl extends NormalRewrittenUrl {
        protected MockNormalRewrittenUrl(String target) {
            super(target);
        }

    }
}
