package runwar.security;

import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.SubjectKeyIdentifier;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.cert.bc.BcX509ExtensionUtils;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.crypto.params.AsymmetricKeyParameter;
import org.bouncycastle.crypto.util.PrivateKeyFactory;
import org.bouncycastle.jce.X509KeyUsage;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.DefaultDigestAlgorithmIdentifierFinder;
import org.bouncycastle.operator.DefaultSignatureAlgorithmIdentifierFinder;
import org.bouncycastle.operator.bc.BcRSAContentSignerBuilder;

import runwar.logging.RunwarLogger;

import org.bouncycastle.cert.X509v3CertificateBuilder;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

import static java.util.Objects.requireNonNull;

/**
 * Generates a self-signed certificate for testing purposes.
 */
public final class SelfSignedCertificate {

    static final Date NOT_BEFORE = new Date(System.currentTimeMillis() - 86400000L * 365);
    static final Date NOT_AFTER = new Date(253402300799000L);
    static final String DEFAULT_HOST = "localhost";
    static final String DEFAULT_PASSWORD = "password";

    private final Certificate certificate;
    private final PrivateKey privateKey;
    private final KeyStore keyStore;

    private SelfSignedCertificate(PrivateKey privateKey, Certificate certificate, KeyStore keyStore)
            throws CertificateException {
        this.privateKey = requireNonNull(privateKey);
        this.certificate = requireNonNull(certificate);
        this.keyStore = requireNonNull(keyStore);
    }

    public Certificate certificate() {
        return certificate;
    }

    public PrivateKey privateKey() {
        return privateKey;
    }

    public KeyStore keyStore() {
        return keyStore;
    }

    public static SelfSignedCertificate generateCertificate() throws GeneralSecurityException, IOException {
        return generateCertificate(DEFAULT_HOST, DEFAULT_PASSWORD);
    }

    public static SelfSignedCertificate generateCertificate(String fqdn, String password)
            throws GeneralSecurityException, IOException {
        return generateCertificate(fqdn, ThreadLocalInsecureRandom.current(), 2048, password);
    }

    public static SelfSignedCertificate generateCertificate(String fqdn, SecureRandom random, int bits, String password)
            throws GeneralSecurityException, IOException {
        if (fqdn == null || fqdn.trim().length() == 0) {
            throw new IllegalArgumentException("FQDN must not be empty");
        }
        if (password == null || password.trim().length() == 0) {
            throw new IllegalArgumentException("Key store password must not be empty");
        }

        final KeyPair keypair = generateKeyPair(random, bits);
        final PrivateKey privateKey = keypair.getPrivate();
        final X509Certificate certificate;

        try {
            certificate = generateCertificate(fqdn, keypair, random);
        } catch (Throwable t) {
            RunwarLogger.SECURITY_LOGGER.debug("Failed to generate a self-signed X.509 certificate using X509v3CertificateBuilder:", t);

            throw new CertificateException(
                    "No provider succeeded to generate a self-signed certificate. See debug log for the root cause.");
        }

        final KeyStore keyStore = generateKeyStore(fqdn, privateKey, certificate, password);

        return new SelfSignedCertificate(privateKey, certificate, keyStore);
    }

    private static KeyPair generateKeyPair(SecureRandom random, int bits) {
        final KeyPair keypair;
        try {
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
            keyGen.initialize(bits, random);
            keypair = keyGen.generateKeyPair();
        } catch (NoSuchAlgorithmException e) {
            // Should not reach here because every Java implementation must have
            // RSA key pair generator.
            throw new Error(e);
        }
        return keypair;
    }

    private static SubjectKeyIdentifier createSubjectKeyIdentifier(Key publicKey) throws IOException {
        try (ASN1InputStream is = new ASN1InputStream(new ByteArrayInputStream(publicKey.getEncoded()))) {
            ASN1Sequence seq = (ASN1Sequence) is.readObject();
            SubjectPublicKeyInfo info = SubjectPublicKeyInfo.getInstance(seq);
            return new BcX509ExtensionUtils().createSubjectKeyIdentifier(info);
        }
    }

    private static X509Certificate generateCertificate(String fqdn, KeyPair keypair, SecureRandom random)
            throws Exception {
        final X500Name subject = new X500Name("CN=" + fqdn);
        final SubjectPublicKeyInfo subPubKeyInfo = SubjectPublicKeyInfo.getInstance(keypair.getPublic().getEncoded());
        final AlgorithmIdentifier sigAlgId = new DefaultSignatureAlgorithmIdentifierFinder().find("SHA1withRSA");
        final AlgorithmIdentifier digAlgId = new DefaultDigestAlgorithmIdentifierFinder().find(sigAlgId);
        final AsymmetricKeyParameter keyParam = PrivateKeyFactory.createKey(keypair.getPrivate().getEncoded());
        final ContentSigner sigGen = new BcRSAContentSignerBuilder(sigAlgId, digAlgId).build(keyParam);

        X509v3CertificateBuilder v3CertBuilder = new X509v3CertificateBuilder(subject, new BigInteger(64, random),
                NOT_BEFORE, NOT_AFTER, subject, subPubKeyInfo);

        v3CertBuilder.addExtension(Extension.basicConstraints, true, new BasicConstraints(true));
        v3CertBuilder.addExtension(Extension.keyUsage, true, new X509KeyUsage(X509KeyUsage.digitalSignature
                | X509KeyUsage.nonRepudiation | X509KeyUsage.keyEncipherment | X509KeyUsage.dataEncipherment));
        v3CertBuilder.addExtension(Extension.subjectKeyIdentifier, false,
                createSubjectKeyIdentifier(keypair.getPublic()));

        JcaX509CertificateConverter converter = new JcaX509CertificateConverter();
        X509Certificate cert = converter.getCertificate(v3CertBuilder.build(sigGen));

        cert.checkValidity();
        cert.verify(keypair.getPublic());

        return cert;
    }

    private static KeyStore generateKeyStore(String fqdn, PrivateKey privateKey, Certificate certificate,
            String password) throws GeneralSecurityException, IOException {
        final KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());

        // Initialize key store
        keyStore.load(null, password.toCharArray());

        keyStore.setKeyEntry(fqdn, privateKey, password.toCharArray(), new Certificate[] { certificate });

        return keyStore;
    }

    final static class ThreadLocalInsecureRandom extends SecureRandom {

        private static final long serialVersionUID = -8209473337192526191L;

        private static final SecureRandom INSTANCE = new ThreadLocalInsecureRandom();

        static SecureRandom current() {
            return INSTANCE;
        }

        private ThreadLocalInsecureRandom() {
        }

        @Override
        public String getAlgorithm() {
            return "insecure";
        }

        @Override
        public void setSeed(byte[] seed) {
        }

        @Override
        public void setSeed(long seed) {
        }

        @Override
        public void nextBytes(byte[] bytes) {
            random().nextBytes(bytes);
        }

        @Override
        public byte[] generateSeed(int numBytes) {
            byte[] seed = new byte[numBytes];
            random().nextBytes(seed);
            return seed;
        }

        @Override
        public int nextInt() {
            return random().nextInt();
        }

        @Override
        public int nextInt(int n) {
            return random().nextInt(n);
        }

        @Override
        public boolean nextBoolean() {
            return random().nextBoolean();
        }

        @Override
        public long nextLong() {
            return random().nextLong();
        }

        @Override
        public float nextFloat() {
            return random().nextFloat();
        }

        @Override
        public double nextDouble() {
            return random().nextDouble();
        }

        @Override
        public double nextGaussian() {
            return random().nextGaussian();
        }

        private static Random random() {
            return ThreadLocalRandom.current();
        }
    }
}