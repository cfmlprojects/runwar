package runwar;

import java.io.IOException;
import java.util.Date;

import io.undertow.UndertowLogger;
import io.undertow.predicate.Predicate;
import io.undertow.predicate.Predicates;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.cache.ResponseCache;
import io.undertow.util.DateUtils;
import io.undertow.util.ETag;
import io.undertow.util.ETagUtils;
import io.undertow.util.Headers;
import io.undertow.util.Methods;
import io.undertow.util.RedirectBuilder;
import io.undertow.util.StatusCodes;
import io.undertow.server.handlers.resource.DirectoryUtils;
import io.undertow.server.handlers.resource.Resource;
import io.undertow.server.handlers.resource.ResourceManager;


/**
* This resource handler will either list the directories or delegate the response to the servlet.
* Has some basic caching and ETag stuff as well
* After more investigation, opted not to use this right now, and instead just enable directory
* browsing on the default servlet instance (always added to every undertow servlet deploy), the
* main reason being that we'd have to re-implement the welcome-file logic from the extended class
* as well, instead of just setting them on the deploymentInfo as we do currently.
* 
* Eventually this class may be useful, so we'll keep it around for a bit.
*
*/
public class ResourceHandler extends io.undertow.server.handlers.resource.ResourceHandler {

    private HttpHandler handlerDelegate;
    private volatile Predicate disallowed = Predicates.prefixes("/META-INF", "META-INF","/WEB-INF","WEB-INF");
    private volatile Predicate cachable = Predicates.truePredicate();
    private volatile long lastExpiryDate;
    private volatile String lastExpiryHeader;

    public ResourceHandler(ResourceManager resourceManager, HttpHandler servletHandler) {
        super(resourceManager);
        this.handlerDelegate = servletHandler;
    }

    @Override
    public void handleRequest(final HttpServerExchange exchange) throws Exception {
        if (exchange.getRequestMethod().equals(Methods.GET) || exchange.getRequestMethod().equals(Methods.POST)) {
            serveResource(exchange, true);
        } else if (exchange.getRequestMethod().equals(Methods.HEAD)) {
            serveResource(exchange, false);
        } else {
            exchange.setResponseCode(405);
            exchange.endExchange();
        }
    }

    private void serveResource(final HttpServerExchange exchange, final boolean sendContent) {
        if (DirectoryUtils.sendRequestedBlobs(exchange)) {
            return;
        }

        if (disallowed.resolve(exchange)) {
            exchange.setResponseCode(403);
            exchange.endExchange();
            return;
        }

        ResponseCache cache = exchange.getAttachment(ResponseCache.ATTACHMENT_KEY);
        final boolean cachable = this.cachable.resolve(exchange);

        // we set caching headers before we try and serve from the cache
        if (cachable && getCacheTime() != null) {
            exchange.getResponseHeaders().put(Headers.CACHE_CONTROL, "public, max-age=" + getCacheTime());
            if (System.currentTimeMillis() > lastExpiryDate) {
                long date = System.currentTimeMillis();
                lastExpiryHeader = DateUtils.toDateString(new Date(date));
                lastExpiryDate = date;
            }
            exchange.getResponseHeaders().put(Headers.EXPIRES, lastExpiryHeader);
        }

        if (cache != null && cachable) {
            if (cache.tryServeResponse()) {
                return;
            }
        }

        exchange.dispatch(new Runnable() {
            @Override
            public void run() {
                Resource resource = null;
                try {
                    resource = getResourceManager().getResource(exchange.getRelativePath());
                } catch (IOException e) {
                    UndertowLogger.REQUEST_IO_LOGGER.ioException(e);
                    exchange.setResponseCode(500);
                    exchange.endExchange();
                    return;
                }
                if (resource == null) {
                    exchange.setResponseCode(404);
                    exchange.endExchange();
                    return;
                }
                //System.out.println(resource.getPath());
                if (resource.isDirectory()) {
                    //System.out.println("ISDIRE");
                    String path = exchange.getRequestPath();
                    if (!path.endsWith("/")) {
                        exchange.setResponseCode(302);
                        exchange.getResponseHeaders().put(Headers.LOCATION,
                                RedirectBuilder.redirect(exchange, exchange.getRelativePath() + "/", true));
                        exchange.endExchange();
                        return;
                    }
                    if (isDirectoryListingEnabled()) {
                        DirectoryUtils.renderDirectoryListing(exchange, resource);
                        return;
                    } else {
                        exchange.setResponseCode(StatusCodes.FORBIDDEN);
                        exchange.endExchange();
                        return;
                    }
                }

                final ETag etag = resource.getETag();
                final Date lastModified = resource.getLastModified();
                if (!ETagUtils.handleIfMatch(exchange, etag, false)
                        || !DateUtils.handleIfUnmodifiedSince(exchange, lastModified)) {
                    exchange.setResponseCode(412);
                    exchange.endExchange();
                    return;
                }
                if (!ETagUtils.handleIfNoneMatch(exchange, etag, true)
                        || !DateUtils.handleIfModifiedSince(exchange, lastModified)) {
                    exchange.setResponseCode(304);
                    exchange.endExchange();
                    return;
                }
                try {
                    handlerDelegate.handleRequest(exchange);
                } catch (Exception e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        });

    }

}
