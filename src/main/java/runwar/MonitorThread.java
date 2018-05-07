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

import static runwar.logging.RunwarLogger.LOG;
import static runwar.Server.bar;

class MonitorThread extends Thread {

    private static final Thread mainThread = Thread.currentThread();
    private final PortRequisitioner ports;
    private final Server server;
    private char[] stopPassword;
    private volatile boolean listening = false;
    private static ServerOptions serverOptions;
    private Thread shutDownThread;

    MonitorThread(Server server) {
        this.server = server;
        serverOptions = Server.getServerOptions();
        this.stopPassword = serverOptions.getStopPassword();
        this.ports = server.getPorts();
        setDaemon(true);
        setName("StopMonitor");
        // add shutdown hook
        addShutDownHook();
    }

    @Override
    public void run() {
        try(ServerSocket serverSocket = new ServerSocket(ports.get("stop").socket, 1, InetAddress.getByName(serverOptions.getHost()))) {
            listening = true;
            LOG.info(bar);
            LOG.info("*** starting 'stop' listener thread - Host: " + serverOptions.getHost()
                    + " - Socket: " + ports.get("stop").socket);
            LOG.info(bar);
            while (listening) {
                LOG.debug("StopMonitor listening for password");
                if (server.getServerState().equals(Server.ServerState.STOPPED) || server.getServerState().equals(Server.ServerState.STOPPING)) {
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
                            listening = false;
                        } else {
                            if (listening) {
                                LOG.warn("Incorrect password used when trying to stop server.");
                            } else {
                                LOG.debug("stopped listening for stop password.");
                            }

                        }
                    } catch (java.net.SocketException e) {
                        // reset
                    }
                }
            }
        } catch (Exception e) {
            LOG.error(e);
            e.printStackTrace();
        } finally {
            LOG.trace("Closed server socket");
            try {
                if (mainThread.isAlive()) {
                    LOG.debug("monitor joining main thread");
                    try {
                        mainThread.interrupt();
                        mainThread.join();
                    } catch (InterruptedException ie) {
                        // expected
                    }
                }
            } catch (Exception e) {
                LOG.error(e);
                e.printStackTrace();
            }
        }
    }

    void stopListening() {
        listening = false;
        LOG.trace("Stopping listening");
        // send a char to the reader so it will stop waiting
        try (Socket s = new Socket(InetAddress.getByName(serverOptions.getHost()), ports.get("stop").socket)) {
            try (OutputStream out = s.getOutputStream()) {
                for (char aStopPassword : stopPassword) {
                    out.write(aStopPassword);
                }
                out.flush();
            }
        } catch (IOException e) {
            LOG.trace(e);
            // expected if already stopping
        }

    }

    void removeShutDownHook() {
        if(shutDownThread != null && Thread.currentThread() != shutDownThread) {
            LOG.debug("Removed shutdown hook");
            Runtime.getRuntime().removeShutdownHook(shutDownThread);
        }
    }

    private void addShutDownHook() {
        if(shutDownThread == null) {
            shutDownThread = new Thread(() -> {
                LOG.debug("Running shutdown hook");
                try {
                    if(!server.getServerState().equals(Server.ServerState.STOPPING) && !server.getServerState().equals(Server.ServerState.STOPPED)) {
                        LOG.debug("shutdown hook:stopServer()");
                        server.stopServer();
                    }
//                    if(tempWarDir != null) {
//                        LaunchUtil.deleteRecursive(tempWarDir);
//                    }
                    if(mainThread.isAlive()) {
                        LOG.debug("shutdown hook joining main thread");
                        mainThread.interrupt();
                        mainThread.join();
                    }
                } catch ( Exception e) {
                    e.printStackTrace();
                }
                LOG.debug("Shutdown hook finished");
            });
            Runtime.getRuntime().addShutdownHook(shutDownThread);
            LOG.debug("Added shutdown hook");
        }
    }


}