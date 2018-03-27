package runwar.options;


import java.io.File;
import java.io.IOException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

import testutils.DefaultServer;

public class CommandLineHandlerTest {

    @Test
    public void testParseArgumentsStringArray() {
        ServerOptions serverOptions = CommandLineHandler.parseArguments("-c src/test/resources/server.json".split(" "));
        assertNotNull(serverOptions);
        assertNotNull(serverOptions.getConfigFile());
        assertEquals(serverOptions.getConfigFile().getPath(), "src/test/resources/server.json");
    }

    @Test
    public void testParseArgumentsOverrideConfigArguments() {
        ServerOptions serverOptions = CommandLineHandler.parseArguments("-c src/test/resources/server.json -p 9999".split(" "));
        assertNotNull(serverOptions);
        assertNotNull(serverOptions.getConfigFile());
        assertEquals(serverOptions.getConfigFile().getPath(), "src/test/resources/server.json");
        assertEquals(serverOptions.getPortNumber(), 9999);
    }
    
    @Test
    public void testParseConfig() {
        ServerOptions serverOptions = CommandLineHandler.parseArguments("-c src/test/resources/server.json -p 9999".split(" "));
        assertNotNull(serverOptions);
        assertNotNull(serverOptions.getConfigFile());
        assertEquals(serverOptions.getConfigFile().getPath(), "src/test/resources/server.json");
        assertEquals(serverOptions.getPortNumber(), 9999);
    }

    @Test
    public void testURLRewriteArguments() {
        String argString = "-war " + DefaultServer.SIMPLEWARPATH + " -urlrewritecheck 0 -urlrewritestatuspath stats";
        ServerOptions serverOptions = CommandLineHandler.parseArguments(argString.split(" "));
        assertEquals(serverOptions.getURLRewriteCheckInterval(), "0");
        assertEquals(serverOptions.getURLRewriteStatusPath(), "/stats");
    }

    @Test
    public void testConfigParserConfigFile() {
        File configFile = new File("src/test/resources/server.json");
        assertTrue(configFile.exists());
        ServerOptions serverOptions = new ConfigParser(configFile).getServerOptions();
        assertNotNull(serverOptions);
        assertNotNull(serverOptions.getConfigFile());
        assertEquals(serverOptions.getConfigFile().getPath(), "src/test/resources/server.json");
    }

    @Test
    public void testConfigParserBadConfigFileFormat() throws IOException {
        File configFile = new File("src/test/resources/server.bad.json");
        String configFilePath = configFile.getCanonicalPath();
        assertTrue(configFile.exists());
        try{
            ServerOptions serverOptions = new ConfigParser(configFile).getServerOptions();
            assertEquals(serverOptions,null);
            fail("Nopers");
        } catch (RuntimeException e) {
            assertEquals("Could not load "+ configFilePath + " : Unexpected End Of File position 79: null",e.getMessage());
        }
       
    }

    @Test
    public void testSetBasicAuthUsers() throws IOException {
        ServerOptions serverOptions = new ServerOptionsImpl();
        serverOptions.setBasicAuth("bob=secret,alice=fun,equals=blah\\=inpass");
        Map<String,String> upMap = serverOptions.getBasicAuth();
        assertEquals(upMap.get("bob"), "secret");
        assertEquals(upMap.get("alice"), "fun");
        assertEquals(upMap.get("equals"), "blah=inpass");
    }

    @Test
    public void testDebugIsFalse() throws IOException {
        ServerOptions serverOptions = CommandLineHandler.parseArguments("-c src/test/resources/server.json --debug-enable false".split(" "));
        assertFalse(serverOptions.isDebug());
        serverOptions = CommandLineHandler.parseArguments("-c src/test/resources/server.json -debug false".split(" "));
        assertFalse(serverOptions.isDebug());
    }

}
