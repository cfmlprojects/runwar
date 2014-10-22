package runwar.options;

import java.io.File;
import java.util.Comparator;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.PosixParser;

import runwar.Server;
import runwar.Stop;
import runwar.logging.Logger;

public class CommandLineHandler {
    private static final Options options = new Options();
    private static PosixParser parser;
    private static final String SYNTAX = " java -jar runwar.jar [-war] path/to/war [options]";
    private static final String HEADER = " The runwar lib wraps undertow with more awwsome. Defaults (parenthetical)";
    private static final String FOOTER = " source: https://github.com/cfmlprojects/runwar.git";
    private static Logger log = Logger.getLogger("CommandLineHandler");
    
    public CommandLineHandler(){
    }

    @SuppressWarnings("static-access")
    public CommandLine parseArguments(String[] args, ServerOptions serverOptions) {
        parser = new PosixParser();
        options.addOption( OptionBuilder
                .withDescription( "path to war" )
                .hasArg()
                .withArgName("path")
                .create("war") );
        
        options.addOption( OptionBuilder
                .withLongOpt( "context" )
                .withDescription( "context path.  (/)" )
                .hasArg().withArgName("context")
                .create("c") );
        
        options.addOption( OptionBuilder
                .withDescription( "host.  (127.0.0.1)" )
                .hasArg().withArgName("host")
                .create("host") );
        
        options.addOption( OptionBuilder
                .withLongOpt( "port" )
                .withDescription( "port number.  (8088)" )
                .hasArg().withArgName("http port").withType(Number.class)
                .create('p') );
        
        options.addOption( OptionBuilder
                .withLongOpt( "stop-port" )
                .withDescription( "stop listener port number. (8779)\n" )
                .hasArg().withArgName("port").withType(Number.class)
                .create("stopsocket") );
        
        options.addOption( OptionBuilder
                .withDescription( "stop backgrounded.  Optional stop-port" )
                .hasOptionalArg().withArgName("stop port")
                .create("stop") );
        
        options.addOption( OptionBuilder
                .withLongOpt( "enable-ajp" )
                .withDescription( "Enable AJP.  Default is false" )
                .hasArg().withArgName("true|false").withType(Boolean.class)
                .create("enableajp") );
        
        options.addOption( OptionBuilder
                .withLongOpt( "ajp-port" )
                .withDescription( "AJP port.  Disabled if not set." )
                .hasArg().withArgName("ajp port").withType(Number.class)
                .create("ajp") );
        
        options.addOption( OptionBuilder
                .withLongOpt( "log-dir" )
                .withDescription( "Log directory.  (WEB-INF/logs)" )
                .hasArg().withArgName("path/to/log/dir")
                .create("logdir") );

        options.addOption( OptionBuilder
                .withLongOpt( "dirs" )
                .withDescription( "List of external directories to serve from" )
                .hasArg().withArgName("path,path,...")
                .create("d") );
        
        options.addOption( OptionBuilder
                .withLongOpt( "libdir" )
                .withDescription( "List of directories to add contents of to classloader" )
                .hasArg().withArgName("path,path,...")
                .create("libs") );
        
        options.addOption( OptionBuilder
                .withLongOpt( "jar" )
                .withDescription( "jar to be added to classpath" )
                .hasArg().withArgName("path")
                .create("j") );
        
        options.addOption( OptionBuilder
                .withLongOpt( "background" )
                .withDescription( "Run in background (true)" )
                .hasArg().withArgName("true|false").withType(Boolean.class)
                .create('b') );
        
        options.addOption( OptionBuilder
                .withDescription( "Log requests to specified file" )
                .hasArg().withArgName("/path/to/log")
                .create("requestlog") );

        options.addOption( OptionBuilder
                .withLongOpt( "open-browser" )
                .withDescription( "Open default web browser after start (false)" )
                .hasArg().withArgName("true|false")
                .create("open") );

        options.addOption( OptionBuilder
                .withLongOpt( "open-url" )
                .withDescription( "URL to open browser to. (http://$host:$port)\n" )
                .hasArg().withArgName("url")
                .create("url") );
        
        options.addOption( OptionBuilder
                .withDescription( "Process ID file." )
                .hasArg().withArgName("pidfile")
                .create("pidfile") );

        options.addOption( OptionBuilder
                .withLongOpt( "timeout" )
                .withDescription( "Startup timout for background process. (50)\n" )
                .hasArg().withArgName("seconds").withType(Number.class)
                .create("t") );

        options.addOption( OptionBuilder
                .withLongOpt( "loglevel" )
                .withDescription( "log level [DEBUG|INFO|WARN|ERROR] (WARN)" )
                .hasArg().withArgName("level")
                .create("level") );

        options.addOption( OptionBuilder
                .withDescription( "set log level to debug" )
                .hasArg().withArgName("true|false").withType(Boolean.class)
                .create("debug") );
        
        options.addOption( OptionBuilder
                .withLongOpt( "processname" )
                .withDescription( "Process name where applicable" )
                .hasArg().withArgName("name")
                .create("procname") );

        options.addOption( OptionBuilder
                .withLongOpt( "iconpath" )
                .withDescription( "tray icon and OS X dock icon png image" )
                .hasArg().withArgName("path")
                .create("icon") );

        options.addOption( OptionBuilder
                .withLongOpt( "webxmlpath" )
                .withDescription( "full path to default web.xml file for configuring the server" )
                .hasArg().withArgName("path")
                .create("webxmlpath") );
        
        options.addOption( OptionBuilder
                .withLongOpt( "railoweb" )
                .withDescription( "full path to railo web config directory" )
                .hasArg().withArgName("path")
                .create("railoweb") );
        
        options.addOption( OptionBuilder
                .withLongOpt( "railoserver" )
                .withDescription( "full path to railo server config directory" )
                .hasArg().withArgName("path")
                .create("railoserver") );
        
        options.addOption( OptionBuilder.withArgName( "property=value" )
                .withLongOpt( "sysprop" )
                .hasArgs(2)
                .withValueSeparator()
                .withDescription( "system property to set" )
                .create("D") );
        
        options.addOption( OptionBuilder
                .withDescription( "comma delinated list of welcome files used if no web.xml file exists" )
                .hasArg().withArgName("index.cfm,default.cfm,...")
                .create("welcomefiles") );

        options.addOption( OptionBuilder
                .withDescription( "enable directory browsing" )
                .hasArg().withArgName("true|false").withType(Boolean.class)
                .create("directorylist") );
        
        options.addOption( new Option( "h", "help", false, "print this message" ) );
        options.addOption( new Option( "v", "version", false, "print runwar version and undertow version" ) );


        try {
            CommandLine line = parser.parse( options, args );
            // parse the command line arguments
            if (line.hasOption("help")) {
                printUsage("Options",0);
            }
            if (line.hasOption("version")) {
                Server.printVersion();
                System.exit(0);
            }
            if (line.hasOption("loglevel")) {
                serverOptions.setLoglevel(line.getOptionValue("loglevel"));
            }

            if (line.hasOption("debug")) {
                Boolean debug= Boolean.valueOf(line.getOptionValue("debug"));
                serverOptions.setDebug(debug);
                if(debug)serverOptions.setLoglevel("DEBUG");
                if(line.hasOption("loglevel")) {
                    System.out.println("Warning:  debug overrides loglevel (both are specified, setting level to DEBUG)");
                }
            }

            if (line.hasOption("background")) {
                serverOptions.setBackground(Boolean.valueOf(line.getOptionValue("background")));
            }
            if (line.hasOption("libs")) {
                String[] list = line.getOptionValue("libs").split(",");
                for (String path : list) {
                    File lib = new File(path);
                    if (!lib.exists() || !lib.isDirectory())
                        printUsage("No such lib directory "+path,1);
                }               
                serverOptions.setLibDirs(line.getOptionValue("libs"));
            }
            if (line.hasOption("welcomefiles")) {
                serverOptions.setWelcomeFiles(line.getOptionValue("welcomefiles").split(","));
            }

            if (line.hasOption("jar")) {
                 File jar = new File(line.getOptionValue("jar"));
                    if (!jar.exists() || jar.isDirectory())
                        printUsage("No such jar "+jar,1);
                    serverOptions.setJarURL(jar.toURI().toURL());
            }
            
            if (line.hasOption("timeout")) {
                serverOptions.setLaunchTimeout(((Number)line.getParsedOptionValue("timeout")).intValue() * 1000);
            }
            if (line.hasOption("stop-port")) {
                serverOptions.setSocketNumber(((Number)line.getParsedOptionValue("stop-port")).intValue());
            }
            if (line.hasOption("war")) {
                String warPath = line.getOptionValue("war");
                File warFile = new File(warPath);
                if(warFile.exists()) {
                    serverOptions.setWarFile(warFile);
                } else {
                    throw new RuntimeException("Could not find war! " + warPath);
                }
            } else if (!line.hasOption("stop")) {
                printUsage("Must specify -war path/to/war, or -stop [-stop-socket]",1);
            } 
            if(line.hasOption("D")){
                final String[] properties = line.getOptionValues("D");
                for (int i = 0; i < properties.length; i++) {
                    log.debugf("setting system property: %s", properties[i].toString()+'='+properties[i+1].toString());
                    System.setProperty(properties[i].toString(),properties[i+1].toString());
                    i++;
                }
            }

            if (line.hasOption("webxmlpath")) {
                String webXmlPath = line.getOptionValue("webxmlpath");
                File webXmlFile = new File(webXmlPath);
                if(webXmlFile.exists()) {
                    serverOptions.setWebXmlFile(webXmlFile);
                } else {
                    throw new RuntimeException("Could not find web.xml! " + webXmlPath);
                }
            }

            if (line.hasOption("stop")) {
                int socketNumber = serverOptions.getSocketNumber();
                if(line.getOptionValue("stop")!=null) {
                    socketNumber = Integer.parseInt(line.getOptionValue("stop")); 
                }
                new Stop().main(new String[] {Integer.toString(socketNumber)});
            }

            if (line.hasOption("context")) {
                serverOptions.setContextPath(line.getOptionValue("context"));
            }
            if (line.hasOption("host")) {
                serverOptions.setHost(line.getOptionValue("host"));
            }
            if (line.hasOption("port")) {
                serverOptions.setPortNumber(((Number)line.getParsedOptionValue("port")).intValue());
            }
            if (line.hasOption("enable-ajp")) {
                serverOptions.setEnableAJP(Boolean.valueOf(line.getOptionValue("enable-ajp")));
            }
            if (line.hasOption("ajp")) {
                serverOptions.setAjpPort(((Number)line.getParsedOptionValue("ajp")).intValue());
            }
            if (line.hasOption("logdir")) {
                serverOptions.setLogDir(line.getOptionValue("logdir"));
            } else {
                File warFile = serverOptions.getWarFile();
                String logDir;
                if(warFile.isDirectory() && new File(warFile,"WEB-INF").exists()) {
                    logDir = warFile.getPath() + "/WEB-INF/logs/";
                } else {
                    String serverConfigDir = System.getProperty("railo.server.config.dir");
                    if(serverConfigDir == null) {
                        logDir = new File(Server.getThisJarLocation().getParentFile(),"server/log/").getAbsolutePath();
                    } else {
                        logDir = new File(serverConfigDir,"log/").getAbsolutePath();                        
                    }
                }
                serverOptions.setLogDir(logDir);
            }
            serverOptions.setCfmlDirs(serverOptions.getWarFile().getAbsolutePath());
            if (line.hasOption("dirs")) {
                serverOptions.setCfmlDirs(line.getOptionValue("dirs"));
            }
            if (line.hasOption("requestlog")) {
                serverOptions.setKeepRequestLog(Boolean.valueOf(line.getOptionValue("requestlog")));
            }
            
            if (line.hasOption("open-browser")) {
                serverOptions.setOpenbrowser(Boolean.valueOf(line.getOptionValue("open")));
            }
            if (line.hasOption("open-url")) {
                serverOptions.setOpenbrowserURL(line.getOptionValue("open-url"));
            }

            if (line.hasOption("pidfile")) {
                serverOptions.setPidFile(line.getOptionValue("pidfile"));
            }

            if (line.hasOption("processname")) {
                serverOptions.setProcessName(line.getOptionValue("processname"));
            }

            if (line.hasOption("icon")) {
                serverOptions.setIconImage(line.getOptionValue("icon"));
            }

            if (line.hasOption("railoserver")) {
                serverOptions.setRailoConfigServerDir(line.getOptionValue("railoserver"));
            }
            if (line.hasOption("railoweb")) {
                serverOptions.setRailoConfigWebDir(line.getOptionValue("railoweb"));
            }
            return line;
        }
        catch( Exception exp ) {
            String msg = exp.getMessage();
            if(msg == null){
                msg = "null : "+exp.getStackTrace()[0].toString();
            }
            printUsage(msg,1);
        }
        return null;
    }    

    static void printUsage(String message, int exitCode) {
        HelpFormatter formatter = new HelpFormatter();
        formatter.setOptionComparator(new Comparator<Option>() {
            public int compare(Option o1, Option o2) {
                if(o1.getOpt().equals("war")) {return -1;} else if(o2.getOpt().equals("war")) {return 1;}
                if(o1.getOpt().equals("p")) {return -1;} else if(o2.getOpt().equals("p")) {return 1;}
                if(o1.getOpt().equals("c")) { return -1; } else if(o2.getOpt().equals("c")) {return 1;}
                if(o1.getOpt().equals("d")) { return -1; } else if(o2.getOpt().equals("d")) {return 1;}
                if(o1.getOpt().equals("b")) { return -1; } else if(o2.getOpt().equals("b")) {return 1;}
                if(o1.getOpt().equals("h")) {return 1;} else if(o2.getOpt().equals("h")) {return -1;}
                if(o1.getOpt().equals("url")) {return 1;} else if(o2.getOpt().equals("url")) {return -1;}
                if(o1.getOpt().equals("open")) {return 1;} else if(o2.getOpt().equals("open")) {return -1;}
                if(o1.getOpt().equals("stopsocket")) {return 1;} else if(o2.getOpt().equals("stopsocket")) {return -1;}
                if(o1.getOpt().equals("stop")) {return 1;} else if(o2.getOpt().equals("stop")) {return -1;}
                return o1.getOpt().compareTo(o2.getOpt());
            }
        });
        formatter.setWidth(80);
        formatter.setSyntaxPrefix("USAGE:");
        formatter.setLongOptPrefix("--");
        //formatter.printHelp( SYNTAX, options,false);
        formatter.printHelp(80, SYNTAX, message + '\n' + HEADER, options, FOOTER, false);
        System.exit(exitCode);
    }


}
