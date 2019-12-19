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
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static runwar.logging.RunwarLogger.LOG;

public class SSLSelfSignTest extends AbstractServerTest {

    public ServerOptions getServerOptions() {
        try {
            Files.list(Paths.get(DefaultServer.getWarFile("default") + "/WEB-INF/"))
                    .forEach(file -> {
                        if (file.toString().matches(".*selfsign\\.[crt|key]+")) {
                            try {
                                LOG.info("Deleting selfsign: " + file);
                                Files.delete(file);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    });
        } catch (IOException e) {
            e.printStackTrace();
        }

        return getDefaultServerOptions()
                .serverName("sslSelfSignTest")
                .sslEnable(true)
                .sslSelfSign(true)
                .sslPort(1553);
    }

    @Test
    public void sslTest() throws IOException {
        HttpGet get = new HttpGet(DefaultServer.getDefaultServerSSLAddress() + "/dumprunwarrequest");
        LOG.info("Get request for SSL address:" + get.getURI());
        HttpResponse result = DefaultServer.getClient().execute(get);
        assertEquals(StatusCodes.OK, result.getStatusLine().getStatusCode());
        String response = HttpClientUtils.readResponse(result);
        LOG.info(response);
        JSONObject responseData = (JSONObject) JSONValue.parse(response);
        assertTrue(Boolean.parseBoolean(responseData.get("isHTTPS").toString()), responseData.toJSONString());
    }


}

