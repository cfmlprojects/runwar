package runwar.undertow;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.undertow.server.handlers.resource.FileResource;
import io.undertow.server.handlers.resource.FileResourceManager;
import io.undertow.server.handlers.resource.Resource;
import static runwar.logging.RunwarLogger.MAPPER_LOG;

public class MappedResourceManager extends FileResourceManager {

    private HashMap<String, File> aliasMap = new HashMap<String, File>();
    private File[] cfmlDirsFiles;
    private File WEBINF = null;
    private static final Matcher CFIDE_REGEX_MATCHER = Pattern.compile("^.?CFIDE").matcher("");
    private static final Matcher WEBINF_REGEX_MATCHER = Pattern.compile(".*?WEB-INF(.+)?").matcher("");
    private final boolean allowResourceChangeListeners;

    public MappedResourceManager(File base, long transferMinSize, String cfmlDirList, boolean allowResourceChangeListeners) {
        super(base, transferMinSize);
        this.allowResourceChangeListeners = allowResourceChangeListeners;
        processMappings(cfmlDirList);
    }

    public MappedResourceManager(File base, long transferMinSize, String cfmlDirList, File webinfDir) {
        this(base, transferMinSize, cfmlDirList, false);
        WEBINF = webinfDir;
        MAPPER_LOG.debugf("Initialized MappedResourceManager - base: %s, web-inf: %s, aliases: %s",base.getAbsolutePath(), webinfDir.getAbsolutePath(), cfmlDirList);
        if (!WEBINF.exists()) {
            throw new RuntimeException("The specified WEB-INF does not exist: " + WEBINF.getAbsolutePath());
        }
    }

    private void processMappings(String cfmlDirList) {
        HashSet<File> dirs = new HashSet<File>();
        String[] dirList = cfmlDirList.split(",");
        for (int x = 0; x < dirList.length; x++) {
            File dir;
            String virtual = "";
            String[] splitted = dirList[x].split("=");
            if (splitted.length == 1) {
                dir = new File(dirList[x].trim());
                dir = dir.getPath().startsWith("..") ? dir.getAbsoluteFile() : dir;
                dirs.add(dir.toPath().normalize().toFile());
            } else {
                virtual = splitted[0].trim();
                virtual = virtual.startsWith("/") ? virtual : "/" + virtual;
                virtual = virtual.endsWith("/") ? virtual.substring(0, virtual.length()-1) : virtual;
                dir = new File(splitted[1].trim());
                dir = dir.getPath().startsWith("..") ? dir.getAbsoluteFile() : dir;
                aliasMap.put(virtual.toLowerCase(), dir.toPath().normalize().toFile());
            }
            String aliasInfo = virtual.isEmpty() ? "" : " as " + virtual;
            if (!dir.exists()) {
                MAPPER_LOG.error("Does not exist, cannot serve content from: " + dir.getAbsolutePath() + aliasInfo);
            } else {
                MAPPER_LOG.info("Serving content from " + dir.getAbsolutePath() + aliasInfo);
            }
        }
        cfmlDirsFiles = dirs.toArray(new File[dirs.size()]);
    };

    public Resource getResource(String path) {
        if(path == null) {
            MAPPER_LOG.error("* getResource got a null path!");
            return null;
        }
//        path = path.trim();
        MAPPER_LOG.debug("* requested: '" + path + "'");
        File reqFile = null;
        try {
            final Matcher webInfMatcher = WEBINF_REGEX_MATCHER.reset(path);
            if (WEBINF != null && webInfMatcher.matches()) {
                if(webInfMatcher.group(1) == null) {
                    reqFile = WEBINF;
                } else {
                    reqFile = new File(WEBINF, webInfMatcher.group(1).replace("WEB-INF", ""));
                }
                MAPPER_LOG.trace("** matched WEB-INF : " + reqFile.getAbsolutePath());
            } else if (CFIDE_REGEX_MATCHER.reset(path).matches()) {
                reqFile = new File(WEBINF.getParentFile(), path);
                MAPPER_LOG.trace("** matched /CFIDE : " + reqFile.getAbsolutePath());
            } else if (!webInfMatcher.matches()) {
                reqFile = new File(getBase(), path);
                MAPPER_LOG.trace("* checking with base path: '" + reqFile.getAbsolutePath() + "'");
                if (!reqFile.exists()) {
                    reqFile = getAliasedFile(aliasMap, path);
                }
                if (reqFile == null) {
                    for (int x = 0; x < cfmlDirsFiles.length; x++) {
                        String absPath = cfmlDirsFiles[x].getCanonicalPath();
                        reqFile = new File(cfmlDirsFiles[x], path.replace(absPath, ""));
                        MAPPER_LOG.tracef("checking: '%s' = '%s'",absPath,reqFile.getAbsolutePath());
                        if (reqFile.exists()) {
                            MAPPER_LOG.tracef("Exists: '%s'",reqFile.getAbsolutePath());
                            break;
                        }
                    }
                }
            }
            if (reqFile != null && reqFile.exists()) {
                reqFile = reqFile.getAbsoluteFile().toPath().normalize().toFile();
                MAPPER_LOG.debugf("** path mapped to: '%s'", reqFile);
                return new FileResource(reqFile, this, path);
            } else {
                MAPPER_LOG.debugf("** No mapped resource for: '%s' (reqFile was: '%s')",path,reqFile != null ? reqFile.getAbsolutePath() : "null");
                return super.getResource(path);
            }
        } catch (MalformedURLException e) {
            MAPPER_LOG.error(e.getMessage());
        } catch (IOException e) {
            MAPPER_LOG.error(e.getMessage());
        }
        return null;
    }

    public static File getAliasedFile(HashMap<String, File> aliasMap, String path) {
        if(path.startsWith("/file:")){
            // groovy servlet asks for /file:... for some reason, when scripts are in an aliased dir
            path = path.replace("/file:", "");
            for( File file : aliasMap.values()) {
                if(path.startsWith(file.getPath())) {
                    return new File(path);
                }
            }
        }
        String pathDir = path.startsWith("/") ? path : "/" + path;
        File file = aliasMap.get(pathDir.toLowerCase());
        if(file != null) {
            return new File(file.getPath());
        }
        while (pathDir.lastIndexOf('/') > 0) {
            pathDir = pathDir.substring(0, pathDir.lastIndexOf('/'));
            if (aliasMap.containsKey(pathDir.toLowerCase())) {
                file = new File(aliasMap.get(pathDir.toLowerCase()), path.substring(pathDir.length()));
                if(file.getPath().indexOf('\\') > 0){
                    file = new File(file.getPath().replace('/', '\\'));
                }
                return file;
            }
        }
        return null;
    }
    
    public HashMap<String, File> getAliasMap() {
        return aliasMap;
    }
    
    @Override
    public boolean isResourceChangeListenerSupported() {
        return allowResourceChangeListeners;
    }
}
