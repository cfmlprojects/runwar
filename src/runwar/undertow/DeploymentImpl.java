package runwar.undertow;

import io.undertow.servlet.api.DeploymentInfo;
import io.undertow.servlet.api.DeploymentManager;
import io.undertow.servlet.api.ServletContainer;
import io.undertow.servlet.handlers.ServletPathMatches;

public class DeploymentImpl extends io.undertow.servlet.core.DeploymentImpl {
    private final ServletPathMatches servletPaths;

    public DeploymentImpl(DeploymentManager deploymentManager, DeploymentInfo deploymentInfo,
            ServletContainer servletContainer) {
        super(deploymentManager, deploymentInfo, servletContainer);
        servletPaths = new ServletPathMatches(this);
    }

    @Override
    public ServletPathMatches getServletPaths() {
        return servletPaths;
    }

}
