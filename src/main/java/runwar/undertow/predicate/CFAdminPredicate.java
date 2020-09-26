package runwar.undertow.predicate;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import io.undertow.attribute.ExchangeAttributes;
import io.undertow.predicate.Predicate;
import io.undertow.predicate.PredicateBuilder;
import io.undertow.predicate.RegularExpressionPredicate;
import io.undertow.server.HttpServerExchange;

/**
 * Predicate that returns true if the incoming URL is to a Lucee or ACF admin
 *
 * @author Brad Wood
 */
public class CFAdminPredicate implements Predicate {
	
    public static final CFAdminPredicate INSTANCE = new CFAdminPredicate();
    private RegularExpressionPredicate regexPredicate;

	public CFAdminPredicate() {
		this.regexPredicate = new RegularExpressionPredicate("^/(CFIDE/administrator|CFIDE/adminapi|CFIDE/AIR|CFIDE/appdeployment|CFIDE/cfclient|CFIDE/classes|CFIDE/componentutils|CFIDE/debug|CFIDE/images|CFIDE/orm|CFIDE/portlets|CFIDE/scheduler|CFIDE/ServerManager|CFIDE/services|CFIDE/websocket|CFIDE/wizards|lucee/admin)/.*", ExchangeAttributes.relativePath(), true, false);
    }

    @Override
    public boolean resolve(final HttpServerExchange exchange) {
        return this.regexPredicate.resolve(exchange);
    }

    @Override
    public String toString() {
        return "cf-admin()";
    }

    public static class Builder implements PredicateBuilder {

        @Override
        public String name() {
            return "cf-admin";
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
        public Predicate build(final Map<String, Object> config) {
            return INSTANCE;
        }
    }
}
