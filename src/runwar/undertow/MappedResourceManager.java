package runwar.undertow;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.HashSet;

import runwar.logging.Logger;
import io.undertow.server.handlers.resource.FileResource;
import io.undertow.server.handlers.resource.FileResourceManager;
import io.undertow.server.handlers.resource.Resource;

public class MappedResourceManager extends FileResourceManager {

    private static Logger log = Logger.getLogger("RunwarLogger");
    private HashMap<String, File> aliasMap = new HashMap<String, File>();
    private File[] cfmlDirsFiles;
    private File WEBINF = null;
    
    private final boolean allowResourceChangeListeners;

    public MappedResourceManager(File base, long transferMinSize, String cfmlDirList) {
        super(base, transferMinSize);
        this.allowResourceChangeListeners = true;
        processMappings(cfmlDirList);
    }

    public MappedResourceManager(File base, long transferMinSize, String cfmlDirList, File file) {
        this(base, transferMinSize, cfmlDirList);
        WEBINF = file;
        if (!WEBINF.exists()) {
            throw new RuntimeException("The specified WEB-INF does not exist: " + WEBINF.getAbsolutePath());
        }
    }

    private void processMappings(String cfmlDirList) {
        HashSet<File> dirs = new HashSet<File>();
        String[] dirList = cfmlDirList.split(",");
        for (int x = 0; x < dirList.length; x++) {
            File dir;
            String[] splitted = dirList[x].split("=");
            if (splitted.length == 1) {
                dir = new File(dirList[x].trim());
                dirs.add(dir);
            } else {
                String virtual = splitted[0].trim();
                virtual = virtual.startsWith("/") ? virtual : "/" + virtual;
                virtual = virtual.endsWith("/") ? virtual.substring(0, virtual.length()-1) : virtual;
                dir = new File(splitted[1].trim());
                aliasMap.put(virtual, dir);
            }
            if (!dir.exists()) {
                log.error("Does not exist, cannot serve content: " + dir.getAbsolutePath());
            } else {
                log.info("Serving content from " + dir.getAbsolutePath());
            }
        }
        cfmlDirsFiles = dirs.toArray(new File[dirs.size()]);
    };
    
    public Resource getResource(String path) {
        log.trace("* requested:" + path);
        File reqFile = null;
        try {
            if (WEBINF != null && (path.startsWith("/WEB-INF") || path.startsWith("./WEB-INF"))) {
                if (path.equals("/WEB-INF") || path.equals("./WEB-INF")) {
                    reqFile = WEBINF;
                }
                reqFile = new File(WEBINF, path.replaceAll(".+WEB-INF", ""));
            } else if (path.startsWith(WEBINF.getPath())) {
                reqFile = new File(WEBINF, path.replace(WEBINF.getPath(), ""));
            } else if (path.startsWith("/CFIDE")) {
                reqFile = new File(WEBINF.getParentFile(), path);
            } else if (!path.startsWith("/WEB-INF")) {
                reqFile = new File(getBase(), path);
                if (!reqFile.exists()) {
                    reqFile = getAliasedFile(aliasMap, path);
                }
                if (reqFile == null) {
                    for (int x = 0; x < cfmlDirsFiles.length; x++) {
                        String absPath = cfmlDirsFiles[x].getCanonicalPath();
                        reqFile = new File(cfmlDirsFiles[x], path.replace(absPath, ""));
                        log.tracef("checking:%s = %s",absPath,reqFile.getAbsolutePath());
                        if (reqFile.exists()) {
                            break;
                        }
                    }
                }
            }
            if (reqFile != null && reqFile.exists()) {
                log.tracef("path mapped to:%s", reqFile.getAbsolutePath());
                return new FileResource(reqFile, this, path);
            } else {
                log.tracef("no mapped resoruce for:%s",path);
                return super.getResource(path);
            }
        } catch (MalformedURLException e) {
            log.error(e.getMessage());
        } catch (IOException e) {
            log.error(e.getMessage());
        }
        return null;
    }

    public static File getAliasedFile(HashMap<String, File> aliasMap, String path) {
        String pathDir = path.startsWith("/") ? path : "/" + path;
        File file = aliasMap.get(pathDir);
        if(file != null) {
            return new File(file.getPath());
        }
        while (pathDir.lastIndexOf('/') > 0) {
            pathDir = pathDir.substring(0, pathDir.lastIndexOf('/'));
            if (aliasMap.containsKey(pathDir)) {
                file = new File(aliasMap.get(pathDir), path.replace(pathDir, ""));
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
        return true;
    }
}
