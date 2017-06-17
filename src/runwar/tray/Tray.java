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
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.imageio.ImageIO;

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
import runwar.logging.Logger;
import runwar.options.ServerOptions;

public class Tray {

    private static Logger log = Logger.getLogger("Tray");
    
    private static SystemTray systemTray;
    private static boolean trayIsHooked = false;

    public static void hookTray(Server server) {
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
            log.debug(e);
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

        final String statusText = processName + " server on " + host + ":" + portNumber + " PID:" + PID;
        
        HashMap<String,String> variableMap = new HashMap<String,String>();
        variableMap.put("defaultTitle", statusText);
        variableMap.put("runwar.port", Integer.toString(portNumber));
        variableMap.put("runwar.processName", processName);
        variableMap.put("runwar.host", host);
        variableMap.put("runwar.stopsocket", Integer.toString(stopSocket));

        final String defaultMenu = "{\"title\" : \"${defaultTitle}\", \"items\": ["
                + "{label:\"Stop Server (${runwar.processName})\", action:\"stopserver\"}"
                + ",{label:\"Open Browser\", action:\"openbrowser\", url:\"http://${runwar.host}:${runwar.port}/\"}"
                + "]}";

        JSONObject menu;
        if (serverOptions.getTrayConfig() != null) {
            menu = getTrayConfig( readFile( serverOptions.getTrayConfig() ), statusText, variableMap );
        } else {
            menu = getTrayConfig( getResourceAsString("runwar/taskbar.json"), statusText, variableMap );
        }
        if (menu == null) {
            log.error("Could not load taskbar properties");
            menu = getTrayConfig( defaultMenu, statusText, variableMap );
        }
        
        systemTray.setStatus( menu.get("title").toString() );
        systemTray.setTooltip( menu.get("tooltip").toString() );
        setIconImage(iconImage);
        
        Menu mainMenu = systemTray.getMenu();
        addMenuItems((JSONArray) menu.get("items"),mainMenu,server);

        trayIsHooked = true;
    }
    
    public static void addMenuItems(JSONArray items, Menu menu, Server server) {
        for (Object ob : items) {
            JSONObject itemInfo = (JSONObject) ob;
            InputStream is = null;
            String label = itemInfo.get("label").toString();
            String hotkey = itemInfo.get("hotkey") != null ? itemInfo.get("hotkey").toString() : "";
            boolean isDisabled = itemInfo.get("disabled") != null && Boolean.parseBoolean(itemInfo.get("disabled").toString());
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
                String action = itemInfo.get("action").toString();
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
                    try {
                        String path;
                        path = Server.getServerOptions().getWarPath();
                        menuItem = new MenuItem(label, is, new BrowseFilesystemAction(path));
                        menuItem.setShortcut('b');
                    } catch (MalformedURLException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                } else {
                    log.error("Unknown menu item action \"" + action + "\" for \"" + label + "\"");
                }
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
        config.put("title", config.get("title") != null ? config.get("title").toString() : defaultTitle );
        config.put("title", replaceMenuTokens(config.get("title").toString(),variableMap));
        config.put("tooltip", config.get("tooltip") != null ? config.get("tooltip").toString() : defaultTitle );
        config.put("tooltip", replaceMenuTokens(config.get("tooltip").toString(),variableMap));
        if (loadItems == null) {
            loadItems = (JSONArray) JSONValue.parse("[]");
        }

        for (Object ob : loadItems) {
            JSONObject itemInfo = (JSONObject) ob;
            if(itemInfo.get("label") == null) {
                log.error("No label for menu item: " + itemInfo.toJSONString());
                continue;
            }
            String label = replaceMenuTokens(itemInfo.get("label").toString(), variableMap);
            itemInfo.put("label",label);
            if(itemInfo.get("action") != null) {
                String action = itemInfo.get("action").toString();
                if (action.toLowerCase().equals("stopserver") && action.toLowerCase().equals("openbrowser")) {
                    log.error("Unknown menu item action \"" + action + "\" for \"" + label + "\"");
                    itemInfo.put("action",null);
                }
            }
            if(itemInfo.get("url") != null) {
                itemInfo.put("action", itemInfo.get("action") != null ? itemInfo.get("action") : "openbrowser");
                itemInfo.put("url", replaceMenuTokens( itemInfo.get("url").toString(), variableMap ));
            }
            items.add(itemInfo);
        }
        config.put("items",items);

        return config;
    }

    private static String replaceMenuTokens(String string, HashMap<String,String> variableMap) {
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
                    log.debug(e);
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
                        log.debug(e);
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
