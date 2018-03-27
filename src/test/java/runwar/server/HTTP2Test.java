package runwar.server;

import testutils.DefaultServer;
import testutils.HttpClientUtils;
import testutils.TestHttpClient;
import io.undertow.util.StatusCodes;
import net.minidev.json.JSONObject;
import net.minidev.json.JSONValue;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.LaxRedirectStrategy;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(DefaultServer.class)
public class HTTP2Test {

    @BeforeAll
    public static void beforeClass() {
        DefaultServer.getServerOptions().setHTTP2Enabled(true);
    }

    @Test
    public void http2testRequest() throws IOException {

        final TestHttpClient client = new TestHttpClient();
        try {
            HttpGet get = new HttpGet(DefaultServer.getDefaultServerHTTP2Address() + "/dumprunwarrequest");
            HttpResponse result = client.execute(get);
            assertEquals(StatusCodes.OK, result.getStatusLine().getStatusCode());
            JSONObject responseData = (JSONObject) JSONValue.parse(HttpClientUtils.readResponse(result));
            assertTrue(Boolean.parseBoolean(responseData.get("isHTTP2").toString()));
        } finally {
            client.getConnectionManager().shutdown();
        }
    }

    @Test
    public void http2testProxiedRequest() throws IOException {
        
        final TestHttpClient client = new TestHttpClient();
        try {
            HttpGet get = new HttpGet(DefaultServer.getDefaultServerURL() + "/dumprunwarrequest");
            client.setRedirectStrategy(new LaxRedirectStrategy());
            HttpResponse result = client.execute(get);
            assertEquals(StatusCodes.OK, result.getStatusLine().getStatusCode());
            JSONObject responseData = (JSONObject) JSONValue.parse(HttpClientUtils.readResponse(result));
            assertTrue(Boolean.parseBoolean(responseData.get("isHTTP2").toString()), get.getURI().toString()+" failed to be http2");
        } finally {
            client.getConnectionManager().shutdown();
        }
    }
    
}