package runwar.server;

import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import runwar.options.ServerOptions;
import runwar.options.ServerOptionsImpl;
import testutils.DefaultServer;

import java.io.File;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith({DefaultServer.class})
public abstract class AbstractServerTest {

    private static final String WARPATH = "src/test/resources/war/simple.war";

    public static ServerOptions getDefaultServerOptions() {
        return new ServerOptionsImpl().warFile(new File(WARPATH))
                .debug(true)
//                    .logLevel("TRACE")
                .background(false)
                .httpPort(0)
                .stopPort(0)
                .sslPort(0)
                .http2ProxySSLPort(0)
                .trayEnable(false)
                .debug(true).background(false);
    }

    abstract ServerOptions getServerOptions();

}

