package runwar.security;



import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;
import java.io.IOException;

import org.junit.jupiter.api.Test;

import runwar.security.SSLUtil;

public class TestSSLUtil {
   
    public TestSSLUtil() {
    }

    @Test
    public void testLoadPEMcertNoPassword() throws IOException {
        File certfile = new File("src/test/resources/ssl/selfsign.crt").getAbsoluteFile();
        File keyfile = new File("src/test/resources/ssl/selfsign.key").getAbsoluteFile();
        SSLUtil.createSSLContext(certfile, keyfile, null, null);
        SSLUtil.createSSLContext(certfile, keyfile, "".toCharArray(), null);

        certfile = new File("src/test/resources/ssl/myssl.crt").getAbsoluteFile();
        keyfile = new File("src/test/resources/ssl/myssl.key").getAbsoluteFile();
        SSLUtil.createSSLContext(certfile, keyfile, "".toCharArray(), null);
    }

    @Test
    public void testLoadWeirdPEMcertNoPassword() throws IOException {
        File certfile = new File("src/test/resources/ssl/myssl.crt").getAbsoluteFile();
        File keyfile = new File("src/test/resources/ssl/myssl.key").getAbsoluteFile();
        SSLUtil.createSSLContext(certfile, keyfile, "".toCharArray(), null);
    }

    @Test
    public void testLoadPEMPasswordcert() throws IOException {
        File certfile = new File("src/test/resources/ssl/selfsign.crt").getAbsoluteFile();
        File keyfile = new File("src/test/resources/ssl/selfsign.key.password").getAbsoluteFile();
        char[] keypass = "password".toCharArray();
        SSLUtil.createSSLContext(certfile, keyfile, keypass, null);
    }

    @Test
    public void testLoadPEMPasswordcertWrongPass() throws IOException {
        File certfile = new File("src/test/resources/ssl/selfsign.crt").getAbsoluteFile();
        File keyfile = new File("src/test/resources/ssl/selfsign.key.password").getAbsoluteFile();
        char[] keypass = "passwordededman".toCharArray();
        try{
            SSLUtil.createSSLContext(certfile, keyfile, keypass, null);
        } catch (Exception e) {
            return;
        }
        fail("An incorrect password should fail, bruh.");
    }

    @Test
    public void testLoadPEMDifferentPasswordcert() throws IOException {
        File certfile = new File("src/test/resources/ssl/selfsign.crt").getAbsoluteFile();
        File keyfile = new File("src/test/resources/ssl/selfsign.key.notpassword").getAbsoluteFile();
        char[] keypass = "notpassword".toCharArray();
        SSLUtil.createSSLContext(certfile, keyfile, keypass, null);
        
    }

    @Test
    public void testAddCerts() throws IOException {
        File certfile = new File("src/test/resources/ssl/selfsign.crt").getAbsoluteFile();
        File keyfile = new File("src/test/resources/ssl/selfsign.key.notpassword").getAbsoluteFile();
        char[] keypass = "notpassword".toCharArray();
        
        String[] addCertfiles = new File("src/test/resources/ssl/myssl.crt").getAbsolutePath().split(",");
        SSLUtil.createSSLContext(certfile, keyfile, keypass, addCertfiles);
    }

    @Test
    public void testLoadSelfCASelfSigned() throws IOException {
        File certfile = new File("src/test/resources/ssl/server.crt").getAbsoluteFile();
        File keyfile = new File("src/test/resources/ssl/server.key").getAbsoluteFile();
        char[] keypass = "notpassword".toCharArray();
        SSLUtil.createSSLContext(certfile, keyfile, keypass, null);        
    }

/*
    @Test
    public void testLoadChain() throws IOException {
        File certfile = new File("src/test/resources/ssl/chain.crt").getAbsoluteFile();
        File keyfile = new File("src/test/resources/ssl/chain.key").getAbsoluteFile();
        char[] keypass = "notpassword".toCharArray();
        SSLUtil.createSSLContext(certfile, keyfile, keypass, null);
    }
*/
    @Test
    public void testLoadPKS12() throws IOException {
        File certfile = new File("src/test/resources/ssl/server.crt").getAbsoluteFile();
        File keyfile = new File("src/test/resources/ssl/server.p12").getAbsoluteFile();
        char[] keypass = "test".toCharArray();
        SSLUtil.createSSLContext(certfile, keyfile, keypass, null);
        
    }

}
