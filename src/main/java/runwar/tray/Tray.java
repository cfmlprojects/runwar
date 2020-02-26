package runwar.tray;

import static runwar.LaunchUtil.displayMessage;
import static runwar.LaunchUtil.getResourceAsString;
import static runwar.LaunchUtil.openURL;
import static runwar.LaunchUtil.readFile;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
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
import runwar.logging.RunwarLogger;
import runwar.options.ServerOptions;
import runwar.util.Utils;

public class Tray {

    private static SystemTray systemTray;
    private static boolean trayIsHooked = false;

    final static String defaultMenu = "{\"title\" : \"${defaultTitle}\", \"items\": ["
            + "{label:\"Stop Server (${runwar.processName})\", action:\"stopserver\"}"
            + ",{label:\"Open Browser\", action:\"openbrowser\", url:\"http://${runwar.host}:${runwar.port}/\"}"
            + "]}";

    private static HashMap<String,String> variableMap;
    private Server server;

    public static void setVariableMap(HashMap<String,String> vm) {
        variableMap = vm;
    }

    public void hookTray(final Server server) {
        this.server = server;
        if(trayIsHooked){
            return;
        }
//        available 3.13+
        SystemTray.AUTO_SIZE = true;
        SystemTray.FORCE_GTK2 = true;
        SystemTray.AUTO_FIX_INCONSISTENCIES=true;
        System.setProperty("SWT_GTK3", "0");
//        SystemTray.FORCE_TRAY_TYPE = TrayType.;
        if ( GraphicsEnvironment.isHeadless() ) {
            RunwarLogger.LOG.debug("Server is in headless mode, System Tray is not supported");
            return;
        }
        try{
            RunwarLogger.LOG.trace("Initializing tray");
            systemTray = SystemTray.get();
            RunwarLogger.LOG.trace("Initialized");
        } catch (java.lang.ExceptionInInitializerError e) {
            RunwarLogger.LOG.debugf("Error initializing tray: %s", e.getMessage());
        }

        if ( systemTray == null ) {
            RunwarLogger.LOG.warn("System Tray is not supported");
            return;
        }

        ServerOptions serverOptions = server.getServerOptions();
        String iconImage = serverOptions.iconImage();
        String host = serverOptions.host();
        int portNumber = serverOptions.httpPort();
        final int stopSocket = serverOptions.stopPort();
        String processName = serverOptions.processName();
        String PID = server.getPID();
        String warpath = serverOptions.warUriString();

        final String statusText = processName + " server on " + host + ":" + portNumber + " PID:" + PID;

        variableMap = new HashMap<String,String>();
        variableMap.put("defaultTitle", statusText);
        variableMap.put("webroot", warpath);
        variableMap.put("logDir", warpath);
        variableMap.put("app.logDir", warpath);
        variableMap.put("web.webroot", warpath);
        variableMap.put("runwar.port", Integer.toString(portNumber));
        variableMap.put("web.http.port", Integer.toString(portNumber));
        variableMap.put("web.ajp.port", Integer.toString(portNumber));
        variableMap.put("runwar.processName", processName);
        variableMap.put("processName", processName);
        variableMap.put("runwar.PID", PID);
        variableMap.put("runwar.host", host);
        variableMap.put("web.host", host);
        variableMap.put("runwar.stopsocket", Integer.toString(stopSocket));

        String trayConfigJSON;
        if (serverOptions.trayConfig() != null) {
            trayConfigJSON = readFile( serverOptions.trayConfig() );
        } else {
            trayConfigJSON = getResourceAsString("runwar/taskbar.json");
        }

        instantiateMenu(trayConfigJSON, statusText, iconImage, variableMap, server);

        trayIsHooked = true;
    }
    
    private static void instantiateMenu(String trayConfigJSON, String statusText, String iconImage, HashMap<String, String> variableMap, Server server ) {
        JSONObject menu;
        setVariableMap(variableMap);
        menu = getTrayConfig( trayConfigJSON, statusText, variableMap );
        if (menu == null) {
            RunwarLogger.LOG.error("Could not load tray config json, using default");
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
                checkbox.setShortcut(label.charAt(0));
                checkbox.setEnabled(!isDisabled);
                menu.add(checkbox);
            }
            else if(itemInfo.get("action") != null) {
                String action = getString(itemInfo, "action", "");
                if (action.equalsIgnoreCase("stopserver")) {
                    menuItem = new MenuItem(label, is, new ExitAction(server));
                    menuItem.setShortcut('s');
                } else if (action.equalsIgnoreCase("restartserver")) {
                    menuItem = new MenuItem(label, is, new RestartAction(server));
                    menuItem.setShortcut('r');
                } else if (action.equalsIgnoreCase("getversion")) {
                    menuItem = new MenuItem("Version: " + Server.getVersion(), is, new GetVersionAction());
                    menuItem.setShortcut('v');
                } else if (action.equalsIgnoreCase("openbrowser")) {
                    String url = itemInfo.get("url").toString();
                    menuItem = new MenuItem(label, is, new OpenBrowserAction(url));
                    menuItem.setShortcut('o');
                } else if (action.equalsIgnoreCase("openfilesystem")) {
                    File path = new File(getString(itemInfo, "path", server.getServerOptions().warUriString()));
                    menuItem = new MenuItem(label, is, new BrowseFilesystemAction(path.getAbsolutePath()));
                    menuItem.setShortcut('b');
                /*} else if (action.equalsIgnoreCase("serverOptionsJson")) {
                    menuItem = new MenuItem(label, is, new ServerOptionsJsonAction(server.getServerOptions()));
                    menuItem.setShortcut('d');
                } else if (action.equalsIgnoreCase("openTerminal")) {
                    menuItem = new MenuItem(label, is, new ServerOptionsJsonAction(server.getServerOptions()));
                    menuItem.setShortcut('d');
                } else if (action.equalsIgnoreCase("serverOptionsSave")) {
                    menuItem = new MenuItem(label, is, new ServerOptionsSaveAction(server.getServerOptions()));
                    menuItem.setShortcut('d');
                } else if (action.equalsIgnoreCase("run")) {
                    String command = getString(itemInfo, "command", "");
                    String workingDirectory = getString(itemInfo, "workingDirectory", "");
                    String output = getString(itemInfo, "output", "dialog");
                    menuItem = new MenuItem(label, is, new RunShellCommandAction(command, workingDirectory, output));
                    menuItem.setShortcut('c');*/
                } else {
                    RunwarLogger.LOG.error("Unknown menu item action \"" + action + "\" for \"" + label + "\"");
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

  /*  public static JSONArray loadItems(JSONArray loadItems) {
        JSONArray items = new JSONArray();
        if (loadItems == null) {
            loadItems = (JSONArray) JSONValue.parse("[]");
        }

        for (Object ob : loadItems) {
            JSONObject itemInfo = (JSONObject) ob;
            if(itemInfo.get("label") == null) {
                RunwarLogger.LOG.error("No label for menu item: " + itemInfo.toJSONString());
                continue;
            }
            String label = getString(itemInfo, "label", "");
            itemInfo.put("label",label);
            if(itemInfo.get("action") != null) {
//                String action = itemInfo.get("action").toString();
            }
            if(itemInfo.get("url") != null) {
                itemInfo.put("action", getString(itemInfo, "action", "openbrowser"));
                itemInfo.put("url", getString(itemInfo, "url", ""));
            } else if(itemInfo.get("items") != null) {
                itemInfo.put("items", loadItems((JSONArray)itemInfo.get("items")));
            }

            items.add(itemInfo);
        }
        return items;
    }*/

    public static JSONObject getTrayConfig(String jsonText, String defaultTitle, HashMap<String, String> variableMap) {
        JSONObject config;
        JSONArray loadItems;
        JSONArray items = new JSONArray();
        setVariableMap(variableMap);
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
                RunwarLogger.LOG.error("No label for menu item: " + itemInfo.toJSONString());
                continue;
            }
            String label = getString(itemInfo, "label", "");
            itemInfo.put("label",label);
            if(itemInfo.get("action") != null) {
                String action = itemInfo.get("action").toString();
                if (action.toLowerCase().equals("stopserver") && action.toLowerCase().equals("openbrowser")) {
                    RunwarLogger.LOG.error("Unknown menu item action \"" + action + "\" for \"" + label + "\"");
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
            if(variableMap.get(key) != null) {
                string = string.replace("${" + key + "}", variableMap.get(key));
            } else {
                RunwarLogger.LOG.error("Could not get key: " + key);
            }
        }
        return string;
    }

    public static void unhookTray() {
        if (systemTray != null) {
            try {
                RunwarLogger.LOG.debug("Removing tray");
                systemTray.shutdown();
                systemTray = null;
            } catch (Exception e) {
                RunwarLogger.LOG.trace(e);
            }
        }
    }

    public static Image getIconImage(String iconImage) {
        Image image = null;
        if (iconImage != null && iconImage.length() != 0) {
            iconImage = iconImage.replaceAll("(^\")|(\"$)", "");
            RunwarLogger.LOG.trace("trying to load icon: " + iconImage);
            if (iconImage.contains("!")) {
                String[] zip = iconImage.split("!");
                try {
                    ZipFile zipFile = new ZipFile(zip[0]);
                    ZipEntry zipEntry = zipFile.getEntry(zip[1].replaceFirst("^[\\/]", ""));
                    InputStream entryStream = zipFile.getInputStream(zipEntry);
                    image = ImageIO.read(entryStream);
                    zipFile.close();
                    RunwarLogger.LOG.trace("loaded image from archive: " + zip[0] + zip[1]);
                } catch (IOException e2) {
                    RunwarLogger.LOG.trace("Could not get zip resource: " + iconImage + "(" + e2.getMessage() + ")");
                }
            } else if (new File(iconImage).exists()) {
                try {
                    image = ImageIO.read(new File(iconImage));
                } catch (IOException e1) {
                    RunwarLogger.LOG.trace("Could not get file resource: " + iconImage + "(" + e1.getMessage() + ")");
                }
            } else {
                RunwarLogger.LOG.debug("trying parent loader for image: " + iconImage);
                URL imageURL = LaunchUtil.class.getClassLoader().getParent().getResource(iconImage);
                if (imageURL == null) {
                    RunwarLogger.LOG.trace("trying loader for image: " + iconImage);
                    imageURL = LaunchUtil.class.getClassLoader().getResource(iconImage);
                }
                if (imageURL != null) {
                    RunwarLogger.LOG.trace("Trying getImage for: " + imageURL);
                    image = Toolkit.getDefaultToolkit().getImage(imageURL);
                }
            }
        } else {
            image = Toolkit.getDefaultToolkit().getImage(Start.class.getResource("/runwar/icon.png"));
        }
        // if bad image, use default
        if (image == null) {
            RunwarLogger.LOG.debug("load icon '"+ iconImage+ "' failed, using default.");
            image = Toolkit.getDefaultToolkit().getImage(Start.class.getResource("/runwar/icon.png"));
        }
        return image;
    }

    public static void setIconImage(String iconImage) {
        if (iconImage != null && iconImage.length() != 0) {
            iconImage = iconImage.replaceAll("(^\")|(\"$)", "");
            RunwarLogger.LOG.trace("trying to load icon: " + iconImage);
            if (iconImage.contains("!")) {
                String[] zip = iconImage.split("!");
                try {
                    ZipFile zipFile = new ZipFile(zip[0]);
                    ZipEntry zipEntry = zipFile.getEntry(zip[1].replaceFirst("^[\\/]", ""));
                    systemTray.setImage( zipFile.getInputStream(zipEntry) );
                    zipFile.close();
                    RunwarLogger.LOG.trace("loaded image from archive: " + zip[0] + zip[1]);
                    return;
                } catch (IOException e2) {
                    RunwarLogger.LOG.trace("Could not get zip resource: " + iconImage + "(" + e2.getMessage() + ")");
                }
            } else if (new File(iconImage).exists()) {
                systemTray.setImage( iconImage );
                return;
            } else {
                RunwarLogger.LOG.trace("trying parent loader for image: " + iconImage);
                URL imageURL = LaunchUtil.class.getClassLoader().getParent().getResource(iconImage);
                if (imageURL == null) {
                    RunwarLogger.LOG.trace("trying loader for image: " + iconImage);
                    imageURL = LaunchUtil.class.getClassLoader().getResource(iconImage);
                }
                if (imageURL != null) {
                    RunwarLogger.LOG.trace("Trying getImage for: " + imageURL);
                    systemTray.setImage( imageURL );
                    return;
                }
            }
        } else {
            RunwarLogger.LOG.trace("no icon image specified");
        }
        // if bad image, use default
        systemTray.setImage( Tray.class.getResource("/runwar/icon.png") );
    }

    public static InputStream getImageInputStream(String iconImage) {
        if (iconImage != null && iconImage.length() != 0) {
            iconImage = iconImage.replaceAll("(^\")|(\"$)", "");
            RunwarLogger.LOG.trace("trying to load icon: " + iconImage);
            if (iconImage.contains("!")) {
                String[] zip = iconImage.split("!");
                try {
                    ZipFile zipFile = new ZipFile(zip[0]);
                    ZipEntry zipEntry = zipFile.getEntry(zip[1].replaceFirst("^[\\/]", ""));
                    InputStream is = zipFile.getInputStream(zipEntry);
                    zipFile.close();
                    return is;
                } catch (IOException e2) {
                    RunwarLogger.LOG.error("Could not get zip resource: " + iconImage + "(" + e2.getMessage() + ")");
                }
            } else if (new File(iconImage).exists()) {
                try {
                    return new FileInputStream(iconImage);
                } catch (FileNotFoundException e) {
                    RunwarLogger.LOG.errorf("Error getting image input stream: %s", e.getMessage());
                }
            } else {
                RunwarLogger.LOG.trace("trying parent loader for image: " + iconImage);
                URL imageURL = LaunchUtil.class.getClassLoader().getParent().getResource(iconImage);
                if (imageURL == null) {
                    RunwarLogger.LOG.trace("trying loader for image: " + iconImage);
                    imageURL = LaunchUtil.class.getClassLoader().getResource(iconImage);
                }
                if (imageURL != null) {
                    try {
                        return imageURL.openStream();
                    } catch (IOException e) {
                        RunwarLogger.LOG.errorf("Error getting image input stream: %s", e.getMessage());
                    }
                }
            }
        }
        return null;
    }
/*
    private static void showDialog(String content){
        final JTextArea jta = new JTextArea(content);
        jta.setEditable(false);
        final JScrollPane jsp = new JScrollPane(jta){
            @Override
            public Dimension getPreferredSize() {
                return new Dimension(580, 420);
            }
        };
        JOptionPane.showMessageDialog( null, jsp, "Output", JOptionPane.INFORMATION_MESSAGE);
    }

    private static class RunShellCommandAction implements ActionListener {
        private String command;
        private String executable;
        private String workingDirectory;
        private String output;

        RunShellCommandAction(String command, String workingDirectory, String output) {
            this.command = command;
            this.workingDirectory= workingDirectory;
            this.output= output.toLowerCase();
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            ByteArrayOutputStream bs = new ByteArrayOutputStream();
            PrintStream ps = new PrintStream(bs);
            ShellProcessBuilder shellProcessBuilder = new ShellProcessBuilder(ps);
            shellProcessBuilder.addArgument(command);
            if(executable != "")
                shellProcessBuilder.setExecutable(executable);
            if(workingDirectory != "")
                shellProcessBuilder.setWorkingDirectory(workingDirectory);

            shellProcessBuilder.start();

            String content = bs.toString();

            if(!output.equals("none")){
                Notify.create()
                        .title("Run Output")
                        .text("Running " + shellProcessBuilder.getCommand())
                        .position(Pos.TOP_RIGHT)
                        // .setScreen(0)
                        .darkStyle()
                        //.shake(1300, 10)
                        // .hideCloseButton()
                        .onAction(new ActionHandler<Notify>() {
                            @Override
                            public void handle(final Notify arg0) {
                                showDialog(content);
                            }
                        }).showConfirm();

                if(!output.equals("dialog")){
                    showDialog(content);
                }

            }
        }
    }

    private static class ServerOptionsJsonAction implements ActionListener {
        ServerOptionsImpl serverOptions;

        ServerOptionsJsonAction(ServerOptions serverOptions) {
            this.serverOptions = (ServerOptionsImpl) serverOptions;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            showDialog(serverOptions.toJson());
        }
    }

    private static class ToggleOnBootAction implements ActionListener {
        ServerOptionsImpl serverOptions;

        ToggleOnBootAction(ServerOptions serverOptions) {
            this.serverOptions = (ServerOptionsImpl) serverOptions;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            Service service = new Service(serverOptions);
            HashMap<String, String> values = new HashMap<>();
            service.commands().forEach(command ->
                values.put(command.name, command.osCommand(OSType.host()))
            );
            SubmitActionlistioner onSubmit = new SubmitActionlistioner() {
                public void actionPerformed(ActionEvent e) {
                    System.out.println(getForm().getFieldValue("start"));
                    System.out.println(getForm().getFieldValue("startForeground"));
                    System.out.println(getForm().getFieldValue("stop"));
                    try {
                        service.generateServiceScripts();
                    } catch (IOException e1) {
                        e1.printStackTrace();
                    }
                }
            };
            JsonForm.renderFormJson("runwar/form/service.form.json", values, variableMap, onSubmit);
        }
    }

    private static class ServerOptionsSaveAction implements ActionListener {
        ServerOptionsImpl serverOptions;

        ServerOptionsSaveAction(ServerOptions serverOptions) {
            this.serverOptions = (ServerOptionsImpl) serverOptions;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            File path = new File(serverOptions.workingDir(),"server.json");
            final JFileChooser fc = new JFileChooser(path);
            JPanel jPanel = new JPanel(new BorderLayout());
            fc.setDialogTitle("Save current options to server.json");
            fc.setName("server.json");
            fc.setSelectedFile(path);
            fc.setAcceptAllFileFilterUsed(true);
            fc.setFileFilter(new FileFilter() {
                @Override
                public boolean accept(File file) {
                    return file.getName().toLowerCase().endsWith(".json") || file.getName().toLowerCase().endsWith(".txt");
                }

                @Override
                public String getDescription() {
                    return "JSON file";
                }
            });
            fc.showSaveDialog(jPanel);

            //showDialog(serverOptions.toJson());
        }
    }
*/
    private static class OpenBrowserAction implements ActionListener {
        private String url;

        public OpenBrowserAction(String url) {
            this.url = url;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            // if binding to all IPs, swap out with localhost.
            url = Utils.replaceHost(url, "0.0.0.0", "127.0.0.1");
            displayMessage(variableMap.get("processName"), "Info", "Opening browser to " + url);
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
                RunwarLogger.LOG.info("Exiting...");
                server.stopServer();
                String message = "Server shut down " + (server.serverWentDown() ? "" : "un") + "successfully, shutting down tray";
                RunwarLogger.LOG.debug( message );
                if(systemTray != null){
                    try {
                        systemTray.shutdown();
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
                System.exit(0);
            } catch (Exception e1) {
                displayMessage(Server.processName, "Error", e1.getMessage());
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
            RunwarLogger.LOG.debug("Trying to open file browser to: " + path);
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
