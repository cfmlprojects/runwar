package testutils;

import org.apache.http.client.HttpRequestRetryHandler;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.conn.ssl.X509HostnameVerifier;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.client.DefaultHttpRequestRetryHandler;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import java.io.IOException;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class TestHttpClient extends DefaultHttpClient {

    private static final X509HostnameVerifier NO_OP_VERIFIER = new X509HostnameVerifier() {
        @Override
        public void verify(String host, SSLSocket ssl) throws IOException {
        }

        @Override
        public void verify(String host, X509Certificate cert) throws SSLException {
        }

        @Override
        public void verify(String host, String[] cns, String[] subjectAlts) throws SSLException {
        }

        @Override
        public boolean verify(String s, SSLSession sslSession) {
            return true;
        }
    };

    private static final List<TestHttpClient> instances = new CopyOnWriteArrayList<>();

    public TestHttpClient() {
        instances.add(this);
    }

    public TestHttpClient(HttpParams params) {
        super(params);
        instances.add(this);
    }

    public TestHttpClient(ClientConnectionManager conman) {
        super(conman);
        instances.add(this);
    }

    public TestHttpClient(ClientConnectionManager conman, HttpParams params) {
        super(conman, params);
        instances.add(this);
    }

    @Override
    protected HttpRequestRetryHandler createHttpRequestRetryHandler() {
        return new DefaultHttpRequestRetryHandler(0, false);
    }

    @Override
    protected HttpParams createHttpParams() {
        HttpParams params = super.createHttpParams();
        HttpConnectionParams.setSoTimeout(params, 30000);
        return params;
    }

    public void setSSLContext(final SSLContext sslContext) {
        SchemeRegistry registry = getConnectionManager().getSchemeRegistry();
        registry.unregister("https");
        if (DefaultServer.getHostAddress(DefaultServer.DEFAULT).equals("localhost")) {
            registry.register(new Scheme("https", 443, new SSLSocketFactory(sslContext)));
            registry.register(
                    new Scheme("https", DefaultServer.getHostSSLPort("default"), new SSLSocketFactory(sslContext)));
        } else {
            registry.register(new Scheme("https", 443, new SSLSocketFactory(sslContext, NO_OP_VERIFIER)));
            registry.register(new Scheme("https", DefaultServer.getHostSSLPort("default"),
                    new SSLSocketFactory(sslContext, NO_OP_VERIFIER)));
        }
    }

    public static void afterTest() {
        for (TestHttpClient i : instances) {
            i.getConnectionManager().shutdown();
        }
        instances.clear();
    }
}