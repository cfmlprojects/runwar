package runwar.undertow;

import io.undertow.attribute.StoredResponse;
import io.undertow.security.api.SecurityContext;
import io.undertow.server.HandlerWrapper;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.Cookie;
import io.undertow.server.handlers.accesslog.AccessLogReceiver;
import io.undertow.server.handlers.accesslog.DefaultAccessLogReceiver;
import io.undertow.server.handlers.builder.HandlerBuilder;
import io.undertow.server.handlers.form.FormData;
import io.undertow.server.handlers.form.FormDataParser;
import io.undertow.util.HeaderValues;
import io.undertow.util.Headers;
import io.undertow.util.LocaleUtils;

import java.util.*;

public class RequestDebugHandler implements HttpHandler {

    private final HttpHandler next;
    private final AccessLogReceiver requestLogReceiver;

    public RequestDebugHandler(final HttpHandler next, AccessLogReceiver requestLogReceiver) {
        this.next = next;
        this.requestLogReceiver = requestLogReceiver;
    }

    @Override
    public void handleRequest(final HttpServerExchange exchange) throws Exception {
        final StringBuilder sb = new StringBuilder();
        // Log pre-service information
        final SecurityContext sc = exchange.getSecurityContext();
        sb.append("\n----------------------------REQUEST---------------------------\n");
        sb.append("               URI=").append(exchange.getRequestURI()).append("\n");
        sb.append(" characterEncoding=").append(exchange.getRequestHeaders().get(Headers.CONTENT_ENCODING)).append("\n");
        sb.append("     contentLength=").append(exchange.getRequestContentLength()).append("\n");
        sb.append("       contentType=").append(exchange.getRequestHeaders().get(Headers.CONTENT_TYPE)).append("\n");
        isAuthenticatedAppend(sb, sc);
        
        Map<String, Cookie> cookies = exchange.getRequestCookies();
        if (cookies != null) {
            for (Map.Entry<String, Cookie> entry : cookies.entrySet()) {
                Cookie cookie = entry.getValue();
                sb.append("            cookie=").append(cookie.getName()).append("=").append(cookie.getValue()).append("\n");
            }
        }
        for (HeaderValues header : exchange.getRequestHeaders()) {
            for (String value : header) {
                sb.append("            header=").append(header.getHeaderName()).append("=").append(value).append("\n");
            }
        }
        sb.append("            locale=").append(LocaleUtils.getLocalesFromHeader(exchange.getRequestHeaders().get(Headers.ACCEPT_LANGUAGE))).append("\n");
        sb.append("            method=").append(exchange.getRequestMethod()).append("\n");
        Map<String, Deque<String>> pnames = exchange.getQueryParameters();
        for (Map.Entry<String, Deque<String>> entry : pnames.entrySet()) {
            String pname = entry.getKey();
            Iterator<String> pvalues = entry.getValue().iterator();
            sb.append("         parameter=");
            sb.append(pname);
            sb.append('=');
            while (pvalues.hasNext()) {
                sb.append(pvalues.next());
                if (pvalues.hasNext()) {
                    sb.append(", ");
                }
            }
            sb.append("\n");
        }
        sb.append("          protocol=").append(exchange.getProtocol()).append("\n");
        sb.append("       queryString=").append(exchange.getQueryString()).append("\n");
        sb.append("        remoteAddr=").append(exchange.getSourceAddress()).append("\n");
        sb.append("        remoteHost=").append(exchange.getSourceAddress().getHostName()).append("\n");
        sb.append("            scheme=").append(exchange.getRequestScheme()).append("\n");
        sb.append("              host=").append(exchange.getRequestHeaders().getFirst(Headers.HOST)).append("\n");
        sb.append("        serverPort=").append(exchange.getDestinationAddress().getPort()).append("\n");
        sb.append("          isSecure=").append(exchange.isSecure()).append("\n");

        exchange.addExchangeCompleteListener((exchange1, nextListener) -> {

            dumpRequestBody(exchange1, sb);

            // Log post-service information
            sb.append("--------------------------RESPONSE--------------------------\n");
            isAuthenticatedAppend(sb, sc);
            sb.append("     contentLength=").append(exchange1.getResponseContentLength()).append("\n");
            sb.append("       contentType=").append(exchange1.getResponseHeaders().getFirst(Headers.CONTENT_TYPE)).append("\n");
            Map<String, Cookie> cookies1 = exchange1.getResponseCookies();
            if (cookies1 != null) {
                for (Cookie cookie : cookies1.values()) {
                    sb.append("            cookie=").append(cookie.getName()).append("=").append(cookie.getValue()).append("; domain=").append(cookie.getDomain()).append("; path=").append(cookie.getPath()).append("\n");
                }
            }
            for (HeaderValues header : exchange1.getResponseHeaders()) {
                for (String value : header) {
                    sb.append("            header=").append(header.getHeaderName()).append("=").append(value).append("\n");
                }
            }
            sb.append("            status=").append(exchange1.getStatusCode()).append("\n");
            String storedResponse = StoredResponse.INSTANCE.readAttribute(exchange1);
            if (storedResponse != null) {
                sb.append("body=\n");
                sb.append(storedResponse);
            }

            sb.append("\n==============================================================");

            nextListener.proceed();
            requestLogReceiver.logMessage(sb.toString());
        });

        // Perform the exchange
        next.handleRequest(exchange);
    }

    private void isAuthenticatedAppend(StringBuilder sb, SecurityContext sc) {
        if (sc != null) {
            if (sc.isAuthenticated()) {
                sb.append("          authType=").append(sc.getMechanismName()).append("\n");
                sb.append("         principle=").append(sc.getAuthenticatedAccount().getPrincipal()).append("\n");
            } else {
                sb.append("          authType=none" + "\n");
            }
        }
    }

    private void dumpRequestBody(HttpServerExchange exchange, StringBuilder sb) {
        try {
            FormData formData = exchange.getAttachment(FormDataParser.FORM_DATA);
            if (formData != null) {
                sb.append("body=\n");

                for (String formField : formData) {
                    Deque<FormData.FormValue> formValues = formData.get(formField);

                    sb.append(formField).append("=");
                    for (FormData.FormValue formValue : formValues) {
                        sb.append(formValue.isFileItem() ? "[file-content]" : formValue.getValue());
                        sb.append("\n");

                        if (formValue.getHeaders() != null) {
                            sb.append("headers=\n");
                            for (HeaderValues header : formValue.getHeaders()) {
                                sb.append("\t").append(header.getHeaderName()).append("=").append(header.getFirst())
                                        .append("\n");

                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static class Builder implements HandlerBuilder {

        @Override
        public String name() {
            return "dump-request";
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
            DefaultAccessLogReceiver requestsLogReceiver = DefaultAccessLogReceiver.builder().build();
            return new RequestDebugHandler(handler, requestsLogReceiver);
        }
    }
}
