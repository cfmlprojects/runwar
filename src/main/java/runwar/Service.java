package runwar;

import runwar.options.ServerOptionsImpl;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import static runwar.LaunchUtil.displayMessage;
import runwar.util.dae.OSType;
public class Service {

    private ServerOptionsImpl serverOptions;
    private Path serverJsonFile;
    private List<Command> commands;

    public Service(ServerOptionsImpl serverOptions) {
       // this(serverOptions, defaultServerJsonFile());
    }

    public List<Command> commands(){
        return commands;
    }

    public Service(ServerOptionsImpl serverOptions, Path serverJsonFile) {
        this.serverOptions = serverOptions;
        this.serverJsonFile = serverJsonFile;
        String name = serverOptions.serverName();
        //daevil = new Daevil(name);
        if (serverJsonFile.toFile().exists()) {
            displayMessage("info", serverJsonFile.toAbsolutePath() + " exists",0);
        }

        commands = new ArrayList<>();
        Command startCommand = new Command("start", "Starts the server");
        //startCommand.osCommand(OSType.WINDOWS, "CALL \"%javacmd%\" -jar \"" + jarPath() + "\" -b true -c \"" + serverJsonFile().toString() + "\"");
        //startCommand.osCommand(OSType.NIX, "$javacmd -jar \"" + jarPath() + "\" -b true -c \"" + serverJsonFile().toString() + "\"");
        commands.add(startCommand);

        Command startForegroundCommand = new Command("startForeground", "Starts the server in the foreground");
        //startForegroundCommand.osCommand(OSType.WINDOWS, "CALL \"%javacmd%\" -jar \"" + jarPath() + "\" -c \"" + serverJsonFile.toString() + "\" -b false");
        //startForegroundCommand.osCommand(OSType.NIX, "$javacmd -jar \"" + jarPath() + "\" -c \"" + serverJsonFile.toString() + "\" -b false");
        commands.add(startForegroundCommand);

        Command stopCommand = new Command("stop", "Stops the server");
//        stopCommand.osCommand(OSType.WINDOWS, "CALL \"%javacmd%\" -jar \"" + jarPath() + "\" -stop -c \"" + serverJsonFile.toString() + "\"");
//        stopCommand.osCommand(OSType.NIX, "$javacmd -jar \"" + jarPath() + "\" -stop -c \"" + serverJsonFile.toString() + "\"");
        commands.add(stopCommand);
    }

    public static String jarPath() {
        URL url;
        try {
            url = Service.class.getProtectionDomain().getCodeSource().getLocation();
        } catch (SecurityException ex) {
            url = Service.class.getResource(Service.class.getSimpleName() + ".class");
        }
        try {
            return new File(url.getFile()).getCanonicalPath();
        } catch (IOException e) {
            return new File(url.getFile()).getAbsolutePath();
        }
    }

    public static Path defaultServerJsonFile() {
        Path path = Paths.get(".");
        try {
            return Paths.get(path.toString() + "/server.json").toFile().getCanonicalFile().toPath();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return Paths.get(path.toString() + "/server.json").toFile().toPath();
    }

    public Path serverJsonFile() {
        if (serverJsonFile == null) {
            Path path = Paths.get(".");
            try {
                serverJsonFile = Paths.get(path.toString() + "/server.json").toFile().getCanonicalFile().toPath();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return serverJsonFile;
    }

    public Service serverOptions(ServerOptionsImpl options) {
        serverOptions = options;
        return this;
    }

    public class Command {
        public String name;
        public String description;
        public Map<OSType, String> osCommand;

        public Command(String name, String description) {
            this.name = name;
            this.description = description;
            osCommand = new HashMap<>();
        }

        public Command osCommand(OSType osType, String command) {
            osCommand.put(osType, command);
            return this;
        }

        public String osCommand(OSType osType) {
            return osCommand.get(osType.typeOf(OSType.NIX)? OSType.NIX : OSType.WINDOWS);
        }
    }
}
