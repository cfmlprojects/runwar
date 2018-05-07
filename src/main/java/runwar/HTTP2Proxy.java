package runwar;

import io.undertow.Handlers;
import io.undertow.Undertow;
import io.undertow.UndertowOptions;
import io.undertow.attribute.ExchangeAttributes;
import io.undertow.protocols.ssl.UndertowXnioSsl;
import io.undertow.server.HttpHandler;
import io.undertow.server.handlers.LearningPushHandler;
import io.undertow.server.handlers.ResponseCodeHandler;
import io.undertow.server.handlers.proxy.LoadBalancingProxyClient;
import io.undertow.server.handlers.proxy.ProxyHandler;
import io.undertow.server.protocol.http2.Http2UpgradeHandler;
import io.undertow.server.session.InMemorySessionManager;
import io.undertow.server.session.SessionAttachmentHandler;
import io.undertow.server.session.SessionCookieConfig;
import io.undertow.util.Headers;
import io.undertow.util.StatusCodes;
import org.xnio.OptionMap;
import org.xnio.Options;
import org.xnio.Xnio;
import org.xnio.XnioWorker;
import runwar.security.SSLUtil;
import runwar.util.PortRequisitioner;

import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import static io.undertow.Handlers.predicate;
import static io.undertow.predicate.Predicates.secure;
import static runwar.logging.RunwarLogger.LOG;

public class HTTP2Proxy {

    private static XnioWorker http2worker;
    private final PortRequisitioner ports;
    private final Xnio xnio;
    private Undertow http2proxy;
    private ProxyHandler proxyHandler;

    HTTP2Proxy(PortRequisitioner ports, final Xnio xnio){
        this.ports = ports;
        this.xnio = xnio;
    }

    void start(SSLContext sslContext) throws IOException {
        http2worker = xnio.createWorker(OptionMap.builder()
                .set(Options.WORKER_IO_THREADS, 8)
                .set(Options.CONNECTION_HIGH_WATER, 1000000)
                .set(Options.CONNECTION_LOW_WATER, 1000000)
                .set(Options.WORKER_TASK_CORE_THREADS, 30)
                .set(Options.WORKER_TASK_MAX_THREADS, 30)
                .set(Options.TCP_NODELAY, true)
                .set(Options.CORK, true)
                .getMap());

        http2proxy = Undertow.builder()
                .setServerOption(UndertowOptions.ENABLE_HTTP2, true)
                .setWorker(http2worker)
                .addHttpsListener(ports.get("http2").socket, ports.get("http2").host, sslContext)
                .setHandler(proxyHandler)
                .build();
        http2proxy.start();
    }


    HttpHandler proxyHandler(HttpHandler httpHandler) throws IOException, URISyntaxException {
        LOG.debug("Enabling HTTP2 Upgrade and LearningPushHandler");
        /*
         * To not be dependent on java9 or crazy requirements, we set up a proxy to enable http2, and swap it with the actual SSL server (thus the port++/port--)
         */
        httpHandler = new Http2UpgradeHandler(httpHandler);
        httpHandler = Handlers.header(predicate(secure(),httpHandler, exchange -> {
            exchange.getResponseHeaders().add(Headers.LOCATION, "https://" + exchange.getHostName()
                    + ":" + (ports.get("http2")) + exchange.getRelativePath());
            exchange.setStatusCode(StatusCodes.TEMPORARY_REDIRECT);
        }), "x-undertow-transport", ExchangeAttributes.transportProtocol());
        httpHandler = new SessionAttachmentHandler(new LearningPushHandler(100, -1, httpHandler),new InMemorySessionManager("runwarsessions"), new SessionCookieConfig());
        LoadBalancingProxyClient http2proxyClient = new LoadBalancingProxyClient()
                .addHost(new URI("https://localhost:"+ ports.get("https").socket), null, new UndertowXnioSsl(Xnio.getInstance(), OptionMap.EMPTY, SSLUtil.createClientSSLContext()), OptionMap.create(UndertowOptions.ENABLE_HTTP2, true))
                .setConnectionsPerThread(20);
        proxyHandler = ProxyHandler.builder().setProxyClient(http2proxyClient).setMaxRequestTime(30000).setNext(ResponseCodeHandler.HANDLE_404).build();
        return httpHandler;
    }

    Undertow getProxy(){
        return http2proxy;
    }

    void stop() {
        http2worker.shutdown();
        http2proxy.stop();
    }


}
