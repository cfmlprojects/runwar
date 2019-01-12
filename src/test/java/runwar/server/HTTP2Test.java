package runwar.server;

import io.undertow.util.StatusCodes;
import net.minidev.json.JSONObject;
import net.minidev.json.JSONValue;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.LaxRedirectStrategy;
import org.junit.jupiter.api.Test;
import runwar.options.ServerOptions;
import testutils.DefaultServer;
import testutils.HttpClientUtils;
import testutils.TestHttpClient;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static runwar.logging.RunwarLogger.LOG;

public class HTTP2Test extends AbstractServerTest {

    public ServerOptions getServerOptions() {
        return getDefaultServerOptions()
                .serverName("http2test")
                .http2Enable(true);
    }

    @Test
    public void http2testRequest() throws IOException {
        HttpGet get = new HttpGet(DefaultServer.getDefaultServerHTTP2Address() + "/dumprunwarrequest");
        LOG.info("Get request for SSLaddress:" + get.getURI());
        HttpResponse result = DefaultServer.getClient().execute(get);
        assertEquals(StatusCodes.OK, result.getStatusLine().getStatusCode());
        JSONObject responseData = (JSONObject) JSONValue.parse(HttpClientUtils.readResponse(result));
        assertTrue(Boolean.parseBoolean(responseData.get("isHTTP2").toString()), responseData.toJSONString());
    }

    @Test
    public void http2testProxiedRequest() throws IOException {
        TestHttpClient client = DefaultServer.getClient();
        HttpGet get = new HttpGet(DefaultServer.getDefaultServerHTTP2Address() + "/dumprunwarrequest");
        LOG.info("Get request for proxied SSL:" + get.getURI());
        client.setRedirectStrategy(new LaxRedirectStrategy());
        HttpResponse result = client.execute(get);
        assertEquals(StatusCodes.OK, result.getStatusLine().getStatusCode());
        JSONObject responseData = (JSONObject) JSONValue.parse(HttpClientUtils.readResponse(result));
        assertTrue(Boolean.parseBoolean(responseData.get("isHTTP2").toString()), get.getURI().toString() + " failed to be http2");
    }

}

