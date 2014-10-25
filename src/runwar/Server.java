package runwar;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.awt.Image;
import java.awt.TrayIcon;

import javax.net.SocketFactory;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;

import runwar.logging.Logger;
import runwar.logging.LogSubverter;
import runwar.options.CommandLineHandler;
import runwar.options.ServerOptions;
import runwar.undertow.MappedResourceManager;
import runwar.undertow.WebXMLParser;
import runwar.util.TeeOutputStream;
import io.undertow.Handlers;
import io.undertow.Undertow;
import io.undertow.Undertow.Builder;
import io.undertow.server.HttpHandler;
import io.undertow.server.handlers.PathHandler;
import io.undertow.servlet.api.DeploymentInfo;
import io.undertow.servlet.api.DeploymentManager;
import io.undertow.servlet.api.ServletInfo;
import io.undertow.servlet.handlers.DefaultServlet;
import static io.undertow.servlet.Servlets.defaultContainer;
import static io.undertow.servlet.Servlets.deployment;
import static io.undertow.servlet.Servlets.servlet;

//import railo.loader.engine.CFMLEngine;

public class Server {

	private static Logger log = Logger.getLogger("RunwarLogger");
	private static ServerOptions serverOptions;

	private static String PID;

	private static URLClassLoader _classLoader;

	static TrayIcon trayIcon;
	private static DeploymentManager manager;
	private static Undertow undertow;
	public static final String bar = "******************************************************************************";
	
	public Server() {
	}
	
	// for openBrowser 
	public Server(int seconds) {
	    Timer timer = new Timer();
	    timer.schedule(this.new OpenBrowserTask(), seconds * 1000);
	}
	
    protected static void initClassLoader(List<URL> _classpath) {
        if (_classLoader == null && _classpath != null && _classpath.size() > 0) {
            log.debugf("classpath: %s",_classpath);
//            _classLoader = new URLClassLoader(_classpath.toArray(new URL[_classpath.size()]),Thread.currentThread().getContextClassLoader());
//          _classLoader = new URLClassLoader(_classpath.toArray(new URL[_classpath.size()]),ClassLoader.getSystemClassLoader());
//          _classLoader = new URLClassLoader(_classpath.toArray(new URL[_classpath.size()]));
          _classLoader = new URLClassLoader(_classpath.toArray(new URL[_classpath.size()]));
//          _classLoader = new XercesFriendlyURLClassLoader(_classpath.toArray(new URL[_classpath.size()]),ClassLoader.getSystemClassLoader());
            //Thread.currentThread().setContextClassLoader(_classLoader);
        }
    }
    
    protected static void setClassLoader(URLClassLoader classLoader){
        _classLoader = classLoader;
    }
    
    public static URLClassLoader getClassLoader(){
        return _classLoader;
    }
    
	public static void startServer(String[] args, URLClassLoader classLoader) throws Exception {
	    setClassLoader(classLoader);
	    startServer(args);
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static void startServer(String[] args) throws Exception {
	    serverOptions = new ServerOptions();
		CommandLine line = new CommandLineHandler().parseArguments(args,serverOptions);
        String processName = serverOptions.getProcessName();
        int portNumber = serverOptions.getPortNumber();
        int socketNumber = serverOptions.getSocketNumber();
        String contextPath = serverOptions.getContextPath();
        String host = serverOptions.getHost();
        File warFile = serverOptions.getWarFile();
        String warPath = serverOptions.getWarPath();
        String loglevel = serverOptions.getLoglevel();

		if (serverOptions.isBackground()) {
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
			int launchTimeout = serverOptions.getLaunchTimeout();
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
	    if(serverOptions.getLogDir() != null) {
			File logDirectory = serverOptions.getLogDir();
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
			System.setProperty("java.library.path",getThisJarLocation().getPath());
		} else {
			System.setProperty("java.library.path",getThisJarLocation().getPath() + ":" + System.getProperty("java.library.path"));
		}
        String osName = System.getProperties().getProperty("os.name");
        String iconPNG = System.getProperty("cfml.server.trayicon");
        if( iconPNG != null && iconPNG.length() > 0) {
            serverOptions.setIconImage(iconPNG);
        }
        String dockIconPath = System.getProperty("cfml.server.dockicon");
        if( dockIconPath == null || dockIconPath.length() == 0) {
            dockIconPath = serverOptions.getIconImage();
        }

        if(osName != null && osName.startsWith("Mac OS X")){
        	Image dockIcon = LaunchUtil.getIconImage(dockIconPath);
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
            catch(Exception e) { log.warn(e); }
        }
		String startingtext = "Starting - port:" + portNumber + " stop-port:" + socketNumber + " warpath:" + warPath;
		startingtext += "\ncontext: "+ contextPath + "  -  version: " + getVersion();
		String cfmlDirs = serverOptions.getCfmlDirs();
		if(cfmlDirs.length() > 0) {
		    startingtext += "\nweb-dirs: " + cfmlDirs;
		}
		startingtext += "\nLog Directory: " + serverOptions.getLogDir().getAbsolutePath();
        System.out.println(bar);
        System.out.println(startingtext);
        //System.out.println("background: " + background);
        System.out.println(bar);
		portNumber = getPortOrErrorOut(portNumber,host);
		socketNumber = getPortOrErrorOut(socketNumber,host);			

		File webinf = new File(warFile,"WEB-INF");
		String libDirs = serverOptions.getLibDirs();
		URL jarURL = serverOptions.getJarURL();
		if(warFile.isDirectory() && webinf.exists()) {
			libDirs = webinf.getAbsolutePath() + "/lib";
			log.info("Using existing WEB-INF/lib of: " + libDirs);
		}
		if(libDirs != null || jarURL != null) {
			List<URL> cp=new ArrayList<URL>();
			if(libDirs!=null)
				cp.addAll(getJarList(libDirs));
			if(jarURL!=null)
				cp.add(jarURL);
			cp.addAll(getClassesList(new File(webinf,"/classes").getAbsolutePath()));
			initClassLoader(cp);
		}

		DeploymentInfo servletBuilder = deployment()
                .setContextPath( contextPath.equals("/") ? "" : contextPath )
                .setDeploymentName(warPath);

		if(!warFile.exists()) {
			throw new RuntimeException("war does not exist: " + warFile.getAbsolutePath());
		}
		if(System.getProperty("coldfusion.home") == null)
			System.setProperty("coldfusion.home",warFile.getAbsolutePath());
		
		String railoConfigWebDir = serverOptions.getRailoConfigWebDir();
		String railoConfigServerDir = serverOptions.getRailoConfigServerDir();
		File webXmlFile = serverOptions.getWebXmlFile();
		if(warFile.isDirectory() && !webinf.exists()) {
	        if(railoConfigWebDir == null) {
	        	File webConfigDirFile = new File(getThisJarLocation().getParentFile(),"engine/railo/railo-web/");
				railoConfigWebDir = webConfigDirFile.getPath();
	        }
	        log.debug("railo.web.config.dir: " + railoConfigWebDir);
	        if(railoConfigServerDir == null || railoConfigServerDir.length() == 0) {
	        	File serverConfigDirFile = new File(getThisJarLocation().getParentFile(),"engine/railo/");
	        	railoConfigServerDir = serverConfigDirFile.getAbsolutePath();
	        }
	        log.debug("railo.server.config.dir: " + railoConfigServerDir);
	        String webinfDir = System.getProperty("railo.webinf");
	        if(webinfDir == null) {
	        	webinfDir = new File(railoConfigServerDir,"WEB-INF/").getPath();
	        }
	        log.debug("railo.webinf: " + webinfDir);

//			servletBuilder.setResourceManager(new CFMLResourceManager(new File(homeDir,"server/"), 100, cfmlDirs));
			File internalRailoRoot = new File(webinfDir);
			internalRailoRoot.mkdirs();
			servletBuilder.setResourceManager(new MappedResourceManager(warFile, 100, cfmlDirs, internalRailoRoot));

			if(webXmlFile != null){
				log.debug("using specified web.xml : " + webXmlFile.getAbsolutePath());
				servletBuilder.setClassLoader(_classLoader);
				WebXMLParser.parseWebXml(webXmlFile, servletBuilder);
			} else {
				servletBuilder.setClassLoader(_classLoader);
				Class cfmlServlet;
				Class restServlet;
				try{
					cfmlServlet = _classLoader.loadClass("railo.loader.servlet.CFMLServlet");
	                log.debug("dynamically loaded CFML servlet from runwar child classloader");
				} catch (java.lang.ClassNotFoundException e) {
					cfmlServlet = Server.class.getClassLoader().loadClass("railo.loader.servlet.CFMLServlet");
					log.debug("dynamically loaded CFML servlet from runwar classloader");
				}
				try{
					restServlet = _classLoader.loadClass("railo.loader.servlet.RestServlet");
				} catch (java.lang.ClassNotFoundException e) {
					restServlet = Server.class.getClassLoader().loadClass("railo.loader.servlet.RestServlet");
				}
				log.debug("loaded servlet classes");
				servletBuilder
	            	.addWelcomePages(serverOptions.getWelcomeFiles())
	            	.addServlets(
		                        servlet("CFMLServlet", cfmlServlet)
        		                        .setRequireWelcomeFileMapping(true)
		                                .addInitParam("configuration",railoConfigWebDir)
		                                .addInitParam("railo-server-root",railoConfigServerDir)
		                                .addMapping("*.cfm")
		                                .addMapping("*.cfc")
		                                .addMapping("/index.cfc/*")
		                                .addMapping("/index.cfm/*")
		                                .addMapping("/index.cfml/*")
		                                .setLoadOnStartup(1)
		                                ,
		                        servlet("RESTServlet", restServlet)
        		                        .setRequireWelcomeFileMapping(true)
		                                .addInitParam("railo-web-directory",railoConfigWebDir)
		                                .addMapping("/rest/*")
		                                .setLoadOnStartup(2));
	        }
		} else if(webinf.exists()) {
			log.debug("found WEB-INF: " + webinf.getAbsolutePath());
			if(_classLoader == null) {
				throw new RuntimeException("FATAL: Could not load any libs for war: " + warFile.getAbsolutePath());
			}
			servletBuilder.setClassLoader(_classLoader);
			servletBuilder.setResourceManager(new MappedResourceManager(warFile, 100, cfmlDirs, webinf));
	        LogSubverter.subvertJDKLoggers(loglevel);
			WebXMLParser.parseWebXml(new File(webinf,"/web.xml"), servletBuilder);
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

		// this prevents us from having to use our own ResourceHandler (directory listing, welcome files, see below) and error handler for now
        servletBuilder.addServlet(new ServletInfo(io.undertow.servlet.handlers.ServletPathMatches.DEFAULT_SERVLET_NAME, DefaultServlet.class)
                .addInitParam("directory-listing", Boolean.toString(serverOptions.isDirectoryListingEnabled())));
		manager = defaultContainer().addDeployment(servletBuilder);
		manager.deploy();
        HttpHandler servletHandler = manager.start();
        log.debug("started servlet deployment manager");
/*
        List welcomePages =  manager.getDeployment().getDeploymentInfo().getWelcomePages();
        CFMLResourceHandler resourceHandler = new CFMLResourceHandler(servletBuilder.getResourceManager(), servletHandler, welcomePages);
        resourceHandler.setDirectoryListingEnabled(directoryListingEnabled);
        PathHandler pathHandler = Handlers.path(Handlers.redirect(contextPath))
                .addPrefixPath(contextPath, resourceHandler);
        HttpHandler errPageHandler = new SimpleErrorPageHandler(pathHandler);
        Builder serverBuilder = Undertow.builder().addHttpListener(portNumber, host).setHandler(errPageHandler);
*/
        PathHandler pathHandler = Handlers.path(Handlers.redirect(contextPath))
                .addPrefixPath(contextPath, servletHandler);
        Builder serverBuilder = Undertow.builder()
                .addHttpListener(portNumber, host).setHandler(pathHandler);

        if(serverOptions.isEnableAJP()) {
			log.info("Enabling AJP protocol on port " + serverOptions.getAjpPort());
			serverBuilder.addAjpListener(serverOptions.getAjpPort(), "localhost");
		}

		try {
			PID = ManagementFactory.getRuntimeMXBean().getName().split("@")[0];
			String pidFile = serverOptions.getPidFile();
			if(pidFile != null && pidFile.length() > 0){
	            File file = new File(pidFile);
			    file.deleteOnExit();
	            PrintWriter writer = new PrintWriter(file);
	            writer.print(PID);
	            writer.close();
			}
		} catch (Exception e) {
			log.error("Unable to get PID:" + e.getMessage());
		}
		if (serverOptions.isKeepRequestLog()) {
			log.error("request log currently unsupported");
		}

		Thread monitor = new MonitorThread(tee, socketNumber);
		monitor.start();
        log.debug("started stop monitor");
		LaunchUtil.hookTray(serverOptions.getIconImage(), host, portNumber, socketNumber, processName, PID);
		log.debug("hooked system tray");

		if (serverOptions.isOpenbrowser()) {
			new Server(3);
		}

		undertow = serverBuilder.build();
		// if this println changes be sure to update the LaunchUtil so it will know success
        String msg = "Server is up - http-port:" + portNumber + " stop-port:" + socketNumber +" PID:" + PID + " version " + getVersion();
        log.debug(msg);
		System.out.println(msg);
		undertow.start();
	}

	public static File getThisJarLocation() {
	    return new File(Server.class.getProtectionDomain().getCodeSource().getLocation().getPath()).getParentFile();
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
							URL url = item.toURI().toURL();
							classpath.add(url);
//							System.out.println("lib: added to classpath: "+fileName);
					}
				}				
			}
		}
		return classpath;
	}

	private static List<URL> getClassesList(String classesDir) throws IOException {
		List<URL> classpath=new ArrayList<URL>();
		if(classesDir == null)
			return classpath;
		File file = new File(classesDir);
		if(file.exists() && file.isDirectory()) {
			for(File item : file.listFiles()) {
				if (!item.isDirectory()) {
					URL url = item.toURI().toURL();
					classpath.add(url);
				}				
			}
		} else {
			log.debug("WEB-INF classes directory ("+file.getAbsolutePath()+") does not exist");
		}
		return classpath;
	}
	
	public static void printVersion() {
        System.out.println(LaunchUtil.getResourceAsString("runwar/version.properties"));
	    System.out.println(LaunchUtil.getResourceAsString("io/undertow/version.properties"));
	}
		
	private static String getVersion() {
	    String[] version = LaunchUtil.getResourceAsString("runwar/version.properties").split("=");
	    return version[version.length-1].trim();
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
				socket = new ServerSocket(socketNumber, 1, InetAddress.getByName(serverOptions.getHost()));
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}

		@Override
		public void run() {
			System.out.println(bar);
			System.out.println("*** starting 'stop' listener thread - Host: "+ serverOptions.getHost() + " - Socket: " + this.socketNumber);
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
				undertow.stop();
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
	        int portNumber = serverOptions.getPortNumber();
	        String host = serverOptions.getHost();
	        String openbrowserURL = serverOptions.getOpenbrowserURL();
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

    public static ServerOptions getServerOptions() {
        return serverOptions;
    }
	
}
