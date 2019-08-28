package runwar.options;


import io.undertow.UndertowOptions;
import org.junit.jupiter.api.Test;
import testutils.DefaultServer;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class CommandLineHandlerTest {

    @Test
    public void testParseArgumentsStringArray() {
        ServerOptions serverOptions = CommandLineHandler.parseArguments("-c src/test/resources/server.json".split(" "));
        assertNotNull(serverOptions);
        assertNotNull(serverOptions.configFile());
        assertEquals(serverOptions.configFile().getPath(), "src/test/resources/server.json");
    }

    @Test
    public void testParseArgumentsOverrideConfigArguments() {
        ServerOptions serverOptions = CommandLineHandler.parseArguments("-c src/test/resources/server.json -p 9999".split(" "));
        assertNotNull(serverOptions);
        assertNotNull(serverOptions.configFile());
        assertEquals(serverOptions.configFile().getPath(), "src/test/resources/server.json");
        assertEquals(serverOptions.httpPort(), 9999);
    }

    @Test
    public void testParseConfig() {
        ServerOptions serverOptions = CommandLineHandler.parseArguments("-c src/test/resources/server.json".split(" "));
        assertEquals(serverOptions.configFile().getPath(), "src/test/resources/server.json");
    }

    @Test
    public void testURLRewriteArguments() {
        String argString = "-war " + DefaultServer.WARPATH + " -urlrewritecheck 0 -urlrewritestatuspath stats";
        ServerOptions serverOptions = CommandLineHandler.parseArguments(argString.split(" "));
        assertEquals(serverOptions.urlRewriteCheckInterval(), "0");
        assertEquals(serverOptions.urlRewriteStatusPath(), "/stats");
    }

    @Test
    public void testConfigParserConfigFile() {
        File configFile = new File("src/test/resources/server.json");
        assertTrue(configFile.exists());
        ServerOptions serverOptions = new ConfigParser(configFile).getServerOptions();
        assertNotNull(serverOptions);
        assertNotNull(serverOptions.configFile());
        assertEquals(serverOptions.configFile().getPath(), "src/test/resources/server.json");
    }

    @Test
    public void testConfigParserBadConfigFileFormat() throws IOException {
        File configFile = new File("src/test/resources/server.bad.json");
        String configFilePath = configFile.getCanonicalPath();
        assertTrue(configFile.exists());
        try {
            ServerOptions serverOptions = new ConfigParser(configFile).getServerOptions();
            assertEquals(serverOptions, null);
            fail("Nopers");
        } catch (RuntimeException e) {
            assertEquals("Could not load " + configFilePath + " : Unexpected End Of File position 79: null", e.getMessage());
        }

    }

    @Test
    public void testSetBasicAuthUsers() {
        ServerOptions serverOptions = new ServerOptionsImpl();
        serverOptions.basicAuth("bob=secret,alice=fun,equals=blah\\=inpass");
        Map<String, String> upMap = serverOptions.basicAuth();
        assertEquals(upMap.get("bob"), "secret");
        assertEquals(upMap.get("alice"), "fun");
        assertEquals(upMap.get("equals"), "blah=inpass");
    }

    @Test
    public void testDebugIsFalse() {
        ServerOptions serverOptions = CommandLineHandler.parseArguments("-c src/test/resources/server.json --debug-enable false".split(" "));
        assertFalse(serverOptions.debug());
        serverOptions = CommandLineHandler.parseArguments("-c src/test/resources/server.json -debug false".split(" "));
        assertFalse(serverOptions.debug());
    }

    @Test
    public void testUndertowOptions() {
        ServerOptions serverOptions = CommandLineHandler.parseArguments("-c src/test/resources/server.json --undertow-options \"MAX_AJP_PACKET_SIZE=65536\"".split(" "));
        assertEquals(65536, serverOptions.undertowOptions().getMap().get(UndertowOptions.MAX_AJP_PACKET_SIZE, 0));
        serverOptions = CommandLineHandler.parseArguments("-c src/test/resources/server.json --undertow-options \"MAX_AJP_PACKET_SIZE=336,ALWAYS_SET_KEEP_ALIVE=true,no_request_timeout=60000\"".split(" "));
        assertEquals(336, serverOptions.undertowOptions().getMap().get(UndertowOptions.MAX_AJP_PACKET_SIZE, 0));
        assertEquals(true, serverOptions.undertowOptions().getMap().get(UndertowOptions.ALWAYS_SET_KEEP_ALIVE, false));
        assertEquals(60000, serverOptions.undertowOptions().getMap().get(UndertowOptions.NO_REQUEST_TIMEOUT, 0));
    }

    @Test
    public void testGetCommandLineArgs() {
        ServerOptionsImpl serverOptions = new ServerOptionsImpl();
        serverOptions.commandLineArgs("-war src/test/resources/war/simple.war -b false -pidfile \"/some/file\"".split(" "));
        assertEquals(12, serverOptions.commandLineArgs().length);
    }

}
