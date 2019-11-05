package runwar;

import java.lang.reflect.Method;
import java.net.URI;

import javax.swing.JOptionPane;

public class BrowserOpener {

    private static final String errMsg = "Error attempting to launch web browser";

    public static void main(String[] args) throws Exception {
        openURL(args[0]);
    }

    public static void openURL(String url) {
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
                    String[] browsers = {"firefox", "chrome", "opera", "konqueror", "epiphany", "mozilla", "netscape"};
                    String browser = null;
                    for (int count = 0; count < browsers.length && browser == null; count++) {
                        if (Runtime.getRuntime().exec(new String[]{"which", browsers[count]}).waitFor() == 0) {
                            browser = browsers[count];
                        }
                    }
                    if (browser == null) {
                        throw new Exception("Could not find web browser");
                    } else {
                        Runtime.getRuntime().exec(new String[]{browser, url});
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, errMsg + ":\n" + e.getLocalizedMessage());
        }
    }

}
