package runwar.security;

import io.undertow.protocols.ssl.SNIContextMatcher;
import io.undertow.protocols.ssl.SNISSLContext;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.asn1.x500.style.IETFUtils;
import org.bouncycastle.cert.jcajce.JcaX509CertificateHolder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMDecryptorProvider;
import org.bouncycastle.openssl.PEMEncryptedKeyPair;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.bouncycastle.openssl.jcajce.JcePEMDecryptorProviderBuilder;
import org.xnio.IoUtils;
import runwar.logging.RunwarLogger;

import javax.crypto.Cipher;
import javax.crypto.EncryptedPrivateKeyInfo;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.net.ssl.*;
import java.io.*;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Enumeration;

public class SSLUtil
{
    private static final String SERVER_KEY_STORE = "runwar/runwar.keystore";
    private static final String SERVER_TRUST_STORE = "runwar/runwar.truststore";
    private static final String CLIENT_KEY_STORE = "runwar/client.keystore";
    private static final String CLIENT_TRUST_STORE = "runwar/client.truststore";
    public static final char[] DEFAULT_STORE_PASSWORD;
    public static final String[] DEFAULT_HOST_NAMES;

    static {
        DEFAULT_STORE_PASSWORD = "password".toCharArray();
        DEFAULT_HOST_NAMES = new String[]{"localhost"};
    }
    
    public static SSLContext createSSLContext() throws IOException {
        RunwarLogger.SECURITY_LOGGER.debug("Creating SSL context from: runwar/runwar.keystore trust store: runwar/runwar.truststore");
        return createSSLContext(getServerKeyStore(), getTrustStore(), DEFAULT_STORE_PASSWORD.clone(), null, false, DEFAULT_HOST_NAMES);
    }

    public static SSLContext createClientSSLContext() throws IOException {
        RunwarLogger.SECURITY_LOGGER.debug("Creating Client SSL context from: runwar/client.keystore trust store: runwar/client.truststore");
        return createSSLContext(loadKeyStore(CLIENT_KEY_STORE), loadKeyStore(CLIENT_TRUST_STORE), DEFAULT_STORE_PASSWORD.clone(), null, false, null);
    }

    public static SSLContext createSSLContext(final File certfile, final File keyFile, char[] passphrase, final String[] addCertificatePaths, final String[] hostNames) throws IOException {
        RunwarLogger.SECURITY_LOGGER.debug("Creating SSL context from cert: [" + certfile + "]  key: [" + keyFile + "]");
        if (passphrase == null || passphrase.length == 0) {
            RunwarLogger.SECURITY_LOGGER.debug("Using default store passphrase of '" + String.copyValueOf(DEFAULT_STORE_PASSWORD) + "'");
            passphrase = DEFAULT_STORE_PASSWORD.clone();
        }
        SSLContext sslContext;
        try {
            final KeyStore derKeystore = keystoreFromDERCertificate(certfile, keyFile, passphrase);
            addCertificates(addCertificatePaths, derKeystore);
            final KeyStore keyStore = KeyStore.getInstance("JKS", "SUN");
            keyStore.load(null, passphrase);
            keyStore.setEntry("someAlias", new KeyStore.TrustedCertificateEntry(derKeystore.getCertificate("serverkey")), null);
            sslContext = createSSLContext(derKeystore, keyStore, passphrase, addCertificatePaths, false, hostNames);
        }
        catch (Exception ex) {
            throw new IOException("Could not load certificate", ex);
        }
        return sslContext;
    }
    
    
    private static SSLContext createSSLContext(final KeyStore keyStore, final KeyStore trustStore, final char[] passphrase, final String[] addCertificatePaths, boolean openssl, String[] hostNames) throws IOException {
        KeyManager[] keyManagers;
        try {
            final KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            keyManagerFactory.init(keyStore, passphrase);
            keyManagers = keyManagerFactory.getKeyManagers();
        }
        catch (NoSuchAlgorithmException ex) {
            throw new IOException("Unable to initialise KeyManager[], no such algorithm", ex);
        }
        catch (UnrecoverableKeyException ex2) {
            throw new IOException("Unable to initialise KeyManager[], unrecoverable key.", ex2);
        }
        catch (KeyStoreException ex3) {
            throw new IOException("Unable to initialise KeyManager[]", ex3);
        }
        addCertificates(addCertificatePaths, keyStore);
        TrustManager[] trustManagers;
        try {
            final TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            trustManagerFactory.init(trustStore);
            trustManagers = trustManagerFactory.getTrustManagers();
        }
        catch (NoSuchAlgorithmException ex4) {
            throw new IOException("Unable to initialise TrustManager[], no such algorithm", ex4);
        }
        catch (KeyStoreException ex5) {
            throw new IOException("Unable to initialise TrustManager[]", ex5);
        }
        SSLContext sslContext;
        try {
            if (openssl && hostNames == null) {
                sslContext = SSLContext.getInstance("openssl.TLS");
            } else {
                sslContext = SSLContext.getInstance("TLS");
//                sslContext = SSLContext.getInstance("TLSv1.2");
            }
            sslContext.init(keyManagers, trustManagers, null);
        }
        catch (NoSuchAlgorithmException ex6) {
            throw new IOException("Unable to create and initialise the SSLContext, no such algorithm", ex6);
        }
        catch (KeyManagementException ex7) {
            throw new IOException("Unable to create and initialise the SSLContext", ex7);
        }
        finally {
            Arrays.fill(passphrase, '*');
        }
        if (hostNames != null) {
            SNIContextMatcher.Builder sniMatchBuilder = new SNIContextMatcher.Builder().setDefaultContext(sslContext);
            for(String hostName : hostNames){
                sniMatchBuilder.addMatch(hostName,sslContext);
            }
            RunwarLogger.SECURITY_LOGGER.debug("Creating SNI SSL context for hosts: " + Arrays.toString(hostNames));
            return new SNISSLContext(sniMatchBuilder.build());
        } else {
            RunwarLogger.SECURITY_LOGGER.debug("Creating NON-SNI SSL context");
            return sslContext;
        }
    }

    public static KeyStore getTrustStore() throws IOException {
        return loadKeyStore(SERVER_TRUST_STORE);
    }

    public static KeyStore getServerKeyStore() throws IOException {
        return loadKeyStore(SERVER_KEY_STORE);
    }

    private static KeyStore loadKeyStore(final String resourcePath) throws IOException {
        final InputStream resourceAsStream = SSLUtil.class.getClassLoader().getResourceAsStream(resourcePath);
        if (resourceAsStream == null) {
            throw new IOException(String.format("Unable to load KeyStore from classpath %s", resourcePath));
        }
        try {
            final KeyStore keyStore = KeyStore.getInstance("JKS");
            keyStore.load(resourceAsStream, DEFAULT_STORE_PASSWORD);
            RunwarLogger.SECURITY_LOGGER.debug("loaded store: " + resourcePath);
            return keyStore;
        }
        catch (Exception ex) {
            throw new IOException(String.format("Unable to load KeyStore %s", resourcePath), ex);
        }
        finally {
            IoUtils.safeClose(resourceAsStream);
        }
    }
    
    public static KeyStore keystoreFromDERCertificate(final File certFile, final File keyFile, final char[] passphrase) throws Exception {
        final String defaultalias = "serverkey";
        final KeyStore keyStore = KeyStore.getInstance("JKS", "SUN");
        keyStore.load(null, passphrase);
        PrivateKey privateKey;
        try {
            privateKey = loadPKCS8PrivateKey(keyFile);
        }
        catch (InvalidKeySpecException ex) {
            privateKey = loadPemPrivateKey(keyFile, passphrase);
        }
        final CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
        final Collection<? extends Certificate> certificates = certificateFactory.generateCertificates(fullStream(certFile));
        final ArrayList<Certificate> certs = new ArrayList<>();
        if (certificates.size() == 1) {
            try(final InputStream fullStream = fullStream(certFile)){
                RunwarLogger.SECURITY_LOGGER.debug("One certificate, no chain:");
                certs.add(certificateFactory.generateCertificate(fullStream));
            }
        }
        else {
            RunwarLogger.SECURITY_LOGGER.debug(String.valueOf(certificates.size()) + " certificates in chain:");
            for(Object certObject : certificates) {
                if(certObject instanceof Certificate) {
                    certs.add((Certificate)certObject);
                } else {
                    throw new RuntimeException("Unknown certificate type: " + certObject.getClass().getName());
                }
            }
//            certs = (Certificate[])generateCertificates.toArray();
        }
        for (Certificate certificate: certs) {
            X500Name x500name = new JcaX509CertificateHolder((X509Certificate) certificate).getSubject();
//            String CN =  IETFUtils.valueToString(x500name.getRDNs(BCStyle.CN)[0].getFirst().getValue());
            RunwarLogger.SECURITY_LOGGER.debugf("   %s  certificate, public key [ %s ] %s", certificate.getType(), certificate.getPublicKey().getAlgorithm(), x500name.toString());
        }
        final char[] copy = Arrays.copyOf(passphrase, passphrase.length);
        Arrays.fill(copy, '*');
        RunwarLogger.SECURITY_LOGGER.debug(String.format("Adding key to store - alias:[%s]  type:[%s %s]  passphrase:[%s]  certs in chain:[%s]", defaultalias, privateKey.getAlgorithm(), privateKey.getFormat(), String.valueOf(copy), certs.size()));
        int certCount = certs.size();
        keyStore.setKeyEntry(defaultalias, privateKey, passphrase, certs.toArray(new Certificate[certCount]));
        return keyStore;
    }
    
    private static PrivateKey loadPKCS8PrivateKey(final byte[] keydata) throws Exception {
        return KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(keydata));
    }
    
    private static PrivateKey loadPKCS8PrivateKey(final File file) throws Exception {
        try(final DataInputStream dataInputStream = new DataInputStream(new FileInputStream(file))){
            final byte[] array = new byte[(int)file.length()];
            dataInputStream.readFully(array);
            dataInputStream.close();
            return loadPKCS8PrivateKey(array);
        }
    }
    
    
    private static PrivateKey loadPKCS8PrivateKey(byte[] keyBytes, char[] passphrase) throws Exception {
        EncryptedPrivateKeyInfo encryptPKInfo = new EncryptedPrivateKeyInfo(keyBytes);
        Cipher cipher = Cipher.getInstance("RSA", "BC");
        PBEKeySpec pbeKeySpec = new PBEKeySpec(passphrase);
        SecretKeyFactory secFac = SecretKeyFactory.getInstance(encryptPKInfo.getAlgName(),"BC");
        Key pbeKey = secFac.generateSecret(pbeKeySpec);
        AlgorithmParameters algParams = encryptPKInfo.getAlgParameters();
        cipher.init(Cipher.DECRYPT_MODE, pbeKey, algParams);
        KeySpec pkcs8KeySpec = encryptPKInfo.getKeySpec(cipher);
        KeyFactory kf = KeyFactory.getInstance("RSA", "BC");
        return kf.generatePrivate(pkcs8KeySpec);
    }
    
    private static PrivateKey loadPemPrivateKey(final File file, final char[] passphrase) throws Exception {
        Security.addProvider(new BouncyCastleProvider());
        try(final PEMParser pemParser = new PEMParser(new BufferedReader(new FileReader(file)))){
            final PEMDecryptorProvider build = new JcePEMDecryptorProviderBuilder().build(passphrase);
            final JcaPEMKeyConverter jcaPEMKeyConverter = new JcaPEMKeyConverter();
            PrivateKey privateKey;
            final Object object = pemParser.readObject();
            if (object instanceof PEMEncryptedKeyPair) {
                privateKey = jcaPEMKeyConverter.getKeyPair(((PEMEncryptedKeyPair)object).decryptKeyPair(build)).getPrivate();
            }
            else {
                PrivateKeyInfo privateKeyInfo;
                if (object instanceof PEMKeyPair) {
                    privateKeyInfo = ((PEMKeyPair)object).getPrivateKeyInfo();
                }
                else {
                    privateKeyInfo = (PrivateKeyInfo)object;
                }
                if (privateKeyInfo != null) {
                    privateKey = new JcaPEMKeyConverter().getPrivateKey(privateKeyInfo);
                }
                else {
                    final KeyStore keyStore = KeyStore.getInstance("PKCS12", "BC");
                    final FileInputStream fileInputStream = new FileInputStream(file);
                    keyStore.load(fileInputStream, passphrase);
                    fileInputStream.close();
                    final Enumeration<String> aliases = keyStore.aliases();
                    String alias = "";
                    while (aliases.hasMoreElements()) {
                        alias = aliases.nextElement();
                    }
                    privateKey = (PrivateKey)keyStore.getKey(alias, passphrase);
                }
            }
            return privateKey;
        }
    }

    private static void addCertificates(String[] addCertificatePaths, KeyStore keyStore) {
        if (addCertificatePaths != null && addCertificatePaths.length > 0) {
            for (int length = addCertificatePaths.length, i = 0; i < length; ++i) {
                addCertificate(keyStore, new File(addCertificatePaths[i]),"addedKey" + i);
            }
        }
    }

    private static void addCertificate(final KeyStore keyStore, final File file, String alias) {
        try {
            final Certificate certificate = CertificateFactory.getInstance("X.509").generateCertificate(fullStream(file));
            keyStore.setCertificateEntry(alias, certificate);
            X500Name x500name = new JcaX509CertificateHolder((X509Certificate) certificate).getSubject();
            String CN =  IETFUtils.valueToString(x500name.getRDNs(BCStyle.CN)[0].getFirst().getValue());
            RunwarLogger.SECURITY_LOGGER.debug("Added certificate file:" + file.getAbsolutePath());
            RunwarLogger.SECURITY_LOGGER.debugf("  %s  certificate, public key [ %s ] CN=%s", certificate.getType(), certificate.getPublicKey().getAlgorithm(), CN);
        }
        catch (Exception ex) {
            RunwarLogger.SECURITY_LOGGER.error("Could not load certificate file:" + file.getAbsolutePath());
            ex.printStackTrace();
        }
    }
    
    private static InputStream fullStream(final File file) throws IOException {
        final FileInputStream fileInputStream = new FileInputStream(file);
        final DataInputStream dataInputStream = new DataInputStream(fileInputStream);
        final byte[] array = new byte[dataInputStream.available()];
        dataInputStream.readFully(array);
        final ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(array);
        IoUtils.safeClose(fileInputStream);
        IoUtils.safeClose(dataInputStream);
        return byteArrayInputStream;
    }
}
