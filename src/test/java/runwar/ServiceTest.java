package runwar;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import runwar.options.ServerOptionsImpl;
import testutils.DefaultServer;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;

class ServiceTest {

    private static final Path workDir = Paths.get("src/test/resources/work/");

    @BeforeAll
    static void beforeAll() throws IOException {
        if (Files.exists(workDir)) {
            Files.walk(workDir)
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
        }
        workDir.toFile().mkdir();
    }

//    @Test
//    void generateServiceScripts() throws IOException {
//        ServerOptionsImpl serverOptions = DefaultServer.getServerOptions();
//        serverOptions.contentDirs("../pub,../src");
//        new Service(serverOptions).generateServiceScripts(workDir);
//    }

//    @Ignore
//    @Test
//    void getServiceScriptCommands() throws IOException {
//        ServerOptionsImpl serverOptions = DefaultServer.getServerOptions();
//        serverOptions.contentDirs("../pub,../src");
//        Service service =  new Service(serverOptions);
//        service.serviceScriptCommands(service.serviceControlScriptPath());
//    }
}
