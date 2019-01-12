package runwar.server;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import runwar.Server;
import runwar.options.ServerOptions;
import testutils.DefaultServer;

import javax.servlet.ServletContext;
import java.io.File;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class CustomWebInfTest extends AbstractServerTest {

    Server server;

    public ServerOptions getServerOptions() {
        return getDefaultServerOptions()
                .webInfDir(new File("src/test/resources/war/customWebInf/WEB-INF"))
                .serverName("basicAuth");
    }

    public void setServer(Server server) {
        this.server = server;
    }

    @Test
    public void testRealPath() {
        ServletContext servletContext = server.getManager().getDeployment().getServletContext();
        String realPath = servletContext.getRealPath("/");
        String basePath = DefaultServer.getWarFile("default").getAbsolutePath();

        String customWebInfPath = new File("src/test/resources/war/customWebInf/WEB-INF").getAbsolutePath();
        assertEquals(basePath, realPath);

        realPath = servletContext.getRealPath("/notthere");
        assertEquals(basePath + "/notthere", realPath);

        realPath = servletContext.getRealPath("/WEB-INF");
        assertEquals(customWebInfPath, realPath);

        realPath = servletContext.getRealPath("/WEB-INF/notthere");
        assertEquals(customWebInfPath + "/notthere", realPath);

    }


}