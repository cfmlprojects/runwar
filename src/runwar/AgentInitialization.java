package runwar;

import java.io.*;
import java.net.*;
import java.security.*;
import java.util.regex.*;

final class AgentInitialization {
	private static final Pattern JAR_REGEX = Pattern
			.compile(".*railo-inst[-.\\d]*.jar");

	boolean loadAgentFromLocalJarFile(File tryFirst) {
		String javaSpecVersion = System
				.getProperty("java.specification.version");

		if (!"1.6 1.7 1.8 1.9".contains(javaSpecVersion)) {
			throw new IllegalStateException("This app requires a Java 6+ VM");
		}
		String jarFilePath = "";
		if(tryFirst != null && tryFirst.exists()) {
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
		if(jarFilePath!=null) {
//		System.out.println("Agent Jar: " + jarFilePath);
			return new AgentLoader(jarFilePath).loadAgent();			
		}
		return false;
	}

	private String discoverPathToJarFile() {
		String jarFilePath = findPathToJarFileFromClasspath();

		if (jarFilePath == null) {
			// This can fail for a remote URL, so it is used as a fallback only:
			jarFilePath = getPathToJarFileContainingThisClass();
		}

		if (jarFilePath != null) {
			return jarFilePath;
		}
		System.out.println("The agent jar was not found in the classpath!");
		return null;
	}

	private String findPathToJarFileFromClasspath() {
		String[] classPath = System.getProperty("java.class.path").split(
				File.pathSeparator);

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
		File libDir = new File(locationPath).getParentFile();
		File localJarFile = new File(libDir, "railo-inst.jar");

		if (localJarFile.exists()) {
			return localJarFile.getPath();
		}

		File localMETAINFFile = new File(locationPath.replace("classes/",
				"META-INF.zip"));
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
