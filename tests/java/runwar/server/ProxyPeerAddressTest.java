package runwar.server;

import testutils.DefaultServer;
import testutils.HttpClientUtils;
import testutils.TestHttpClient;
import io.undertow.util.Headers;
import io.undertow.util.Protocols;
import io.undertow.util.StatusCodes;
import net.minidev.json.JSONObject;
import net.minidev.json.JSONValue;
import runwar.options.ServerOptions;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;

@RunWith(DefaultServer.class)
public class ProxyPeerAddressTest {

    @BeforeClass
    public static void beforeClass() {
        DefaultServer.getServerOptions().setProxyPeerAddressEnabled(true);
    }

    @Test
    public void testForwardFor() throws IOException {

        String port = "8088";
        String forwardFor = "some.domain.forwarded";
        Assert.assertTrue(DefaultServer.getServerOptions().isProxyPeerAddressEnabled());
        final TestHttpClient client = new TestHttpClient();
        try {
            HttpGet get = new HttpGet(DefaultServer.getDefaultServerURL() + "/dumprunwarrequest");
            get.addHeader(Headers.X_FORWARDED_FOR_STRING, forwardFor);
            HttpResponse result = client.execute(get);
            Assert.assertEquals(StatusCodes.OK, result.getStatusLine().getStatusCode());
            JSONObject responseData = (JSONObject) JSONValue.parse(HttpClientUtils.readResponse(result));
            Assert.assertEquals(forwardFor + ":0", responseData.get("remoteAddr"));
            Assert.assertEquals(forwardFor, responseData.get("remoteHost"));
            Assert.assertEquals("localhost:" + port, responseData.get("host"));
            Assert.assertEquals(forwardFor,
                    ((JSONObject) responseData.get("headers")).get(Headers.X_FORWARDED_FOR_STRING));
        } finally {
            client.getConnectionManager().shutdown();
        }
    }

    @Test
    public void testForwardForHostPort() throws IOException {
        
        String forwardFor = "localhost";
        String forwardPort = "8765";
        Assert.assertTrue(DefaultServer.getServerOptions().isProxyPeerAddressEnabled());
        final TestHttpClient client = new TestHttpClient();
        try {
            HttpGet get = new HttpGet(DefaultServer.getDefaultServerURL() + "/dumprunwarrequest");
            get.addHeader(Headers.X_FORWARDED_FOR_STRING, forwardFor);
            get.addHeader(Headers.X_FORWARDED_HOST_STRING, forwardFor);
            get.addHeader(Headers.X_FORWARDED_PORT_STRING, forwardPort);
            HttpResponse result = client.execute(get);
            Assert.assertEquals(StatusCodes.OK, result.getStatusLine().getStatusCode());
            JSONObject responseData = (JSONObject) JSONValue.parse(HttpClientUtils.readResponse(result));
            Assert.assertEquals(forwardFor + ":" + forwardPort, responseData.get("host"));
            Assert.assertEquals(forwardFor, responseData.get("remoteHost"));
            Assert.assertEquals(forwardFor,
                    ((JSONObject) responseData.get("headers")).get(Headers.X_FORWARDED_FOR_STRING));
        } finally {
            client.getConnectionManager().shutdown();
        }
    }
}