/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package runwar.util.dae;

import org.jsoftbiz.utils.OS;

import java.nio.file.Files;
import java.nio.file.Paths;

/**
 *
 * @author pyro
 */
public enum OSType {

    ANY("ANY"),
    NIX("NIX"),
    NIX_DEBIANISH("NIX"),
    NIX_RHELISH("NIX"),
    NIX_DARWINISH("NIX"),
    WINDOWS("WINDOWS");

    private String type;
    private static String ttyConfig;

    OSType(String type) {
        this.type = type;
    }

    public static OSType host() {
        OS os = OS.getOs();

        if (os.getName().contains("Windows"))
            return OSType.WINDOWS;

        if (Files.exists(Paths.get("/Library/LaunchDaemons/")))
            return OSType.NIX_DARWINISH;

        if (Files.exists(Paths.get("/lib/lsb/init-functions")))
            return OSType.NIX_DEBIANISH;

        if (Files.exists(Paths.get("/etc/init.d/functions")))
            return OSType.NIX_RHELISH;

        if (os.getName().contains("nix"))
            return OSType.NIX;

        throw new IllegalArgumentException("Unknown os type: " + os.getPlatformName());
    }

    public String type() {
        return type;
    }

    public boolean typeOf(OSType osType) {
        switch (osType) {
            case ANY:
                return true;
            case NIX:
                return type.equals("NIX") || this == ANY;
            case NIX_DARWINISH:
                return this == NIX_DARWINISH || this == ANY;
            case NIX_DEBIANISH:
                return this == NIX_DEBIANISH || this == ANY;
            case NIX_RHELISH:
                return this == NIX_RHELISH || this == ANY;
            case WINDOWS:
                return this == WINDOWS || this == ANY;
        }
        return false;
    }


}