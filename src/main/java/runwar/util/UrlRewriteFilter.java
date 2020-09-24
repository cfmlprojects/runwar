/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package runwar.util;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import org.jboss.logging.Logger;
import org.tuckey.web.filters.urlrewrite.Conf;
import org.tuckey.web.filters.urlrewrite.UrlRewriter;

/**
 *
 * @author mgmathus
 */
public class UrlRewriteFilter extends org.tuckey.web.filters.urlrewrite.UrlRewriteFilter {

    Logger LOG = Logger.getLogger("runwar.server"); 
    @Override
    public void loadUrlRewriter(FilterConfig filterConfig) throws ServletException {
        try {
            String confPathStr = filterConfig.getInitParameter("confPath");
            LOG.trace("Config rewrite file:" + confPathStr);
            InputStream inputStream = new FileInputStream(confPathStr);
            boolean type = true;
            if(confPathStr.endsWith("xml")){
                type = false;
            }
            Conf conf1 = new Conf(filterConfig.getServletContext(), inputStream, confPathStr, null, type);
            checkConf(conf1);
        } catch (FileNotFoundException e) {
            throw new ServletException(e);
        }
    }
}
