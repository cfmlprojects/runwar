package runwar.security;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.X500NameBuilder;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.asn1.x509.*;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.jce.X509KeyUsage;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.DefaultAlgorithmNameFinder;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import runwar.logging.RunwarLogger;

import javax.xml.bind.DatatypeConverter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.math.BigInteger;
import java.nio.file.Path;
import java.security.*;
import java.security.cert.*;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

import static java.util.Objects.requireNonNull;
import static runwar.logging.RunwarLogger.LOG;
import static runwar.logging.RunwarLogger.SECURITY_LOGGER;

import java.security.cert.Certificate;


/**
 * Generates a self-signed certificate for testing purposes.
 */
public final class SelfSignedCertificate {

    public static final BouncyCastleProvider BOUNCY_CASTLE_PROVIDER = new BouncyCastleProvider();
    static final String DEFAULT_HOST = "127.0.0.1";
    static final String DEFAULT_PASSWORD = "password";
    private static BouncyCastleProvider bouncyCastleProvider;

    static {
        bouncyCastleProvider = BOUNCY_CASTLE_PROVIDER;
    }

    private final Certificate certificate;
    private final PrivateKey privateKey;
    private final KeyStore keyStore;
    private final Boolean rsa;
    private final ContentSigner signer;

    private SelfSignedCertificate(ContentSigner signer, PrivateKey privateKey, Certificate certificate, KeyStore keyStore, Boolean rsa)
            throws CertificateException {
        this.privateKey = requireNonNull(privateKey);
        this.certificate = requireNonNull(certificate);
        this.keyStore = requireNonNull(keyStore);
        this.rsa = rsa;
        this.signer = signer;
    }

    public static Certificate generateCertificate(SecureRandom random, KeyPair keyPair, ContentSigner signer, String subjectDN) throws OperatorCreationException, CertificateException, IOException, NoSuchAlgorithmException {

// fill in certificate fields
        X500Name subject = new X500NameBuilder(BCStyle.INSTANCE)
                .addRDN(BCStyle.CN, subjectDN)
                .addRDN(BCStyle.O,"O")
                .addRDN(BCStyle.OU,"OU")
                .build();

        byte[] id = new byte[20];
        random.nextBytes(id);
        BigInteger serial = new BigInteger(160, random);
        X509v3CertificateBuilder certificate = new JcaX509v3CertificateBuilder(
                subject,
                serial,
                Date.from(LocalDate.now().atStartOfDay(ZoneOffset.UTC).toInstant()),
                Date.from(LocalDate.of(2035, 1, 1).atStartOfDay(ZoneOffset.UTC).toInstant()),
                subject,
                keyPair.getPublic());
        certificate.addExtension(Extension.subjectKeyIdentifier, false, id);
        certificate.addExtension(Extension.authorityKeyIdentifier, false, id);
        BasicConstraints constraints = new BasicConstraints(true);
        certificate.addExtension(
                Extension.basicConstraints,
                true,
                constraints.getEncoded());
        X509KeyUsage usage = new X509KeyUsage(KeyUsage.keyCertSign | KeyUsage.digitalSignature);
        certificate.addExtension(Extension.keyUsage, false, usage.getEncoded());
        ExtendedKeyUsage usageEx = new ExtendedKeyUsage(new KeyPurposeId[]{
                KeyPurposeId.id_kp_serverAuth,
                KeyPurposeId.id_kp_clientAuth
        });

//        GeneralNames subjectAltName = new GeneralNames(new GeneralName(GeneralName.dNSName, "127.0.0.1"));
//        certificate.addExtension(Extension.subjectAlternativeName, false, subjectAltName);

        certificate.addExtension(
                Extension.extendedKeyUsage,
                false,
                usageEx.getEncoded());

        X509CertificateHolder holder = certificate.build(signer);
        JcaX509CertificateConverter converter = new JcaX509CertificateConverter();
        converter.setProvider(bouncyCastleProvider);
        X509Certificate x509 = converter.getCertificate(holder);
        return x509;
    }

    public static SelfSignedCertificate generateCertificate() throws GeneralSecurityException, IOException {
        return generateCertificate(DEFAULT_HOST, DEFAULT_PASSWORD);
    }

    public static SelfSignedCertificate generateCertificate(Path certFile, Path keyFile) throws GeneralSecurityException, IOException {
        if(certFile.toFile().exists()){
            LOG.warnf("Certificate file %s already exists", certFile.toAbsolutePath().normalize());
        }
        if(keyFile.toFile().exists()){
            LOG.warnf("Private key file %s already exists", keyFile.toAbsolutePath().normalize());
        }
        if(certFile.toFile().exists() || keyFile.toFile().exists()){
            LOG.warn("Files already exist, not generating self-signed certificate!");
            return null;
        }
        SelfSignedCertificate selfSignedCertificate = generateCertificate(DEFAULT_HOST, DEFAULT_PASSWORD);
        String cert = selfSignedCertificate.certificatePem();
        String key = selfSignedCertificate.privateKeyPem();
        try(FileWriter fw = new FileWriter(certFile.toFile())){
            fw.write(cert);
        } catch (Exception e){
            e.printStackTrace();
        }
        try(FileWriter fw = new FileWriter(keyFile.toFile())){
            fw.write(key);
        } catch (Exception e){
            e.printStackTrace();
        }
        String algorithmName = new DefaultAlgorithmNameFinder().getAlgorithmName(selfSignedCertificate.signer().getAlgorithmIdentifier());
        LOG.info("Generated self-signed " + selfSignedCertificate.certificate().getType() + " certificate using signing algorithm: " + algorithmName);
        LOG.info("Certificate: " + certFile.toAbsolutePath());
//            System.out.println(cert);
        LOG.debug(selfSignedCertificate.certificate().toString().replaceAll("(?s)modulus:(.*)","\1"));
        LOG.info("Private Key: " + keyFile.toAbsolutePath());
        return selfSignedCertificate;
    }

    public static SelfSignedCertificate generateCertificate(String fqdn, String password)
            throws GeneralSecurityException, IOException {
        return generateCertificate(true, fqdn, ThreadLocalInsecureRandom.current(), 2048, password);
    }

    public static SelfSignedCertificate generateCertificate(boolean rsa, String fqdn, String password)
            throws GeneralSecurityException, IOException {
        return generateCertificate(rsa, fqdn, ThreadLocalInsecureRandom.current(), 2048, password);
    }

    public static SelfSignedCertificate generateCertificate(boolean rsa, String fqdn, SecureRandom random, int bits, String password)
            throws GeneralSecurityException, IOException {
        if (fqdn == null || fqdn.trim().length() == 0) {
            throw new IllegalArgumentException("FQDN must not be empty");
        }
        if (password == null || password.trim().length() == 0) {
            throw new IllegalArgumentException("Key store password must not be empty");
        }

        final KeyPair keypair = generateKeyPair(rsa, random, bits);
        final PrivateKey privateKey = keypair.getPrivate();
        final X509Certificate certificate;
        final ContentSigner signer;

        try {
            certificate = generateCertificate(fqdn, keypair, random);
        } catch (Throwable t) {
            SECURITY_LOGGER.debug("Failed to generate a self-signed X.509 certificate using X509v3CertificateBuilder:", t);
            throw new CertificateException( "No provider succeeded to generate a self-signed certificate. " + t.getMessage());
        }
        try{
            signer = generateSigner(keypair);
        } catch (OperatorCreationException e){
            throw new CertificateException( "could not generate signer. " + e.getMessage());
        }

        final KeyStore keyStore = generateKeyStore(fqdn, privateKey, certificate, password);

        return new SelfSignedCertificate(signer, privateKey, certificate, keyStore, rsa);
    }

    private static KeyPair generateKeyPair(boolean rsa, SecureRandom random, int bits) {
        final KeyPair keypair;
        try {
            if(rsa){
                KeyPairGenerator keypairGen = KeyPairGenerator.getInstance("RSA", bouncyCastleProvider);
                keypairGen.initialize(bits, random);
                keypair = keypairGen.generateKeyPair();
            } else {
                KeyPairGenerator keypairGen = KeyPairGenerator.getInstance("EC", bouncyCastleProvider);
                keypairGen.initialize(256, random);
                keypair = keypairGen.generateKeyPair();
            }
        } catch (NoSuchAlgorithmException e) {
            throw new Error(e);
        }
        return keypair;
    }

    private static ContentSigner generateSigner(KeyPair keyPair) throws OperatorCreationException {
        String signAlg = "SHA256withRSA";
        if(!keyPair.getPublic().getAlgorithm().toUpperCase().contains("RSA")){
            signAlg = "SHA256withECDSA";
        }
        final ContentSigner signer = new JcaContentSignerBuilder(signAlg).build(keyPair.getPrivate());
        return signer;
    }

    private static X509Certificate generateCertificate(String fqdn, KeyPair keypair, SecureRandom random)
            throws Exception {
        X509Certificate cert = (X509Certificate) generateCertificate(random, keypair, generateSigner(keypair), fqdn);

        cert.checkValidity();
        cert.verify(keypair.getPublic(), bouncyCastleProvider);

        return cert;
    }

    private static X509Certificate generateCertificate(String fqdn, KeyPair keypair, ContentSigner signer, SecureRandom random)
            throws Exception {
        X509Certificate cert = (X509Certificate) generateCertificate(random, keypair, signer, fqdn);

        cert.checkValidity();
        cert.verify(keypair.getPublic(), bouncyCastleProvider);

        return cert;
    }

    private static KeyStore generateKeyStore(String fqdn, PrivateKey privateKey, Certificate certificate,
                                             String password) throws GeneralSecurityException, IOException {
        final KeyStore keyStore = KeyStore.getInstance("PKCS12", bouncyCastleProvider);

        // Initialize key store
        keyStore.load(null, password.toCharArray());

        keyStore.setKeyEntry(fqdn, privateKey, password.toCharArray(), new Certificate[]{certificate});

        return keyStore;
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

    public boolean rsa() {
        return rsa;
    }

    public ContentSigner signer() {
        return signer;
    }

    public String certificatePem() throws CertificateEncodingException {
        byte[] serialized = certificate.getEncoded();

        try (StringWriter sw = new StringWriter()) {
            sw.write("-----BEGIN CERTIFICATE-----\n");
            sw.write(DatatypeConverter.printBase64Binary(serialized).replaceAll("(.{64})", "$1\n"));
            sw.write("\n-----END CERTIFICATE-----\n");
            return sw.toString();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    public String privateKeyPem() {
        byte[] serialized = privateKey.getEncoded();
        try (StringWriter sw = new StringWriter()) {
            sw.write("-----BEGIN PRIVATE KEY-----\n");
            sw.write(DatatypeConverter.printBase64Binary(serialized).replaceAll("(.{64})", "$1\n"));
            sw.write("\n-----END PRIVATE KEY-----\n");
            return sw.toString();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    final static class ThreadLocalInsecureRandom extends SecureRandom {

        private static final long serialVersionUID = -8209473337192526191L;

        private static final SecureRandom INSTANCE = new ThreadLocalInsecureRandom();

        private ThreadLocalInsecureRandom() {
        }

        static SecureRandom current() {
            return INSTANCE;
        }

        private static Random random() {
            return ThreadLocalRandom.current();
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
    }
}