package runwar.util;

import java.io.ByteArrayInputStream;
import java.util.Enumeration;

import org.bouncycastle.openssl.PEMDecryptorProvider;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMEncryptedKeyPair;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.bouncycastle.openssl.jcajce.JcePEMDecryptorProviderBuilder;
import org.bouncycastle.openssl.PEMParser;

import java.io.BufferedReader;
import java.io.FileReader;
import java.security.Security;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.io.DataInputStream;
import java.io.FileInputStream;
import java.security.spec.KeySpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.KeyFactory;
import java.util.Collection;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.spec.InvalidKeySpecException;

import org.xnio.IoUtils;

import javax.crypto.Cipher;
import javax.crypto.EncryptedPrivateKeyInfo;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.net.ssl.TrustManager;
import javax.net.ssl.KeyManager;

import java.util.Arrays;
import java.security.KeyManagementException;

import javax.net.ssl.TrustManagerFactory;

import java.security.AlgorithmParameters;
import java.security.Key;
import java.security.KeyStoreException;
import java.security.UnrecoverableKeyException;
import java.security.NoSuchAlgorithmException;

import javax.net.ssl.KeyManagerFactory;

import java.io.InputStream;
import java.security.KeyStore;
import java.io.File;
import java.io.IOException;

import javax.net.ssl.SSLContext;

import runwar.logging.Logger;
import sun.security.x509.X509CertImpl;

public class SSLUtil
{
    private static Logger log;
    private static final String SERVER_KEY_STORE = "runwar/runwar.keystore";
    private static final String SERVER_TRUST_STORE = "runwar/runwar.truststore";
    private static final char[] DEFAULT_STORE_PASSWORD;
    
    static {
        SSLUtil.log = Logger.getLogger("RunwarLogger");
        DEFAULT_STORE_PASSWORD = "password".toCharArray();
    }
    
    public static SSLContext createSSLContext() throws IOException {
        SSLUtil.log.debug("Creating SSL context from: runwar/runwar.keystore trust store: runwar/runwar.truststore");
        return createSSLContext(loadKeyStore(SERVER_KEY_STORE), loadKeyStore(SERVER_TRUST_STORE), DEFAULT_STORE_PASSWORD, null);
    }
    
    public static SSLContext createSSLContext(final File certfile, final File keyFile, char[] passphrase, final String[] addCertificatePaths) throws IOException {
        SSLUtil.log.debug("Creating SSL context from cert: [" + certfile + "]  key: [" + keyFile + "]");
        if (passphrase == null || passphrase.length == 0) {
            SSLUtil.log.debug("Using default store passphrase of '" + String.copyValueOf(DEFAULT_STORE_PASSWORD) + "'");
            passphrase = DEFAULT_STORE_PASSWORD;
        }
        SSLContext sslContext;
        try {
            final KeyStore keystoreFromDERCertificate = keystoreFromDERCertificate(certfile, keyFile, passphrase);
            if (addCertificatePaths != null && addCertificatePaths.length > 0) {
                for (int length = addCertificatePaths.length, i = 0; i < length; ++i) {
                    addCertificate(keystoreFromDERCertificate, new File(addCertificatePaths[i]), "addedKey"+i);
                }
            }
            final KeyStore keyStore = KeyStore.getInstance("JKS", "SUN");
            keyStore.load(null, passphrase);
            keyStore.setEntry("someAlias", new KeyStore.TrustedCertificateEntry(keystoreFromDERCertificate.getCertificate("serverkey")), null);
            sslContext = createSSLContext(keystoreFromDERCertificate, keyStore, passphrase, addCertificatePaths);
        }
        catch (Exception ex) {
            throw new IOException("Could not load certificate", ex);
        }
        return sslContext;
    }
    
    
    private static SSLContext createSSLContext(final KeyStore keyStore, final KeyStore trustStore, final char[] passphrase, final String[] addCertificatePaths) throws IOException {
        KeyManager[] keyManagers;
        try {
            final KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            keyManagerFactory.init(keyStore, passphrase);
            keyManagers = keyManagerFactory.getKeyManagers();
        }
        catch (NoSuchAlgorithmException ex) {
            throw new IOException("Unable to initialise KeyManager[]", ex);
        }
        catch (UnrecoverableKeyException ex2) {
            throw new IOException("Unable to initialise KeyManager[]", ex2);
        }
        catch (KeyStoreException ex3) {
            throw new IOException("Unable to initialise KeyManager[]", ex3);
        }
        if (addCertificatePaths != null && addCertificatePaths.length > 0) {
            for (int length = addCertificatePaths.length, i = 0; i < length; ++i) {
                addCertificate(keyStore, new File(addCertificatePaths[i]),"addedKey" + i);
            }
        }
        TrustManager[] trustManagers;
        try {
            final TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            trustManagerFactory.init(trustStore);
            trustManagers = trustManagerFactory.getTrustManagers();
        }
        catch (NoSuchAlgorithmException ex4) {
            throw new IOException("Unable to initialise TrustManager[]", ex4);
        }
        catch (KeyStoreException ex5) {
            throw new IOException("Unable to initialise TrustManager[]", ex5);
        }
        SSLContext sslContext;
        try {
            sslContext = SSLContext.getInstance("TLS");
            sslContext.init(keyManagers, trustManagers, null);
        }
        catch (NoSuchAlgorithmException ex6) {
            throw new IOException("Unable to create and initialise the SSLContext", ex6);
        }
        catch (KeyManagementException ex7) {
            throw new IOException("Unable to create and initialise the SSLContext", ex7);
        }
        Arrays.fill(passphrase, '*');
        return sslContext;
    }
    
    private static KeyStore loadKeyStore(final String resourcePath) throws IOException {
        final InputStream resourceAsStream = SSLUtil.class.getClassLoader().getResourceAsStream(resourcePath);
        if (resourceAsStream == null) {
            throw new IOException(String.format("Unable to load KeyStore from classpath %s", resourcePath));
        }
        try {
            final KeyStore keyStore = KeyStore.getInstance("JKS");
            keyStore.load(resourceAsStream, DEFAULT_STORE_PASSWORD);
            SSLUtil.log.debug("loaded store: " + resourcePath);
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
        final Collection<? extends Certificate> generateCertificates = certificateFactory.generateCertificates(fullStream(certFile));
        Certificate[] certs = new Certificate[generateCertificates.toArray().length];
        if (generateCertificates.size() == 1) {
            final InputStream fullStream = fullStream(certFile);
            SSLUtil.log.debug("One certificate, no chain:");
            certs[0] = certificateFactory.generateCertificate(fullStream);
        }
        else {
            SSLUtil.log.debug(String.valueOf(generateCertificates.size()) + " certificates in chain:");
            int i = 0;
            for(Object certObject : generateCertificates) {
                if(certObject instanceof Certificate) {
                    certs[i++] = (Certificate)certObject;
                } else if(certObject instanceof X509CertImpl) {
                    certs[i++] = (Certificate)certObject;
                } else {
                    throw new RuntimeException("Unknown certificate type: " + certObject.getClass().getName());
                }
            }
//            certs = (Certificate[])generateCertificates.toArray();
        }
        for (int length = certs.length, i = 0; i < length; ++i) {
            final Certificate certificate = certs[i];
            SSLUtil.log.debug("   " + certificate.getType() + "  certificate, public key [ " + certificate.getPublicKey().getAlgorithm() + " " + certificate.getPublicKey().getFormat() + " ]");
        }
        final char[] copy = Arrays.copyOf(passphrase, passphrase.length);
        Arrays.fill(copy, '*');
        SSLUtil.log.debug(String.format("Adding key to store - alias:[%s]  type:[%s %s]  passphrase:[%s]  certs in chain:[%s]", defaultalias, privateKey.getAlgorithm(), privateKey.getFormat(), String.valueOf(copy), certs.length));
        keyStore.setKeyEntry(defaultalias, privateKey, passphrase, certs);
        return keyStore;
    }
    
    private static PrivateKey loadPKCS8PrivateKey(final byte[] keydata) throws Exception {
        return KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(keydata));
    }
    
    private static PrivateKey loadPKCS8PrivateKey(final File file) throws Exception {
        final DataInputStream dataInputStream = new DataInputStream(new FileInputStream(file));
        final byte[] array = new byte[(int)file.length()];
        dataInputStream.readFully(array);
        dataInputStream.close();
        return loadPKCS8PrivateKey(array);
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
        final PEMParser pemParser = new PEMParser(new BufferedReader(new FileReader(file)));
        final PEMDecryptorProvider build = new JcePEMDecryptorProviderBuilder().build(passphrase);
        final JcaPEMKeyConverter jcaPEMKeyConverter = new JcaPEMKeyConverter();
        PrivateKey privateKey;
        try {
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
        }
        finally {
            pemParser.close();
        }
        pemParser.close();
        return privateKey;
    }

    
    private static void addCertificate(final KeyStore keyStore, final File file, String alias) {
        try {
            final Certificate generateCertificate = CertificateFactory.getInstance("X.509").generateCertificate(fullStream(file));
            keyStore.setCertificateEntry(alias, generateCertificate);
            SSLUtil.log.error("Added certificate file:" + file.getAbsolutePath());
            SSLUtil.log.debug("   " + generateCertificate.getType() + "  certificate, public key [ " + generateCertificate.getPublicKey().getAlgorithm() + " " + generateCertificate.getPublicKey().getFormat() + " ]");
        }
        catch (Exception ex) {
            SSLUtil.log.error("Could not load certificate file:" + file.getAbsolutePath());
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
