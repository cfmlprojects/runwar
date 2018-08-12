package runwar.server;

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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static runwar.logging.RunwarLogger.LOG;

@ExtendWith({SSLSelfSignTest.ServerConfig.class, DefaultServer.class})
public class SSLSelfSignTest {


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

    @ExtendWith({DefaultServer.class})
    public static class ServerConfig implements BeforeAllCallback {
        @Override
        public void beforeAll(ExtensionContext context) throws Exception {
//            new File(DefaultServer.warFile("default"),"/WEB-INF/selfsign.crt").delete();
//            new File(DefaultServer.warFile("default"),"/WEB-INF/selfsign.key").delete();

            DefaultServer.resetServerOptions()
                    .serverName("sslSelfSignTest")
                    .sslEnable(true)
                    .sslSelfSign(true)
                    .sslPort(1553);

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

        }

    }


}

