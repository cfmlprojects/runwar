package runwar.options;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ServerOptionsTest {

    @Test
    public void testToJsonString() {
        ServerOptionsImpl serverOptions = new ServerOptionsImpl();
        System.out.println(serverOptions.toJson());
    }

    @Test
    public void testGeneratedConfigWithConfigParser() {
        ServerOptionsImpl serverOptions = new ServerOptionsImpl();
        serverOptions.contentDirs("../pub,../src");
        String json = serverOptions.toJson();
        System.out.println(json);
        serverOptions = (ServerOptionsImpl) new ConfigParser(json).getServerOptions();
        assertNotNull(serverOptions);
        assertNotNull(serverOptions.contentDirectories());
        assertEquals(2, serverOptions.contentDirectories().size());
    }
}