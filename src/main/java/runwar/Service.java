package runwar;

import daevil.Daevil;
import daevil.OSType;
import runwar.options.ServerOptions;
import runwar.options.ServerOptionsImpl;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Service {

    public static Path generateServiceScripts(ServerOptionsImpl serverOptions, Path path) throws IOException {
        String name = serverOptions.serverName();
        Daevil daevil = new Daevil(name);
        URL url;
        try {
            url = Service.class.getProtectionDomain().getCodeSource().getLocation();
        } catch (SecurityException ex) {
            url = Service.class.getResource(Service.class.getSimpleName() + ".class");
        }
//        String war = Paths.get(".").relativize(serverOptions.warFile().toPath()).toFile().getCanonicalPath();

        serverOptions.pidFile(daevil.pidFile.get());
        Path serverJsonFile = Paths.get(path.toString() + "/server.json").toFile().getCanonicalFile().toPath();
        serverOptions.toJson(serverJsonFile);

        String war = serverOptions.warFile().getCanonicalPath();
        String jarPath = new File(url.getFile()).getCanonicalPath();
        Daevil.log.info("Jar: " + jarPath);
        Daevil.log.info("war: " + war);

        daevil.javaResolver();
        daevil.description.set("Service control for " + name);
        daevil.controlScript().fileName.set(daevil.ctlScript.get());
        daevil.logFileOut.set("/var/log/" + name + ".log");
        daevil.pidFile.set("/var/run/@{name}/@{name}.pid".replace("@{name}",name));

        daevil.startOption("start", "Starts the server")
                .command(OSType.WINDOWS, "CALL \"%javacmd%\" -jar \"" + jarPath +"\" -b true -c \"" + serverJsonFile.toString() + "\"")
                .command(OSType.NIX, "$javacmd -jar \"" + jarPath +"\" -b true -c \"" + serverJsonFile.toString() + "\"");

        daevil.startOption("start-foreground", "Starts the server in the foreground")
                .command(OSType.WINDOWS, "CALL \"%javacmd%\" -jar \"" + jarPath +"\" -c \"" + serverJsonFile.toString() + "\" -b false")
                .command(OSType.NIX, "$javacmd -jar \"" + jarPath +"\" -c \"" + serverJsonFile.toString() + "\" -b false");

        daevil.stopOption("stop", "Stops the server")
                .command(OSType.WINDOWS, "CALL \"%javacmd%\" -jar \"" + jarPath+"\" -stop -c \"" + serverJsonFile.toString() + "\"")
                .command(OSType.NIX, "$javacmd -jar \"" + jarPath+"\" -stop -c \"" + serverJsonFile.toString() + "\"");

        daevil.serviceImg.set("/icon.png");

        daevil.generateScripts(OSType.host(), path);
        Daevil.log.info("Generated service control script: " + path.toAbsolutePath().toString() + '/' + daevil.ctlScript.get());
        Daevil.log.info(daevil.controlScript(OSType.host()).displayCommands(OSType.host()));

        return path;
    }
}
