package runwar.security;


import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.fail;

public class TestSSLUtil {
    String[] hostNames = new String[]{"localhost"};

    public TestSSLUtil() {
    }

    @Test
    public void testLoadPEMcertNoPassword() throws IOException {
        File certfile = new File("src/test/resources/ssl/selfsign.crt").getAbsoluteFile();
        File keyfile = new File("src/test/resources/ssl/selfsign.key").getAbsoluteFile();
        SSLUtil.createSSLContext(certfile, keyfile, null, null, hostNames);
        SSLUtil.createSSLContext(certfile, keyfile, "".toCharArray(), null, hostNames);

        certfile = new File("src/test/resources/ssl/myssl.crt").getAbsoluteFile();
        keyfile = new File("src/test/resources/ssl/myssl.key").getAbsoluteFile();
        SSLUtil.createSSLContext(certfile, keyfile, "".toCharArray(), null, hostNames);
    }

    @Test
    public void testLoadWeirdPEMcertNoPassword() throws IOException {
        File certfile = new File("src/test/resources/ssl/myssl.crt").getAbsoluteFile();
        File keyfile = new File("src/test/resources/ssl/myssl.key").getAbsoluteFile();
        SSLUtil.createSSLContext(certfile, keyfile, "".toCharArray(), null, hostNames);
    }

    @Test
    public void testLoadPEMPasswordcert() throws IOException {
        File certfile = new File("src/test/resources/ssl/selfsign.crt").getAbsoluteFile();
        File keyfile = new File("src/test/resources/ssl/selfsign.key.password").getAbsoluteFile();
        char[] keypass = "password".toCharArray();
        SSLUtil.createSSLContext(certfile, keyfile, keypass, null, hostNames);
    }

    @Test
    public void testLoadPEMPasswordcertWrongPass() throws IOException {
        File certfile = new File("src/test/resources/ssl/selfsign.crt").getAbsoluteFile();
        File keyfile = new File("src/test/resources/ssl/selfsign.key.password").getAbsoluteFile();
        char[] keypass = "passwordededman".toCharArray();
        try{
            SSLUtil.createSSLContext(certfile, keyfile, keypass, null, hostNames);
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
        SSLUtil.createSSLContext(certfile, keyfile, keypass, null, hostNames);
        
    }

    @Test
    public void testAddCerts() throws IOException {
        File certfile = new File("src/test/resources/ssl/selfsign.crt").getAbsoluteFile();
        File keyfile = new File("src/test/resources/ssl/selfsign.key.notpassword").getAbsoluteFile();
        char[] keypass = "notpassword".toCharArray();
        
        String[] addCertfiles = new File("src/test/resources/ssl/myssl.crt").getAbsolutePath().split(",");
        SSLUtil.createSSLContext(certfile, keyfile, keypass, addCertfiles, hostNames);
    }

    @Test
    public void testLoadSelfCASelfSigned() throws IOException {
        File certfile = new File("src/test/resources/ssl/server.crt").getAbsoluteFile();
        File keyfile = new File("src/test/resources/ssl/server.key").getAbsoluteFile();
        char[] keypass = "notpassword".toCharArray();
        SSLUtil.createSSLContext(certfile, keyfile, keypass, null, hostNames);
    }

    @Test
    public void testLoadPKS12() throws IOException {
        File certfile = new File("src/test/resources/ssl/server.crt").getAbsoluteFile();
        File keyfile = new File("src/test/resources/ssl/server.p12").getAbsoluteFile();
        char[] keypass = "test".toCharArray();
        SSLUtil.createSSLContext(certfile, keyfile, keypass, null, hostNames);

    }

}
