package runwar.server;

import io.undertow.util.StatusCodes;
import net.minidev.json.JSONObject;
import net.minidev.json.JSONValue;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.LaxRedirectStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.TestReporter;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import runwar.options.ServerOptions;
import testutils.DefaultServer;
import testutils.HttpClientUtils;
import testutils.TestHttpClient;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static runwar.logging.RunwarLogger.LOG;

@ExtendWith({HTTP2Test.ServerConfig.class, DefaultServer.class})
public class HTTP2Test {

    @ExtendWith({DefaultServer.class})
    public static class ServerConfig implements BeforeAllCallback {
        @Override
        public void beforeAll(ExtensionContext context) throws Exception {
            DefaultServer.resetServerOptions()
                    .setServerName("http2test")
                    .setHTTP2Enabled(true);
        }

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

