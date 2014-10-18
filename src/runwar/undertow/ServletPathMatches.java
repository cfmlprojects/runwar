package runwar.undertow;

import static io.undertow.servlet.handlers.ServletPathMatch.Type.REDIRECT;

import java.io.IOException;

import io.undertow.server.handlers.resource.Resource;
import io.undertow.servlet.api.Deployment;
import io.undertow.servlet.handlers.ServletPathMatch;

public class ServletPathMatches extends io.undertow.servlet.handlers.ServletPathMatches{

    public ServletPathMatches(Deployment deployment) {
        super(deployment);
        // TODO Auto-generated constructor stub
    }

    @Override
    public ServletPathMatch getServletHandlerByPath(final String path) {
        ServletPathMatch match = getServletHandlerByPath(path);
        if (!match.isRequiredWelcomeFileMatch()) {
            return match;
        }
        return match;

    }    
}
