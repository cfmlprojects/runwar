package runwar;

import runwar.logging.Logger;
import io.undertow.server.DefaultResponseListener;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;

public class ErrorHandler implements HttpHandler {

    private static final Logger log = Logger.getLogger("RunwarLogger");
    private final HttpHandler next;

    public ErrorHandler(final HttpHandler next) {
        this.next = next;
    }

    @Override
    public void handleRequest(final HttpServerExchange exchange) throws Exception {
        exchange.addDefaultResponseListener(new DefaultResponseListener() {
            @Override
            public boolean handleDefaultResponse(final HttpServerExchange exchange) {
                if (!exchange.isResponseChannelAvailable()) {
                    System.out.println("NOTIN");
                    return false;
                }
                String message = "Location: " + exchange.getRequestPath() + " generated no content, maybe verify any errorPage locations? (status code: "
                        + exchange.getStatusCode() + ")";
                log.error(message);
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
            }
        });
        try {
            next.handleRequest(exchange);
        } catch (Exception e) {
            System.out.println("ErrorHandler handleRequest triggered");
            log.error(e);
            if (exchange.isResponseChannelAvailable()) {
            }
        }
    }
}