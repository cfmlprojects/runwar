package runwar.undertow;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.InvalidPathException;
import java.nio.file.LinkOption;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.Optional;

import io.undertow.server.handlers.resource.FileResource;
import io.undertow.server.handlers.resource.FileResourceManager;
import io.undertow.server.handlers.resource.Resource;
import runwar.options.ServerOptions;

import static runwar.logging.RunwarLogger.MAPPER_LOG;

public class MappedResourceManager extends FileResourceManager {

    private ServerOptions serverOptions;
    private Boolean forceCaseSensitiveWebServer;
    private Boolean forceCaseInsensitiveWebServer;
    private Boolean cacheServletPaths;
    private HashMap<String, Path> aliases;
    private ConcurrentHashMap<String, Path> caseInsensitiveCache = new ConcurrentHashMap<String, Path>();
    private ConcurrentHashMap<String, Optional<Resource>> servletPathCache = new ConcurrentHashMap<String, Optional<Resource>>();
    private HashSet<Path> contentDirs;
    private File WEBINF = null, CFIDE = null;
    private static boolean isCaseSensitiveFS = caseSensitivityCheck(); 
    private static final Pattern CFIDE_REGEX_PATTERN;
    private static final Pattern WEBINF_REGEX_PATTERN;
    private final FileResource baseResource;

    static {
        CFIDE_REGEX_PATTERN = Pattern.compile("(?i)^[\\\\/]?CFIDE([\\\\/].*)?");
        WEBINF_REGEX_PATTERN = Pattern.compile("(?i)^[\\\\/]?WEB-INF([\\\\/].*)?");
   }

    private final boolean allowResourceChangeListeners;

    public MappedResourceManager(File base, long transferMinSize, Set<Path> contentDirs, Map<String,Path> aliases, File webinfDir, ServerOptions serverOptions) {
        super(base, transferMinSize);
        this.allowResourceChangeListeners = false;
        this.contentDirs = (HashSet<Path>) contentDirs;
        this.aliases = (HashMap<String, Path>) aliases;
        this.serverOptions = serverOptions; 
        this.forceCaseSensitiveWebServer = serverOptions.caseSensitiveWebServer() != null && serverOptions.caseSensitiveWebServer();
        this.forceCaseInsensitiveWebServer = serverOptions.caseSensitiveWebServer() != null && !serverOptions.caseSensitiveWebServer();
        this.cacheServletPaths = serverOptions.cacheServletPaths();
        this.baseResource = new FileResource( getBase(), this, "/");
        
        if(webinfDir != null){
            WEBINF = webinfDir;
            CFIDE = new File(WEBINF.getParentFile(),"CFIDE");
            MAPPER_LOG.debugf("Initialized MappedResourceManager - base: %s, web-inf: %s, contentDirs: %s, aliases: %s",base.getAbsolutePath(), WEBINF.getAbsolutePath(), contentDirs, aliases);
            if (!WEBINF.exists()) {
                throw new RuntimeException("The specified WEB-INF does not exist: " + WEBINF.getAbsolutePath());
            }
        } else {
            MAPPER_LOG.debugf("Initialized MappedResourceManager - base: %s, web-inf: %s, contentDirs: %s, aliases: %s",base.getAbsolutePath(), contentDirs, aliases);
        }
    }

    public Resource getResource(String path) {
        if(path == null) {
            MAPPER_LOG.error("* getResource got a null path!");
            return null;
        }
        MAPPER_LOG.debug("* requested: '" + path + "'");
        
        // If cache is enabled and this path is found, return it
        if( cacheServletPaths ) {
        	Optional<Resource> cacheCheck = servletPathCache.get( path );
        	if( cacheCheck != null ) {
        		if( cacheCheck.isPresent() ) {
                	MAPPER_LOG.debugf("** path mapped to (cached): '%s'", cacheCheck.get().getFile().toString() );	
        		} else {
                	MAPPER_LOG.debugf("** path mapped to (cached): '%s'", "null (not found)" );        			
        		}
            	return cacheCheck.orElse( null );	
        	}
        }
        
        if( path.equals( "/" ) ) {
        	MAPPER_LOG.debugf("** path mapped to (cached): '%s'", getBase());
            return this.baseResource;
        }

        
        try {
	        Path reqFile = null;
	        final Matcher webInfMatcher = WEBINF_REGEX_PATTERN.matcher(path);
	        final Matcher cfideMatcher = CFIDE_REGEX_PATTERN.matcher(path);
	        if (WEBINF != null && webInfMatcher.matches()) {
	            if(webInfMatcher.group(1) == null) {
	                reqFile = Paths.get(WEBINF.toURI());
	            } else {
	                reqFile = Paths.get(WEBINF.getAbsolutePath(), webInfMatcher.group(1).replace("WEB-INF", ""));
	            }
	            MAPPER_LOG.trace("** matched WEB-INF : " + reqFile.toAbsolutePath().toString());
	        } else if (cfideMatcher.matches()) {
	            if(cfideMatcher.group(1) == null) {
	                reqFile = Paths.get(CFIDE.toURI());
	            } else {
	                reqFile = Paths.get(CFIDE.getAbsolutePath(), cfideMatcher.group(1).replace("CFIDE", ""));
	            }
	            MAPPER_LOG.trace("** matched /CFIDE : " + reqFile.toAbsolutePath().toString());
	        } else if (!webInfMatcher.matches()) {
	            reqFile = Paths.get(getBase().getAbsolutePath(), path);
	            MAPPER_LOG.trace("* checking with base path: '" + reqFile.toAbsolutePath().toString() + "'");
	            if (!Files.exists(reqFile)) {
	                reqFile = getAliasedFile(aliases, path);
	            }
	           /* if (reqFile == null) {
	                for (Path cfmlDirsFile : contentDirs) {
	                    reqFile = Paths.get(cfmlDirsFile.toString(), path.replace(cfmlDirsFile.toAbsolutePath().toString(), ""));
	                    MAPPER_LOG.tracef("checking: '%s' = '%s'", cfmlDirsFile.toAbsolutePath().toString(), reqFile.toAbsolutePath());
	                    if (Files.exists(reqFile)) {
	                        MAPPER_LOG.tracef("Exists: '%s'", reqFile.toAbsolutePath().toString());
	                        break;
	                    }
	                }
	            }*/
	        }
	        
	        if (reqFile != null ) {
	 	        reqFile = pathExists(reqFile);
	        }
	        
	        if (reqFile == null ) {
 	           MAPPER_LOG.debugf("** No mapped resource for: '%s' (reqFile was: '%s')",path,reqFile != null ? reqFile.toString() : "null");
 	           Resource superResult = super.getResource(path); 	            
			   if( cacheServletPaths ) {
				   servletPathCache.put( path, Optional.ofNullable( superResult ) );  
			   }
 	           return superResult;
	        }	        		
	        		
            if(reqFile.toString().indexOf('\\') > 0) {
                reqFile = Paths.get(reqFile.toString().replace('/', '\\'));
            }
            
            // Check for Windows doing silly things with file canonicalization
            String originalPath = reqFile.toString();

            // The real path will return the actual file on the file system that is matched
            // the original path may be in the wrong case and may have extra junk on the end that Windows removes when it canonicalizes
            String realPath = reqFile.toRealPath(LinkOption.NOFOLLOW_LINKS).toString();
            String originalPathCase;
            String realPathCase;

            // If this is a case insensitive file system like Windows and we're not forcing the web server to be case sensitive
            // then compare the paths regardless of case.  Or if this is a case sensitive file system like Linux
            // and we're forcing it to be case insensitive            
            if( (!isCaseSensitiveFS && !forceCaseSensitiveWebServer) || ( isCaseSensitiveFS && forceCaseInsensitiveWebServer ) ) {
            	originalPathCase = originalPath.toLowerCase();
            	realPathCase = realPath.toLowerCase();
            // For case sensitive file systems like Linux OR if we're forcing the web server to be case sensitive
            // compare the real path exactly
            } else {
            	originalPathCase = originalPath;
            	realPathCase = realPath;
            }

            // make sure the path we found on the file system matches what was asked for.
            if( !originalPathCase.equals( realPathCase ) ) {
            	throw new InvalidPathException( "Real file path [" + realPath + "] doesn't match [" + originalPath + "]", "" );
            }
            
            MAPPER_LOG.debugf("** path mapped to: '%s'", reqFile);
	            
		   if( cacheServletPaths ) {
			   servletPathCache.put( path, Optional.of( new FileResource(reqFile.toFile(), this, path) ) );  
		   }
            return new FileResource(reqFile.toFile(), this, path);
            
        } catch( InvalidPathException e ){
            MAPPER_LOG.debugf("** InvalidPathException for: '%s'",path != null ? path : "null");
            MAPPER_LOG.debug("** " + e.getMessage());
            
 		    if( cacheServletPaths ) {
 		 	   servletPathCache.put( path, Optional.empty() );  
 		    }
            return null;
        } catch( IOException e ){
            MAPPER_LOG.debugf("** IOException for: '%s'",path != null ? path : "null");
            MAPPER_LOG.debug("** " + e.getMessage());

 		    if( cacheServletPaths ) {
 		 	   servletPathCache.put( path, Optional.empty() );  
 		    }
            return null;
        }
    }

    static Path getAliasedFile(HashMap<String, Path> aliasMap, String path) {
        if(path.startsWith("/file:")){
            // groovy servlet asks for /file:... for some reason, when scripts are in an aliased dir
            path = path.replace("/file:", "");
            for( Path file : aliasMap.values()) {
                if(path.startsWith(file.toAbsolutePath().toString())) {
                    return Paths.get(path);
                }
            }
        }

        String pathDir = path.startsWith("/") ? path : "/" + path;
        Path file = aliasMap.get(pathDir.toLowerCase());
        if(file != null) {
            return file;
        }
        while (pathDir.lastIndexOf('/') > 0) {
            pathDir = pathDir.substring(0, pathDir.lastIndexOf('/'));
            if (aliasMap.containsKey(pathDir.toLowerCase())) {
                file = Paths.get(aliasMap.get(pathDir.toLowerCase()).toString() + '/' + path.substring(pathDir.length())).normalize();
                if(file.toString().indexOf('\\') > 0){
                    file = Paths.get(file.toString().replace('/', '\\'));
                }
                return file;
            }
        }
        return null;
    }

    Path pathExists(Path path) {
       Boolean defaultCheck = Files.exists( path );
       if( defaultCheck ) {
           return path;
       }
       if( isCaseSensitiveFS && forceCaseInsensitiveWebServer ) {
            MAPPER_LOG.debugf("*** Case insensitive check for %s",path);
            
            Path cacheLookup = caseInsensitiveCache.get(path.toString());
            if( cacheLookup != null && Files.exists( cacheLookup ) ) {
                MAPPER_LOG.tracef("*** Case insensitive lookup found in cache %s -> %s",path, cacheLookup);
            	return cacheLookup;
            } else if( cacheLookup != null ) {
                MAPPER_LOG.tracef("*** Case insensitive lookup removed from cache %s",path);
            	caseInsensitiveCache.remove(path.toString());
            }
            
        	String realPath = "";
        	String[] pathSegments = path.toString().replace('\\', '/').split( "/" );
        	if( pathSegments.length > 0 && pathSegments[0].contains(":") ){
        		realPath = pathSegments[0];
        	}
        	Boolean first = true;
        	for( String thisSegment : pathSegments ) {
        		// Skip windows drive letter
        		if( realPath == pathSegments[0] && pathSegments[0].contains(":") && first ) {
            		first = false;
        			continue;
        		}
        		// Skip empty segments
        		if( thisSegment.length() == 0 ) {
        			continue;
        		}
        		
        		Boolean found = false;
        		for( String thisChild : new File( realPath + "/" ).list() ) {
        			// We're taking the FIRST MATCH.  Buyer beware
        			if( thisSegment.equalsIgnoreCase(thisChild)) {
        				realPath += "/" + thisChild;
        				found = true;
        				break;
        			}
        		}
    			// If we made it through the inner loop without a match, we've hit a dead end
        		if( !found ) {
        			return null;	
        		}
        	}
			// If we made it through the outer loop, we've found a match
        	Path realPathFinal = Paths.get( realPath );

            MAPPER_LOG.tracef("*** Case insensitive lookup put in cache %s -> %s",path,realPathFinal);
        	caseInsensitiveCache.put(path.toString(), realPathFinal );
        	return realPathFinal;
      }
      return null;
    }

    private void processMappings(String cfmlDirList) {
        HashSet<Path> dirs = new HashSet<>();
        //        Stream.of(cfmlDirList.split(","))
        //                .map (elem -> new String(elem))
        //                .collect(Collectors.toList());
        Stream.of(cfmlDirList.split(",")).forEach( aDirList -> {
            Path path;
            String dir;
            String virtual = "";
            String[] directoryAndAliasList = aDirList.trim().split("=");
            if(directoryAndAliasList.length == 1){
                dir = directoryAndAliasList[0].trim();
            } else {
                dir = directoryAndAliasList[1].trim();
                virtual = directoryAndAliasList[0].trim();
            }
            dir = dir.endsWith("/") ? dir : dir + '/';
            path = Paths.get(dir).normalize().toAbsolutePath();
            if(virtual.length() == 0){
                dirs.add(path);
            } else {
                virtual = virtual.startsWith("/") ? virtual : "/" + virtual;
                virtual = virtual.endsWith("/") ? virtual.substring(0, virtual.length() - 1) : virtual;
                aliases.put(virtual.toLowerCase(), path);
            }
            String aliasInfo = virtual.isEmpty() ? "" : " as " + virtual;
            if (!path.toFile().exists()) {
                MAPPER_LOG.errorf("Does not exist, cannot serve content from: " + path + aliasInfo);
            } else {
                MAPPER_LOG.info("Serving content from " + path + aliasInfo);
            }
        });
        contentDirs = dirs;
    }

    HashMap<String, Path> getAliases() {
        return aliases;
    }

    @Override
    public boolean isResourceChangeListenerSupported() {
        return allowResourceChangeListeners;
    }

    /**
     * Super nasty thing to display caller chain, just for debugging/experiments
     * @return list of steps
     */
    private static String getRecentSteps() {
        StringBuilder recentSteps = new StringBuilder();
        StackTraceElement[] traceElements = Thread.currentThread().getStackTrace();
        final int maxStepCount = 3;
        final int skipCount = 2;

        for (int i = Math.min(maxStepCount + skipCount, traceElements.length) - 1; i >= skipCount; i--) {
            String className = traceElements[i].getClassName().substring(traceElements[i].getClassName().lastIndexOf(".") + 1);
            recentSteps.append(" >> ").append(className).append(".").append(traceElements[i].getMethodName()).append("()");
        }

        return recentSteps.toString();
    }
    
    private static boolean caseSensitivityCheck() {
	    try {
	        File currentWorkingDir = new File(System.getProperty("user.dir"));
	        File case1 = new File(currentWorkingDir, "case1");
	        File case2 = new File(currentWorkingDir, "Case1");
	        case1.createNewFile();
	        if (case2.createNewFile()) {
	        	MAPPER_LOG.debug("FileSystem of working directory is case sensitive");
	            case1.delete();
	            case2.delete();
	            return true;
	        } else {
	        	MAPPER_LOG.debug("FileSystem of working directory is NOT case sensitive");
	            case1.delete();
	            return false;
	        }
	    } catch (Throwable e) {
	    	MAPPER_LOG.debug("Error detecting case sensitivity of file system.");
	    	e.printStackTrace();
	    }
        return true;
	}
    
    public void clearCaches() {
    	caseInsensitiveCache.clear();
        servletPathCache.clear();
    }

}
