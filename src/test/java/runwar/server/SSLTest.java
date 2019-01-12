package runwar.server;

import io.undertow.util.StatusCodes;
import net.minidev.json.JSONObject;
import net.minidev.json.JSONValue;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.junit.jupiter.api.Test;
import runwar.options.ServerOptions;
import testutils.DefaultServer;
import testutils.HttpClientUtils;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static runwar.logging.RunwarLogger.LOG;

public class SSLTest extends AbstractServerTest {

    public ServerOptions getServerOptions() {
        return getDefaultServerOptions()
                .serverName("ssltest")
                .sslEnable(true)
                .sslPort(1553);
    }

    @Test
    public void http2testRequest() throws IOException {
        HttpGet get = new HttpGet(DefaultServer.getDefaultServerSSLAddress() + "/dumprunwarrequest");
        LOG.info("Get request for SSLaddress:" + get.getURI());
        HttpResponse result = DefaultServer.getClient().execute(get);
        assertEquals(StatusCodes.OK, result.getStatusLine().getStatusCode());
        JSONObject responseData = (JSONObject) JSONValue.parse(HttpClientUtils.readResponse(result));
        assertTrue(Boolean.parseBoolean(responseData.get("isHTTPS").toString()), responseData.toJSONString());
        LOG.info(responseData);
    }


}

