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

    public static ServerOptions parseArguments(String[] args) {
    	ServerOptions serverOptions = new ServerOptions();
    	parseArguments(args,serverOptions);
    	return serverOptions;
    }

    @SuppressWarnings("static-access")
    public static ServerOptions parseArguments(String[] args, ServerOptions serverOptions) {
        parser = new PosixParser();
        options.addOption( OptionBuilder
                .withLongOpt( "config" )
                .withDescription( "config file" )
                .hasArg().withArgName("file")
                .create("c") );
        
        options.addOption( OptionBuilder
                .withDescription( "path to war" )
                .hasArg()
                .withArgName("path")
                .create("war") );
        
        options.addOption( OptionBuilder
                .withLongOpt( "server-name" )
                .withDescription( "server name (default)" )
                .hasArg()
                .withArgName("name")
                .create("name") );
        
        options.addOption( OptionBuilder
                .withLongOpt( "context-path" )
                .withDescription( "context path.  (/)" )
                .hasArg().withArgName("context")
                .create("context") );
        
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
        		.withLongOpt( "stop-password" )
        		.withDescription( "Pasword checked when stopping server\n" )
        		.hasArg().withArgName("password")
        		.create("password") );
        
        options.addOption( OptionBuilder
                .withDescription( "stop backgrounded.  Optional stop-port" )
                .hasOptionalArg().withArgName("port")
                .hasOptionalArg().withArgName("password")
                .withValueSeparator(' ')
                .create("stop") );
        
        options.addOption( OptionBuilder
        		.withLongOpt( "http-enable" )
        		.withDescription( "Enable HTTP.  Default is true ,unless SSL or AJP are enabled." )
        		.hasArg().withArgName("true|false").withType(Boolean.class)
        		.create("httpenable") );
        
        options.addOption( OptionBuilder
                .withLongOpt( "ajp-enable" )
                .withDescription( "Enable AJP.  Default is false.  When enabled, http is disabled by default." )
                .hasArg().withArgName("true|false").withType(Boolean.class)
                .create("ajpenable") );
        
        options.addOption( OptionBuilder
                .withLongOpt( "urlrewrite-enable" )
                .withDescription( "Enable URL Rewriting.  Default is true." )
                .hasArg().withArgName("true|false").withType(Boolean.class)
                .create("urlrewriteenable") );

        options.addOption( OptionBuilder
                .withLongOpt( "urlrewrite-file" )
                .withDescription( "URL rewriting config file." )
                .hasArg().withArgName("path/to/urlrewrite/file")
                .create("urlrewritefile") );

        options.addOption( OptionBuilder
        		.withLongOpt( "ssl-enable" )
        		.withDescription( "Enable SSL.  Default is false.  When enabled, http is disabled by default." )
        		.hasArg().withArgName("true|false").withType(Boolean.class)
        		.create("sslenable") );
        
        options.addOption( OptionBuilder
        		.withLongOpt( "ssl-port" )
        		.withDescription( "SSL port.  Disabled if not set." )
        		.hasArg().withArgName("port").withType(Number.class)
        		.create("sslport") );
        
        options.addOption( OptionBuilder
        		.withLongOpt( "ssl-cert" )
        		.withDescription( "SSL certificate file in x509 (PKS#12) format." )
        		.hasArg().withArgName("certificate")
        		.create("sslcert") );
        
        options.addOption( OptionBuilder
        		.withLongOpt( "ssl-key" )
        		.withDescription( "SSL private key file in DER (PKS#8) format." )
        		.hasArg().withArgName("key")
        		.create("sslkey") );
        
        options.addOption( OptionBuilder
        		.withLongOpt( "ssl-keypass" )
        		.withDescription( "SSL key passphrase." )
        		.hasArg().withArgName("passphrase")
        		.create("sslkeypass") );
        
        options.addOption( OptionBuilder
                .withLongOpt( "ajp-port" )
                .withDescription( "AJP port.  Disabled if not set." )
                .hasArg().withArgName("ajp port").withType(Number.class)
                .create("ajpport") );
        
        options.addOption( OptionBuilder
                .withLongOpt( "log-dir" )
                .withDescription( "Log directory.  (WEB-INF/logs)" )
                .hasArg().withArgName("path/to/log/dir")
                .create("logdir") );

        options.addOption( OptionBuilder
                .withLongOpt( "request-log" )
                .withDescription( "Log requests to specified file" )
                .hasArg().withArgName("/path/to/log")
                .create("requestlog") );

        options.addOption( OptionBuilder
                .withLongOpt( "dirs" )
                .withDescription( "List of external directories to serve from" )
                .hasArg().withArgName("path,path,...")
                .create("d") );
        
        options.addOption( OptionBuilder
                .withLongOpt( "lib-dirs" )
                .withDescription( "List of directories to add contents of to classloader" )
                .hasArg().withArgName("path,path,...")
                .create("libdirs") );
        
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
                .withLongOpt( "pid-file" )
                .withDescription( "Process ID file." )
                .hasArg().withArgName("pidfile")
                .create("pidfile") );

        options.addOption( OptionBuilder
                .withLongOpt( "timeout" )
                .withDescription( "Startup timout for background process. (50)\n" )
                .hasArg().withArgName("seconds").withType(Number.class)
                .create("t") );

        options.addOption( OptionBuilder
                .withLongOpt( "log-level" )
                .withDescription( "log level [DEBUG|INFO|WARN|ERROR] (WARN)" )
                .hasArg().withArgName("level")
                .create("level") );

        options.addOption( OptionBuilder
                .withLongOpt( "debug-enable" )
                .withDescription( "set log level to debug" )
                .hasArg().withArgName("true|false").withType(Boolean.class)
                .create("debug") );
        
        options.addOption( OptionBuilder
                .withLongOpt( "processname" )
                .withDescription( "Process name where applicable" )
                .hasArg().withArgName("name")
                .create("procname") );

        options.addOption( OptionBuilder
                .withLongOpt( "tray-icon" )
                .withDescription( "tray icon and OS X dock icon png image" )
                .hasArg().withArgName("path")
                .create("icon") );

        options.addOption( OptionBuilder
                .withLongOpt( "tray-config" )
                .withDescription( "tray menu config path" )
                .hasArg().withArgName("path")
                .create("trayconfig") );
        
        options.addOption( OptionBuilder
                .withLongOpt( "web-xml-path" )
                .withDescription( "full path to default web.xml file for configuring the server" )
                .hasArg().withArgName("path")
                .create("webxmlpath") );
        
        options.addOption( OptionBuilder
                .withLongOpt( "railo-web-config" )
                .withDescription( "full path to railo web config directory" )
                .hasArg().withArgName("path")
                .create("railoweb") );
        
        options.addOption( OptionBuilder
                .withLongOpt( "railo-server-config" )
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
                .withLongOpt( "welcome-files" )
                .withDescription( "comma delinated list of welcome files used if no web.xml file exists" )
                .hasArg().withArgName("index.cfm,default.cfm,...")
                .create("welcomefiles") );

        options.addOption( OptionBuilder
                .withLongOpt( "directory-index" )
                .withDescription( "enable directory browsing" )
                .hasArg().withArgName("true|false").withType(Boolean.class)
                .create("directoryindex") );
        
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
            if (line.hasOption("c")) {
                String config = line.getOptionValue("c");
                serverOptions = new ConfigParser(getFile(config)).getServerOptions();
                serverOptions.setConfigFile(getFile(config));
                return serverOptions;
            }
            
            if (line.hasOption("name")) {
                serverOptions.setServerName(line.getOptionValue("name"));
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
            if (line.hasOption("libdirs")) {
                String[] list = line.getOptionValue("libdirs").split(",");
                for (String path : list) {
                    File lib = new File(path);
                    if (!lib.exists() || !lib.isDirectory())
                        printUsage("No such lib directory "+path,1);
                }               
                serverOptions.setLibDirs(line.getOptionValue("libdirs"));
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
            if (line.hasOption("password")) {
            	serverOptions.setStopPassword(line.getOptionValue("password").toCharArray());
            }
            if (line.hasOption("stop-port")) {
                serverOptions.setSocketNumber(((Number)line.getParsedOptionValue("stop-port")).intValue());
            }
            if (line.hasOption("war")) {
                String warPath = line.getOptionValue("war");
                serverOptions.setWarFile(getFile(warPath));
            } else if (!line.hasOption("stop") && !line.hasOption("c")) {
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
                serverOptions.setAction("stop");
                String[] values = line.getOptionValues("stop");
                if(values != null && values.length > 0) {
                    serverOptions.setSocketNumber(Integer.parseInt(values[0])); 
                }
                if(values != null && values.length >= 1) {
                	serverOptions.setStopPassword(values[1].toCharArray()); 
                }
            } else {
                serverOptions.setAction("start");
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
            if (line.hasOption("ajpport")) {
                serverOptions.setEnableHTTP(false)
                	.setEnableAJP(true).setAJPPort(((Number)line.getParsedOptionValue("ajpport")).intValue());
            }
            if (line.hasOption("sslport")) {
            	serverOptions.setEnableHTTP(false).setEnableSSL(true).setSSLPort(((Number)line.getParsedOptionValue("sslport")).intValue());
            }
            if (line.hasOption("sslcert")) {
            	serverOptions.setSSLCertificate(getFile(line.getOptionValue("sslcert")));
                if (!line.hasOption("sslkey") || !line.hasOption("sslkey")) {
                    throw new RuntimeException("Using a SSL certificate requires -sslkey /path/to/file and -sslkeypass pass**** arguments!");  	
                }
            }
            if (line.hasOption("sslkey")) {
            	serverOptions.setSSLKey(getFile(line.getOptionValue("sslkey")));
            }
            if (line.hasOption("sslkeypass")) {
            	serverOptions.setSSLKeyPass(line.getOptionValue("sslkeypass").toCharArray());
            }
            if (line.hasOption("urlrewritefile")) {
                serverOptions.setURLRewriteFile(getFile(line.getOptionValue("urlrewritefile")));
            }
            if (line.hasOption("enableajp")) {
            	serverOptions.setEnableAJP(Boolean.valueOf(line.getOptionValue("enableajp")));
            }
            if (line.hasOption("enablessl")) {
                serverOptions.setEnableHTTP(false).setEnableSSL(Boolean.valueOf(line.getOptionValue("enablessl")));
            }
            if (line.hasOption("enablehttp")) {
            	serverOptions.setEnableHTTP(Boolean.valueOf(line.getOptionValue("enablehttp")));
            }
            if (line.hasOption("logdir")) {
                serverOptions.setLogDir(line.getOptionValue("logdir"));
            } else {
                if(serverOptions.getWarFile() != null){
                	File warFile = serverOptions.getWarFile();
                	String logDir;
                	if(warFile.isDirectory() && new File(warFile,"WEB-INF").exists()) {
                		logDir = warFile.getPath() + "/WEB-INF/logs/";
                	} else {
                		String serverConfigDir = System.getProperty("railo.server.config.dir");
                		if(serverConfigDir == null) {
                			logDir = new File(Server.getThisJarLocation().getParentFile(),"engine/railo/server/log/").getAbsolutePath();
                		} else {
                			logDir = new File(serverConfigDir,"log/").getAbsolutePath();                        
                		}
                	}
                	serverOptions.setLogDir(logDir);
                }
            }
            if(serverOptions.getWarFile() != null){
            	serverOptions.setCfmlDirs(serverOptions.getWarFile().getAbsolutePath());
            }
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

            if (line.hasOption("trayconfig")) {
                serverOptions.setTrayConfig(getFile(line.getOptionValue("trayconfig")));
            }
            
            if (line.hasOption("railoserver")) {
                serverOptions.setRailoConfigServerDir(line.getOptionValue("railoserver"));
            }
            if (line.hasOption("railoweb")) {
                serverOptions.setRailoConfigWebDir(line.getOptionValue("railoweb"));
            }
    	    if(serverOptions.getLoglevel().equals("DEBUG")) {
    	    	for(Option arg: line.getOptions()) {
    	    		log.debug(arg);
    	    		log.debug(arg.getValue());
    	    	}
    	    }
            return serverOptions;
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
    
    static File getFile(String path) {
        File file = new File(path);
        if(!file.exists()) {
            throw new RuntimeException("File not found: " + path);
        }
    	return file;
    }

    static void printUsage(String message, int exitCode) {
        HelpFormatter formatter = new HelpFormatter();
        formatter.setOptionComparator(new Comparator<Option>() {
            public int compare(Option o1, Option o2) {
                if(o1.getOpt().equals("war")) {return -1;} else if(o2.getOpt().equals("war")) {return 1;}
                if(o1.getOpt().equals("p")) {return -1;} else if(o2.getOpt().equals("p")) {return 1;}
                if(o1.getOpt().equals("c")) { return -1; } else if(o2.getOpt().equals("c")) {return 1;}
                if(o1.getOpt().equals("context")) { return -1; } else if(o2.getOpt().equals("context")) {return 1;}
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
        if(exitCode == 0) {
            formatter.printHelp(80, SYNTAX, message + '\n' + HEADER, options, FOOTER, false);
        } else {
            System.out.println("USAGE:  " + SYNTAX + '\n' + message);
        }
        System.exit(exitCode);
    }


}
