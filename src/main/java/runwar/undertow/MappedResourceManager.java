package runwar.undertow;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import io.undertow.server.handlers.resource.FileResource;
import io.undertow.server.handlers.resource.FileResourceManager;
import io.undertow.server.handlers.resource.Resource;

import static runwar.logging.RunwarLogger.MAPPER_LOG;

public class MappedResourceManager extends FileResourceManager {

    private HashMap<String, Path> aliases;
    private HashSet<Path> contentDirs;
    private File WEBINF = null, CFIDE = null;
    private static final Pattern CFIDE_REGEX_PATTERN;
    private static final Pattern WEBINF_REGEX_PATTERN;

    static {
        CFIDE_REGEX_PATTERN = Pattern.compile("(?i)^.*[\\\\/]?CFIDE([\\\\/].*)?");
        WEBINF_REGEX_PATTERN = Pattern.compile("(?i)^.*[\\\\/]?WEB-INF([\\\\/].*)?");
    }

    private final boolean allowResourceChangeListeners;

    public MappedResourceManager(File base, long transferMinSize, Set<Path> contentDirs, Map<String,Path> aliases, File webinfDir) {
        super(base, transferMinSize);
        this.allowResourceChangeListeners = false;
        this.contentDirs = (HashSet<Path>) contentDirs;
        this.aliases = (HashMap<String, Path>) aliases;
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
            if (reqFile == null) {
                for (Path cfmlDirsFile : contentDirs) {
                    reqFile = Paths.get(cfmlDirsFile.toString(), path.replace(cfmlDirsFile.toAbsolutePath().toString(), ""));
                    MAPPER_LOG.tracef("checking: '%s' = '%s'", cfmlDirsFile.toAbsolutePath().toString(), reqFile.toAbsolutePath());
                    if (Files.exists(reqFile)) {
                        MAPPER_LOG.tracef("Exists: '%s'", reqFile.toAbsolutePath().toString());
                        break;
                    }
                }
            }
        }
        if (reqFile != null && Files.exists(reqFile)) {
            if(reqFile.toString().indexOf('\\') > 0) {
                reqFile = Paths.get(reqFile.toString().replace('/', '\\'));
            }
            MAPPER_LOG.debugf("** path mapped to: '%s'", reqFile);
            return new FileResource(reqFile.toFile(), this, path);
        } else {
            MAPPER_LOG.debugf("** No mapped resource for: '%s' (reqFile was: '%s')",path,reqFile != null ? reqFile.toString() : "null");
            return super.getResource(path);
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

}
