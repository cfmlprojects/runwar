package runwar.server;

import java.io.File;
import runwar.Server;
import runwar.Stop;
import runwar.options.ServerOptions;
import runwar.options.ServerOptionsImpl;
import testutils.DefaultServer;

import org.junit.jupiter.api.Test;

public class StartStopTest {

    public StartStopTest() {
    }

    @Test
    public void testAliasMapProcessed() {
        ServerOptions serverOptions = new ServerOptionsImpl();
        serverOptions.setWarFile(new File(DefaultServer.WARPATH)).setDebug(true).setBackground(false)
                .setTrayEnabled(false);
        Server server = new Server();
        try {
            server.startServer(serverOptions);
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            Stop.stopServer(serverOptions, false);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
