package runwar.undertow;

import io.undertow.server.HandlerWrapper;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.builder.HandlerBuilder;
import runwar.undertow.SendErrorHandler;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * A handler that sets an error response code, but continues the exchange
 * 
 * Remove this if/when this ticket is complete:
 * https://issues.redhat.com/browse/UNDERTOW-1747
 *
 * @author Brad Wood
 */
public class SendErrorHandler implements HttpHandler {
    private final int responseCode;
    private final HttpHandler next;
 
    /**
     * Construct a new instance.
     *
     * @param responseCode the response code to set
     */
    public SendErrorHandler(HttpHandler next, final int responseCode) {
        this.next = next;
        this.responseCode = responseCode;
    }

    @Override
    public void handleRequest(final HttpServerExchange exchange) throws Exception {
        exchange.setStatusCode(responseCode);
        next.handleRequest(exchange);
    }
    
    @Override
    public String toString() {
    	return "send-error( " + responseCode + " )";
    }


    public static class Builder implements HandlerBuilder {

        @Override
        public String name() {
            return "send-error";
        }

        @Override
        public Map<String, Class<?>> parameters() {
            Map<String, Class<?>> params = new HashMap<>();
            params.put("response-code", Integer.class);
            return params;
        }

        @Override
        public Set<String> requiredParameters() {
            final Set<String> req = new HashSet<>();
            req.add("response-code");
            return req;
        }

        @Override
        public String defaultParameter() {
            return "response-code";
        }

        @Override
        public HandlerWrapper build(Map<String, Object> config) {
            return new Wrapper((Integer) config.get("response-code"));
        }

    }

    private static class Wrapper implements HandlerWrapper {

        private final Integer responseCode;

        private Wrapper(Integer responseCode) {
            this.responseCode = responseCode;
        }


        @Override
        public HttpHandler wrap(HttpHandler handler) {
            return new SendErrorHandler(handler, responseCode);
        }
    }
    
}


