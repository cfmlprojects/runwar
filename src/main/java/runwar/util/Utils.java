/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package runwar.util;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import net.minidev.json.JSONObject;
import runwar.logging.RunwarLogger;

/**
 *
 * @author pyro
 */
public class Utils {

    private static String OS = System.getProperty("os.name");

    public static String replaceHost(String openbrowserURL, String oldHost, String newHost) {
        String url = openbrowserURL;
        try {
            URL address = new URL(openbrowserURL);
            String host = address.getHost();
            if (host.equalsIgnoreCase(oldHost)) {
                URL ob = new URL(address.getProtocol(), newHost, address.getPort(), address.getFile());
                openbrowserURL = ob.toString();
            }
        } catch (MalformedURLException ex) {
            ex.printStackTrace();
            openbrowserURL = url;
        }
        return openbrowserURL;
    }

    public static boolean isWindows() {

        return (OS.toLowerCase().indexOf("win") >= 0);

    }

    public static boolean isMac() {

        return (OS.toLowerCase().indexOf("mac") >= 0);

    }

    public static boolean isUnix() {

        return (OS.indexOf("nix") >= 0 || OS.indexOf("nux") >= 0 || OS.indexOf("aix") > 0);

    }

    public static String[] shells = new String[]{"/bin/bash", "/usr/bin/bash",
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

    public static String availableShellPick() {
        String shell = "";
        for (String curShell : shells) {
            if (new File(curShell).canExecute()) {
                shell = curShell;
                break;
            }
        }
        return shell;
    }

    public static Object getIgnoreCase(JSONObject jobj, String key) {

        Iterator<String> iter = jobj.keySet().iterator();
        while (iter.hasNext()) {
            String key1 = iter.next();
            if (key1.equalsIgnoreCase(key)) {
                return jobj.get(key1);
            }
        }

        return null;

    }

    public static boolean containsCaseInsensitive(String s, List<String> l) {
        for (String string : l) {
            if (string.equalsIgnoreCase(s)) {
                return true;
            }
        }
        return false;
    }
}
