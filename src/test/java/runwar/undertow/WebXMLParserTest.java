package runwar.undertow;

import static io.undertow.servlet.Servlets.deployment;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;

import io.undertow.servlet.api.ServletSessionConfig;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import io.undertow.servlet.api.DeploymentInfo;
import io.undertow.servlet.api.ServletInfo;
import java.util.Map;

public class WebXMLParserTest {

    private DeploymentInfo deploymentInfo;

    public WebXMLParserTest() {
        deploymentInfo = deployment()
                .setContextPath("")
                .setServletSessionConfig(new ServletSessionConfig())
                .setTempDir(new File(System.getProperty("java.io.tmpdir")))
                .setDeploymentName("test").setClassLoader(this.getClass().getClassLoader());
    }

    @BeforeAll
    public static void before() {
//        LogSubverter.subvertLoggers("TRACE");
    }

    @Test
    public void testCommandBoxLuceeWebXML() {
        File webinf = new File("src/test/resources/xml/cmdbox");
        File webxml = new File(webinf, "lucee4.web.xml");
        boolean ignoreWelcomePages = false;
        boolean ignoreRestMappings = false;
        WebXMLParser.parseWebXml(webxml, webinf, deploymentInfo, ignoreWelcomePages, ignoreRestMappings);
        Map<String, ServletInfo> gfg = deploymentInfo.getServlets();
        for (Map.Entry<String, ServletInfo> entry : gfg.entrySet()) {
            Map<String, String> gfg2 = entry.getValue().getInitParams();
            for (Map.Entry<String, String> entry2 : gfg2.entrySet()) {
                System.out.println("Key = " + entry2.getKey()
                        + ", Value = " + entry2.getValue());
            }
        }
        assertEquals(2, deploymentInfo.getServlets().size());
    }

    @Test
    public void testCommandBoxWebXML() {
        File webinf = new File("src/test/resources/xml");
        File webxml = new File(webinf, "web.xml");
        boolean ignoreWelcomePages = false;
        boolean ignoreRestMappings = false;
        WebXMLParser.parseWebXml(webxml, webinf, deploymentInfo, ignoreWelcomePages, ignoreRestMappings);
        Map<String, ServletInfo> gfg = deploymentInfo.getServlets();
        for (Map.Entry<String, ServletInfo> entry : gfg.entrySet()) {
            Map<String, String> gfg2 = entry.getValue().getInitParams();
            for (Map.Entry<String, String> entry2 : gfg2.entrySet()) {
                System.out.println("Key = " + entry2.getKey()
                        + ", Value = " + entry2.getValue());
            }
        }
        assertEquals(2, deploymentInfo.getServlets().size());
    }

    @Test
    public void testSpecialCharInPath() {
        File webinf = new File("src/test/resources/directorypaths/dollar$ign");
        File webxml = new File(webinf, "web.xml");
        boolean ignoreWelcomePages = false;
        boolean ignoreRestMappings = false;
        WebXMLParser.parseWebXml(webxml, webinf, deploymentInfo, ignoreWelcomePages, ignoreRestMappings);
        assertEquals(2, deploymentInfo.getServlets().size());
    }

}
