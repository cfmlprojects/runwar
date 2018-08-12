package runwar.security;

import org.junit.jupiter.api.Test;

import runwar.security.SelfSignedCertificate.ThreadLocalInsecureRandom;

import static org.junit.jupiter.api.Assertions.*;

public class SelfSignedCertificateTest {

    @Test
    public void testCertificate() throws Exception {
        final SelfSignedCertificate ssc = SelfSignedCertificate.generateCertificate();
        assertNotNull(ssc.certificate());
    }

    @Test
    public void testPrivateKey() throws Exception {
        final SelfSignedCertificate ssc = SelfSignedCertificate.generateCertificate();
        assertNotNull(ssc.privateKey());
    }

    @Test
    public void testKeyStore() throws Exception {
        final SelfSignedCertificate ssc = SelfSignedCertificate.generateCertificate();
        assertNotNull(ssc.keyStore());
    }

    @Test
    public void testGenerateCertificate() throws Exception {
        final SelfSignedCertificate ssc = SelfSignedCertificate.generateCertificate();
        assertEquals(ssc.keyStore().getCertificate(SelfSignedCertificate.DEFAULT_HOST), ssc.certificate());
    }

    @Test
    public void testGenerateCertificateWithCustomFQDN() throws Exception {
        final SelfSignedCertificate ssc = SelfSignedCertificate.generateCertificate("cfmlprojects.org", "password");
        assertEquals(ssc.keyStore().getCertificate("cfmlprojects.org"),ssc.certificate());
    }


    @Test
    public void testGenerateCertificateWithCustomFQDNAndKeyLength() throws Exception {
        final SelfSignedCertificate ssc =
                SelfSignedCertificate.generateCertificate(true, "cfmlprojects.org", ThreadLocalInsecureRandom.current(), 4096, "password");
        assertEquals(ssc.keyStore().getCertificate("cfmlprojects.org"),ssc.certificate());
        assertEquals("RSA", ssc.certificate().getPublicKey().getAlgorithm());
        assertTrue(ssc.rsa());
    }
    @Test
    public void testGenerateCertificateWithEC() throws Exception {
        final SelfSignedCertificate ssc =
                SelfSignedCertificate.generateCertificate(false, "cfmlprojects.org", ThreadLocalInsecureRandom.current(), 4096, "password");
        assertEquals(ssc.keyStore().getCertificate("cfmlprojects.org"),ssc.certificate());
        assertEquals("EC", ssc.certificate().getPublicKey().getAlgorithm());
        assertFalse(ssc.rsa());
    }
}