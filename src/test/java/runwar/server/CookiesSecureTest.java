package runwar.server;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.junit.jupiter.api.Test;
import runwar.options.ServerOptions;
import testutils.DefaultServer;
import testutils.HttpClientUtils;

import static org.junit.jupiter.api.Assertions.*;

public class CookiesSecureTest extends AbstractServerTest {

    public ServerOptions getServerOptions() {
        return getDefaultServerOptions()
                .serverName("secureCookies")
                .secureCookies(true)
                .sslEnable(true);
    }

    @Test
    public void testHasSecureCookies() throws Exception {
        assertTrue(DefaultServer.getServerOptions().cookieHttpOnly());
        assertTrue(DefaultServer.getServerOptions().cookieSecure());
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