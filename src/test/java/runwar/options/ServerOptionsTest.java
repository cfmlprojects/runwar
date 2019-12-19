package runwar.options;

import io.undertow.UndertowOptions;
import org.junit.jupiter.api.Test;
import org.xnio.OptionMap;
import org.xnio.Options;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class ServerOptionsTest {

//    @Test
//    public void testToJsonString() {
//        ServerOptionsImpl serverOptions = new ServerOptionsImpl();
//        System.out.println(serverOptions.toJson());
//    }
//
//    @Test
//    public void testGeneratedConfigWithConfigParser() {
//        ServerOptionsImpl serverOptions = new ServerOptionsImpl();
//        serverOptions.contentDirs("../pub,../src");
//        String json = serverOptions.toJson();
//        System.out.println(json);
//        serverOptions = (ServerOptionsImpl) new ConfigParser(json).getServerOptions();
//        assertNotNull(serverOptions);
//        assertNotNull(serverOptions.contentDirectories());
//        assertEquals(2, serverOptions.contentDirectories().size());
//    }

    @Test
    public void testSetXniOptions() {
        ServerOptionsImpl serverOptions = new ServerOptionsImpl();
        assertEquals(true, serverOptions.xnioOptions().getMap().get(Options.TCP_NODELAY));
        serverOptions.xnioOptions("WORKER_IO_THREADS=16,TCP_NODELAY=false");
        OptionMap map = serverOptions.xnioOptions().getMap();
        assertEquals(16, map.get(Options.WORKER_IO_THREADS, 21));
        assertEquals(false, map.get(Options.TCP_NODELAY, true));
    }

    @Test
    public void testSetUndertowOptions() {
        ServerOptionsImpl serverOptions = new ServerOptionsImpl();
        serverOptions.undertowOptions("ENABLE_HTTP2=true,MAX_CONCURRENT_REQUESTS_PER_CONNECTION=21");
        OptionMap map = serverOptions.undertowOptions().getMap();
        assertEquals(21, map.get(UndertowOptions.MAX_CONCURRENT_REQUESTS_PER_CONNECTION, 99));
        assertEquals(true, map.get(UndertowOptions.ENABLE_HTTP2, false));
    }
}