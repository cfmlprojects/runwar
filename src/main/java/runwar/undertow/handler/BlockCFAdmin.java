package runwar.undertow.handler;

import runwar.undertow.predicate.CFAdminPredicate;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import io.undertow.Handlers;
import io.undertow.predicate.Predicate;
import io.undertow.server.HandlerWrapper;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.SetErrorHandler;
import io.undertow.server.handlers.builder.HandlerBuilder;

/**
 * A {@link HttpHandler} that only allows traffic from localhost
 *
 * @author Brad Wood
 */
public final class BlockCFAdmin implements HttpHandler {

    private static Predicate adminPredicate = new CFAdminPredicate();
    private SetErrorHandler sendError;
    private HttpHandler next;

    public BlockCFAdmin(final HttpHandler handler) {
        this.sendError = Handlers.setErrorHandler( 404, handler );
        this.next = handler;
    }

    @Override
    public void handleRequest(final HttpServerExchange exchange) throws Exception {
    	// If this request is for the CF Admin
    	if( adminPredicate.resolve(exchange)) {
    		// Send a 404
    		this.sendError.handleRequest(exchange);
    	// Otherwise, proceed as before to the next handler
    	} else {
    		this.next.handleRequest(exchange);
    	}
    }

    @Override
    public String toString() {
        return "block-cf-admin()";
    }

    public static class Builder implements HandlerBuilder {

        @Override
        public String name() {
            return "block-cf-admin";
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
            return new BlockCFAdmin(handler);
        }
    }
}
