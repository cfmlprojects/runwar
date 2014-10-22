package runwar.undertow;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;

import runwar.logging.Logger;

import io.undertow.server.handlers.resource.FileResource;
import io.undertow.server.handlers.resource.FileResourceManager;
import io.undertow.server.handlers.resource.Resource;

public class MappedResourceManager extends FileResourceManager {

    private static Logger log = Logger.getLogger("RunwarLogger");
    private File[] cfmlDirsFiles;
    private String[] cfmlDirs;
    private File WEBINF = null;

    public MappedResourceManager(File base, long transferMinSize, String cfmlDirList) {
        super(base, transferMinSize);
        this.cfmlDirs = cfmlDirList.split(",");
        this.cfmlDirsFiles = new File[cfmlDirs.length];
        for (int x = 0; x < cfmlDirs.length; x++) {
            File dir = new File(cfmlDirs[x]);
            cfmlDirsFiles[x] = dir;
            if (!dir.exists()) {
                log.error("Does not exist, cannot serve content: " + dir.getAbsolutePath());
            } else {
                log.info("Serving content from " + dir.getAbsolutePath());
            }
        }
    }

    public MappedResourceManager(File base, long transferMinSize, String cfmlDirList, File file) {
        super(base, transferMinSize);
        this.cfmlDirs = cfmlDirList.split(",");
        this.cfmlDirsFiles = new File[cfmlDirs.length];
        this.WEBINF = file;
        if (!this.WEBINF.exists()) {
            throw new RuntimeException("The specified WEB-INF does not exist: " + this.WEBINF.getAbsolutePath());
        }
        for (int x = 0; x < cfmlDirs.length; x++) {
            File dir = new File(cfmlDirs[x]);
            cfmlDirsFiles[x] = dir;
            if (!dir.exists()) {
                log.error("Does not exist, cannot serve content: " + dir.getAbsolutePath());
            } else {
                log.info("Serving content from " + dir.getAbsolutePath());
            }
        }
    }

    public Resource getResource(String path) {
//        log.trace("* requested:" + path);
        File reqFile = null;
        try {
            if (WEBINF != null && path.startsWith("/WEB-INF")) {
                if (path.equals("/WEB-INF")) {
                    reqFile = WEBINF;
                }
                reqFile = new File(WEBINF, path.replace("/WEB-INF",""));
            } else if (!path.startsWith("/WEB-INF")) {
                reqFile = new File(getBase(), path);
                if (!reqFile.exists()) {
                    for (int x = 0; x < cfmlDirs.length; x++) {
                        String absPath = cfmlDirsFiles[x].getCanonicalPath();
                        reqFile = new File(cfmlDirsFiles[x], path.replace(absPath, ""));
//                        log.tracef("checking:%s = %s",absPath,reqFile.getAbsolutePath());
                        if (reqFile.exists()) {
                            break;
                        }
                    }
                }
            }
            if(reqFile!=null && reqFile.exists()) {
//                log.tracef("path mapped to:%s", reqFile.getAbsolutePath());
                return new FileResource(reqFile, this, path);
            } else {
//                log.tracef("no mapped resoruce for:%s",path);
                return super.getResource(path);
            }
        } catch (MalformedURLException e) {
            log.error(e.getMessage());
        } catch (IOException e) {
            log.error(e.getMessage());
        }
        return null;
    }

}
