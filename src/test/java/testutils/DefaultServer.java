package testutils;

import java.io.File;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.List;

import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import io.undertow.util.NetworkUtils;
import runwar.Server;
import runwar.options.ServerOptions;
import runwar.options.ServerOptionsImpl;

/**
 * A class that starts a server before the test suite.
 */
public class DefaultServer implements BeforeEachCallback, AfterEachCallback, BeforeAllCallback {

    static final String DEFAULT = "default";
    public static final int APACHE_PORT = 9080;
    public static final int APACHE_SSL_PORT = 9443;
    public static final int BUFFER_SIZE = Integer.getInteger("test.bufferSize", 8192 * 3);
    public static final String SIMPLEWARPATH = "src/test/resources/war/simple.war";

    private static boolean first = true;
    private static volatile Server server = null;
    private static final boolean https = Boolean.getBoolean("test.https");
    private static final int runs = Integer.getInteger("test.runs", 1);

    private static ServerOptions serverOptions;

    @Override
    public void beforeAll(ExtensionContext context) throws Exception {
        serverOptions = new ServerOptionsImpl();
    }

    @Override
    public void beforeEach(ExtensionContext context) throws Exception {
        try {
            serverOptions.setWarFile(new File(SIMPLEWARPATH)).setDebug(true).setBackground(false).setTrayEnabled(false);
            server = new Server();
            server.startServer(serverOptions);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void afterEach(ExtensionContext context) throws Exception {
        server.stopServer();
    }

    public static ServerOptions getServerOptions() {
        return serverOptions;
    }

    public static String getDefaultServerURL() {
        return "http://" + NetworkUtils.formatPossibleIpv6Address(getHostAddress(DEFAULT)) + ":" + getHostPort(DEFAULT);
    }

    public static InetSocketAddress getDefaultServerAddress() {
        return new InetSocketAddress(DefaultServer.getHostAddress("default"), DefaultServer.getHostPort("default"));
    }

    public static String getDefaultServerSSLAddress() {
        return "https://" + NetworkUtils.formatPossibleIpv6Address(getHostAddress(DEFAULT)) + ":"
                + getHostSSLPort(DEFAULT);
    }

    public static String getDefaultServerHTTP2Address() {
        return "https://" + NetworkUtils.formatPossibleIpv6Address(getHostAddress(DEFAULT)) + ":"
                + getHTTP2Port(DEFAULT);
    }

    public static String getHostAddress(String serverName) {
        return System.getProperty(serverName + ".server.address", "localhost");
    }

    public static String getHostAddress() {
        return getHostAddress(DEFAULT);
    }

    public static int getHostPort(String serverName) {
        return Integer.getInteger(serverName + ".server.port", 8088);
    }

    public static int getHTTP2Port(String serverName) {
        return Integer.getInteger(serverName + ".server.port", 1443);
    }

    public static int getHostPort() {
        return getHostPort(DEFAULT);
    }

    public static int getHostSSLPort(String serverName) {
        return Integer.getInteger(serverName + ".server.sslPort", 1443);
    }

    public static boolean isHttps() {
        return https;
    }

}