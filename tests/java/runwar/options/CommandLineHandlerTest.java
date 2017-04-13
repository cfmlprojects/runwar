package runwar.options;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import org.junit.Test;

import runwar.options.ServerOptions;

public class CommandLineHandlerTest {

    @Test
    public void testParseArgumentsStringArray() {
        ServerOptions serverOptions = CommandLineHandler.parseArguments("-c tests/resource/server.json".split(" "));
        assertNotNull(serverOptions);
        assertNotNull(serverOptions.getConfigFile());
        assertEquals(serverOptions.getConfigFile().getPath(), "tests/resource/server.json");
    }

    @Test
    public void testParseArgumentsOverrideConfigArguments() {
        ServerOptions serverOptions = CommandLineHandler.parseArguments("-c tests/resource/server.json -p 9999".split(" "));
        assertNotNull(serverOptions);
        assertNotNull(serverOptions.getConfigFile());
        assertEquals(serverOptions.getConfigFile().getPath(), "tests/resource/server.json");
        assertEquals(serverOptions.getPortNumber(), 9999);
    }
    
    @Test
    public void testConfigParserConfigFile() {
        File configFile = new File("tests/resource/server.json");
        assertTrue(configFile.exists());
        ServerOptions serverOptions = new ConfigParser(configFile).getServerOptions();
        assertNotNull(serverOptions);
        assertNotNull(serverOptions.getConfigFile());
        assertEquals(serverOptions.getConfigFile().getPath(), "tests/resource/server.json");
    }

    @Test
    public void testConfigParserBadConfigFileFormat() throws IOException {
        File configFile = new File("tests/resource/server.bad.json");
        String configFilePath = configFile.getCanonicalPath();
        assertTrue(configFile.exists());
        try{
            ServerOptions serverOptions = new ConfigParser(configFile).getServerOptions();
            assertEquals(serverOptions,null);
            fail();
        } catch (RuntimeException e) {
            assertEquals("Could not load "+ configFilePath + " : Unexpected End Of File position 79: null",e.getMessage());
        }
       
    }

    @Test
    public void testSetBasicAuthUsers() throws IOException {
        ServerOptions serverOptions = new ServerOptions();
        serverOptions.setBasicAuth("bob=secret,alice=fun,equals=blah\\=inpass");
        Map<String,String> upMap = serverOptions.getBasicAuth();
        assertEquals(upMap.get("bob"), "secret");
        assertEquals(upMap.get("alice"), "fun");
        assertEquals(upMap.get("equals"), "blah=inpass");
    }
    
}
