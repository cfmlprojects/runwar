package runwar;

import java.io.*;
import java.net.*;
import java.security.*;
import java.util.regex.*;

import org.jboss.logging.Logger;

final class AgentInitialization {
	private static final Pattern JAR_REGEX = Pattern
			.compile(".*railo-inst[-.\\d]*.jar");

	private static Logger log = Logger.getLogger("RunwarLogger");

	boolean loadAgentFromLocalJarFile(File tryFirst) {
		String javaSpecVersion = System
				.getProperty("java.specification.version");

		if (!"1.6 1.7 1.8 1.9".contains(javaSpecVersion)) {
			throw new IllegalStateException("This app requires a Java 6+ VM");
		}
		log.debug("Trying to load java agent from local jar file");
		String jarFilePath = "";
		if(tryFirst != null && tryFirst.exists()) {
			log.debug("Trying first:" + tryFirst.getAbsolutePath());
			for(File file : tryFirst.listFiles()) {
				if (JAR_REGEX.matcher(file.getPath()).matches()) {
					jarFilePath = file.getAbsolutePath();
					break;
				}	
			}
//			System.out.println("Found Agent Jar: " + jarFilePath);
		} else {
			jarFilePath = discoverPathToJarFile();
		}
		log.debug("Loading agent from:" + jarFilePath);
		if(jarFilePath!=null && jarFilePath.length() > 0) {
			return new AgentLoader(jarFilePath).loadAgent();			
		} else {
			log.warn("The agent loader was not found for auto-initialization");
		}
		return false;
	}

	private String discoverPathToJarFile() {
		log.debug("Searching for java agent");
		String jarFilePath = findPathToJarFileFromClasspath();

		if (jarFilePath == null) {
			// This can fail for a remote URL, so it is used as a fallback only:
			jarFilePath = getPathToJarFileContainingThisClass();
		}

		if (jarFilePath != null) {
			return jarFilePath;
		}
		log.warn("The agent jar was not found in the classpath!");
		System.out.println("The agent jar was not found in the classpath!");
		return null;
	}

	private String findPathToJarFileFromClasspath() {
		String[] classPath = System.getProperty("java.class.path").split(
				File.pathSeparator);

		log.debug("Trying to load java agent from classpath");
		for (String cpEntry : classPath) {
			if (JAR_REGEX.matcher(cpEntry).matches()) {
				return cpEntry;
			}
		}

		return null;
	}

	private String getPathToJarFileContainingThisClass() {
		CodeSource codeSource = AgentInitialization.class.getProtectionDomain()
				.getCodeSource();

		log.debug("Trying to load java agent from jar containing this class");
		if (codeSource == null) {
			return null;
		}

		URL location = codeSource.getLocation();
		String locationPath = location.getPath();
		String jarFilePath;

		if (locationPath.endsWith(".jar")) {
			jarFilePath = findLocalJarOrZipFileFromLocationOfCurrentClassFile(locationPath);
		} else {
			jarFilePath = findJarFileContainingCurrentClass(location);
		}

		return jarFilePath;
	}

	private String findLocalJarOrZipFileFromLocationOfCurrentClassFile(
			String locationPath) {
		log.debug("Trying to load java agent from location of current class file");
		File libDir = new File(locationPath).getParentFile();
		File localJarFile = new File(libDir, "railo-inst.jar");

		if (localJarFile.exists()) {
			return localJarFile.getPath();
		}

		File localMETAINFFile = new File(locationPath.replace("classes/",
				"META-INF.zip"));
		log.debug("Trying to load java agent from " + localMETAINFFile.getPath());
		return localMETAINFFile.getPath();
	}

	private String findJarFileContainingCurrentClass(URL location) {
		// URI is used to deal with spaces and non-ASCII characters.
		URI jarFileURI;
		try {
			jarFileURI = location.toURI();
		} catch (URISyntaxException e) {
			throw new RuntimeException(e);
		}

		// Certain environments (JBoss) use something other than "file:", which
		// is not accepted by File.
		if (!"file".equals(jarFileURI.getScheme())) {
			String locationPath = location.toExternalForm();
			int p = locationPath.indexOf(':');
			return locationPath.substring(p + 2);
		}

		return new File(jarFileURI).getPath();
	}
}
