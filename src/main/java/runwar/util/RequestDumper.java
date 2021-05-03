package runwar.util;

import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Deque;
import java.util.Iterator;
import java.util.Map;

import io.undertow.security.api.SecurityContext;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.ServerConnection;
import io.undertow.server.handlers.Cookie;
import io.undertow.server.protocol.http2.Http2ServerConnection;
import io.undertow.servlet.handlers.ServletRequestContext;
import io.undertow.util.HeaderValues;
import io.undertow.util.Headers;
import io.undertow.util.LocaleUtils;
import net.minidev.json.JSONObject;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.cert.jcajce.JcaX509CertificateHolder;

@SuppressWarnings("deprecation")
public class RequestDumper implements HttpHandler {

    @Override
    public void handleRequest(final HttpServerExchange exchange) throws Exception {
        final SecurityContext sc = exchange.getSecurityContext();
        final JSONObject jsonObject = new JSONObject();
        final ServletRequestContext servletRequestContext = exchange.getAttachment(ServletRequestContext.ATTACHMENT_KEY);
        final String realPath = servletRequestContext == null ? "" : servletRequestContext.getCurrentServletContext().getRealPath(exchange.getRequestURI());

        jsonObject.put("URI",exchange.getRequestURI());
        jsonObject.put("characterEncoding",exchange.getRequestHeaders().get(Headers.CONTENT_ENCODING));
        jsonObject.put("contentLength",exchange.getRequestContentLength());
        jsonObject.put("contentType",exchange.getRequestHeaders().get(Headers.CONTENT_TYPE));
        ServerConnection connection = exchange.getConnection();
        jsonObject.put("isHTTP2",(connection instanceof Http2ServerConnection));
        jsonObject.put("isHTTPS",(connection.getSslSessionInfo() != null) );
        jsonObject.put("realPath",realPath);
        if(connection.getSslSessionInfo() != null){
            final StringBuilder sb = new StringBuilder();
            sb.append("Ciphers: " + connection.getSslSessionInfo().getCipherSuite());
            JSONObject localCerts = new JSONObject();
            Arrays.stream(connection.getSslSession().getLocalCertificates()).forEach(
                    certificate -> {
                        try {
                            X500Name x500name = new JcaX509CertificateHolder((X509Certificate) certificate ).getSubject();
                            localCerts.put("subject",x500name.toString());
                            Arrays.stream(x500name.toString().split(",")).forEach(part->{
                                localCerts.put(part.split("=")[0],part.split("=")[1]);
                            });
                            localCerts.put("cipher",((X509Certificate) certificate).getSigAlgName());
                            sb.append(x500name.toString());
                        } catch (CertificateEncodingException e) {
                            e.printStackTrace();
                        }
                    }
            );
            jsonObject.put("localCertificates",localCerts);
            jsonObject.put("sslSessionInfo",sb.toString());
        } else {
            jsonObject.put("sslSessionInfo","");
        }
        if (sc != null) {
            if (sc.isAuthenticated()) {
                jsonObject.put("authType",sc.getMechanismName());
                jsonObject.put("principle",sc.getAuthenticatedAccount().getPrincipal().getName());
            } else {
                jsonObject.put("authType","none");
            }
        }
        
        final JSONObject cookieData = new JSONObject();
        Map<String, Cookie> cookies = exchange.getRequestCookies();
        if (cookies != null) {
            for (Map.Entry<String, Cookie> entry : cookies.entrySet()) {
                Cookie cookie = entry.getValue();
                cookieData.put(cookie.getName(), cookie.getValue());
            }
        }
        jsonObject.put("cookies", cookieData);

        final JSONObject headerData = new JSONObject();
        for (HeaderValues header : exchange.getRequestHeaders()) {
            for (String value : header) {
                headerData.put(header.getHeaderName().toString(), value);
            }
        }
        jsonObject.put("headers", headerData);

        final JSONObject responseHeaderData = new JSONObject();
        for (HeaderValues header : exchange.getResponseHeaders()) {
            for (String value : header) {
                responseHeaderData.put(header.getHeaderName().toString(), value);
            }
        }
        jsonObject.put("responseHeaders", responseHeaderData);

        jsonObject.put("locale", LocaleUtils.getLocalesFromHeader(exchange.getRequestHeaders().get(Headers.ACCEPT_LANGUAGE)));
        jsonObject.put("method", exchange.getRequestMethod());

        final JSONObject queryParamData = new JSONObject();
        Map<String, Deque<String>> pnames = exchange.getQueryParameters();
        for (Map.Entry<String, Deque<String>> entry : pnames.entrySet()) {
            String pname = entry.getKey();
            final StringBuilder valueSB = new StringBuilder();
            Iterator<String> pvalues = entry.getValue().iterator();
            while (pvalues.hasNext()) {
                valueSB.append(pvalues.next());
                if (pvalues.hasNext()) {
                    valueSB.append(", ");
                }
            }
            queryParamData.put(pname, valueSB.toString());
        }
        jsonObject.put("queryParameters", queryParamData);

        jsonObject.put("protocol", exchange.getProtocol());
        jsonObject.put("queryString", exchange.getQueryString());
        jsonObject.put("remoteAddr", exchange.getSourceAddress());
        jsonObject.put("remoteHost", exchange.getSourceAddress().getHostName());
        jsonObject.put("scheme", exchange.getRequestScheme());
        jsonObject.put("host", exchange.getRequestHeaders().getFirst(Headers.HOST));
        jsonObject.put("serverPort", exchange.getDestinationAddress().getPort());
        
        exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/plain");
        exchange.getResponseSender().send(jsonObject.toJSONString());
    }

}