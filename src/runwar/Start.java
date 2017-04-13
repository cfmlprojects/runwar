package runwar;

import io.undertow.Undertow;
import io.undertow.client.UndertowClient;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.ResponseCodeHandler;
import io.undertow.server.handlers.proxy.LoadBalancingProxyClient;
import io.undertow.server.handlers.proxy.LoadBalancingProxyClient.Host;
import io.undertow.server.handlers.proxy.ProxyHandler;
import io.undertow.util.Headers;

import java.net.Socket;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.net.URLDecoder;

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
        if(schemeHostAndPort.length != 3) {
            throw new RuntimeException("hosts for balancehost should have scheme, host and port, e.g.: http://127.0.0.1:55555");
        }
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
            final List<String> balanceHosts = new ArrayList<String>();
            log.info("Initializing...");
            final LoadBalancingProxyClient loadBalancer = new LoadBalancingProxyClient();
            for(String balanceHost : serverOptions.getLoadBalance()) {
                if(serverOptions.getWarFile() != null) {
                    log.info("Starting instance of " + serverOptions.getWarFile().getParent() +"...");
                    launchServer(balanceHost,serverOptions);
                }
                loadBalancer.addHost(new URI(balanceHost));
                balanceHosts.add(balanceHost);
                log.info("Added balanced host: " + balanceHost);
                Thread.sleep(3000);
            }
            int port = serverOptions.getPortNumber();
            loadBalancer.setConnectionsPerThread(20);
            log.info("Hosts loaded");

            log.info("Starting load balancer on 127.0.0.1 port " + port + "...");
            Undertow reverseProxy = Undertow.builder().addHttpListener(port, "localhost").setIoThreads(4)
                    .setHandler(new ProxyHandler(loadBalancer, 30000, ResponseCodeHandler.HANDLE_404)).build();
            reverseProxy.start();
            log.info("View balancer admin on http://127.0.0.1:9080");
            Undertow adminServer = Undertow.builder()
                    .addHttpListener(9080, "localhost")
                    .setHandler(new HttpHandler() {
                        @Override
                        public void handleRequest(final HttpServerExchange exchange) throws Exception {
                            exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/html");
                            if(exchange.getQueryParameters().get("addHost") != null) {
                                String balanceHost = URLDecoder.decode(exchange.getQueryParameters().get("addHost").toString(),"UTF-8");
                                balanceHost = balanceHost.replaceAll("]|\\[", "");
                                loadBalancer.addHost(new URI(balanceHost));
                                balanceHosts.add(balanceHost);
                            }
                            if(exchange.getQueryParameters().get("removeHost") != null) {
                                String balanceHost = URLDecoder.decode(exchange.getQueryParameters().get("removeHost").toString(),"UTF-8");
                                balanceHost = balanceHost.replaceAll("]|\\[", "");
                                loadBalancer.removeHost(new URI(balanceHost));
                                balanceHosts.remove(balanceHost);
                            }
                            String response = "";
//                            response += URLDecoder.decode(exchange.getQueryString(),"UTF-8");
                            for(String balanceHost : balanceHosts) {
                                String[] schemeHostAndPort = balanceHost.split(":");
                                String host = schemeHostAndPort[1].replaceAll("^//", "");
                                int port = Integer.parseInt(schemeHostAndPort[2]);
                                response += balanceHost + " <a href='?removeHost=" + balanceHost + "'>remove</a> Listening: "+serverListening(host, port)+"<br/>";
                            }
                            exchange.getResponseSender().send("<h3>Balanced Hosts</h3><form action='?'> Add Host:<input type='text' name='addHost' value='http://127.0.0.1:7070'><input type='submit'></form>" + response);
                        }
                    }).build();
            adminServer.start();
            
            
            log.info("Started load balancer.");
            
        } else {
            
            Server server = new Server();
            server.startServer(serverOptions);
            
        }
	}
	
    public static boolean serverListening(String host, int port) {
        Socket s = null;
        try {
            s = new Socket(host, port);
            return true;
        } catch (Exception e) {
            return false;
        } finally {
            if (s != null)
                try {
                    s.close();
                } catch (Exception e) {
                }
        }
    }
}
