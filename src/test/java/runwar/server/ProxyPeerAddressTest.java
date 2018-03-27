package runwar.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.undertow.util.Headers;
import io.undertow.util.StatusCodes;
import net.minidev.json.JSONObject;
import net.minidev.json.JSONValue;
import testutils.DefaultServer;
import testutils.HttpClientUtils;
import testutils.TestHttpClient;

@ExtendWith(DefaultServer.class)
public class ProxyPeerAddressTest {

    @BeforeAll
    public static void beforeClass() {
        DefaultServer.getServerOptions().setProxyPeerAddressEnabled(true);
    }

    @Test
    public void testForwardFor() throws IOException {

        String port = "8088";
        String forwardFor = "some.domain.forwarded";
        assertTrue(DefaultServer.getServerOptions().isProxyPeerAddressEnabled());
        final TestHttpClient client = new TestHttpClient();
        try {
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
        } finally {
            client.getConnectionManager().shutdown();
        }
    }

    @Test
    public void testForwardForHostPort() throws IOException {
        
        String forwardFor = "localhost";
        String forwardPort = "8765";
        assertTrue(DefaultServer.getServerOptions().isProxyPeerAddressEnabled());
        final TestHttpClient client = new TestHttpClient();
        try {
            HttpGet get = new HttpGet(DefaultServer.getDefaultServerURL() + "/dumprunwarrequest");
            get.addHeader(Headers.X_FORWARDED_FOR_STRING, forwardFor);
            get.addHeader(Headers.X_FORWARDED_HOST_STRING, forwardFor);
            get.addHeader(Headers.X_FORWARDED_PORT_STRING, forwardPort);
            HttpResponse result = client.execute(get);
            assertEquals(StatusCodes.OK, result.getStatusLine().getStatusCode());
            JSONObject responseData = (JSONObject) JSONValue.parse(HttpClientUtils.readResponse(result));
            assertEquals(forwardFor + ":" + forwardPort, responseData.get("host"));
            assertEquals(forwardFor, responseData.get("remoteHost"));
            assertEquals(forwardFor,
                    ((JSONObject) responseData.get("headers")).get(Headers.X_FORWARDED_FOR_STRING));
        } finally {
            client.getConnectionManager().shutdown();
        }
    }
}