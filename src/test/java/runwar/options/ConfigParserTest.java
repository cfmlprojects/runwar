package runwar.options;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

import testutils.DefaultServer;

public class ConfigParserTest {

    @Test
    public void testParseArgumentsOverrideConfigArguments() {
        ServerOptions serverOptions = CommandLineHandler.parseArguments("-c src/test/resources/server.json -p 9999".split(" "));
        assertNotNull(serverOptions);
        assertNotNull(serverOptions.getConfigFile());
        assertEquals(serverOptions.getConfigFile().getPath(), "src/test/resources/server.json");
    }
    
    @Test
    public void testParseConfig() {
        ConfigParser configParser = new ConfigParser(new File("src/test/resources/server.json"));
        ServerOptions serverOptions = configParser.getServerOptions();
        assertNotNull(serverOptions);
        assertEquals(8080, serverOptions.getPortNumber());
        assertEquals(new File("src/test/resources/./war/simple.war/").getAbsolutePath(), serverOptions.getWarFile().getAbsolutePath());
        assertEquals(240 * 1000, serverOptions.getLaunchTimeout());
        assertEquals(50123, serverOptions.getSocketNumber());
        assertEquals("DEBUG", serverOptions.getLoglevel());
        assertEquals("./logs", serverOptions.getLogDir().getPath());
        assertEquals(2, serverOptions.getTrayConfigJSON().size());
        assertEquals("/first=local/path,/absolutepath=/absolute", serverOptions.getCfmlDirs());
        assertEquals("127.0.1.1", serverOptions.getHost());
        assertEquals(3, serverOptions.getWelcomeFiles().length);
        assertEquals("index.cfm", serverOptions.getWelcomeFiles()[0]);
        assertEquals("/path/to/500.html", serverOptions.getErrorPages().get(500));
        assertEquals("/path/to/404.html", serverOptions.getErrorPages().get(404));
        assertEquals("/path/to/default.html", serverOptions.getErrorPages().get(1));
    }

}
