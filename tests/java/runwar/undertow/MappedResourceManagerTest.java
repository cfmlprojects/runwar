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
        String mappings = "/images=C:\\someplace\\somewhereelse, /moreimages=/this/is/somewhere, /docs/=C:\\someplace\\docs, /one/two=/buckle/my/shoe/, blah=C:\\foo, plain, path";
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
