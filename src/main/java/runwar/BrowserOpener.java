package runwar;

import java.lang.reflect.Method;
import java.net.URI;
import java.util.Arrays;

import javax.swing.JOptionPane;
import runwar.util.Utils;

public class BrowserOpener {

    private static final String errMsg = "Error attempting to launch web browser";

    public static void main(String[] args) throws Exception {
        openURL(args[0], "");
    }

    public static void openURL(String url, String prefered_browser) {
        String osName = System.getProperty("os.name");
        if (url == null) {
            System.out.println("ERROR: No URL specified to open the browser to!");
            return;
        }
        try {
            System.out.println(url);
            if (osName.startsWith("Mac OS")) {
                Class<?> fileMgr = Class.forName("com.apple.eio.FileManager");
                Method openURL = fileMgr.getDeclaredMethod("openURL", new Class[]{String.class});
                openURL.invoke(null, new Object[]{url});
            } else if (osName.startsWith("Windows")) {
                Runtime.getRuntime().exec("rundll32 url.dll,FileProtocolHandler " + url);
            } else { // assume Unix or Linux
                // try default first
                try {
                    Class<?> desktopClass = Class.forName("java.awt.Desktop");
                    Object desktopObject = desktopClass.getMethod("getDesktop", (Class[]) null).invoke(null, (Object[]) null);
                    Method openURL = desktopClass.getDeclaredMethod("browse", new Class[]{URI.class});
                    openURL.invoke(desktopObject, new Object[]{new URI(url)});
                } catch (Exception e) {
                    e.printStackTrace();
                    if (Utils.isWindows()) {
                        Utils.openInBrowser(prefered_browser, url, 1);
                    } else if (Utils.isMac()) {
                        Utils.openInBrowser(prefered_browser, url, 2);
                    } else {
                        Utils.openInBrowser(prefered_browser, url, 3);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, errMsg + ":\n" + e.getLocalizedMessage());
        }
    }

}
