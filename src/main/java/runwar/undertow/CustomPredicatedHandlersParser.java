package runwar.undertow;

import io.undertow.predicate.Predicate;
import io.undertow.predicate.PredicateParser;
import io.undertow.predicate.Predicates;
import io.undertow.server.HandlerWrapper;
import io.undertow.server.handlers.builder.HandlerParser;
import io.undertow.server.handlers.builder.PredicatedHandler;
import io.undertow.server.handlers.builder.PredicatedHandlersParser;
import io.undertow.util.ChainedHandlerWrapper;
import java.util.Arrays;

/**
 * Parser for the undertow-handlers.conf file.
 * <p/>
 * This file has a line by line syntax, specifying predicate -> handler. If no
 * predicate is specified then the line is assumed to just contain a handler.
 *
 * @author Stuart Douglas
 */
public class CustomPredicatedHandlersParser extends PredicatedHandlersParser {

    public static PredicatedHandler parseAndGetHandler(final String line, final ClassLoader classLoader) {
        if (line.trim().length() > 0) {
            Predicate predicate;
            HandlerWrapper handler;
            String[] parts = line.split("->");
            if (parts.length == 2) {
                predicate = PredicateParser.parse(parts[0], classLoader);
                handler = HandlerParser.parse(parts[1], classLoader);
            } else if (parts.length == 1) {
                predicate = Predicates.truePredicate();
                handler = HandlerParser.parse(parts[0], classLoader);
            } else {
                predicate = PredicateParser.parse(parts[0], classLoader);
                HandlerWrapper[] handlers = new HandlerWrapper[parts.length - 1];
                for (int i = 0; i < handlers.length; ++i) {
                    handlers[i] = HandlerParser.parse(parts[i + 1], classLoader);
                }
                handler = new ChainedHandlerWrapper(Arrays.asList(handlers));
            }
            return new PredicatedHandler(predicate, handler);
        } else {
            return null;
        }
    }

    
}
