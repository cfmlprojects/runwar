package runwar.undertow;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

import org.junit.Test;

public class MappedResourceManagerTest {
    private HashMap<String, File> aliasMap = new HashMap<String, File>();
    private File reqFile = null;

    public MappedResourceManagerTest() {
        String mappings = "/images=C:\\someplace\\somewhereelse, "
                + "/moreimages=/this/is/somewhere,"
                + "/docs/=C:\\someplace\\docs,"
                + "/one/two=/buckle/my/shoe/, "
                + "blah=C:\\foo, "
                + "plain, "
                + "path, "
                + "rel=../bar, "
                + "relative=/root/na/../bar, "
                + "foo=myFolder";
        MappedResourceManager man = new MappedResourceManager(new File("/tmp"),111,mappings);
        aliasMap = man.getAliasMap();
        try {
            man.close();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    @Test
    public void testAliasMapProcessed() {
        assertNotNull(aliasMap.get("/images"));
        assertNotNull(aliasMap.get("/one/two"));
        assertNotNull(aliasMap.get("/docs"));
        assertNotNull(aliasMap.get("/blah"));
        assertNotNull(aliasMap.get("/foo"));
        assertNotNull(aliasMap.get("/relative"));
    }
    
    @Test
    public void testAliasMapLookup() {

        reqFile = MappedResourceManager.getAliasedFile(aliasMap, "/images/someimage.jpg");
        assertNotNull(reqFile);
        assertEquals("C:\\someplace\\somewhereelse\\someimage.jpg", reqFile.getPath());

        reqFile = MappedResourceManager.getAliasedFile(aliasMap, "/moreimages/someimage.jpg");
        assertNotNull(reqFile);
        assertEquals("/this/is/somewhere/someimage.jpg", reqFile.getPath());

        reqFile = MappedResourceManager.getAliasedFile(aliasMap, "/moreimages/subdir/someimage.jpg");
        assertNotNull(reqFile);
        assertEquals("/this/is/somewhere/subdir/someimage.jpg", reqFile.getPath());

        reqFile = MappedResourceManager.getAliasedFile(aliasMap, "/moreimages/subdir/");
        assertNotNull(reqFile);
        assertEquals("/this/is/somewhere/subdir", reqFile.getPath());

        reqFile = MappedResourceManager.getAliasedFile(aliasMap, "/docs/somedoc.doc");
        assertNotNull(reqFile);
        assertEquals("C:\\someplace\\docs\\somedoc.doc", reqFile.getPath());

        reqFile = MappedResourceManager.getAliasedFile(aliasMap, "/image/someimage.jpg");
        assertNull(reqFile);

        reqFile = MappedResourceManager.getAliasedFile(aliasMap, "/image/images/someimage.jpg");
        assertNull(reqFile);

        reqFile = MappedResourceManager.getAliasedFile(aliasMap, "/foo/bar.txt");
        assertNotNull(reqFile);
        assertEquals("myFolder/bar.txt", reqFile.getPath());

        reqFile = MappedResourceManager.getAliasedFile(aliasMap, "/foo/foo.txt");
        assertNotNull(reqFile);
        assertEquals("myFolder/foo.txt", reqFile.getPath());
        
    }
    
    @Test
    public void testAliasMapCaseInsensitiveLookup() {


        reqFile = MappedResourceManager.getAliasedFile(aliasMap, "/DOCS/somedoc.doc");
        assertNotNull(reqFile);
        assertEquals("C:\\someplace\\docs\\somedoc.doc", reqFile.getPath());

        reqFile = MappedResourceManager.getAliasedFile(aliasMap, "/ImAGe/someimage.jpg");
        assertNull(reqFile);

        reqFile = MappedResourceManager.getAliasedFile(aliasMap, "/ImAGes/someimage.jpg");
        assertEquals("C:\\someplace\\somewhereelse\\someimage.jpg", reqFile.getPath());
        
        reqFile = MappedResourceManager.getAliasedFile(aliasMap, "/fOO/bar.txt");
        assertNotNull(reqFile);
        assertEquals("myFolder/bar.txt", reqFile.getPath());

        reqFile = MappedResourceManager.getAliasedFile(aliasMap, "/FOO/foo.txt");
        assertNotNull(reqFile);
        assertEquals("myFolder/foo.txt", reqFile.getPath());
        
    }
        
    @Test
    public void testAliasMapGroovyLookup() {
        reqFile = MappedResourceManager.getAliasedFile(aliasMap, "/file:/this/is/somewhere/some.groovy");
        assertNotNull(reqFile);
        assertEquals("/this/is/somewhere/some.groovy", reqFile.getPath());
    }
    
    @Test
    public void testAliasRelative() {
        File thisDir = new File(System.getProperty("user.dir"));

        reqFile = MappedResourceManager.getAliasedFile(aliasMap, "/relative/fun/wee.txt");
        assertNotNull(reqFile);
        assertEquals("/root/bar/fun/wee.txt", reqFile.getPath());

        reqFile = MappedResourceManager.getAliasedFile(aliasMap, "/rel/fun/wee.txt");
        assertNotNull(reqFile);
        assertEquals(thisDir.getParentFile().getPath() + "/bar/fun/wee.txt", reqFile.getPath());
    }

    @Test
    public void testAliasMapTricky() {
        reqFile = MappedResourceManager.getAliasedFile(aliasMap, "/images");
        assertNotNull(reqFile);
        assertEquals("C:\\someplace\\somewhereelse", reqFile.getPath());

        reqFile = MappedResourceManager.getAliasedFile(aliasMap, "/moreimages/");
        assertNotNull(reqFile);
        assertEquals("/this/is/somewhere/", reqFile.getPath());
        
        reqFile = MappedResourceManager.getAliasedFile(aliasMap, "/moreimages");
        assertNotNull(reqFile);
        assertEquals("/this/is/somewhere", reqFile.getPath());
        
        reqFile = MappedResourceManager.getAliasedFile(aliasMap, "/one/two/threenothere");
        assertNotNull(reqFile);
        assertEquals("/buckle/my/shoe/threenothere", reqFile.getPath());
        
    }

    @Test
    public void testAliasMapNulls() {

        reqFile = MappedResourceManager.getAliasedFile(aliasMap, "/image/someimage.jpg");
        assertNull(reqFile);

        reqFile = MappedResourceManager.getAliasedFile(aliasMap, "/image/images/someimage.jpg");
        assertNull(reqFile);

    }

}
