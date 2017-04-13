package runwar.util;


import java.io.File;
import java.io.IOException;

import junit.framework.TestCase;

import org.junit.Test;

public class TestSSLUtil extends TestCase {
   
    public TestSSLUtil() {
    }

    @Test
    public void testLoadPEMcertNoPassword() throws IOException {
        File certfile = new File("tests/resource/ssl/selfsign.crt").getAbsoluteFile();
        File keyfile = new File("tests/resource/ssl/selfsign.key").getAbsoluteFile();
        SSLUtil.createSSLContext(certfile, keyfile, null, null);
        SSLUtil.createSSLContext(certfile, keyfile, "".toCharArray(), null);

        certfile = new File("tests/resource/ssl/myssl.crt").getAbsoluteFile();
        keyfile = new File("tests/resource/ssl/myssl.key").getAbsoluteFile();
        SSLUtil.createSSLContext(certfile, keyfile, "".toCharArray(), null);
    }

    @Test
    public void testLoadWeirdPEMcertNoPassword() throws IOException {
        File certfile = new File("tests/resource/ssl/myssl.crt").getAbsoluteFile();
        File keyfile = new File("tests/resource/ssl/myssl.key").getAbsoluteFile();
        SSLUtil.createSSLContext(certfile, keyfile, "".toCharArray(), null);
    }

    @Test
    public void testLoadPEMPasswordcert() throws IOException {
        File certfile = new File("tests/resource/ssl/selfsign.crt").getAbsoluteFile();
        File keyfile = new File("tests/resource/ssl/selfsign.key.password").getAbsoluteFile();
        char[] keypass = "password".toCharArray();
        SSLUtil.createSSLContext(certfile, keyfile, keypass, null);
    }

    @Test
    public void testLoadPEMPasswordcert2() throws IOException {
        File certfile = new File("tests/resource/ssl/selfsigned_pass.crt").getAbsoluteFile();
        File keyfile = new File("tests/resource/ssl/selfsigned_pass.key").getAbsoluteFile();
        char[] keypass = "password".toCharArray();
        SSLUtil.createSSLContext(certfile, keyfile, keypass, null);
    }
    
    @Test
    public void testLoadPEMPasswordcertWrongPass() throws IOException {
        File certfile = new File("tests/resource/ssl/selfsign.crt").getAbsoluteFile();
        File keyfile = new File("tests/resource/ssl/selfsign.key.password").getAbsoluteFile();
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
        File certfile = new File("tests/resource/ssl/selfsign.crt").getAbsoluteFile();
        File keyfile = new File("tests/resource/ssl/selfsign.key.notpassword").getAbsoluteFile();
        char[] keypass = "notpassword".toCharArray();
        SSLUtil.createSSLContext(certfile, keyfile, keypass, null);
        
    }

    @Test
    public void testAddCerts() throws IOException {
        File certfile = new File("tests/resource/ssl/selfsign.crt").getAbsoluteFile();
        File keyfile = new File("tests/resource/ssl/selfsign.key.notpassword").getAbsoluteFile();
        char[] keypass = "notpassword".toCharArray();
        
        String[] addCertfiles = new File("tests/resource/ssl/myssl.crt").getAbsolutePath().split(",");
        SSLUtil.createSSLContext(certfile, keyfile, keypass, addCertfiles);
    }

    @Test
    public void testLoadSelfCASelfSigned() throws IOException {
        File certfile = new File("tests/resource/ssl/server.crt").getAbsoluteFile();
        File keyfile = new File("tests/resource/ssl/server.key").getAbsoluteFile();
        char[] keypass = "notpassword".toCharArray();
        SSLUtil.createSSLContext(certfile, keyfile, keypass, null);        
    }

    @Test
    public void testLoadChain() throws IOException {
        File certfile = new File("tests/resource/ssl/delme.crt").getAbsoluteFile();
        File keyfile = new File("tests/resource/ssl/delme.key").getAbsoluteFile();
        char[] keypass = "notpassword".toCharArray();
        SSLUtil.createSSLContext(certfile, keyfile, keypass, null);        
    }
    
    @Test
    public void testLoadPKS12() throws IOException {
        File certfile = new File("tests/resource/ssl/server.crt").getAbsoluteFile();
        File keyfile = new File("tests/resource/ssl/server.p12").getAbsoluteFile();
        char[] keypass = "test".toCharArray();
        SSLUtil.createSSLContext(certfile, keyfile, keypass, null);
        
    }

}
