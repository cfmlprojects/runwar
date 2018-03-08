package runwar.tray;

import static runwar.LaunchUtil.displayMessage;
import static runwar.LaunchUtil.getResourceAsString;
import static runwar.LaunchUtil.openURL;
import static runwar.LaunchUtil.readFile;

import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.imageio.ImageIO;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dorkbox.systemTray.Checkbox;
import dorkbox.systemTray.Menu;
import dorkbox.systemTray.MenuItem;
import dorkbox.systemTray.Separator;
import dorkbox.systemTray.SystemTray;
import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import net.minidev.json.JSONValue;
import runwar.LaunchUtil;
import runwar.Server;
import runwar.Start;
import runwar.options.ServerOptions;

public class Tray {

    private static Logger log = LoggerFactory.getLogger("Tray");
    
    private static SystemTray systemTray;
    private static boolean trayIsHooked = false;

    final static String defaultMenu = "{\"title\" : \"${defaultTitle}\", \"items\": ["
            + "{label:\"Stop Server (${runwar.processName})\", action:\"stopserver\"}"
            + ",{label:\"Open Browser\", action:\"openbrowser\", url:\"http://${runwar.host}:${runwar.port}/\"}"
            + "]}";

    private static HashMap<String,String> variableMap;

    public static void hookTray(final Server server) {
        if(trayIsHooked){
            return;
        }
        SystemTray.FORCE_GTK2 = true;
        System.setProperty("SWT_GTK3", "0");
//        SystemTray.FORCE_TRAY_TYPE = TrayType.;
        if ( GraphicsEnvironment.isHeadless() ) {
            log.debug("Server is in headless mode, System Tray is not supported");
            return;
        }
        try{
            systemTray = SystemTray.get();
        } catch (java.lang.ExceptionInInitializerError e) {
            log.debug("Error initializing tray: %s", e.getMessage());
        }

        if ( systemTray == null ) {
            log.warn("System Tray is not supported");
            return;
        }

        ServerOptions serverOptions = Server.getServerOptions();
        String iconImage = serverOptions.getIconImage();
        String host = serverOptions.getHost();
        int portNumber = serverOptions.getPortNumber();
        final int stopSocket = serverOptions.getSocketNumber();
        String processName = serverOptions.getProcessName();
        String PID = server.getPID();
        String warpath = serverOptions.getWarPath();

        final String statusText = processName + " server on " + host + ":" + portNumber + " PID:" + PID;

        variableMap = new HashMap<String,String>();
        variableMap.put("defaultTitle", statusText);
        variableMap.put("webroot", warpath);
        variableMap.put("runwar.port", Integer.toString(portNumber));
        variableMap.put("runwar.processName", processName);
        variableMap.put("runwar.PID", PID);
        variableMap.put("runwar.host", host);
        variableMap.put("runwar.stopsocket", Integer.toString(stopSocket));

        String trayConfigJSON;
        if (serverOptions.getTrayConfig() != null) {
            trayConfigJSON = readFile( serverOptions.getTrayConfig() );
        } else {
            trayConfigJSON = getResourceAsString("runwar/taskbar.json");
        }

        instantiateMenu(trayConfigJSON, statusText, iconImage, variableMap, server);

        trayIsHooked = true;
    }
    
    private static void instantiateMenu(String trayConfigJSON, String statusText, String iconImage, HashMap<String, String> variableMap, Server server ) {
        JSONObject menu;
        menu = getTrayConfig( trayConfigJSON, statusText, variableMap );
        if (menu == null) {
            log.error("Could not load tray config json, using default");
            menu = getTrayConfig( defaultMenu, statusText, variableMap );
        }
        systemTray.setStatus( getString(menu, "title", "") );
        systemTray.setTooltip( getString(menu, "tooltip", "") );
        setIconImage(iconImage);

        Menu mainMenu = systemTray.getMenu();
        addMenuItems((JSONArray) menu.get("items"), mainMenu, server);
        
    }

    public static void addMenuItems(JSONArray items, Menu menu, Server server) {
        for (Object ob : items) {
            JSONObject itemInfo = (JSONObject) ob;
            InputStream is = null;
            String label = getString(itemInfo, "label", "");
            String hotkey = getString(itemInfo, "hotkey", "");
            boolean isDisabled = Boolean.parseBoolean(getString(itemInfo, "disabled", "false"));
            if(itemInfo.get("image") != null) {
                is = getImageInputStream(itemInfo.get("image").toString());
            }
            MenuItem menuItem = null;
            if(itemInfo.get("items") != null) {
                Menu submenu = new Menu(label, is);
                submenu.setShortcut(label.charAt(0));
                menu.add(submenu);
                addMenuItems((JSONArray) itemInfo.get("items"),submenu,server);
            }
            else if(itemInfo.get("separator") != null) {
                menu.add(new Separator());
            }
            else if(itemInfo.get("checkbox") != null) {
                Checkbox checkbox = new Checkbox(label, null);
                checkbox.setShortcut('€');
                checkbox.setEnabled(!isDisabled);
                menu.add(checkbox);
            }
            else if(itemInfo.get("action") != null) {
                String action = getString(itemInfo, "action", "");
                if (action.toLowerCase().equals("stopserver")) {
                    menuItem = new MenuItem(label, is, new ExitAction(server));
                    menuItem.setShortcut('s');
                } else if (action.toLowerCase().equals("restartserver")) {
                    menuItem = new MenuItem(label, is, new RestartAction(server));
                    menuItem.setShortcut('r');
                } else if (action.toLowerCase().equals("getversion")) {
                    menuItem = new MenuItem("Version: " + Server.getVersion(), is, new GetVersionAction());
                    menuItem.setShortcut('v');
                } else if (action.toLowerCase().equals("openbrowser")) {
                    String url = itemInfo.get("url").toString();
                    menuItem = new MenuItem(label, is, new OpenBrowserAction(url));
                    menuItem.setShortcut('o');
                } else if (action.toLowerCase().equals("openfilesystem")) {
                    File path = new File(getString(itemInfo, "path", Server.getServerOptions().getWarPath()));
                    menuItem = new MenuItem(label, is, new BrowseFilesystemAction(path.getAbsolutePath()));
                    menuItem.setShortcut('b');
                } else {
                    log.error("Unknown menu item action \"" + action + "\" for \"" + label + "\"");
                }
            } else {
                menuItem = new MenuItem(label, is);
            }
            if (menuItem != null) {
                if (hotkey.length() > 0) {
                    menuItem.setShortcut(hotkey.charAt(0));
                }
                menuItem.setEnabled(!isDisabled);
                menu.add(menuItem);
            }
            try {
                if (is != null)
                    is.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static JSONObject getTrayConfig(String jsonText, String defaultTitle, HashMap<String, String> variableMap) {
        JSONObject config;
        JSONArray loadItems;
        JSONArray items = new JSONArray();
        if(jsonText == null) {
            return null;
        }
        Object menuObject = JSONValue.parse(jsonText);
        if(menuObject instanceof JSONArray) {
            config = new JSONObject();
            loadItems = (JSONArray) menuObject;
        } else {
            config = (JSONObject) JSONValue.parse(jsonText);
            loadItems = (JSONArray) config.get("items");
        }
        String title = getString(config, "title", defaultTitle);
        config.put("title", title );
        String tooltip = getString(config, "tooltip", defaultTitle);
        // SystemTray limits tooltip to 64, so enforce that and maybe clean up a cut-off word
        if(tooltip.length() > 64){
            tooltip = tooltip.substring(0, 61);
            tooltip = tooltip.substring(0, Math.min(tooltip.length(), tooltip.lastIndexOf(" "))) + "...";
        }
        config.put("tooltip", tooltip );
        if (loadItems == null) {
            loadItems = (JSONArray) JSONValue.parse("[]");
        }

        for (Object ob : loadItems) {
            JSONObject itemInfo = (JSONObject) ob;
            if(itemInfo.get("label") == null) {
                log.error("No label for menu item: " + itemInfo.toJSONString());
                continue;
            }
            String label = getString(itemInfo, "label", "");
            itemInfo.put("label",label);
            if(itemInfo.get("action") != null) {
                String action = itemInfo.get("action").toString();
                if (action.toLowerCase().equals("stopserver") && action.toLowerCase().equals("openbrowser")) {
                    log.error("Unknown menu item action \"" + action + "\" for \"" + label + "\"");
                    itemInfo.put("action",null);
                }
            }
            if(itemInfo.get("url") != null) {
                itemInfo.put("action", getString(itemInfo, "action", "openbrowser"));
                itemInfo.put("url", getString(itemInfo, "url", ""));
            }
            items.add(itemInfo);
        }
        config.put("items",items);

        return config;
    }

    private static String getString(JSONObject menu, String key, String defaultValue) {
        String value = menu.get(key) != null ? menu.get(key).toString() : defaultValue;
        return replaceMenuTokens(value);
    }

    private static String replaceMenuTokens(String string) {
        for(String key : variableMap.keySet() ) {
            string = string.replace("${" + key + "}", variableMap.get(key) );
        }
        return string;
    }

    public static void unhookTray() {
        if (systemTray != null) {
            try {
                log.debug("Removing tray icon");
                systemTray.shutdown();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static Image getIconImage(String iconImage) {
        Image image = null;
        if (iconImage != null && iconImage.length() != 0) {
            iconImage = iconImage.replaceAll("(^\")|(\"$)", "");
            log.debug("trying to load icon: " + iconImage);
            if (iconImage.contains("!")) {
                String[] zip = iconImage.split("!");
                try {
                    ZipFile zipFile = new ZipFile(zip[0]);
                    ZipEntry zipEntry = zipFile.getEntry(zip[1].replaceFirst("^[\\/]", ""));
                    InputStream entryStream = zipFile.getInputStream(zipEntry);
                    image = ImageIO.read(entryStream);
                    zipFile.close();
                    log.debug("loaded image from archive: " + zip[0] + zip[1]);
                } catch (IOException e2) {
                    log.debug("Could not get zip resource: " + iconImage + "(" + e2.getMessage() + ")");
                }
            } else if (new File(iconImage).exists()) {
                try {
                    image = ImageIO.read(new File(iconImage));
                } catch (IOException e1) {
                    log.debug("Could not get file resource: " + iconImage + "(" + e1.getMessage() + ")");
                }
            } else {
                log.debug("trying parent loader for image: " + iconImage);
                URL imageURL = LaunchUtil.class.getClassLoader().getParent().getResource(iconImage);
                if (imageURL == null) {
                    log.debug("trying loader for image: " + iconImage);
                    imageURL = LaunchUtil.class.getClassLoader().getResource(iconImage);
                }
                if (imageURL != null) {
                    log.debug("Trying getImage for: " + imageURL);
                    image = Toolkit.getDefaultToolkit().getImage(imageURL);
                }
            }
        } else {
            image = Toolkit.getDefaultToolkit().getImage(Start.class.getResource("/runwar/icon.png"));
        }
        // if bad image, use default
        if (image == null) {
            log.debug("Bad image, using default.");
            image = Toolkit.getDefaultToolkit().getImage(Start.class.getResource("/runwar/icon.png"));
        }
        return image;
    }

    public static void setIconImage(String iconImage) {
        if (iconImage != null && iconImage.length() != 0) {
            iconImage = iconImage.replaceAll("(^\")|(\"$)", "");
            log.debug("trying to load icon: " + iconImage);
            if (iconImage.contains("!")) {
                String[] zip = iconImage.split("!");
                try {
                    ZipFile zipFile = new ZipFile(zip[0]);
                    ZipEntry zipEntry = zipFile.getEntry(zip[1].replaceFirst("^[\\/]", ""));
                    systemTray.setImage( zipFile.getInputStream(zipEntry) );
                    zipFile.close();
                    log.debug("loaded image from archive: " + zip[0] + zip[1]);
                    return;
                } catch (IOException e2) {
                    log.debug("Could not get zip resource: " + iconImage + "(" + e2.getMessage() + ")");
                }
            } else if (new File(iconImage).exists()) {
                systemTray.setImage( iconImage );
                return;
            } else {
                log.debug("trying parent loader for image: " + iconImage);
                URL imageURL = LaunchUtil.class.getClassLoader().getParent().getResource(iconImage);
                if (imageURL == null) {
                    log.debug("trying loader for image: " + iconImage);
                    imageURL = LaunchUtil.class.getClassLoader().getResource(iconImage);
                }
                if (imageURL != null) {
                    log.debug("Trying getImage for: " + imageURL);
                    systemTray.setImage( imageURL );
                    return;
                }
            }
        }
        // if bad image, use default
        systemTray.setImage( Start.class.getResource("/runwar/icon.png") );
    }

    public static InputStream getImageInputStream(String iconImage) {
        if (iconImage != null && iconImage.length() != 0) {
            iconImage = iconImage.replaceAll("(^\")|(\"$)", "");
            log.debug("trying to load icon: " + iconImage);
            if (iconImage.contains("!")) {
                String[] zip = iconImage.split("!");
                try {
                    ZipFile zipFile = new ZipFile(zip[0]);
                    ZipEntry zipEntry = zipFile.getEntry(zip[1].replaceFirst("^[\\/]", ""));
                    InputStream is = zipFile.getInputStream(zipEntry);
                    zipFile.close();
                    return is;
                } catch (IOException e2) {
                    log.debug("Could not get zip resource: " + iconImage + "(" + e2.getMessage() + ")");
                }
            } else if (new File(iconImage).exists()) {
                try {
                    return new FileInputStream(iconImage);
                } catch (FileNotFoundException e) {
                    log.debug("Error getting image input stream: %s", e.getMessage());
                }
            } else {
                log.debug("trying parent loader for image: " + iconImage);
                URL imageURL = LaunchUtil.class.getClassLoader().getParent().getResource(iconImage);
                if (imageURL == null) {
                    log.debug("trying loader for image: " + iconImage);
                    imageURL = LaunchUtil.class.getClassLoader().getResource(iconImage);
                }
                if (imageURL != null) {
                    try {
                        return imageURL.openStream();
                    } catch (IOException e) {
                        log.debug("Error getting image input stream: %s", e.getMessage());
                    }
                }
            }
        }
        return null;
    }


    private static class OpenBrowserAction implements ActionListener {
        private String url;

        public OpenBrowserAction(String url) {
            this.url = url;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            displayMessage("Info", "Opening browser to " + url);
            openURL(url);
        }
    }

    private static class ExitAction implements ActionListener {

        private Server server;

        public ExitAction(Server server) {
            this.server = server;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            try {
                System.out.println("Exiting...");
                server.stopServer();
                String message = "Server shut down " + (server.serverWentDown() ? "" : "un") + "successfully, shutting down tray";
                log.debug( message );
                try {
                    systemTray.shutdown();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
                System.exit(0);
            } catch (Exception e1) {
                displayMessage("Error", e1.getMessage());
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e2) {
                }
                System.exit(1);
            } finally {
                try {
                    systemTray.shutdown();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
                System.exit(0);
            }
        }
    }

    private static class RestartAction implements ActionListener {
        
        private Server server;
        
        public RestartAction(Server server) {
            this.server = server;
        }
        
        @Override
        public void actionPerformed(ActionEvent e) {
            try {
                System.out.println("Restarting...");
                server.restartServer();
            } catch (Exception e1) {
                displayMessage("Error", e1.getMessage());
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e2) {
                }
            }
        }
    }
    
    private static class GetVersionAction implements ActionListener {
        
        @Override
        public void actionPerformed(ActionEvent e) {
            try {
                final MenuItem entry = (MenuItem) e.getSource();
                entry.setText("Version: " + Server.getVersion());
            } catch (Exception e1) {
                displayMessage("Error", e1.getMessage());
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e2) {
                }
            }
        }
    }
    
    private static class BrowseFilesystemAction implements ActionListener {
        private String path;

        public BrowseFilesystemAction(String path) {
            this.path = path;
        }
        @Override
        public void actionPerformed(ActionEvent e) {
            log.debug("Trying to open file browser to: " + path);
            try {
                LaunchUtil.browseDirectory(path);
            }
            catch(Exception ex) {
                if (!LaunchUtil.isLinux() || !LaunchUtil.execute(new String[] {"xdg-open", path})) {
                    displayMessage("Error", "Sorry, unable to open the file browser: " + ex.getLocalizedMessage());
                }
            }
        }
    }
}
