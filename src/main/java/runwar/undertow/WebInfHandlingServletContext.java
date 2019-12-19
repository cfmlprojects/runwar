package runwar.undertow;

import io.undertow.server.handlers.resource.Resource;
import io.undertow.servlet.api.Deployment;
import io.undertow.servlet.api.DeploymentInfo;
import io.undertow.servlet.api.ServletContainer;
import io.undertow.servlet.spec.ServletContextImpl;
import io.undertow.util.CanonicalPathUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

public class WebInfHandlingServletContext extends ServletContextImpl {
    private final DeploymentInfo deploymentInfo;

    public WebInfHandlingServletContext(ServletContainer servletContainer, Deployment deployment) {
        super(servletContainer, deployment);
        this.deploymentInfo = deployment.getDeploymentInfo();

    }

    @Override
    public String getRealPath(final String path) {
        if (path == null) {
            return null;
        }
        String canonicalPath = CanonicalPathUtils.canonicalize(path);
        Resource resource;
        try {
            System.out.println(path);
            resource = deploymentInfo.getResourceManager().getResource(canonicalPath);

            if (resource == null) {
                String left = path;
                String realPath;
                // Extrapolate from increasingly trimmed paths.
                while (left.length() > 0) {
                    int lastIndex = left.lastIndexOf('/');
                    left = left.substring(0, lastIndex);
                    resource = deploymentInfo.getResourceManager().getResource(lastIndex > 0 ? left : "/");
                    if (resource != null) {
                        String right = path.substring(lastIndex);
                        right = right.replace('/', File.separatorChar);
                        realPath = resource.getFilePath().toAbsolutePath().toString() + right;
                        return realPath;
                    }
                }
                //UNDERTOW-373 even though the resource does not exist we still need to return a path
                Resource deploymentRoot = deploymentInfo.getResourceManager().getResource("/");
                if(deploymentRoot == null) {
                    return null;
                }
                Path root = deploymentRoot.getFilePath();
                if(root == null) {
                    return null;
                }
                if(!canonicalPath.startsWith("/")) {
                    canonicalPath = "/" + canonicalPath;
                }
                if(File.separatorChar != '/') {
                    canonicalPath = canonicalPath.replace('/', File.separatorChar);
                }
                return root.toAbsolutePath().toString() + canonicalPath;
            }
        } catch (IOException e) {
            return null;
        }
        Path file = resource.getFilePath();
        if (file == null) {
            return null;
        }
        return file.toAbsolutePath().toString();
    }
}
