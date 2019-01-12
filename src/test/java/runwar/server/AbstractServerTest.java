package runwar.server;

import org.junit.jupiter.api.extension.ExtendWith;
import runwar.options.ServerOptions;
import testutils.DefaultServer;

@ExtendWith({DefaultServer.class})
abstract class AbastractServerTest {

    ServerOptions serverOptions;

}

