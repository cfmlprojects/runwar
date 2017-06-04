package testutils;

import java.io.File;
import java.net.InetSocketAddress;

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

/**
 * A class that starts a server before the test suite.
 */
public class DefaultServer extends BlockJUnit4ClassRunner {

    static final String DEFAULT = "default";
    public static final int APACHE_PORT = 9080;
    public static final int APACHE_SSL_PORT = 9443;
    public static final int BUFFER_SIZE = Integer.getInteger("test.bufferSize", 8192 * 3);

    private static boolean first = true;
    private static Server server;
    private static final boolean https = Boolean.getBoolean("test.https");
    private static final int runs = Integer.getInteger("test.runs", 1);

    private static final ServerOptions serverOptions = new ServerOptions();

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
        runInternal(notifier);
        super.run(notifier);
    }

    private static void runInternal(final RunNotifier notifier) {
        if (first) {
            first = false;
            try {
                server = new Server();
                serverOptions.setWarFile(new File("tests/war/simple.war")).setDebug(true).setBackground(false);
                server.startServer(serverOptions);
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

    public static String getHostAddress(String serverName) {
        return System.getProperty(serverName + ".server.address", "localhost");
    }

    public static String getHostAddress() {
        return getHostAddress(DEFAULT);
    }

    public static int getHostPort(String serverName) {
        return Integer.getInteger(serverName + ".server.port", 8088);
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