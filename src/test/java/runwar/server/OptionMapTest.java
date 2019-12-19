package runwar.server;

import io.undertow.util.StatusCodes;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.junit.jupiter.api.Test;
import runwar.options.ServerOptions;
import testutils.DefaultServer;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static runwar.logging.RunwarLogger.LOG;

public class OptionMapTest extends AbstractServerTest {

    public ServerOptions getServerOptions() {
        return getDefaultServerOptions()
                .serverName("optionMapTest")
                .xnioOptions("WORKER_IO_THREADS=16,TCP_NODELAY=false")
                .undertowOptions("MAX_PARAMETERS=1");
    }

    @Test
    public void maxParametersExceededRequest() throws IOException {
        HttpGet get = new HttpGet(DefaultServer.getDefaultServerURL() + "/dumprunwarrequest?wee=1&hoo=2");
        LOG.info("Get request :" + get.getURI());
        HttpResponse result = DefaultServer.getClient().execute(get);
        assertEquals(StatusCodes.BAD_REQUEST, result.getStatusLine().getStatusCode());
    }

    @Test
    public void maxParametersRequest() throws IOException {
        HttpGet get = new HttpGet(DefaultServer.getDefaultServerURL() + "/dumprunwarrequest?wee=1");
        LOG.info("Get request :" + get.getURI());
        HttpResponse result = DefaultServer.getClient().execute(get);
        assertEquals(StatusCodes.OK, result.getStatusLine().getStatusCode());
    }

}

