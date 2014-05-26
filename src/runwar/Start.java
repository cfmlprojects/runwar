package runwar;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLDecoder;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.SimpleFormatter;
import java.awt.AWTException;
import java.awt.Image;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.Toolkit;
import java.awt.TrayIcon;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.imageio.ImageIO;
import javax.net.SocketFactory;
import javax.servlet.ServletException;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.jboss.logging.Logger;

import io.undertow.Handlers;
import io.undertow.Undertow;
import io.undertow.Undertow.Builder;
import io.undertow.UndertowLogger;
import io.undertow.server.HandlerWrapper;
import io.undertow.server.HttpHandler;
import io.undertow.server.handlers.PathHandler;
import io.undertow.server.handlers.resource.FileResourceManager;
import io.undertow.server.handlers.resource.ResourceHandler;
import io.undertow.server.protocol.ajp.AjpOpenListener;
import io.undertow.servlet.api.DeploymentInfo;
import io.undertow.servlet.api.DeploymentManager;
import static io.undertow.servlet.Servlets.defaultContainer;
import static io.undertow.servlet.Servlets.deployment;
import static io.undertow.servlet.Servlets.servlet;
import static io.undertow.Handlers.resource;

//import railo.loader.engine.CFMLEngine;

public class Start {

	private static Logger log = Logger.getLogger("RunwarLogger");
	private static String loglevel = "WARN";

	private static final Options options = new Options();
	private static String PID;

	private static String warPath;
	private static String contextPath = "/";
	private static String host = "127.0.0.1";
	private static int portNumber = 8088;
	private static int ajpPort = 8009;
	private static int socketNumber = 8779;
	private static String logDir;
	private static String cfmlDirs;
	private static boolean background = true;
	private static boolean keepRequestLog = false;
	private static boolean openbrowser = false;
	private static String openbrowserURL;
	private static String pidFile;
	private static boolean enableAJP;
	private static int launchTimeout = 50 * 1000; // 50 secs
	private static PosixParser parser;
	private static final String SYNTAX = " java -jar runwar.jar [-war] path/to/war [options]";
	private static final String HEADER = " The runwar lib wraps undertow with more awwsome. Defaults (parenthetical)";
	private static final String FOOTER = " source: https://github.com/cfmlprojects/runwar.git";
	private static String processName= "RunWAR";
	private static URLClassLoader _classLoader;
	private static String libDirs = null;
	private static URL jarURL = null;
    private static final File thisJarLocation = new File(Start.class.getProtectionDomain().getCodeSource().getLocation().getPath()).getParentFile();

	static TrayIcon trayIcon;
	private static boolean debug = false;
	private static File warFile;
	private static String iconImage = null;
	private static DeploymentManager manager;
	private static Undertow server;
	public static final Set<String> loggers = new HashSet<String>(Arrays.asList(new String[] {
			"RunwarLogger",
			"org.jboss.logging",
			"org.xnio.Xnio",
			"org.xnio.nio.NioXnio",
			"io.undertow.UndertowLogger"
	}));
	public static final String bar = "*******************************************************************";
	
	// for openBrowser 
	public Start(int seconds) {
		Timer timer = new Timer();
		timer.schedule(this.new OpenBrowserTask(), seconds * 1000);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static void main(String[] args) throws Exception {
		CommandLine line = parseArguments(args);
		subvertLoggers(loglevel, loggers);
		if (background) {
			// this will eventually system.exit();
			List<String> argarray = new ArrayList<String>();
			for(String arg : args) {
				if(arg.contains("background")||arg.startsWith("-b")) {
					continue;
				} else {
					argarray.add(arg);
				}
			}
			argarray.add("--background");
			argarray.add("false");
			LaunchUtil.relaunchAsBackgroundProcess(launchTimeout, argarray.toArray(new String[argarray.size()]), processName);
			// just in case
			Thread.sleep(200);
			System.exit(0);
		}
		TeeOutputStream tee = null;
	    if(loglevel.equals("DEBUG")) {
	    	for(Option arg: line.getOptions()) {
	    		log.debug(arg);
	    		log.debug(arg.getValue());
	    	}
	    }

	    if(logDir != null) {
			File logDirectory = new File(logDir);
			logDirectory.mkdir();
			if(logDirectory.exists()) {
				log.info("Logging to " + logDirectory + "/server.out.txt");
				tee = new TeeOutputStream(System.out, new FileOutputStream(logDirectory + "/server.out.txt"));
				PrintStream newOut = new PrintStream(tee, true);
				System.setOut(newOut);
				System.setErr(newOut);
			} else {
				log.error("Could not create log: " + logDirectory + "/server.out.txt");
			}
		}

		new AgentInitialization().loadAgentFromLocalJarFile(new File(warFile,"/WEB-INF/lib/"));

		// hack to prevent . being picked up as the system path (jacob.x.dll)
		if(System.getProperty("java.library.path") == null) {
			System.setProperty("java.library.path",thisJarLocation.getPath());
		} else {
			System.setProperty("java.library.path",thisJarLocation.getPath() + ":" + System.getProperty("java.library.path"));
		}
        String osName = System.getProperties().getProperty("os.name");
        String dockIconPath = System.getProperty("cfml.server.dockicon");
        if(osName != null && osName.startsWith("Mac OS X") && (dockIconPath == null || dockIconPath != "")){
        	Image dockIcon = ImageIO.read(Start.class.getResource("/runwar/icon.png"));
    		if(dockIconPath != null) {
				URL imageURL = Start.class.getClassLoader().getParent().getResource(dockIconPath);
				if(imageURL == null) {
					dockIcon = ImageIO.read(Start.class.getResource(dockIconPath));
				}
				if(imageURL != null) {
					dockIcon = ImageIO.read(imageURL);
				} else {
					dockIcon = ImageIO.read(Start.class.getResource("/runwar/icon.png"));
				}
    		}
    		System.setProperty("com.apple.mrj.application.apple.menu.about.name",processName);
    		System.setProperty("com.apple.mrj.application.growbox.intrudes","false");
    		System.setProperty("apple.laf.useScreenMenuBar","true");
    		System.setProperty("-Xdock:name",processName);
            try{
            	Class<?> appClass = Class.forName("com.apple.eawt.Application");
            	Method getAppMethod = appClass.getMethod("getApplication");
            	Object appInstance = getAppMethod.invoke(null);
            	Method dockMethod = appInstance.getClass().getMethod("setDockIconImage", java.awt.Image.class);
            	dockMethod.invoke(appInstance, dockIcon);
            }
            catch(Exception e) { }
        }
		System.out.println(bar);
		System.out.println("Starting - port:" + portNumber + " stop-port:" + socketNumber + " warpath: " + warPath);
		portNumber = getPortOrErrorOut(portNumber,host);
		socketNumber = getPortOrErrorOut(socketNumber,host);			
		System.out.println("contextPath: " + contextPath);
		System.out.println("Log Directory: " + logDir);
		System.out.println(bar);
		//System.out.println("warpath:"+warPath+" contextPath:" + contextPath + " host:"+host+" port:" + portNumber + " cfml-dirs:"+cfmlDirs);
		//System.out.println("background: " + background);

		File webinf = new File(warFile,"WEB-INF");
		if(warFile.isDirectory() && webinf.exists()) {
			libDirs = webinf.getAbsolutePath() + "/lib";
			log.info("Using WEB-INF/lib of: " + libDirs);
		}
		if(libDirs != null || jarURL != null) {
			List<URL> cp=new ArrayList<URL>();
			if(libDirs!=null)
				cp.addAll(getJarList(libDirs));
			if(jarURL!=null)
				cp.add(jarURL);
			initClassLoader(cp);
		}

		DeploymentInfo servletBuilder = deployment()
                .setContextPath( contextPath.equals("/") ? "" : contextPath )
                .setDeploymentName(warPath);

		if(!warFile.exists()) {
			throw new RuntimeException("war does not exist: " + warFile.getAbsolutePath());
		}
		
		if(warFile.isDirectory() && !webinf.exists()) {
	        String webConfigDir = System.getProperty("railo.web.config.dir");
	        if(webConfigDir == null) {
	        	File webConfigDirFile = new File(thisJarLocation.getParentFile(),"server/railo-web/");
				webConfigDir = webConfigDirFile.getPath();
	        }
	        log.debug("railo.web.config.dir: " + webConfigDir);
	        String serverConfigDir = System.getProperty("railo.server.config.dir");
	        if(serverConfigDir == null) {
	        	File serverConfigDirFile = new File(thisJarLocation.getParentFile(),"server/");
	        	serverConfigDir = serverConfigDirFile.getAbsolutePath();
	        }
	        log.debug("railo.server.config.dir: " + serverConfigDir);
	        String webinfDir = System.getProperty("railo.webinf");
	        if(webinfDir == null) {
	        	webinfDir = new File(serverConfigDir,"WEB-INF/").getPath();
	        }
	        log.debug("railo.webinf: " + webinfDir);
			servletBuilder.setClassLoader(Start.class.getClassLoader());
			Class cfmlServlet;
			Class restServlet;
			try{
				cfmlServlet = servletBuilder.getClass().getClassLoader().loadClass("railo.loader.servlet.CFMLServlet");
			} catch (java.lang.ClassNotFoundException e) {
				cfmlServlet = _classLoader.loadClass("railo.loader.servlet.CFMLServlet");
			}
			try{
				restServlet = servletBuilder.getClass().getClassLoader().loadClass("railo.loader.servlet.RestServlet");
			} catch (java.lang.ClassNotFoundException e) {
				restServlet = _classLoader.loadClass("railo.loader.servlet.RestServlet");
			}
			log.debug("loaded servlet classes");
			servletBuilder
            	.addWelcomePages(new String[] {"index.cfm","index.cfml","index.html","index.htm"})
            	.addServlets(
	                        servlet("CFMLServlet", cfmlServlet)
	                                .addInitParam("configuration",webConfigDir)
	                                .addInitParam("railo-server-root",serverConfigDir)
	                                .addMapping("*.cfm")
	                                .addMapping("*.cfc")
	                                .addMapping("/index.cfc/*")
	                                .addMapping("/index.cfm/*")
	                                .addMapping("/index.cfml/*")
	                                .setLoadOnStartup(1)
	                                ,
	                        servlet("RESTServlet", restServlet)
	                                .addInitParam("railo-web-directory",webConfigDir)
	                                .addMapping("/rest/*")
	                                .setLoadOnStartup(2));
//			servletBuilder.setResourceManager(new CFMLResourceManager(new File(homeDir,"server/"), 100, cfmlDirs));
			File internalRailoRoot = new File(webinfDir);
			internalRailoRoot.mkdirs();
			servletBuilder.setResourceManager(new CFMLResourceManager(warFile, 100, cfmlDirs, internalRailoRoot));
		} else if(webinf.exists()) {
			if(_classLoader == null) {
				throw new RuntimeException("FATAL: Could not load any libs for war: " + warFile.getAbsolutePath());				
			}
			servletBuilder.setClassLoader(_classLoader);
			servletBuilder.setResourceManager(new CFMLResourceManager(warFile, 100, cfmlDirs, webinf));
			UndertowWebXMLParser.parseWebXml(new File(webinf,"/web.xml"), servletBuilder);
		} else {
			throw new RuntimeException("Didn't know how to handle war:"+warFile.getAbsolutePath());
		}
		/*      
		servletBuilder.addInitialHandlerChainWrapper(new HandlerWrapper() {
            @Override
            public HttpHandler wrap(final HttpHandler handler) {
                return resource(new FileResourceManager(new File(libDir,"server/WEB-INF"), 100))
                        .setDirectoryListingEnabled(true);
            }
        });
		 */
		manager = defaultContainer().addDeployment(servletBuilder);
		manager.deploy();
        HttpHandler servletHandler = manager.start();
        log.debug("started manager");
        PathHandler pathHandler = Handlers.path(Handlers.redirect(contextPath))
                .addPrefixPath(contextPath, servletHandler);
        Builder serverBuilder = Undertow.builder()
        		.addHttpListener(portNumber, "localhost").setHandler(pathHandler);

        if(enableAJP) {
			log.info("Enabling AJP protocol on port " + ajpPort);
			serverBuilder.addAjpListener(ajpPort, "localhost");
		}

		try {
			PID = ManagementFactory.getRuntimeMXBean().getName().split("@")[0];
		} catch (Exception e) {
			log.error("Unable to get PID");
		}
		if (keepRequestLog) {
			log.info("request log currently unsupported");
		}

		Thread monitor = new MonitorThread(tee, socketNumber);
		monitor.start();
        log.debug("started stop monitor");
		String iconPNG = System.getProperty("cfml.server.trayicon");
		if( iconPNG != null) {
			iconImage = iconPNG;				
		}
		LaunchUtil.hookTray(iconImage, host, portNumber, socketNumber, processName, PID);
		log.debug("hooked system tray");

		if (openbrowser) {
			new Start(3);
		}

		server = serverBuilder.build();
		// if this println changes be sure to update the LaunchUtil so it will know success
        log.debug("starting server");
		System.out.println("Server is up - http-port:" + portNumber + " stop-port:" + socketNumber +" PID:" + PID);
		server.start();
	}

	private static void subvertLoggers(String level, Set<String> loggers) {
		System.setProperty("java.util.logging.ConsoleHandler.formatter", "java.util.logging.SimpleFormatter");
		System.setProperty("java.util.logging.SimpleFormatter.format", "%1$tY-%1$tm-%1$td %1$tH:%1$tM:%1$tS %4$s %2$s %5$s%6$s%n");
        java.util.logging.ConsoleHandler chandler = new java.util.logging.ConsoleHandler();
        if(level.trim().toUpperCase().equals("TRACE"))
        	level = "FINER";
        if(level.trim().toUpperCase().equals("WARN"))
        	level = "WARNING";
        if(level.trim().toUpperCase().equals("DEBUG"))
        	level = "FINEST";
        java.util.logging.Level LEVEL = java.util.logging.Level.parse(level);
        chandler.setLevel(LEVEL);
		java.util.logging.LogManager logManager = java.util.logging.LogManager.getLogManager();
		for(Enumeration<String> loggerNames = logManager.getLoggerNames(); loggerNames.hasMoreElements();){
	        String name = loggerNames.nextElement();
	        java.util.logging.Logger nextLogger = logManager.getLogger(name);
	        if(loggers.contains(name) && nextLogger != null) {
	        	nextLogger.setUseParentHandlers(false);
	        	nextLogger.setLevel(LEVEL);
	        	if(nextLogger.getHandlers() != null) {
	        		for(java.util.logging.Handler handler : nextLogger.getHandlers()) {
	        			nextLogger.removeHandler(handler);
	        		}
	        		nextLogger.addHandler(chandler);	        		
	        	}
	        }
	    }		
	}
	
	private static int getPortOrErrorOut(int portNumber, String host) {
        try {
			ServerSocket nextAvail = new ServerSocket(portNumber, 1, InetAddress.getByName(host));
			portNumber = nextAvail.getLocalPort();
			nextAvail.close();
			return portNumber;
		} catch (java.net.BindException e) {
			throw new RuntimeException("Error getting port "+portNumber+"!  Cannot start.  " + e.getMessage());
		} catch (UnknownHostException e) {
			throw new RuntimeException("Unknown host ("+host+")");
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	private static List<URL> getJarList(String libDirs) throws IOException {
		List<URL> classpath=new ArrayList<URL>();
		String[] list = libDirs.split(",");
		if (list == null)
			return classpath;

		for (String path : list) {
			if (".".equals(path) || "..".equals(path))
				continue;

			File file = new File(path);
			for(File item : file.listFiles()) {
				String fileName = item.getAbsolutePath();
				if (!item.isDirectory()) {
					if (fileName.toLowerCase().endsWith(".jar") || fileName.toLowerCase().endsWith(".zip")) {
						if(!fileName.toLowerCase().contains("servlet") && !fileName.toLowerCase().contains("runwar")) {
							URL url = item.toURI().toURL();
							classpath.add(url);
//							System.out.println("lib: added to classpath: "+fileName);
						}
					}
				}				
			}
		}
		return classpath;
	}

	protected static void initClassLoader(List<URL> _classpath) {
		if (_classLoader == null && _classpath != null && _classpath.size() > 0) {
			/*
			ClassLoader context = Thread.currentThread().getContextClassLoader();
			if (context == null)
				_classLoader = new URLClassLoader(_classpath.toArray(new URL[_classpath.size()]));
			else
				_classLoader = new URLClassLoader(_classpath.toArray(new URL[_classpath.size()]), context);
			Thread.currentThread().setContextClassLoader(_classLoader);
			System.out.println("OOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOUT"+_classpath.size());
			*/
			_classLoader = new URLClassLoader(_classpath.toArray(new URL[_classpath.size()]),Thread.currentThread().getContextClassLoader());
			//Thread.currentThread().setContextClassLoader(_classLoader);
		}
	}	
	
	@SuppressWarnings("static-access")
	private static CommandLine parseArguments(String[] args) {
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
				.withLongOpt( "host" )
				.withDescription( "host.  (127.0.0.1)" )
				.hasArg().withArgName("host")
				.create("o") );
		
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
		
		options.addOption( new Option( "h", "help", false, "print this message" ) );

		try {
			CommandLine line = parser.parse( options, args );
		    // parse the command line arguments
		    if (line.hasOption("help")) {
		    	printUsage("Options",0);
		    }
		    if (line.hasOption("background")) {
		    	background = Boolean.valueOf(line.getOptionValue("background"));
		    }
		    if (line.hasOption("libs")) {
                File lib = new File(line.getOptionValue("libs"));
                if (!lib.exists() || !lib.isDirectory())
                	printUsage("No such lib directory "+lib,1);
                libDirs = line.getOptionValue("libs");
            }

		    if (line.hasOption("jar")) {
		    	 File jar = new File(line.getOptionValue("jar"));
	                if (!jar.exists() || jar.isDirectory())
	                	printUsage("No such jar "+jar,1);
	                jarURL = jar.toURI().toURL();
	        }
		    
		    if (line.hasOption("timeout")) {
		    	launchTimeout = ((Number)line.getParsedOptionValue("timeout")).intValue() * 1000;
		    }
		    if (line.hasOption("stop-port")) {
		    	socketNumber = ((Number)line.getParsedOptionValue("stop-port")).intValue();
		    }
		    if (line.hasOption("war")) {
		    	warPath = line.getOptionValue("war");
		    	warFile = new File(warPath);
		    	if(warFile.exists()) {
		    		warPath = warFile.toURI().toURL().toString();
		    	} else {
		    		throw new RuntimeException("Could not find war! " + warPath);
		    	}
		    } else if (!line.hasOption("stop")) {
		    	printUsage("Must specify -war path/to/war, or -stop [-stop-socket]",1);
		    } 
		    if (line.hasOption("stop")) {
		    	if(line.getOptionValue("stop")!=null) {
		    		socketNumber = Integer.parseInt(line.getOptionValue("stop")); 
		    	}
		    	new Stop().main(new String[] {Integer.toString(socketNumber)});
		    }
		    if (line.hasOption("context")) {
		    	contextPath = line.getOptionValue("context");
		    }
		    if (line.hasOption("host")) {
		    	host  = line.getOptionValue("host");
		    }
		    if (line.hasOption("port")) {
		    	portNumber = ((Number)line.getParsedOptionValue("port")).intValue();
		    }
		    if (line.hasOption("enable-ajp")) {
		    	enableAJP = Boolean.valueOf(line.getOptionValue("enable-ajp"));
		    }
		    if (line.hasOption("ajp")) {
		    	ajpPort = ((Number)line.getParsedOptionValue("ajp")).intValue();
		    }
		    if (line.hasOption("logdir")) {
		    	logDir= line.getOptionValue("logdir");
		    } else {
		    	if(warFile.isDirectory() && new File(warFile,"WEB-INF").exists()) {
		    		logDir = warFile.getPath() + "/WEB-INF/logs/";
		    	} else {
			        String serverConfigDir = System.getProperty("railo.server.config.dir");
			        if(serverConfigDir == null) {
			        	logDir = new File(thisJarLocation.getParentFile(),"server/log/").getAbsolutePath();
			        } else {
			        	logDir = new File(serverConfigDir,"log/").getAbsolutePath();			        	
			        }
		    	}
		    }
			cfmlDirs = warFile.getAbsolutePath();
		    if (line.hasOption("dirs")) {
		    	cfmlDirs= line.getOptionValue("dirs");
		    }
		    if (line.hasOption("requestlog")) {
		    	keepRequestLog = Boolean.valueOf(line.getOptionValue("requestlog"));
		    }
		    if (line.hasOption("loglevel")) {
		    	loglevel = line.getOptionValue("loglevel").toUpperCase();
		    }

		    if (line.hasOption("debug")) {
		    	debug= Boolean.valueOf(line.getOptionValue("debug"));
		    	if(debug)loglevel = "DEBUG";
		    	if(line.hasOption("loglevel")) {
		    		System.out.println("Warning:  debug overrides loglevel (both are specified, setting level to DEBUG)");
		    	}
		    }
		    
		    if (line.hasOption("open-browser")) {
		    	openbrowser = Boolean.valueOf(line.getOptionValue("open"));
		    }
		    if (line.hasOption("open-url")) {
		    	openbrowserURL = line.getOptionValue("open-url");
		    }

		    if (line.hasOption("pidfile")) {
		    	pidFile  = line.getOptionValue("pidfile");
		    }

		    if (line.hasOption("processname")) {
		    	processName  = line.getOptionValue("processname");
		    }

		    if (line.hasOption("icon")) {
		    	iconImage  = line.getOptionValue("icon");
		    }
		    return line;
		}
		catch( Exception exp ) {
	    	printUsage(exp.getMessage(),1);
		}
		return null;
	}

	private static void printUsage(String message, int exitCode) {
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

	private static class MonitorThread extends Thread {

		private ServerSocket socket;
		private TeeOutputStream stdout;
		private int socketNumber;

		public MonitorThread(TeeOutputStream tee, int socketNumber) {
			stdout = tee;
			setDaemon(true);
			setName("StopMonitor");
			this.socketNumber = socketNumber;
			try {
				socket = new ServerSocket(socketNumber, 1, InetAddress.getByName("127.0.0.1"));
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}

		@Override
		public void run() {
			System.out.println(bar);
			System.out.println("*** starting 'stop' listener thread - Host: 127.0.0.1 - Socket: " + this.socketNumber);
			System.out.println(bar);
			Socket accept;
			try {
				accept = socket.accept();
				BufferedReader reader = new BufferedReader(new InputStreamReader(accept.getInputStream()));
				reader.readLine();
				System.out.println(bar);
				System.out.println("*** stopping server");
				System.out.println(bar);
				manager.undeploy();
				server.stop();
				accept.close();
				socket.close();
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
			try {
				if(stdout != null) 
					stdout.close();
			} catch (Exception e) {
				System.out.println("Redirect:  Unable to close this log file!");
			}
			System.exit(0);
		}
	}

	public static boolean serverCameUp(int timeout, long sleepTime, InetAddress server, int port) {
		long start = System.currentTimeMillis();
		while ((System.currentTimeMillis() - start) < timeout) {
			if (!checkServerIsUp(server, port)) {
				try {
					Thread.sleep(sleepTime);
				} catch (InterruptedException e) {
					return false;
				}
			} else {
				return true;
			}
		}
		return false;
	}

	public static boolean checkServerIsUp(InetAddress server, int port) {
		Socket sock = null;
		try {
			sock = SocketFactory.getDefault().createSocket(server, port);
			sock.setSoLinger(true, 0);
			return true;
		} catch (IOException e) {
			return false;
		} finally {
			if (sock != null) {
				try {
					sock.close();
				} catch (IOException e) {
					// don't care
				}
			}
		}
	}

	class OpenBrowserTask extends TimerTask {
		public void run() {
			System.out.println("Waiting upto 35 seconds for "+host+":"+portNumber+"...");
			if(openbrowserURL == null || openbrowserURL.length() == 0) {
				openbrowserURL = "http://" + host + ":" + portNumber;
			}
			try {
				if (serverCameUp(35000, 3000, InetAddress.getByName(host), portNumber)) {
					if(!openbrowserURL.startsWith("http")) {
						openbrowserURL = (!openbrowserURL.startsWith("/")) ? "/"+openbrowserURL : openbrowserURL;
						openbrowserURL = "http://" + host + ":" + portNumber + openbrowserURL;
					}
					System.out.println("Opening browser to..." + openbrowserURL);
					BrowserOpener.openURL(openbrowserURL.trim());
				} else {
					System.out.println("could not open browser to..." + openbrowserURL + "... timeout...");					
				}
			} catch (Exception e) {
				log.error(e.getMessage());
			}
			return;
		}
	}
	
}
