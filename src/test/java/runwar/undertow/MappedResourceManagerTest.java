package runwar.undertow;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import runwar.options.ServerOptionsImpl;

import static org.junit.jupiter.api.Assertions.*;

public class MappedResourceManagerTest {
    private static HashMap<String, Path> aliasMap = new HashMap<>();
    private static final File baseDir = new File(System.getProperty("java.io.tmpdir") + "/mapper");
    private static final File docsDir = new File(baseDir,"docs");
    private static final File apostropheDir = new File(baseDir,"dan'oReiley");
    private static final File numbersDir = new File(baseDir,"2344564/234");
    private static final File imagesDir = new File(baseDir,"images");
    private static final File moreimagesDir = new File(baseDir,"moreimages");
    private static final File somewhereElseDir = new File(baseDir,"someplace/somewhereelse/");
    private static final File barDir = new File(baseDir,"bar");
    private static final File fooDir = new File(baseDir,"foo/");
    private static final File buckleDir= new File(baseDir,"buckle/my/shoe");
    private static final File[] testDirs = new File[]{ baseDir, docsDir, apostropheDir, numbersDir, imagesDir, moreimagesDir, somewhereElseDir, barDir, fooDir, buckleDir };
    private Path reqFile = null;

    MappedResourceManagerTest() {
    }

    @BeforeAll
    static void beforeAll() throws IOException {
        if(baseDir.exists()){
            Files.walkFileTree(Paths.get(baseDir.getAbsolutePath()), new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Files.delete(file);
                    return FileVisitResult.CONTINUE;
                }
                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                    Files.delete(dir);
                    return FileVisitResult.CONTINUE;
                }
            });
        }
        Stream.of(testDirs).forEach(testDir -> {
            if(!testDir.mkdirs()){
                throw new RuntimeException("Could not create test folder " + testDir.getAbsolutePath());
            }
        });
        String mappings = "/images=" + imagesDir.getAbsolutePath() + ","
                + "/moreimages="+ moreimagesDir.getAbsolutePath() + ","
                + "/docs/=" + docsDir.getAbsolutePath() + ","
                + "/one/two=" + buckleDir.getAbsolutePath() + ","
                + "blah =" + barDir.getAbsolutePath() + ","
                + "/foo=" + fooDir.getAbsolutePath() + ","
                + "/apostrophe=" + apostropheDir.getAbsolutePath() + ","
                + "numbers=" + numbersDir.getAbsolutePath() + ","
                + "src, "
                + "src/test , "
                + "rel=.., "
                + "relative=" + buckleDir.getAbsolutePath() + "/../../,"
                + "foobar=../" + Paths.get(".") + "";

        ServerOptionsImpl serverOptions = new ServerOptionsImpl();
        serverOptions.contentDirs(mappings);
        Set<Path> contentDirs = new HashSet<>();
        Map<String,Path> aliases = new HashMap<>();
        serverOptions.contentDirectories().forEach(s -> contentDirs.add(Paths.get(s)));
        serverOptions.aliases().forEach((s, s2) -> aliases.put(s,Paths.get(s2)));

        MappedResourceManager man = new MappedResourceManager(baseDir,111, contentDirs, aliases,null);
        aliasMap = man.getAliases();
        try {
            man.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @DisplayName("Should have loaded alias map")
    @ParameterizedTest(name = "{index} => message=''{0}''")
    @ValueSource(strings = {"/images", "/one/two", "/docs", "/blah", "/foo", "/relative"})
    void testAliasMapProcessed(String alias) {
        assertNotNull(aliasMap.get(alias));
    }

    @Test
    void testAliasMapLookup() {

        reqFile = MappedResourceManager.getAliasedFile(aliasMap, "/images/someimage.jpg");
        assertNotNull(reqFile);
        assertEquals(imagesDir.getAbsolutePath() + "/someimage.jpg", reqFile.toString());

        reqFile = MappedResourceManager.getAliasedFile(aliasMap, "/moreimages/someimage.jpg");
        assertNotNull(reqFile);
        assertEquals(moreimagesDir.getAbsolutePath() + "/someimage.jpg", reqFile.toString());

        reqFile = MappedResourceManager.getAliasedFile(aliasMap, "/moreimages/subdir/someimage.jpg");
        assertNotNull(reqFile);
        assertEquals(moreimagesDir.getAbsolutePath() + "/subdir/someimage.jpg", reqFile.toString());

        reqFile = MappedResourceManager.getAliasedFile(aliasMap, "/moreimages/subdir/");
        assertNotNull(reqFile);
        assertEquals(moreimagesDir.getAbsolutePath() + "/subdir", reqFile.toString());

        reqFile = MappedResourceManager.getAliasedFile(aliasMap, "/image/someimage.jpg");
        assertNull(reqFile);

        reqFile = MappedResourceManager.getAliasedFile(aliasMap, "/image/images/someimage.jpg");
        assertNull(reqFile);

        reqFile = MappedResourceManager.getAliasedFile(aliasMap, "/foobar/bar.txt");
        assertNotNull(reqFile);
        assertEquals(Paths.get("..").toAbsolutePath().normalize() + "/bar.txt", reqFile.toString());

        reqFile = MappedResourceManager.getAliasedFile(aliasMap, "/rel/afile.txt");
        assertNotNull(reqFile);
        assertEquals(Paths.get("..").toAbsolutePath().normalize() + "/afile.txt", reqFile.toString());

        reqFile = MappedResourceManager.getAliasedFile(aliasMap, "/foo/bar.txt");
        assertNotNull(reqFile);
        assertEquals(fooDir.getAbsolutePath() + "/bar.txt", reqFile.toString());

        reqFile = MappedResourceManager.getAliasedFile(aliasMap, "/foo/foo.txt");
        assertNotNull(reqFile);
        assertEquals(fooDir.getAbsolutePath() + "/foo.txt", reqFile.toString());

    }

    @Test
    void testAliasMapCaseInsensitiveLookup() {

        reqFile = MappedResourceManager.getAliasedFile(aliasMap, "/DOCS/somedoc.doc");
        assertNotNull(reqFile);
        assertEquals(docsDir.getAbsolutePath() + "/somedoc.doc", reqFile.toString());

        reqFile = MappedResourceManager.getAliasedFile(aliasMap, "/ImAGe/someimage.jpg");
        assertNull(reqFile);

        reqFile = MappedResourceManager.getAliasedFile(aliasMap, "/ImAGes/someimage.jpg");
        assertEquals(imagesDir + "/someimage.jpg", reqFile.toString());

        reqFile = MappedResourceManager.getAliasedFile(aliasMap, "/fOO/bar.txt");
        assertNotNull(reqFile);
        assertEquals(fooDir.getAbsolutePath() + "/bar.txt", reqFile.toString());

        reqFile = MappedResourceManager.getAliasedFile(aliasMap, "/FOO/foo.txt");
        assertNotNull(reqFile);
        assertEquals(fooDir.getAbsolutePath() + "/foo.txt", reqFile.toString());

    }

    @Test
    void testAliasMapGroovyLookup() {
        reqFile = MappedResourceManager.getAliasedFile(aliasMap, "/file:" + fooDir.getAbsolutePath() + "/some.groovy");
        assertNotNull(reqFile);
        assertEquals(fooDir.getAbsolutePath() + "/some.groovy", reqFile.toString());
    }

    @Test
    void testAliasRelative() {
        File thisDir = new File(System.getProperty("user.dir"));

        reqFile = MappedResourceManager.getAliasedFile(aliasMap, "/relative/fun/wee.txt");
        assertNotNull(reqFile);
        assertEquals(buckleDir.getParentFile().getParentFile().getAbsolutePath() + "/fun/wee.txt", reqFile.toString());

        reqFile = MappedResourceManager.getAliasedFile(aliasMap, "/rel/fun/wee.txt");
        assertNotNull(reqFile);
        assertEquals(Paths.get("..").toAbsolutePath().normalize() + "/fun/wee.txt", reqFile.toString());
    }

    @Test
    void testAliasMapTricky() {
        reqFile = MappedResourceManager.getAliasedFile(aliasMap, "/images");
        assertNotNull(reqFile);
        assertEquals(imagesDir.getAbsolutePath(), reqFile.toString());

        reqFile = MappedResourceManager.getAliasedFile(aliasMap, "/moreimages/");
        assertNotNull(reqFile);
        assertEquals(moreimagesDir.getAbsolutePath(), reqFile.toString());

        reqFile = MappedResourceManager.getAliasedFile(aliasMap, "/one/two/threenothere");
        assertNotNull(reqFile);
        assertEquals(buckleDir.getAbsolutePath() + "/threenothere", reqFile.toString());

    }

    @Test
    void testAliasMapNulls() {

        reqFile = MappedResourceManager.getAliasedFile(aliasMap, "/image/someimage.jpg");
        assertNull(reqFile);

        reqFile = MappedResourceManager.getAliasedFile(aliasMap, "/image/images/someimage.jpg");
        assertNull(reqFile);

    }

}
