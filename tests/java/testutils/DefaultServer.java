package testutils;

import java.io.File;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.List;

import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.runner.Description;
import org.junit.runner.Result;
import org.junit.runner.notification.RunListener;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.Statement;

import io.undertow.util.NetworkUtils;
import runwar.Server;
import runwar.options.ServerOptions;
import runwar.options.ServerOptionsImpl;

/**
 * A class that starts a server before the test suite.
 */
public class DefaultServer extends BlockJUnit4ClassRunner {

    static final String DEFAULT = "default";
    public static final int APACHE_PORT = 9080;
    public static final int APACHE_SSL_PORT = 9443;
    public static final int BUFFER_SIZE = Integer.getInteger("test.bufferSize", 8192 * 3);
    public static final String SIMPLEWARPATH = "tests/resource/war/simple.war";

    private static boolean first = true;
    private static volatile Server server = null;
    private static final boolean https = Boolean.getBoolean("test.https");
    private static final int runs = Integer.getInteger("test.runs", 1);

    private static ServerOptions serverOptions;

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
    
    public DefaultServer(Class<?> klass) throws InitializationError {
        super(klass);
    }

    public static ServerOptions getServerOptions() {
        return serverOptions;
    }

    @Override
    public Description getDescription() {
        return super.getDescription();
    }

    @Override
    public void run(final RunNotifier notifier) {
        notifier.addListener(new RunListener() {
            @Override
            public void testStarted(Description description) throws Exception {
                super.testStarted(description);
            }

            @Override
            public void testFinished(Description description) throws Exception {
                super.testFinished(description);
            }
        });

        // run the beforeClass ones first so they can set the config, side result is they're run twice. :/
        serverOptions = new ServerOptionsImpl();
        serverOptions
          .setWarFile(new File(SIMPLEWARPATH))
          .setDebug(true).setBackground(false)
          .setTrayEnabled(false);

        List<FrameworkMethod> methos = getTestClass().getAnnotatedMethods(BeforeClass.class);
        runChild(methos.get(0), new RunNotifier());

        try {
            if(server == null) {
                server = new Server();
                server.startServer(serverOptions);
            } else {
                server.restartServer(serverOptions);
            }
            super.run(notifier);
        } catch (Exception e) {
           e.printStackTrace();
        } finally {
            server.stopServer();
        }

 //       runInternal(notifier);
    }

    private static void runInternal(final RunNotifier notifier) {
            try {
                server = server == null ? new Server() : server;
                String state = server.getServerState();
                serverOptions.setWarFile(new File(SIMPLEWARPATH)).setDebug(true).setBackground(false);
                if(server.getServerState() == Server.ServerState.STARTED){
                    server.restartServer(serverOptions);
                } else {
                    server.startServer(serverOptions);
                    
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            notifier.addListener(new RunListener() {
                @Override
                public void testRunFinished(final Result result) throws Exception {
                    server.stopServer();
                }
            });
    }

    @Override
    protected Description describeChild(FrameworkMethod method) {
        if (runs > 1 && method.getAnnotation(Ignore.class) == null) {
            return describeRepeatTest(method);
        }
        return super.describeChild(method);
    }

    private Description describeRepeatTest(FrameworkMethod method) {

        Description description = Description.createSuiteDescription(testName(method) + " [" + runs + " times]",
                method.getAnnotations());

        for (int i = 1; i <= runs; i++) {
            description.addChild(Description.createTestDescription(getTestClass().getJavaClass(),
                    "[" + i + "] " + testName(method)));
        }
        return description;
    }

    @Override
    protected void runChild(FrameworkMethod method, RunNotifier notifier) {
        try {
            if (runs > 1) {
                Statement statement = methodBlock(method);
                Description description = describeChild(method);
                for (Description desc : description.getChildren()) {
                    runLeaf(statement, desc, notifier);
                }
            } else {
                super.runChild(method, notifier);
            }
        } finally {
            TestHttpClient.afterTest();
        }
    }

    @Override
    protected Statement methodInvoker(FrameworkMethod method, Object test) {
        System.out.println("invoking: " + method.toString());
        return super.methodInvoker(method, test);
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

    public static class Parameterized extends org.junit.runners.Parameterized {

        public Parameterized(Class<?> klass) throws Throwable {
            super(klass);
        }

        @Override
        public void run(final RunNotifier notifier) {
            runInternal(notifier);
            super.run(notifier);
        }
    }

    public static boolean isHttps() {
        return https;
    }

}