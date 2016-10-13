
import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.Test;

import runwar.Server;

public class ServerTest {
   
    public ServerTest() {
    }

    @Test
    public void testAliasMapProcessed() {
        Server server = new Server();
        try {
            server.startServer(new String[]{
//                    "-war", "/home/valliant/.CommandBox/server/A0D069B465368745062AAD7343543B93-3.2.1-snapshot/adobe-2016.0.02.299200/",
                    "-war", "/home/valliant/.CommandBox/server/A656C84280FCD8775CA53AED75AF4ED4-3.2.1-snapshot/",
                    "--lib-dirs", "/home/valliant/.CommandBox/server/A656C84280FCD8775CA53AED75AF4ED4-3.2.1-snapshot/lucee-5.0.0.252/WEB-INF/lib",
                    "-b", "false"
                    });
            server.stopServer();
//            System.exit(0);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }
    

}
