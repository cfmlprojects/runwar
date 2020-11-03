package runwar;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JOptionPane;
import runwar.logging.RunwarLogger;
import runwar.util.Utils;
import static runwar.util.Utils.containsCaseInsensitive;

public class BrowserOpener {

    private static final String errMsg = "Error attempting to launch web browser";

    public static void main(String[] args) throws Exception {
        openURL(args[0], "");
    }

    public static void openURL(String url, String preferred_browser) {
        String osName = System.getProperty("os.name");
        if (url == null) {
            RunwarLogger.LOG.warn("ERROR: No URL specified to open the browser to!");
            return;
        }
        try {
            RunwarLogger.LOG.info(url);
            if (osName.startsWith("Mac OS")) {
                if (!preferred_browser.equalsIgnoreCase("default")) {
                    try {
                        openInBrowser(preferred_browser, url, 2);
                    } catch (Exception k) {
                        RunwarLogger.LOG.info("Launching on default browser due:", k);
                        defaultMac(url);
                    }
                } else {
                    defaultMac(url);
                }

            } else if (osName.startsWith("Windows")) {
                if (!preferred_browser.equalsIgnoreCase("default")) {
                    try {
                        openInBrowser(preferred_browser, url, 1);
                    } catch (Exception k) {
                        RunwarLogger.LOG.info("Launching on default browser due:", k);
                        Runtime.getRuntime().exec("rundll32 url.dll,FileProtocolHandler " + url);
                    }
                } else {
                    Runtime.getRuntime().exec("rundll32 url.dll,FileProtocolHandler " + url);
                }
            } else { // assume Unix or Linux
                // try default first
                try {
                    if (!preferred_browser.equalsIgnoreCase("default")) {
                        try {
                            openInBrowser(preferred_browser, url, 3);
                        } catch (Exception k) {
                            RunwarLogger.LOG.info("Launching on default browser due:", k);
                            defaultNix(url);
                        }
                    } else {
                        defaultNix(url);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    if (Utils.isWindows()) {
                        openInBrowser(preferred_browser, url, 1);
                    } else if (Utils.isMac()) {
                        openInBrowser(preferred_browser, url, 2);
                    } else {
                        openInBrowser(preferred_browser, url, 3);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, errMsg + ":\n" + e.getLocalizedMessage());
        }
    }

    public static void defaultNix(String url) throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, URISyntaxException {
        Class<?> desktopClass = Class.forName("java.awt.Desktop");
        Object desktopObject = desktopClass.getMethod("getDesktop", (Class[]) null).invoke(null, (Object[]) null);
        Method openURL = desktopClass.getDeclaredMethod("browse", new Class[]{URI.class});
        openURL.invoke(desktopObject, new Object[]{new URI(url)});
    }

    public static void defaultMac(String url) throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        Class<?> fileMgr = Class.forName("com.apple.eio.FileManager");
        Method openURL = fileMgr.getDeclaredMethod("openURL", new Class[]{String.class});
        openURL.invoke(null, new Object[]{url});
    }

    public static void openInBrowser(String preferred_browser, String url, int os) throws IOException, Exception {
        String[] browsers = {"firefox", "chrome", "opera", "konqueror", "epiphany"};

        if (!preferred_browser.equalsIgnoreCase("default")) {
            switch (os) {
                case 1:
                    openUrlInBrowserOnWindows(preferred_browser, url);
                    break;
                case 2:
                    openUrlInBrowserOnMacOS(preferred_browser, url);
                    break;
                //*nix
                case 3:
                    try {
                        Runtime.getRuntime().exec(new String[]{preferred_browser, url});
                    } catch (Exception e) {
                        //preferred browser was not executed
                        RunwarLogger.LOG.error("Could not find preferred web browser.", e);
                        searchAvailableBrowser(browsers, url);

                    }
                    break;
            }

        } else {
            searchAvailableBrowser(browsers, url);
        }
    }

    public static void searchAvailableBrowser(String[] browsers, String url) throws Exception {
        String browser = null;
        for (int count = 0; count < browsers.length && browser == null; count++) {
            if (Runtime.getRuntime().exec(new String[]{"which", browsers[count]}).waitFor() == 0) {
                browser = browsers[count];
            }
        }
        if (browser == null) {
            RunwarLogger.LOG.error("Could not find web browser.");
            throw new Exception("Could not find web browser");
        } else {
            Runtime.getRuntime().exec(new String[]{browser, url});
        }
    }

    public static void openUrlInBrowserOnWindows(String preferredBrowser, String url) throws Exception {
        try {
            Map<String,String> browsers =new HashMap<>();
            browsers.put("firefox","firefox");
            browsers.put("chrome","chrome");
            browsers.put("edge","MicrosoftEdge");
            browsers.put("ie","iexplore");
            browsers.put("opera","opera");
            if (!preferredBrowser.equalsIgnoreCase("default") && containsCaseInsensitive(preferredBrowser, new ArrayList<String>(browsers.keySet()))) {
                Runtime runtime = Runtime.getRuntime();
                String[] args = {"cmd.exe", "/c", "start", browsers.get(preferredBrowser), url};
                Process p = runtime.exec(args);
            } else {
                //opening url on default browser
                Runtime runtime = Runtime.getRuntime();
                String[] args = {"cmd.exe", "/c", "start", url};
                Process p = runtime.exec(args);
            }
        } catch (Exception e) {
            RunwarLogger.LOG.error("Error opening Browser.", e);
            throw e;
        }
    }

    public static void openUrlInBrowserOnMacOS(String preferredBrowser, String url) throws Exception {
        try {
            Map<String,String> browsers = new HashMap<>();
            browsers.put("firefox","Firefox");
            browsers.put("chrome","Google Chrome");
            browsers.put("edge","Microsoft Edge");
            browsers.put("safari","Safari");
            browsers.put("opera","Opera");
            if (!preferredBrowser.equalsIgnoreCase("default") && containsCaseInsensitive(preferredBrowser,new ArrayList<String>(browsers.keySet()))) {
                //using multiline osascript
                Runtime runtime = Runtime.getRuntime();
                String applescriptCommand = "tell application \"" + browsers.get(preferredBrowser) + "\"\n"
                        + "	open location \"" + url + "\"\n"
                        + "end tell";
                String[] args = {"osascript", "-e", applescriptCommand};
                runtime.exec(args);
            } else {
                //opening url on default browser
                Runtime runtime = Runtime.getRuntime();
                String[] args = {"osascript", "-e", "open location \"" + url + "\""};
                runtime.exec(args);
            }
        } catch (Exception e) {
            RunwarLogger.LOG.error("Error opening Browser.", e);
            throw e;
        }
    }

}
