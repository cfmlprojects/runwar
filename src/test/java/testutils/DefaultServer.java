package testutils;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;

import io.undertow.server.handlers.proxy.ProxyHandler;
import io.undertow.util.Headers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.extension.*;

import io.undertow.util.NetworkUtils;
import runwar.Server;
import runwar.logging.LoggerFactory;
import runwar.options.ServerOptions;
import runwar.options.ServerOptionsImpl;
import runwar.security.SSLUtil;

import javax.net.ssl.SSLContext;

/**
 * A class that starts a server before the test suite.
 */
@ExtendWith(ServerConfigParameterResolver.class)
public class DefaultServer implements BeforeEachCallback, AfterEachCallback, BeforeAllCallback, AfterAllCallback {

    static final String DEFAULT = "default";
    public static final String WARPATH = "src/test/resources/war/simple.war";
    private static volatile TestHttpClient client = null;
    private static SSLContext clientSslContext;

    private volatile Server server = null;
    private static final boolean https = Boolean.getBoolean("test.https");
    public static volatile String string = "Blank";

    private static volatile ServerOptions serverOptions;

    @Override
    public void beforeAll(ExtensionContext context) throws Exception {
        serverOptions = getServerOptions();
        LoggerFactory.configure(serverOptions);
        System.out.println("DefaultServer: Running before all:" + serverOptions.serverName());
        server = new Server();
        System.out.println("DefaultServer: Starting...");
        try {
            server.startServer(serverOptions);
        } catch (Exception e){
            e.printStackTrace();
            Assertions.fail("Could not start server");
        }
        if(server.getServerState() != Server.ServerState.STARTING && server.getServerState() != Server.ServerState.STARTED){
            Assertions.fail("Did not start server");
        }
        System.out.println("DefaultServer: started.");
    }

    @Override
    public void beforeEach(ExtensionContext context) {
        System.out.println("DefaultServer: http port:" + serverOptions.httpPort());
        System.out.println("DefaultServer: https port:" + serverOptions.sslPort());
        System.out.println("DefaultServer: basic auth:" + serverOptions.basicAuthEnable());
        System.out.println("started test...");
        client = new TestHttpClient();
        client.setSSLContext(getClientSSLContext());
    }

    @Override
    public void afterEach(ExtensionContext context) {
        System.out.println("finished test");
        TestHttpClient.afterTest();
        client = null;
    }

    @Override
    public void afterAll(ExtensionContext context) throws InterruptedException {
        server.stopServer();
        System.out.println("stopped server");
        server = null;
    }

    public static synchronized ServerOptionsImpl getServerOptions() {
        if(serverOptions == null){
            serverOptions = new ServerOptionsImpl();
            serverOptions.warFile(new File(WARPATH))
                    .debug(true)
//                    .logLevel("TRACE")
                    .background(false)
                    .httpPort(0)
                    .stopPort(0)
                    .sslPort(0)
                    .http2ProxySSLPort(0)
                    .trayEnable(false);
        }
        return (ServerOptionsImpl) serverOptions.debug(true).background(false);
    }

    public static synchronized ServerOptions resetServerOptions() {
        serverOptions = null;
        return getServerOptions();
    }

    public static TestHttpClient getClient() {
        return client;
    }

    public static String getDefaultServerURL() {
        return "http://" + NetworkUtils.formatPossibleIpv6Address(getHostAddress(DEFAULT)) + ":" + getHostPort(DEFAULT);
    }

    public static InetSocketAddress getDefaultServerAddress() {
        return new InetSocketAddress(DefaultServer.getHostAddress(DEFAULT), DefaultServer.getHostPort(DEFAULT));
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

    public static File getWarFile(String serverName) {
        return serverOptions.warFile();
    }

    public static int getHostPort(String serverName) {
        return serverOptions.httpPort();
    }

    public static int getHTTP2Port(String serverName) {
        return getServerOptions().http2ProxySSLPort();
    }

    public static int getHostSSLPort(String serverName) {
        return getServerOptions().sslPort();
    }

    public static boolean isHttps() {
        return https;
    }

    /**
     * When using the default SSL settings returns the corresponding client context.
     * <p/>
     * If a test case is initialising a custom server side SSLContext then the test case will be responsible for creating it's
     * own client side.
     *
     * @return The client side SSLContext.
     */
    public static SSLContext getClientSSLContext() {
        if (clientSslContext == null) {
            clientSslContext = createClientSslContext();
        }
        return clientSslContext;
    }

    public static SSLContext createClientSslContext() {
        try {
            return SSLUtil.createSSLContext();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void setupProxyHandlerForSSL(ProxyHandler proxyHandler) {
        proxyHandler.addRequestHeader(Headers.SSL_CLIENT_CERT, "%{SSL_CLIENT_CERT}", DefaultServer.class.getClassLoader());
        proxyHandler.addRequestHeader(Headers.SSL_CIPHER, "%{SSL_CIPHER}", DefaultServer.class.getClassLoader());
        proxyHandler.addRequestHeader(Headers.SSL_SESSION_ID, "%{SSL_SESSION_ID}", DefaultServer.class.getClassLoader());
    }
}