package runwar.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

import runwar.security.SelfSignedCertificate.ThreadLocalInsecureRandom;

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
                SelfSignedCertificate.generateCertificate("cfmlprojects.org", ThreadLocalInsecureRandom.current(), 4096, "password");
        assertEquals(ssc.keyStore().getCertificate("cfmlprojects.org"),ssc.certificate());
    }
}