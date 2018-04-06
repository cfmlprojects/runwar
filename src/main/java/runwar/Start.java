package runwar;

import io.undertow.Undertow;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.ResponseCodeHandler;
import io.undertow.server.handlers.proxy.LoadBalancingProxyClient;
import io.undertow.server.handlers.proxy.ProxyHandler;
import io.undertow.util.Headers;

import java.io.File;
import java.net.Socket;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;


import java.net.URLDecoder;

import runwar.logging.LoggerFactory;
import runwar.logging.RunwarLogger;
import runwar.options.CommandLineHandler;
import runwar.options.ConfigParser;
import runwar.options.ServerOptions;

public class Start {


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
        RunwarLogger.LOG.info("Starting instance: " + host + " on port "+ schemeHostAndPort[2]);
        LaunchUtil.relaunchAsBackgroundProcess(serverOptions.setHost(host)
                .setPortNumber(port).setSocketNumber(stopPort).setLoadBalance(""), false);
	    
	}

	public static boolean containsCaseInsensitive(List<String> l, String s){
        return l.stream().anyMatch(x -> x.equalsIgnoreCase(s) || x.equalsIgnoreCase('-' + s));
    }

	public static void main(String[] args) throws Exception {
        ServerOptions serverOptions = CommandLineHandler.parseLogArguments(args);
        LoggerFactory.configure(serverOptions);
        if(args.length == 0) {
            if(new File("server.json").exists()) {
                serverOptions = new ConfigParser(new File("server.json")).getServerOptions();
            } else {
                serverOptions = CommandLineHandler.parseArguments(args); // print usage
            }
        } else {
            serverOptions = CommandLineHandler.parseArguments(args);
        }
        if(serverOptions.getLoadBalance() != null && serverOptions.getLoadBalance().length > 0) {
            final List<String> balanceHosts = new ArrayList<String>();
            RunwarLogger.LOG.info("Initializing...");
            final LoadBalancingProxyClient loadBalancer = new LoadBalancingProxyClient();
            for(String balanceHost : serverOptions.getLoadBalance()) {
                if(serverOptions.getWarFile() != null) {
                    RunwarLogger.LOG.info("Starting instance of " + serverOptions.getWarFile().getParent() +"...");
                    launchServer(balanceHost,serverOptions);
                }
                loadBalancer.addHost(new URI(balanceHost));
                balanceHosts.add(balanceHost);
                RunwarLogger.LOG.info("Added balanced host: " + balanceHost);
                Thread.sleep(3000);
            }
            int port = serverOptions.getPortNumber();
            loadBalancer.setConnectionsPerThread(20);
            RunwarLogger.LOG.info("Hosts loaded");

            RunwarLogger.LOG.info("Starting load balancer on 127.0.0.1 port " + port + "...");
            ProxyHandler proxyHandler = ProxyHandler.builder().setProxyClient(loadBalancer)
                    .setMaxRequestTime(30000).setNext(ResponseCodeHandler.HANDLE_404).build();
            Undertow reverseProxy = Undertow.builder().addHttpListener(port, "localhost").setIoThreads(4)
                    .setHandler(proxyHandler).build();
            reverseProxy.start();
            RunwarLogger.LOG.info("View balancer admin on http://127.0.0.1:9080");
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
                                int port = schemeHostAndPort.length >2 ? Integer.parseInt(schemeHostAndPort[2]) : 80 ;
                                response += balanceHost + " <a href='?removeHost=" + balanceHost + "'>remove</a> Listening: "+serverListening(host, port)+"<br/>";
                            }
                            exchange.getResponseSender().send("<h3>Balanced Hosts</h3><form action='?'> Add Host:<input type='text' name='addHost' placeholder='http://127.0.0.1:7070'><input type='submit'></form>" + response);
                        }
                    }).build();
            adminServer.start();
            
            
            RunwarLogger.LOG.info("Started load balancer.");
            
        } else {
            
            Server server = new Server();
            try {
                server.startServer(serverOptions);
            } catch (Exception e) {
                e.printStackTrace();
                System.exit(1);
            }
            
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
