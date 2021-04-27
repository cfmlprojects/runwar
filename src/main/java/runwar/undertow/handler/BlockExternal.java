package runwar.undertow.handler;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import io.undertow.server.HandlerWrapper;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.IPAddressAccessControlHandler;
import io.undertow.server.handlers.builder.HandlerBuilder;

/**
 * A {@link HttpHandler} that only allows traffic from localhost
 *
 * @author Brad Wood
 */
public final class BlockExternal implements HttpHandler {

    private IPAddressAccessControlHandler IPAddresshandler;

    public BlockExternal(final HttpHandler handler) {
        this.IPAddresshandler = new IPAddressAccessControlHandler( handler, 404 )
        		.setDefaultAllow(false)
        		.addAllow("127.*.*.*");        
    }

    @Override
    public void handleRequest(final HttpServerExchange exchange) throws Exception {
    	this.IPAddresshandler.handleRequest(exchange);
    }

    @Override
    public String toString() {
        return "block-external()";
    }

    public static class Builder implements HandlerBuilder {

        @Override
        public String name() {
            return "block-external";
        }

        @Override
        public Map<String, Class<?>> parameters() {
            return Collections.emptyMap();
        }

        @Override
        public Set<String> requiredParameters() {
            return Collections.emptySet();
        }

        @Override
        public String defaultParameter() {
            return null;
        }

        @Override
        public HandlerWrapper build(Map<String, Object> config) {
            return new Wrapper();
        }

    }

    private static class Wrapper implements HandlerWrapper {
        @Override
        public HttpHandler wrap(HttpHandler handler) {
            return new BlockExternal(handler);
        }
    }
}
