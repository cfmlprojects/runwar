package runwar;

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
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.Socket;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.imageio.ImageIO;
import javax.swing.JOptionPane;

import org.jboss.logging.Logger;

public class LaunchUtil {

	private static Logger log = Logger.getLogger("RunwarLogger");
	private static boolean relaunching;
	public static final Set<String> replicateProps = new HashSet<String>(Arrays.asList(new String[] {
			"cfml.cli.home",
			"railo.server.config.dir",
			"railo.web.config.dir",
			"cfml.server.trayicon",
			"cfml.server.dockicon"
	}));

	public static File getJreExecutable() throws FileNotFoundException {
		String jreDirectory = System.getProperty("java.home");
		if (jreDirectory == null) {
			throw new IllegalStateException("java.home");
		}
		final String javaPath = System.getProperty("java.home") + File.separator + "bin" + File.separator + "java"
				+ (File.separator.equals("\\") ? ".exe" : "");
		File exe = new File(javaPath);
		if (!exe.isFile()) {
			throw new FileNotFoundException(exe.toString());
		}
		//if(debug)System.out.println("Java: "+javaPath);
		return exe;
	}

	public static void launch(List<String> cmdarray, int timeout) throws IOException, InterruptedException {
		//byte[] buffer = new byte[1024];

		ProcessBuilder processBuilder = new ProcessBuilder(cmdarray);
		processBuilder.redirectErrorStream(true);
		Process process = processBuilder.start();
		Thread.sleep(500);
		InputStream is = process.getInputStream();
		InputStreamReader isr = new InputStreamReader(is);
		BufferedReader br = new BufferedReader(isr);
		log.debug("launching: " + cmdarray.toString());
		log.debug("timeout of " + timeout / 1000 + " seconds");
		String line;
		int exit = -1;
		long start = System.currentTimeMillis();
		System.out.print("Starting in background - ");
		while ((System.currentTimeMillis() - start) < timeout) {
			if (br.ready() && (line = br.readLine()) != null) {
				// Outputs your process execution
				try {
					exit = process.exitValue();
					if (exit == 0) {
						// Process finished
						while((line = br.readLine())!= null) {
							log.debug(line);
						}
						System.exit(0);
					} else if(exit==1) {
						System.out.println();
						printExceptionLine(line);
						while((line = br.readLine())!= null) {
							printExceptionLine(line);
						}
						System.exit(1);
					}
				} catch (IllegalThreadStateException t) {
					// This exceptions means the process has not yet finished.
					// decide to continue, exit(0), or exit(1)
					processOutout(line, process);
				}
			}
			Thread.sleep(100);
		}
		if((System.currentTimeMillis() - start) > timeout) {
			process.destroy();
			System.out.println();
			System.err.println("ERROR: Startup exceeded timeout of " + timeout / 1000 + " seconds - aborting!");
			System.exit(1);
		}
		System.out.println("Server is up - ");
		System.exit(0);
	}

	private static boolean processOutout(String line, Process process) {
		log.info("processoutput: " + line);
		if(line.indexOf("Server is up - ") != -1) {
			// start up was successful, quit out
			System.out.println(line);
			System.exit(0);
		} else if(line.indexOf("Exception in thread \"main\" java.lang.RuntimeException") != -1) {
			return true;
		}
		return false;
	}
	
	public static void printExceptionLine(String line){
		final String msg = "java.lang.RuntimeException: ";
		log.debug(line);
		String formatted = line.contains(msg) ? line.substring(line.indexOf(msg)+msg.length()) : line;
		formatted = formatted.matches("^\\s+at runwar.Start.*") ? "" : formatted.trim();
		if(formatted.length()>0) {
			System.err.println(formatted);
		}
	}

	public static void relaunchAsBackgroundProcess(int timeout, String[] args, String processName) {
		try {
			if(relaunching)
				return;
			relaunching = true;
			String path = LaunchUtil.class.getProtectionDomain().getCodeSource().getLocation().getPath();
			log.info("Starting background "+ processName + " from: " + path + " ");
			String decodedPath = URLDecoder.decode(path, "UTF-8");
			decodedPath = new File(decodedPath).getPath();
			List<String> cmdarray = new ArrayList<String>();
			cmdarray.add(getJreExecutable().toString());
			List<String> currentVMArgs = getCurrentVMArgs();
			for(String arg : currentVMArgs) {
				cmdarray.add(arg);
			}
			cmdarray.add("-jar");
			for(String propertyName : replicateProps) {
				String property = System.getProperty(propertyName);
				if(property != null) {
					cmdarray.add("-D"+propertyName+"="+property);			
				}
			}
			cmdarray.add(decodedPath);
			for(String arg : args) {
				cmdarray.add(arg);
			}
			launch(cmdarray,timeout);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private static void appendToBuffer(List<String> resultBuffer, StringBuffer buf) {
		if (buf.length() > 0) {
			resultBuffer.add(buf.toString());
			buf.setLength(0);
		}
	}
	
	public static List<String> getCurrentVMArgs(){
		RuntimeMXBean RuntimemxBean = ManagementFactory.getRuntimeMXBean();
		List<String> arguments = RuntimemxBean.getInputArguments();
		return arguments;
	}

	public static String[] tokenizeArgs(String argLine) {
		List<String> resultBuffer = new java.util.ArrayList<String>();

		if (argLine != null) {
			int z = argLine.length();
			boolean insideQuotes = false;
			StringBuffer buf = new StringBuffer();

			for (int i = 0; i < z; ++i) {
				char c = argLine.charAt(i);
				if (c == '"') {
					appendToBuffer(resultBuffer, buf);
					insideQuotes = !insideQuotes;
				} else if (c == '\\') {
					if ((z > i + 1) && ((argLine.charAt(i + 1) == '"') || (argLine.charAt(i + 1) == '\\'))) {
						buf.append(argLine.charAt(i + 1));
						++i;
					} else {
						buf.append("\\");
					}
				} else {
					if (insideQuotes) {
						buf.append(c);
					} else {
						if (Character.isWhitespace(c)) {
							appendToBuffer(resultBuffer, buf);
						} else {
							buf.append(c);
						}
					}
				}
			}
			appendToBuffer(resultBuffer, buf);

		}

		String[] result = new String[resultBuffer.size()];
		return resultBuffer.toArray(result);
	}

	static void hookTray(String iconImage, String host, int portNumber, final int stopSocket, String processName, String PID) {
		
		if (SystemTray.isSupported()) {
			
			Image image = null;
			if(iconImage != null && iconImage.length() != 0) {
				iconImage = iconImage.replaceAll("(^\")|(\"$)", "");
				log.debug("trying to load icon: "+iconImage);
				if(iconImage.contains("!")) {
			        String[] zip = iconImage.split("!");
				    try {
				    	ZipFile zipFile = new ZipFile(zip[0]);
				    	ZipEntry zipEntry = zipFile.getEntry(zip[1].replaceFirst("^[\\/]", "")) ;
				    	InputStream entryStream = zipFile.getInputStream(zipEntry);
						image = ImageIO.read(entryStream);
						zipFile.close();
						log.debug("loaded image from archive: " + zip[0] + zip[1]);
					} catch (IOException e2) {
						log.debug("Could not get zip resource: " + iconImage + "(" + e2.getMessage() +")");
					}
				} else if(new File(iconImage).exists()) {
					try {
						image = ImageIO.read(new File(iconImage));
					} catch (IOException e1) { 
						log.debug("Could not get file resource: " + iconImage + "(" + e1.getMessage() +")");
					}
				} else {
					log.debug("trying parent loader for image: " + iconImage);
					URL imageURL = LaunchUtil.class.getClassLoader().getParent().getResource(iconImage);
					if(imageURL == null) {
						log.debug("trying loader for image: " + iconImage);
						imageURL = LaunchUtil.class.getClassLoader().getResource(iconImage);
					}
					if(imageURL != null) {
						log.debug("Trying getImage for: " + imageURL);
						image = Toolkit.getDefaultToolkit().getImage(imageURL);
					} 				
				}
			} else {
				image = Toolkit.getDefaultToolkit().getImage(Start.class.getResource("/runwar/icon.png"));
			}
			// if bad image, use default
			if(image == null || image.getHeight(null) == -1) {
				log.debug("Bad image, using default.");
				image = Toolkit.getDefaultToolkit().getImage(Start.class.getResource("/runwar/icon.png"));
			}
			MouseListener mouseListener = new MouseListener() {
				public void mouseClicked(MouseEvent e) {}
				public void mouseEntered(MouseEvent e) {}
				public void mouseExited(MouseEvent e) {}
				public void mousePressed(MouseEvent e) {}
				public void mouseReleased(MouseEvent e) {}
			};
			final String railoAdminURL = "http://"+host+":"+portNumber + "/railo-context/admin/server.cfm";

			final TrayIcon trayIcon = new TrayIcon(image, processName + " server on " + host + ":" + portNumber + " PID:" + PID);

			PopupMenu popup = new PopupMenu();
			MenuItem item = new MenuItem("Stop Server (" + processName + ")");
			item.addActionListener(new ExitActionListener(trayIcon,host,stopSocket));
			popup.add(item);
			item = new MenuItem("Open Browser");
			item.addActionListener(new OpenBrowserActionListener(trayIcon,"http://"+host+":"+portNumber + "/"));
			popup.add(item);
			item = new MenuItem("Open Admin");
			item.addActionListener(new OpenBrowserActionListener(trayIcon,railoAdminURL));
			popup.add(item);

			trayIcon.setPopupMenu(popup);
			trayIcon.setImageAutoSize(true);
			trayIcon.addMouseListener(mouseListener);
			
			final SystemTray tray = SystemTray.getSystemTray();
			try {
				tray.add(trayIcon);
			} catch (AWTException e) {
				System.err.println("TrayIcon could not be added.");
			}
			
		} else {
			
			//  System Tray is not supported
			
		}
	}	
	private static class OpenBrowserActionListener implements ActionListener {
		private TrayIcon trayIcon;
		private String url;

		public OpenBrowserActionListener(TrayIcon trayIcon, String url) {
			this.url = url;
			this.trayIcon = trayIcon;
		}
		
		@Override
		public void actionPerformed(ActionEvent e) {
			trayIcon.displayMessage("Browser", "Opening browser", TrayIcon.MessageType.INFO);
			openURL(url);
		}
	}

	private static class ExitActionListener implements ActionListener {
		private TrayIcon trayIcon;
		private int stopsocket;
		private String host;
		
		public ExitActionListener(TrayIcon trayIcon, String host, int stopsocket) {
			this.trayIcon = trayIcon;
			this.stopsocket = stopsocket;
			this.host = host;
		}
		
		@Override
		public void actionPerformed(ActionEvent e) {
			try {
	        	Socket s = new Socket(InetAddress.getByName(host), stopsocket);
		        OutputStream out = s.getOutputStream();
	        	out.write(("\r\n").getBytes());
	        	out.flush();
	        	s.close();
	        	out.close();
				System.out.println("Exiting...");
				System.exit(0);
			} catch (Exception e1) { 
				trayIcon.displayMessage("Error", e1.getMessage(), TrayIcon.MessageType.INFO);
				try {
					Thread.sleep(5000);
				} catch (InterruptedException e2) {
				}
				System.exit(1);
			}
		}
	}
	
	public static void openURL(String url) {
		String osName = System.getProperty("os.name");
		if(url == null) {
			System.out.println("ERROR: No URL specified to open the browser to!");
			return;
		}
		try {
			System.out.println(url);
			if (osName.startsWith("Mac OS")) {
				Class<?> fileMgr = Class.forName("com.apple.eio.FileManager");
				Method openURL = fileMgr.getDeclaredMethod("openURL", new Class[] { String.class });
				openURL.invoke(null, new Object[] { url });
			} else if (osName.startsWith("Windows"))
				Runtime.getRuntime().exec("rundll32 url.dll,FileProtocolHandler " + url);
			else { // assume Unix or Linux
				String[] browsers = { "firefox", "opera", "konqueror", "epiphany", "mozilla", "netscape" };
				String browser = null;
				for (int count = 0; count < browsers.length && browser == null; count++)
					if (Runtime.getRuntime().exec(new String[] { "which", browsers[count] }).waitFor() == 0)
						browser = browsers[count];
				if (browser == null)
					throw new Exception("Could not find web browser");
				else
					Runtime.getRuntime().exec(new String[] { browser, url });
			}
		} catch (Exception e) {
			e.printStackTrace();
			JOptionPane.showMessageDialog(null, e.getMessage() + ":\n" + e.getLocalizedMessage());
		}
	}

}
