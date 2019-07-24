package runwar;

//import daevil.Daevil;
//import daevil.OSType;
import runwar.options.ServerOptions;
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
import java.util.regex.Pattern;

import static runwar.LaunchUtil.displayMessage;

public class Service {

    private ServerOptionsImpl serverOptions;
    private Path serverJsonFile;
    //private List<Command> commands;
    //private Daevil daevil;

    public Service(ServerOptionsImpl serverOptions) {
       // this(serverOptions, defaultServerJsonFile());
    }

    /*public List<Command> commands(){
        return commands;
    }*/

   /* public Service(ServerOptionsImpl serverOptions, Path serverJsonFile) {
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
    }*/

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

    /*public Path serviceControlScriptPath() {
        return Paths.get("./" + daevil.controlScript(OSType.host()).fileName);
    }*/

    /*public List<Command> serviceScriptCommands(Path serviceControlScriptPath) {
        String controlScript = LaunchUtil.readFile(serviceControlScriptPath().toFile());
        List<Command> commands = new ArrayList<>();
        Pattern commandPattern = Pattern.compile("^.*function.*");
        return commands;
    }*/

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

    /*public Path generateServiceScripts() throws IOException {
        return generateServiceScripts(serverJsonFile.getParent());
    }*/

    /*public Path generateServiceScripts(Path path) throws IOException {
        String jarPath = jarPath();
//        String war = Paths.get(".").relativize(serverOptions.warFile().toPath()).toFile().getCanonicalPath();

        serverOptions.pidFile(daevil.pidFile.get());
        if (!serverJsonFile.toFile().exists()) {
            serverOptions.toJson(serverJsonFile);
        } else {
            displayMessage("info","Not overwriting " + serverJsonFile);
        }

        String war = serverOptions.warFile().getCanonicalPath();
        //Daevil.log.info("Jar: " + jarPath);
        //Daevil.log.info("war: " + war);
        String name = serverOptions.serverName();

        daevil.javaResolver();
        daevil.description.set("Service control for " + name);
        daevil.controlScript().fileName.set(daevil.ctlScript.get());
        daevil.logFileOut.set("/var/log/" + name + ".log");
        daevil.pidFile.set("/var/run/@{name}/@{name}.pid".replace("@{name}", name));

        commands.forEach(command -> {
            daevil.addOption(command.name, command.description)
                    .command(OSType.WINDOWS, command.osCommand(OSType.WINDOWS))
                    .command(OSType.NIX, command.osCommand(OSType.NIX));

        });


        daevil.serviceImg.set("/icon.png");

        daevil.generateScripts(OSType.host(), path);
        Daevil.log.info("Generated service control script: " + path.toAbsolutePath().toString() + '/' + daevil.ctlScript.get());
        Daevil.log.info(daevil.controlScript(OSType.host()).displayCommands(OSType.host()));
        
        return path;
    }*/

   /* public class Command {
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
    }*/
}
