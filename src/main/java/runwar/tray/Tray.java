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
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import dorkbox.notify.Notify;
import dorkbox.notify.Pos;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import javax.swing.JButton;
import javax.swing.JPanel;
import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import net.minidev.json.JSONValue;
import runwar.LaunchUtil;
import runwar.Server;
import runwar.Service;
import runwar.Start;
import runwar.gui.JsonForm;
import runwar.logging.RunwarLogger;
import runwar.options.ServerOptions;
import runwar.options.ServerOptionsImpl;
import runwar.util.Utils;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import runwar.gui.SubmitActionlistioner;
import runwar.util.dae.OSType;

public class Tray {

    private static SystemTray systemTray;
    private static boolean trayIsHooked = false;

    final static String defaultMenu = "{\"title\" : \"${defaultTitle}\", \"items\": ["
            + "{label:\"Stop Server (${runwar.processName})\", action:\"stopserver\"}"
            + ",{label:\"Open Browser\", action:\"openbrowser\", url:\"http://${runwar.host}:${runwar.port}/\"}"
            + "]}";

    private static HashMap<String, String> variableMap;
    private Server server;

    public static void setVariableMap(HashMap<String, String> vm) {
        variableMap = vm;
    }

    public void hookTray(final Server server) {
        this.server = server;
        if (trayIsHooked) {
            return;
        }
//        available 3.13+
        SystemTray.AUTO_SIZE = true;
        SystemTray.FORCE_GTK2 = true;
        SystemTray.AUTO_FIX_INCONSISTENCIES = true;
        System.setProperty("SWT_GTK3", "0");
//        SystemTray.FORCE_TRAY_TYPE = TrayType.;
        if (GraphicsEnvironment.isHeadless()) {
            RunwarLogger.LOG.debug("Server is in headless mode, System Tray is not supported");
            return;
        }
        try {
            RunwarLogger.LOG.trace("Initializing tray");
            systemTray = SystemTray.get();
            RunwarLogger.LOG.trace("Initialized");
        } catch (java.lang.ExceptionInInitializerError e) {
            RunwarLogger.LOG.debugf("Error initializing tray: %s", e.getMessage());
        }

        if (systemTray == null) {
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

        variableMap = new HashMap<String, String>();
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
            trayConfigJSON = readFile(serverOptions.trayConfig());
        } else {
            trayConfigJSON = getResourceAsString("runwar/taskbar.json");
        }

        instantiateMenu(trayConfigJSON, statusText, iconImage, variableMap, server);

        trayIsHooked = true;
    }

    private void instantiateMenu(String trayConfigJSON, String statusText, String iconImage, HashMap<String, String> variableMap, Server server) {
        JSONObject menu;
        setVariableMap(variableMap);
        menu = getTrayConfig(trayConfigJSON, statusText, variableMap);
        if (menu == null) {
            RunwarLogger.LOG.error("Could not load tray config json, using default");
            menu = getTrayConfig(defaultMenu, statusText, variableMap);
        }
        systemTray.setStatus(getString(menu, "title", ""));
        systemTray.setTooltip(getString(menu, "tooltip", ""));
        setIconImage(iconImage);

        Menu mainMenu = systemTray.getMenu();
        addMenuItems((JSONArray) menu.get("items"), mainMenu, server);

    }

    public void addMenuItems(JSONArray items, Menu menu, Server server) {
        System.setProperty("os.name", System.getProperty("os.name").toLowerCase());
        for (Object ob : items) {
            JSONObject itemInfo = (JSONObject) ob;
            InputStream is = null;
            String label = getString(itemInfo, "label", "");
            String hotkey = getString(itemInfo, "hotkey", "");
            boolean isDisabled = Boolean.parseBoolean(getString(itemInfo, "disabled", "false"));
            if (itemInfo.get("image") != null) {
                is = getImageInputStream(itemInfo.get("image").toString());
            } else {
                //check if property action is used
                if (itemInfo.get("action") != null) {
                    //set Defaults
                    try {
                        if (itemInfo.get("command") != null && itemInfo.get("command").toString().startsWith("box ")) {
                            is = getClass().getResourceAsStream("/box.png");
                        } else {
                            switch ((String) itemInfo.get("action")) {
                                case "run":
                                    is = getClass().getResourceAsStream("/run.png");
                                    break;
                                case "runAsync":
                                    is = getClass().getResourceAsStream("/run_async.png");
                                    break;
                                case "runTerminal":
                                    is = getClass().getResourceAsStream("/icon_terminal.png");
                                    break;
                            }
                        }
                    } catch (Exception e) {
                        RunwarLogger.LOG.error("Can't get icon for tray option " + e.getMessage());
                    }
                }
            }
            MenuItem menuItem = null;
            if (itemInfo.get("items") != null) {
                Menu submenu = new Menu(label, is);
                submenu.setShortcut(label.charAt(0));
                menu.add(submenu);
                addMenuItems((JSONArray) itemInfo.get("items"), submenu, server);
            } else if (itemInfo.get("separator") != null) {
                menu.add(new Separator());
            } else if (itemInfo.get("checkbox") != null) {
                Checkbox checkbox = new Checkbox(label, null);
                checkbox.setShortcut(label.charAt(0));
                checkbox.setEnabled(!isDisabled);
                menu.add(checkbox);
            } else if (itemInfo.get("action") != null) {
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
                } else if (action.equalsIgnoreCase("run")) {
                    String command = getString(itemInfo, "command", "");
                    String workingDirectory = getString(itemInfo, "workingDirectory", "");
                    String shell = getString(itemInfo, "shell", server.getServerOptions().defaultShell());
                    Boolean waitResponse = true;
                    try {
                        waitResponse = Boolean.parseBoolean(getString(itemInfo, "waitResponse", "true"));
                    } catch (Exception e) {
                        RunwarLogger.LOG.error("Invalid waitResponse value");
                        waitResponse = true;
                    }
                    menuItem = new MenuItem(label, is, new RunShellCommandAction(command, workingDirectory, waitResponse, shell));
                } else if (action.equalsIgnoreCase("runAsync")) {
                    String command = getString(itemInfo, "command", "");
                    String workingDirectory = getString(itemInfo, "workingDirectory", "");
                    String shell = getString(itemInfo, "shell", server.getServerOptions().defaultShell());
                    menuItem = new MenuItem(label, is, new RunShellCommandAction(command, workingDirectory, false, shell));
                } else if (action.equalsIgnoreCase("runTerminal")) {
                    String command = getString(itemInfo, "command", "");
                    String workingDirectory = getString(itemInfo, "workingDirectory", "");
                    String shell = getString(itemInfo, "shell", server.getServerOptions().defaultShell());
                    String waitResponse = getString(itemInfo, "waitResponse", "true");
                    RunwarLogger.LOG.info("This action (runTerminal) cannot wait for response, ignoring -> waitResponse:" + waitResponse);

                    if (Utils.isMac()) {
                        RunwarLogger.LOG.info("Executing on Mac OS X");
                        command = "osascript -e 'tell app \"Terminal\" to do script \"" + command + "\"'";
                    } else if (Utils.isWindows()) {
                        RunwarLogger.LOG.info("Executing on Windows");
                        command = "start cmd.exe /k \"" + command + "\"";
                    } else if (Utils.isUnix()) {
                        RunwarLogger.LOG.info("Executing on *NIX");
                    } else {
                        RunwarLogger.LOG.error("Your OS is not currently supported to perform this action");
                    }

                    menuItem = new MenuItem(label, is, new RunShellCommandAction(command, workingDirectory, false, shell));
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
                if (is != null) {
                    is.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static JSONObject getTrayConfig(String jsonText, String defaultTitle, HashMap<String, String> variableMap) {
        JSONObject config;
        JSONArray loadItems;
        JSONArray items = new JSONArray();
        setVariableMap(variableMap);
        if (jsonText == null) {
            return null;
        }

        Object menuObject = JSONValue.parse(jsonText);
        if (menuObject instanceof JSONArray) {
            config = new JSONObject();
            loadItems = (JSONArray) menuObject;
        } else {
            config = (JSONObject) JSONValue.parse(jsonText);
            loadItems = (JSONArray) config.get("items");
        }
        String title = getString(config, "title", defaultTitle);
        config.put("title", title);
        String tooltip = getString(config, "tooltip", defaultTitle);
        // SystemTray limits tooltip to 64, so enforce that and maybe clean up a cut-off word
        if (tooltip.length() > 64) {
            tooltip = tooltip.substring(0, 61);
            tooltip = tooltip.substring(0, Math.min(tooltip.length(), tooltip.lastIndexOf(" "))) + "...";
        }
        config.put("tooltip", tooltip);
        if (loadItems == null) {
            loadItems = (JSONArray) JSONValue.parse("[]");
        }

        for (Object ob : loadItems) {
            JSONObject itemInfo = (JSONObject) ob;
            if (itemInfo.get("label") == null) {
                RunwarLogger.LOG.error("No label for menu item: " + itemInfo.toJSONString());
                continue;
            }
            String label = getString(itemInfo, "label", "");
            itemInfo.put("label", label);
            if (itemInfo.get("action") != null) {
                String action = itemInfo.get("action").toString();
                if (action.toLowerCase().equals("stopserver") && action.toLowerCase().equals("openbrowser")) {
                    RunwarLogger.LOG.error("Unknown menu item action \"" + action + "\" for \"" + label + "\"");
                    itemInfo.put("action", null);
                }
            }
            if (itemInfo.get("url") != null) {
                itemInfo.put("action", getString(itemInfo, "action", "openbrowser"));
                itemInfo.put("url", getString(itemInfo, "url", ""));
            }
            items.add(itemInfo);
        }
        config.put("items", items);

        return config;
    }

    private static String getString(JSONObject menu, String key, String defaultValue) {
        String value = menu.get(key) != null ? menu.get(key).toString() : defaultValue;
        return replaceMenuTokens(value);
    }

    private static String replaceMenuTokens(String string) {
        for (String key : variableMap.keySet()) {
            if (variableMap.get(key) != null) {
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
            RunwarLogger.LOG.debug("load icon '" + iconImage + "' failed, using default.");
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
                    systemTray.setImage(zipFile.getInputStream(zipEntry));
                    zipFile.close();
                    RunwarLogger.LOG.trace("loaded image from archive: " + zip[0] + zip[1]);
                    return;
                } catch (IOException e2) {
                    RunwarLogger.LOG.trace("Could not get zip resource: " + iconImage + "(" + e2.getMessage() + ")");
                }
            } else if (new File(iconImage).exists()) {
                systemTray.setImage(iconImage);
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
                    systemTray.setImage(imageURL);
                    return;
                }
            }
        } else {
            RunwarLogger.LOG.trace("no icon image specified");
        }
        // if bad image, use default
        systemTray.setImage(Tray.class.getResource("/runwar/icon.png"));
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
/////

    private static void showDialog(String content) {
        final JTextArea jta = new JTextArea(content);
        jta.setEditable(false);
        final JScrollPane jsp = new JScrollPane(jta) {
            @Override
            public Dimension getPreferredSize() {
                return new Dimension(580, 420);
            }
        };
        JOptionPane.showMessageDialog(null, jsp, "Output", JOptionPane.INFORMATION_MESSAGE);
    }

    private static class StreamGobbler implements Runnable {

        private InputStream inputStream;
        private Consumer<String> consumer;

        public StreamGobbler(InputStream inputStream, Consumer<String> consumer) {
            this.inputStream = inputStream;
            this.consumer = consumer;
        }

        @Override
        public void run() {
            new BufferedReader(new InputStreamReader(inputStream)).lines()
                    .forEach(consumer);

        }
    }

    public static class ConsumerClass {

        JTextArea jta;
        final static String newline = "\n";

        public ConsumerClass(JTextArea jta) {
            this.jta = jta;
        }

        public void printString(String text) {
            jta.append(newline);
            jta.append(text);
        }
    }

    private static class RunShellCommandAction implements ActionListener {

        private String command;
        private String shell;
        private String workingDirectory;
        private boolean waitResponse;

        String[] shells = new String[]{"/bin/bash", "/usr/bin/bash",
            "/bin/pfbash", "/usr/bin/pfbash",
            "/bin/csh", "/usr/bin/csh",
            "/bin/pfcsh", "/usr/bin/pfcsh",
            "/bin/jsh", "/usr/bin/jsh",
            "/bin/ksh", "/usr/bin/ksh",
            "/bin/pfksh", "/usr/bin/pfksh",
            "/bin/ksh93", "/usr/bin/ksh93",
            "/bin/pfksh93", "/usr/bin/pfksh93",
            "/bin/pfsh", "/usr/bin/pfsh",
            "/bin/tcsh", "/usr/bin/tcsh",
            "/bin/pftcsh", "/usr/bin/pftcsh",
            "/usr/xpg4/bin/sh", "/usr/xp4/bin/pfsh",
            "/bin/zsh", "/usr/bin/zsh",
            "/bin/pfzsh", "/usr/bin/pfzsh",
            "/bin/sh", "/usr/bin/sh",};

        RunShellCommandAction(String command, String workingDirectory, Boolean waitResponse, String shell) {
            this.command = command;
            this.workingDirectory = workingDirectory;
            this.waitResponse = waitResponse;
            this.shell = shell;
        }

        public String AvailableShellPick(String[] shells) {
            for (String curShell : shells) {
                if (new File(curShell).canExecute()) {
                    shell = curShell;
                    break;
                }
            }
            return shell;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            try {
                RunwarLogger.LOG.info("RunShellCommandAction ------");
                boolean isWindows = System.getProperty("os.name")
                        .toLowerCase().startsWith("windows");

                ProcessBuilder builder = new ProcessBuilder();
                if (isWindows) {
                    builder.command("cmd.exe", "/c", command);
                } else {
                    if (shell != null && shell.isEmpty()) {
                        shell = AvailableShellPick(shells);
                        builder.command(shell, "-c", command);
                    } else {
                        try {
                            if (new File(shell).canExecute()) {
                                shell = AvailableShellPick(shells);
                            }
                        } catch (Exception ex) {
                            RunwarLogger.LOG.error("Selected shell " + shell + " is not executable" + ex.getMessage());
                        }
                        builder.command(shell, "-c", command);
                    }
                }
                if (workingDirectory != "") {
                    builder.directory(new File(workingDirectory));
                }

                Process process = builder.start();

                int exitCode = 99;
                if (waitResponse) {
                    JTextArea jta = new JTextArea("");
                    ConsumerClass cc = new ConsumerClass(jta);
                    StreamGobbler streamGobbler
                            = new StreamGobbler(process.getInputStream(), cc::printString);
                    ExecutorService service = Executors.newSingleThreadExecutor();
                    Future tsk = service.submit(streamGobbler);
                    JScrollPane jsp = new JScrollPane(jta) {
                        @Override
                        public Dimension getPreferredSize() {
                            return new Dimension(680, 420);
                        }
                    };

                    String cmd = null;
                    for (String bc : builder.command()) {
                        cmd = bc;
                        break;
                    }

                    Runnable r = new MyRunnable(process, jsp, service, tsk, jta, cmd);
                    new Thread(r).start();

                    RunwarLogger.LOG.info("Content:" + exitCode);
                    Notify.create()
                            .title("Run Output")
                            .text("Executed " + builder.command())
                            .position(Pos.TOP_RIGHT)
                            .hideAfter(5000)
                            .darkStyle()
                            .showConfirm();
                } else {
                    RunwarLogger.LOG.info("Not waiting for response:" + exitCode);
                    Notify.create()
                            .title("Run Output")
                            .text("Executed " + builder.command())
                            .position(Pos.TOP_RIGHT)
                            .darkStyle()
                            .hideAfter(5000)
                            .showConfirm();
                    RunwarLogger.LOG.warn("Not waiting for response when running -->>" + command);
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    public static class MyRunnable implements Runnable {

        Process process;
        JScrollPane jsp;
        ExecutorService service;
        Future tsk;
        JTextArea jta;
        String title;

        public MyRunnable(Process process, JScrollPane jsp, ExecutorService service, Future tsk, JTextArea jta, String title) {
            this.process = process;
            this.jsp = jsp;
            this.service = service;
            this.tsk = tsk;
            this.jta = jta;
            this.title = title;
        }

        public void showFrame(JScrollPane jsp, JTextArea jta) {
            try {
                JFrame frame = new JFrame("Command Output");
                jsp = new JScrollPane(jta) {
                    @Override
                    public Dimension getPreferredSize() {
                        return new Dimension(680, 420);
                    }
                };
                frame.setTitle(title);
                frame.getContentPane().add(jsp, BorderLayout.CENTER);
                frame.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
                frame.setSize(new Dimension(880, 620));
                JButton b1 = new JButton("Stop Command");
                b1.setSize(20, 100);
                b1.setLocation(500, 350);
                b1.addActionListener(new java.awt.event.ActionListener() {
                    public void actionPerformed(java.awt.event.ActionEvent evt) {
                        b1.setEnabled(false);
                        stopCommandAttempt(evt);
                        //b1.setEnabled(true);
                    }
                });
                frame.getContentPane().add(b1, BorderLayout.SOUTH);
                frame.setLocationRelativeTo(null);
                frame.setVisible(true);
                int exitCode = process.waitFor();
                this.jta.append("\n" + "Exit Code:" + exitCode);
            } catch (InterruptedException ex) {
                RunwarLogger.LOG.info("An Error Occurred:" + ex.getMessage());
            }
        }

        private void stopCommandAttempt(java.awt.event.ActionEvent evt) {
            try {
                RunwarLogger.LOG.info("Attempting to stop Command...");
                process.destroy();
                process.waitFor(5000, TimeUnit.MILLISECONDS);
                if (process.isAlive()) {
                    RunwarLogger.LOG.warn("Attemting to destroy process forcibly");
                    process.destroyForcibly();
                }

                if (!process.isAlive()) {
                    RunwarLogger.LOG.info("Process Stopped");
                } else {
                    RunwarLogger.LOG.info("Process cannot be Stopped");
                }
                if (service != null) {
                    service.shutdown();
                }
                if (tsk != null) {
                    tsk.cancel(true);
                };
            } catch (InterruptedException ex) {
                RunwarLogger.LOG.info("An Error Occurred:" + ex.getMessage());
            }
        }

        public void run() {
            try {
                showFrame(jsp, jta);

                //int exitCode = process.waitFor();                
                //RunwarLogger.LOG.info("Exit code:" + exitCode);
            } catch (Exception e) {
                e.printStackTrace();
                System.err.println(e);
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
            RunwarLogger.LOG.info("ServerOptionsJsonAction ------");
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
            RunwarLogger.LOG.info("ToggleOnBootAction ------");
            Service service = new Service(serverOptions);
            HashMap<String, String> values = new HashMap<>();
            service.commands().forEach(command
                    -> values.put(command.name, command.osCommand(OSType.host()))
            );
            SubmitActionlistioner onSubmit = new SubmitActionlistioner() {
                public void actionPerformed(ActionEvent e) {
                    RunwarLogger.LOG.info(getForm().getFieldValue("start"));
                    RunwarLogger.LOG.info(getForm().getFieldValue("startForeground"));
                    RunwarLogger.LOG.info(getForm().getFieldValue("stop"));
                    try {
                        //service.generateServiceScripts();
                    } catch (Exception e1) {
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
            RunwarLogger.LOG.info("ServerOptionsSaveAction ------");
            File path = new File(serverOptions.workingDir(), "server.json");
            final JFileChooser fc = new JFileChooser(path);
            JPanel jPanel = new JPanel(new BorderLayout());
            fc.setDialogTitle("Save current options to server.json");
            fc.setName("server.json");
            fc.setSelectedFile(path);
            fc.setAcceptAllFileFilterUsed(true);
            fc.setFileFilter(new javax.swing.filechooser.FileFilter() {
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
/////

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
                RunwarLogger.LOG.debug(message);
                if (systemTray != null) {
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
                RunwarLogger.LOG.info("Restarting...");
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
            } catch (Exception ex) {
                if (!LaunchUtil.isLinux() || !LaunchUtil.execute(new String[]{"xdg-open", path})) {
                    displayMessage("Error", "Sorry, unable to open the file browser: " + ex.getLocalizedMessage());
                }
            }
        }
    }

    public static String getDefaultMenu() {
        return defaultMenu;
    }

}
