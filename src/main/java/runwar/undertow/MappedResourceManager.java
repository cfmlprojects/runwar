package runwar.undertow;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.undertow.server.handlers.resource.FileResource;
import io.undertow.server.handlers.resource.FileResourceManager;
import io.undertow.server.handlers.resource.Resource;

import static runwar.logging.RunwarLogger.MAPPER_LOG;

public class MappedResourceManager extends FileResourceManager {

    private HashMap<String, File> aliasMap = new HashMap<>();
    private File[] cfmlDirsFiles;
    private File WEBINF = null, CFIDE = null;
    private static final Matcher CFIDE_REGEX_MATCHER;
    private static final Matcher WEBINF_REGEX_MATCHER;

    static {
        CFIDE_REGEX_MATCHER = Pattern.compile("(?i)^.*[\\\\/]?CFIDE([\\\\/].*)?").matcher("");
        WEBINF_REGEX_MATCHER = Pattern.compile("(?i).*[\\\\/]?WEB-INF([\\\\/].*)?").matcher("");
    }

    private final boolean allowResourceChangeListeners;

    public MappedResourceManager(File base, long transferMinSize, String cfmlDirList, boolean allowResourceChangeListeners) {
        super(base, transferMinSize);
        this.allowResourceChangeListeners = allowResourceChangeListeners;
        processMappings(cfmlDirList);
    }

    public MappedResourceManager(File base, long transferMinSize, String cfmlDirList, File webinfDir) {
        this(base, transferMinSize, cfmlDirList, false);
        WEBINF = webinfDir;
        CFIDE = new File(WEBINF.getParentFile(),"CFIDE");
        MAPPER_LOG.debugf("Initialized MappedResourceManager - base: %s, web-inf: %s, aliases: %s",base.getAbsolutePath(), webinfDir.getAbsolutePath(), cfmlDirList);
        if (!WEBINF.exists()) {
            throw new RuntimeException("The specified WEB-INF does not exist: " + WEBINF.getAbsolutePath());
        }
    }

    private void processMappings(String cfmlDirList) {
        HashSet<File> dirs = new HashSet<>();
        String[] dirList = cfmlDirList.split(",");
        for (String aDirList : dirList) {
            File dir;
            String virtual = "";
            String[] directoryAndAliasList = aDirList.split("=");
            if (directoryAndAliasList.length == 1) {
                dir = new File(aDirList.trim());
                dir = dir.getPath().startsWith("..") ? dir.getAbsoluteFile() : dir;
                dirs.add(dir.toPath().normalize().toFile());
            } else {
                virtual = directoryAndAliasList[0].trim();
                virtual = virtual.startsWith("/") ? virtual : "/" + virtual;
                virtual = virtual.endsWith("/") ? virtual.substring(0, virtual.length() - 1) : virtual;
                dir = new File(directoryAndAliasList[1].trim());
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
        int directoriesAliased = dirs.size();
        cfmlDirsFiles = dirs.toArray(new File[directoriesAliased]);
    }

    public Resource getResource(String path) {
        if(path == null) {
            MAPPER_LOG.error("* getResource got a null path!");
            return null;
        }
        MAPPER_LOG.debug("* requested: '" + path + "'");
        File reqFile = null;
        try {
            final Matcher webInfMatcher = WEBINF_REGEX_MATCHER.reset(path);
            final Matcher cfideMatcher = CFIDE_REGEX_MATCHER.reset(path);
            if (WEBINF != null && webInfMatcher.matches()) {
                if(webInfMatcher.group(1) == null) {
                    reqFile = WEBINF;
                } else {
                    reqFile = new File(WEBINF, webInfMatcher.group(1).replace("WEB-INF", ""));
                }
                MAPPER_LOG.trace("** matched WEB-INF : " + reqFile.getAbsolutePath());
            } else if (cfideMatcher.matches()) {
                if(cfideMatcher.group(1) == null) {
                    reqFile = CFIDE;
                } else {
                    reqFile = new File(CFIDE, cfideMatcher.group(1).replace("CFIDE", ""));
                }
                MAPPER_LOG.trace("** matched /CFIDE : " + reqFile.getAbsolutePath());
            } else if (!webInfMatcher.matches()) {
                reqFile = new File(getBase(), path);
                MAPPER_LOG.trace("* checking with base path: '" + reqFile.getAbsolutePath() + "'");
                if (!reqFile.exists()) {
                    reqFile = getAliasedFile(aliasMap, path);
                }
                if (reqFile == null) {
                    for (File cfmlDirsFile : cfmlDirsFiles) {
                        String absPath = cfmlDirsFile.getCanonicalPath();
                        reqFile = new File(cfmlDirsFile, path.replace(absPath, ""));
                        MAPPER_LOG.tracef("checking: '%s' = '%s'", absPath, reqFile.getAbsolutePath());
                        if (reqFile.exists()) {
                            MAPPER_LOG.tracef("Exists: '%s'", reqFile.getAbsolutePath());
                            break;
                        }
                    }
                }
            }
            if (reqFile != null && reqFile.exists()) {
                if(reqFile.getPath().indexOf('\\') > 0){
                    reqFile = new File(reqFile.getPath().replace('/', '\\'));
                }
//                reqFile = reqFile.getAbsoluteFile().toPath().normalize().toFile();
                MAPPER_LOG.debugf("** path mapped to: '%s'", reqFile);
                return new FileResource(reqFile, this, path);
            } else {
                MAPPER_LOG.debugf("** No mapped resource for: '%s' (reqFile was: '%s')",path,reqFile != null ? reqFile.getAbsolutePath() : "null");
                return super.getResource(path);
            }
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

    HashMap<String, File> getAliasMap() {
        return aliasMap;
    }

    @Override
    public boolean isResourceChangeListenerSupported() {
        return allowResourceChangeListeners;
    }
}
