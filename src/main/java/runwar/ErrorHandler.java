package runwar;

import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import runwar.logging.RunwarLogger;

public class ErrorHandler implements HttpHandler {

    private final HttpHandler next;

    ErrorHandler(final HttpHandler next) {
        this.next = next;
    }

    @Override
    public void handleRequest(final HttpServerExchange inExchange) {
        inExchange.addDefaultResponseListener(exchange -> {
            if (!exchange.isResponseChannelAvailable()) {
                RunwarLogger.CONTEXT_LOG.warn("The response channel was closed prematurely.  Request path: " + exchange.getRequestPath() + " status-code: " + exchange.getStatusCode() );
                return false;
            }
            if(exchange.getStatusCode() != 200)
                RunwarLogger.CONTEXT_LOG.errorf("Location: '%s' generated no content, maybe verify any errorPage locations? (status code: %s)", exchange.getRequestPath(), exchange.getStatusCode());
/*
            // does not seem to actually return the content, no idea why
            if (exchange.getStatusCode() != 200) {
                final String errorPage = "<html><head><title>Error</title></head><body>"+ message + "</body></html>";
                exchange.getResponseHeaders().put(Headers.CONTENT_LENGTH, "" + errorPage.length());
                exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/html");
                Sender sender = exchange.getResponseSender();
                sender.send(errorPage);
                return true;
            }
*/
            return false;
        });
        try {
            next.handleRequest(inExchange);
        } catch (Exception e) {
            RunwarLogger.CONTEXT_LOG.error("ErrorHandler handleRequest triggered", e);
        }
    }
}