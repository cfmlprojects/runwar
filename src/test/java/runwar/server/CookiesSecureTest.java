package runwar.server;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.util.EntityUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import runwar.options.ServerOptions;
import testutils.DefaultServer;
import testutils.HttpClientUtils;

import static io.undertow.util.Headers.SET_COOKIE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith({CookiesSecureTest.ServerConfig.class, DefaultServer.class})
public class CookiesSecureTest {

    @ExtendWith({DefaultServer.class})
    public static class ServerConfig implements BeforeAllCallback {
        @Override
        public void beforeAll(ExtensionContext context) throws Exception {
            DefaultServer.resetServerOptions()
                    .setServerName("secureCookies")
                    .setSecureCookies(true)
                    .setEnableSSL(true);
        }
    }

    @Test
    public void testHasSecureCookies() throws Exception {
        assertTrue(DefaultServer.getServerOptions().isCookieHttpOnly());
        assertTrue(DefaultServer.getServerOptions().isCookieSecure());
        String url = DefaultServer.getDefaultServerHTTP2Address() + "/dumprunwarrequest";
        HttpGet get = new HttpGet(url);
        HttpResponse result = DefaultServer.getClient().execute(get);
        String content = HttpClientUtils.readResponse(result);
        assertNotNull(content);
//        System.out.println(content);

        Header[] values = result.getHeaders("HttpOnly");
        assertEquals(1, values.length);
        String value = values[0].getValue();
        assertEquals("true", value);

        values = result.getHeaders("Secure");
        assertEquals(1, values.length);
        value = values[0].getValue();
        assertEquals("true", value);

    }

}