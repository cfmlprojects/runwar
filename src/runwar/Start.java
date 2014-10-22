package runwar;

public class Start {

	// for openBrowser 
	public Start(int seconds) {
	    new Server(seconds);
	}

	public static void main(String[] args) throws Exception {
	    Server.startServer(args);
	}
	
}
