package runwar;

import org.xnio.IoUtils;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyFactory;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.Security;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Arrays;
import java.util.Collection;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;

import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;

import runwar.logging.Logger;

public class SSLUtil {
	
	private static Logger log = Logger.getLogger("RunwarLogger");
	private static final String SERVER_KEY_STORE = "runwar/runwar.keystore";
    private static final String SERVER_TRUST_STORE = "runwar/runwar.truststore";
    private static final char[] STORE_PASSWORD = "password".toCharArray();
    
	public static SSLContext createSSLContext() throws IOException {
		log.debug("Creating SSL context from: " + SERVER_KEY_STORE + " trust store: " + SERVER_TRUST_STORE);
		return createSSLContext(loadKeyStore(SERVER_KEY_STORE), loadKeyStore(SERVER_TRUST_STORE));
	}

	public static SSLContext createSSLContext(File certfile, File keyfile, char[] passphrase) throws IOException {
		log.debug("Creating SSL context from cert: " + certfile + " key: " + keyfile);
		
		SSLContext context = null;
		try {
			context = createSSLContext(keystoreFromDERCertificate(certfile, keyfile, passphrase), loadKeyStore(SERVER_TRUST_STORE));
		} catch (Exception e) {
			throw new IOException("Could not load certificate",e);
		}
		return context;
	}
	
    private static SSLContext createSSLContext(final KeyStore keyStore, final KeyStore trustStore) throws IOException {
        KeyManager[] keyManagers;
        try {
            KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            keyManagerFactory.init(keyStore, STORE_PASSWORD);
            keyManagers = keyManagerFactory.getKeyManagers();
        } catch (NoSuchAlgorithmException e) {
            throw new IOException("Unable to initialise KeyManager[]", e);
        } catch (UnrecoverableKeyException e) {
            throw new IOException("Unable to initialise KeyManager[]", e);
        } catch (KeyStoreException e) {
            throw new IOException("Unable to initialise KeyManager[]", e);
        }

        TrustManager[] trustManagers = null;
        try {
            TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            trustManagerFactory.init(trustStore);
            trustManagers = trustManagerFactory.getTrustManagers();
        } catch (NoSuchAlgorithmException e) {
            throw new IOException("Unable to initialise TrustManager[]", e);
        } catch (KeyStoreException e) {
            throw new IOException("Unable to initialise TrustManager[]", e);
        }

        SSLContext sslContext;
        try {
            sslContext = SSLContext.getInstance("TLS");
            sslContext.init(keyManagers, trustManagers, null);
        } catch (NoSuchAlgorithmException e) {
            throw new IOException("Unable to create and initialise the SSLContext", e);
        } catch (KeyManagementException e) {
            throw new IOException("Unable to create and initialise the SSLContext", e);
        }

        return sslContext;
    }

	private static KeyStore loadKeyStore(final String name) throws IOException {
        final InputStream stream = SSLUtil.class.getClassLoader().getResourceAsStream(name);
        if(stream == null)
            throw new IOException(String.format("Unable to load KeyStore from classpath %s", name));
        try {
            KeyStore loadedKeystore = KeyStore.getInstance("JKS");
            loadedKeystore.load(stream, STORE_PASSWORD);
            log.debug("loaded store: " + name);
            return loadedKeystore;
        } catch (Exception e) {
            throw new IOException(String.format("Unable to load KeyStore %s", name), e);
        } finally {
            IoUtils.safeClose(stream);
        }
    }

	public static KeyStore keystoreFromDERCertificate ( File certfile, File keyfile, char[] passphrase) throws Exception {
        
        String defaultalias = "serverkey";
        PrivateKey ff;
        KeyStore ks = KeyStore.getInstance("JKS", "SUN");
        ks.load( null , STORE_PASSWORD);
        try {
        	// try the pks8 java format first
        	ff = loadPKCS8PrivateKey(keyfile);
        } catch (Exception e) {
        	// use the rsa format from openssl
        	ff = loadRSAPrivateKey(keyfile);
        }

        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        InputStream certstream = fullStream (certfile);
        Collection<?> c = cf.generateCertificates(certstream) ;
        Certificate[] certs = new Certificate[c.toArray().length];

        if (c.size() == 1) {
            certstream = fullStream (certfile);
            log.debug("One certificate, no chain.");
            Certificate cert = cf.generateCertificate(certstream) ;
            certs[0] = cert;
        } else {
        	log.debug("Certificate chain length: "+c.size());
            certs = (Certificate[])c.toArray();
        }
        ks.setKeyEntry(defaultalias, ff, 
                       passphrase,
                       certs );
        Arrays.fill(passphrase, '*');
        return ks;
    }
	
	private static PrivateKey loadPKCS8PrivateKey(File f) throws Exception {
		    FileInputStream fis = new FileInputStream(f);
		    DataInputStream dis = new DataInputStream(fis);
		    byte[] keyBytes = new byte[(int) f.length()];
		    dis.readFully(keyBytes);
		    dis.close();
		    PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
		    KeyFactory kf = KeyFactory.getInstance("RSA");
		    return kf.generatePrivate(spec);
	}
	
	private static PrivateKey loadRSAPrivateKey(File f) throws Exception {
		BufferedReader br = new BufferedReader(new FileReader(f));
		Security.addProvider(new BouncyCastleProvider());
		PEMParser pp = new PEMParser(br);
		PrivateKeyInfo pemKeyPair = (PrivateKeyInfo) pp.readObject();
		PrivateKey kp = new JcaPEMKeyConverter().getPrivateKey(pemKeyPair);
		pp.close();
		return kp;
	}
	
	private static InputStream fullStream ( File fname ) throws IOException {
        FileInputStream fis = new FileInputStream(fname);
        DataInputStream dis = new DataInputStream(fis);
        byte[] bytes = new byte[dis.available()];
        dis.readFully(bytes);
        ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
        IoUtils.safeClose(fis);
        IoUtils.safeClose(dis);
        return bais;
    }	
}
