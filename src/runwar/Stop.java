package runwar;

import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;

public class Stop {

    public static void main(String[] args) throws Exception {
    	int socketNumber = Integer.parseInt(args[0]);
        String host = args[1];
        try{
        	Socket s = new Socket(InetAddress.getByName(host), socketNumber);
	        OutputStream out = s.getOutputStream();
	        System.out.println("*** sending stop request to socket : " + socketNumber);
        	out.write(("\r\n").getBytes());
        	out.flush();
        	s.close();
        	out.close();
        } catch (Exception e) {
        	System.err.println("Could not stop server.  Are you sure it is running, and listing for stop requests on port "+socketNumber+"?");
            System.exit(1);
        }
        System.out.println("*** stopped.");
        System.out.println(Server.bar);
        System.exit(0);
    }

}
