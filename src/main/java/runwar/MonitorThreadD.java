package runwar;

import runwar.options.ServerOptions;
import runwar.util.PortRequisitioner;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;

import static runwar.logging.RunwarLogger.MONITOR_LOG;
import static runwar.Server.bar;

class MonitorThreadD extends Thread {

    private static final Thread mainThread = Thread.currentThread();
    private final PortRequisitioner ports;
    private final Server server;
    private char[] stopPassword;
    private volatile boolean listening = false;
    private static ServerOptions serverOptions;
    private Thread shutDownThread;

    MonitorThreadD(Server server) {
        this.server = server;
        serverOptions = server.getServerOptions();
        this.stopPassword = serverOptions.stopPassword();
        this.ports = server.getPorts();
        setDaemon(true);
        setName("StopMonitor");
        // add shutdown hook
        addShutDownHook();
    }

    @Override
    public void run() {
        try(ServerSocket serverSocket = new ServerSocket(ports.get("stop").socket, 1, Server.getInetAddress(serverOptions.host()))) {
            listening = true;
            MONITOR_LOG.info(bar);
            MONITOR_LOG.info("*** starting 'stop' listener thread - Host: " + serverOptions.host()
                    + " - Socket: " + ports.get("stop").socket);
            MONITOR_LOG.info(bar);
            while (listening) {
                MONITOR_LOG.debug("StopMonitor listening for password");
                if (server.getServerState().equals(Server.ServerState.STOPPED) || server.getServerState().equals(Server.ServerState.STOPPING)) {
                    MONITOR_LOG.debug("Server is in a stopping state, stopping listing for shutdown call");
                    listening = false;
                }
                try (Socket clientSocket = serverSocket.accept()) {
                    int r, i = 0;
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()))) {
                        while (listening && (r = reader.read()) != -1) {
                            char ch = (char) r;
                            if (stopPassword.length > i && ch == stopPassword[i]) {
                                i++;
                            } else {
                                i = 0; // prevent prefix only matches
                            }
                        }
                        if (i == stopPassword.length) {
                            //Daevil.log.debug("Password matched, stopping");
                            listening = false;
                        } else {
                            if (listening) {
                                MONITOR_LOG.warn("Incorrect password used when trying to stop server.");
                            } else {
                                MONITOR_LOG.debug("stopped listening for stop password.");
                            }

                        }
                    } catch (SocketException e) {
                        // reset
                        e.printStackTrace();
                        MONITOR_LOG.debug(e);
                    }
                }
            }
        } catch (Exception e) {
            MONITOR_LOG.error(e);
            e.printStackTrace();
        } finally {
            MONITOR_LOG.trace("Closed stop socket");
            try {
                if (mainThread.isAlive()) {
                    MONITOR_LOG.trace("monitor joining main thread");
                    try {
                        mainThread.interrupt();
                        mainThread.join();
                    } catch (InterruptedException ie) {
                        MONITOR_LOG.trace(ie);
                    }
                } else {
                    MONITOR_LOG.trace("main thread is dead, not joining");
                    if(shutDownThread != null && Thread.currentThread() != shutDownThread) {
                        MONITOR_LOG.trace("shutdown hook is there-- running it");
                        shutDownThread.run();
                    }
                }
            } catch (Exception e) {
                MONITOR_LOG.error(e);
                e.printStackTrace();
            }
        }
    }

    void stopListening() {
        listening = false;
        MONITOR_LOG.trace("Stopping listening");
        // send a char to the reader so it will stop waiting
        try (Socket s = new Socket(Server.getInetAddress(serverOptions.host()), ports.get("stop").socket)) {
            try (OutputStream out = s.getOutputStream()) {
                for (char aStopPassword : stopPassword) {
                    out.write(aStopPassword);
                }
                out.flush();
            }
        } catch (IOException e) {
            e.printStackTrace();
            // expected if already stopping
            //MONITOR_LOG.trace(e);
        }
    }

    void removeShutDownHook() {
        if(shutDownThread != null && Thread.currentThread() != shutDownThread) {
            if(Runtime.getRuntime().removeShutdownHook(shutDownThread)){
                MONITOR_LOG.debug("Removed shutdown hook");
            } else {
                MONITOR_LOG.trace("Already removed shutdown hook, or it wasn't there yet.");
            }
        }
    }

    private void addShutDownHook() {
        if(shutDownThread == null) {
            shutDownThread = new Thread(() -> {
                MONITOR_LOG.debug("Running shutdown hook");
                try {
                    if(!server.getServerState().equals(Server.ServerState.STOPPING) && !server.getServerState().equals(Server.ServerState.STOPPED)) {
                        MONITOR_LOG.debug("shutdown hook:stopServer()");
                        server.stopServer();
                    }
                    if(mainThread.isAlive()) {
                        MONITOR_LOG.debug("shutdown hook joining main thread from shutdown hook");
                        mainThread.interrupt();
                        mainThread.join();
                    }
                } catch ( Exception e) {
                    e.printStackTrace();
                }
                MONITOR_LOG.debug("Shutdown hook finished");
            });
            Runtime.getRuntime().addShutdownHook(shutDownThread);
            MONITOR_LOG.debug("Added shutdown hook");
        }
    }


}

