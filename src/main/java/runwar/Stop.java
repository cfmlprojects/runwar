package runwar;

import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;

import runwar.options.CommandLineHandler;
import runwar.options.ServerOptions;

public class Stop {

    public static void main(String[] args) throws Exception {
        stopServer(args, true);
    }

    public static void stopServer(String[] args, boolean andExit) throws Exception {
        stopServer(CommandLineHandler.parseArguments(args), andExit);
    }
    
    public static void stopServer(ServerOptions serverOptions, boolean andExit) throws Exception {
        int socketNumber = serverOptions.getSocketNumber();
        String host = serverOptions.getHost();
        char[] stoppassword = serverOptions.getStopPassword();
        try {
            InetAddress addr = InetAddress.getByName(host);
            Socket s = new Socket(addr, socketNumber);
            OutputStream out = s.getOutputStream();
            System.out.println("*** sending stop request to socket : " + socketNumber);
            for (int i = 0; i < stoppassword.length; i++) {
                out.write(stoppassword[i]);
            }
            out.flush();
            out.close();
            s.close();
            if (!Server.serverWentDown(10000, 500, addr, socketNumber)) {
                System.err
                        .println("Timeout stopping server.  Did you set a stop-password, and are you passing it?  Check the log for more information.");
                System.exit(1);
            }
        } catch (Exception e) {
            System.err
                    .println("Could not stop server.  Are you sure it is running, and listing for stop requests on port "
                            + socketNumber + "?");
            System.exit(1);
        }
        System.out.println("*** stopped.");
        System.out.println(Server.bar);
        if (andExit) {
            System.exit(0);
        }
        Thread.currentThread().interrupt();
        return;
    }

}
