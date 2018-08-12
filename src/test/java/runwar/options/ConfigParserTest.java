package runwar.options;

import java.io.File;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

public class ConfigParserTest {

    @Test
    public void testParseArgumentsOverrideConfigArguments() {
        ServerOptions serverOptions = CommandLineHandler.parseArguments("-c src/test/resources/server.json -p 9999".split(" "));
        assertNotNull(serverOptions);
        assertNotNull(serverOptions.configFile());
        assertEquals(serverOptions.configFile().getPath(), "src/test/resources/server.json");
    }
    
    @Test
    public void testParseConfig() {
        ConfigParser configParser = new ConfigParser(new File("src/test/resources/server.json"));
        ServerOptions serverOptions = configParser.getServerOptions();
        assertNotNull(serverOptions);
        assertEquals(8080, serverOptions.httpPort());
        assertEquals(new File("src/test/resources/./war/simple.war/").getAbsolutePath(), serverOptions.warFile().getAbsolutePath());
        assertEquals(240 * 1000, serverOptions.launchTimeout());
        assertEquals(50123, serverOptions.stopPort());
        assertEquals("DEBUG", serverOptions.logLevel());
        assertEquals("./logs", serverOptions.logDir().getPath());
        assertEquals(2, serverOptions.trayConfigJSON().size());
        assertEquals("/first=local/path,/absolutepath=/absolute", serverOptions.contentDirs());
        assertEquals("127.0.1.1", serverOptions.host());
        assertEquals(3, serverOptions.welcomeFiles().length);
        assertEquals("index.cfm", serverOptions.welcomeFiles()[0]);
        assertEquals("/path/to/500.html", serverOptions.errorPages().get(500));
        assertEquals("/path/to/404.html", serverOptions.errorPages().get(404));
        assertEquals("/path/to/default.html", serverOptions.errorPages().get(1));
    }

}
