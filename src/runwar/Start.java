package runwar;

import io.undertow.Undertow;
import io.undertow.server.handlers.ResponseCodeHandler;
import io.undertow.server.handlers.proxy.LoadBalancingProxyClient;
import io.undertow.server.handlers.proxy.ProxyHandler;

import java.net.URI;

import runwar.logging.Logger;
import runwar.options.CommandLineHandler;
import runwar.options.ServerOptions;

public class Start {

    private static Logger log = Logger.getLogger("RunwarLogger");

    // for openBrowser 
	public Start(int seconds) {
	    new Server(seconds);
	}

	private static void launchServer(String balanceHost, ServerOptions serverOptions){
        String[] schemeHostAndPort = balanceHost.split(":");
        String host = schemeHostAndPort[1].replaceAll("^//", "");
        int port = Integer.parseInt(schemeHostAndPort[2]);
        int stopPort = port+1;
        log.info("Starting instance: " + host + " on port "+ schemeHostAndPort[2]);
        LaunchUtil.relaunchAsBackgroundProcess(serverOptions.setHost(host)
                .setPortNumber(port).setSocketNumber(stopPort).setLoadBalance(""), false);
	    
	}
	
	public static void main(String[] args) throws Exception {
		ServerOptions serverOptions = CommandLineHandler.parseArguments(args);
        if(serverOptions.getLoadBalance() != null && serverOptions.getLoadBalance().length > 0) {
            log.info("Starting instances...");
            LoadBalancingProxyClient loadBalancer = new LoadBalancingProxyClient();
            int i = 0;
            for(String balanceHost : serverOptions.getLoadBalance()) {
                if(serverOptions.getWarFile() != null) {
                    launchServer(balanceHost,serverOptions);
                }
                loadBalancer.addHost(new URI(balanceHost));
                i++;
                Thread.sleep(3000);
            }
            loadBalancer.setConnectionsPerThread(20);
            log.info("Started instances.");

            log.info("Starting load balancer...");
            Undertow reverseProxy = Undertow.builder().addHttpListener(8080, "localhost").setIoThreads(4)
                    .setHandler(new ProxyHandler(loadBalancer, 30000, ResponseCodeHandler.HANDLE_404)).build();
            reverseProxy.start();
            log.info("Started load balancer.");
        } else {
            Server server = new Server();
            server.startServer(serverOptions);
            
        }
	}
	
}
