package runwar.options;

import static org.junit.Assert.*;

import org.junit.Test;

import runwar.options.ServerOptions;

public class CommandLineHandlerTest {

    @Test
    public void testParseArgumentsStringArray() {
        ServerOptions serverOptions = CommandLineHandler.parseArguments("-c tests/resource/server.json".split(" "));
        assertEquals(serverOptions.getConfigFile().getPath(),"tests/resource/server.json");
    }

}
