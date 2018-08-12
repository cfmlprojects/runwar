package runwar.server;

import io.undertow.util.Headers;
import io.undertow.util.StatusCodes;
import net.minidev.json.JSONObject;
import net.minidev.json.JSONValue;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import testutils.DefaultServer;
import testutils.HttpClientUtils;
import testutils.TestHttpClient;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith({ProxyPeerAddressTest.ServerConfig.class, DefaultServer.class})
public class ProxyPeerAddressTest {

    @ExtendWith({DefaultServer.class})
    public static class ServerConfig implements BeforeAllCallback {
        @Override
        public void beforeAll(ExtensionContext context) {
            DefaultServer.resetServerOptions()
                    .serverName("proxyPeerAddress")
                    .proxyPeerAddressEnable(true);
        }

    }

    @Test
    public void testForwardFor() throws IOException {

        String port = "" + DefaultServer.getHostPort("default");
        String forwardFor = "some.domain.forwarded";
        assertTrue(DefaultServer.getServerOptions().proxyPeerAddressEnable());
        TestHttpClient client = DefaultServer.getClient();
        HttpGet get = new HttpGet(DefaultServer.getDefaultServerURL() + "/dumprunwarrequest");
        get.addHeader(Headers.X_FORWARDED_FOR_STRING, forwardFor);
        HttpResponse result = client.execute(get);
        assertEquals(StatusCodes.OK, result.getStatusLine().getStatusCode());
        JSONObject responseData = (JSONObject) JSONValue.parse(HttpClientUtils.readResponse(result));
        assertEquals(forwardFor + ":0", responseData.get("remoteAddr"));
        assertEquals(forwardFor, responseData.get("remoteHost"));
        assertEquals("localhost:" + port, responseData.get("host"));
        assertEquals(forwardFor,
                ((JSONObject) responseData.get("headers")).get(Headers.X_FORWARDED_FOR_STRING));
    }

    @Test
    public void testForwardForHostPort() throws IOException {

        String forwardFor = "localhost";
        String forwardPort = "8765";
        assertTrue(DefaultServer.getServerOptions().proxyPeerAddressEnable());
        TestHttpClient client = DefaultServer.getClient();
        HttpGet get = new HttpGet(DefaultServer.getDefaultServerURL() + "/dumprunwarrequest");
        get.addHeader(Headers.X_FORWARDED_FOR_STRING, forwardFor);
        get.addHeader(Headers.X_FORWARDED_HOST_STRING, forwardFor);
        get.addHeader(Headers.X_FORWARDED_PORT_STRING, forwardPort);
        System.out.println(get.getURI());
        HttpResponse result = client.execute(get);
        assertEquals(StatusCodes.OK, result.getStatusLine().getStatusCode());
        JSONObject responseData = (JSONObject) JSONValue.parse(HttpClientUtils.readResponse(result));
        assertEquals(forwardFor + ":" + forwardPort, responseData.get("host"));
        assertEquals(forwardFor, responseData.get("remoteHost"));
        assertEquals(forwardFor,
                ((JSONObject) responseData.get("headers")).get(Headers.X_FORWARDED_FOR_STRING));
    }

}