package com.liheit.im.utils;

import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Collection;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import okhttp3.OkHttpClient;
import okio.Buffer;

/**
 * Created by daixun on 2018/10/25.
 * 添加对https 的支持
 */

public class SSLSupport {

    public OkHttpClient.Builder addSupport(OkHttpClient.Builder builder){
        Log.e("file:sslsuport");
        return builder.sslSocketFactory(getSSLSocketFactory())
                .hostnameVerifier(getHostnameVerifier());
    }

    public static SSLSocketFactory getSSLSocketFactory() {
        try {
            Log.e("file:sslsuport");
            SSLContext sslContext = SSLContext.getInstance("SSL");
            sslContext.init(null, getTrustManager(), new SecureRandom());
            return sslContext.getSocketFactory();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    //获取TrustManager
    private static TrustManager[] getTrustManager() {
        TrustManager[] trustAllCerts = new TrustManager[]{
                new X509TrustManager() {
                    @Override
                    public void checkClientTrusted(X509Certificate[] chain, String authType) {
                    }

                    @Override
                    public void checkServerTrusted(X509Certificate[] chain, String authType) {
                    }

                    @Override
                    public X509Certificate[] getAcceptedIssuers() {
                        return new X509Certificate[]{};
                    }
                }
        };
        return trustAllCerts;
    }

    //获取HostnameVerifier
    public static HostnameVerifier getHostnameVerifier() {
        HostnameVerifier hostnameVerifier = new HostnameVerifier() {
            @Override
            public boolean verify(String s, SSLSession sslSession) {
                return true;
            }
        };
        return hostnameVerifier;
    }

    /**
     * Returns an input stream containing one or more certificate PEM files. This implementation just
     * embeds the PEM files in Java strings; most applications will instead read this from a resource
     * file that gets bundled with the application.
     */
    private InputStream trustedCertificatesInputStream() {
        // PEM files for root certificates of Comodo and Entrust. These two CAs are sufficient to view
        // https://publicobject.com (Comodo) and https://squareup.com (Entrust). But they aren't
        // sufficient to connect to most HTTPS sites including https://godaddy.com and https://visa.com.
        // Typically developers will need to get a PEM file from their organization's TLS administrator.
        String comodoRsaCertificationAuthority = "-----BEGIN CERTIFICATE-----\n" +
                "MIIFlzCCBH+gAwIBAgIQCPpZ0Bl3UknXibwQiGntJDANBgkqhkiG9w0BAQsFADBy\n" +
                "MQswCQYDVQQGEwJDTjElMCMGA1UEChMcVHJ1c3RBc2lhIFRlY2hub2xvZ2llcywg\n" +
                "SW5jLjEdMBsGA1UECxMURG9tYWluIFZhbGlkYXRlZCBTU0wxHTAbBgNVBAMTFFRy\n" +
                "dXN0QXNpYSBUTFMgUlNBIENBMB4XDTE4MDgyNDAwMDAwMFoXDTE5MDgyNDEyMDAw\n" +
                "MFowGTEXMBUGA1UEAxMOd3d3LmxpaGVpdC5jb20wggEiMA0GCSqGSIb3DQEBAQUA\n" +
                "A4IBDwAwggEKAoIBAQCbyHuyVFQgsiBp/5MPjcAhMFr9CRQi6vVBzPo1CvEdtXws\n" +
                "NkzT+9UsN0pbr/sREFdzWLkSUaQ32QLMn28krI0gkq7cXH8nuHqsmxdcMHnS476z\n" +
                "99fG/pYVPcRbtguZNBD9ldxeDAAIJXxQVurbGSzpp7Ag4hn/FFQLMutATldAtIXJ\n" +
                "Kb7J4CDE66CnhLprKAujmBYAsV2S8L1mbuKyY+gYkhA0mLJvlFQ/zf0RRjXkZK8I\n" +
                "Jyc6BCCxwDGKP5iX5u6PutVsWQD4xCyhsbujX91+dIiF+GIc8uljaWzx+d2BkeF0\n" +
                "RWnhqZQPy0HqtKu9JeGc0vRp3ukOPkfH6p9OlqdHAgMBAAGjggKAMIICfDAfBgNV\n" +
                "HSMEGDAWgBR/05nzoEcOMQBWViKOt8ye3coBijAdBgNVHQ4EFgQUC2q9QXCH5Rix\n" +
                "q52Wl9W/3l+e6f0wJQYDVR0RBB4wHIIOd3d3LmxpaGVpdC5jb22CCmxpaGVpdC5j\n" +
                "b20wDgYDVR0PAQH/BAQDAgWgMB0GA1UdJQQWMBQGCCsGAQUFBwMBBggrBgEFBQcD\n" +
                "AjBMBgNVHSAERTBDMDcGCWCGSAGG/WwBAjAqMCgGCCsGAQUFBwIBFhxodHRwczov\n" +
                "L3d3dy5kaWdpY2VydC5jb20vQ1BTMAgGBmeBDAECATCBgQYIKwYBBQUHAQEEdTBz\n" +
                "MCUGCCsGAQUFBzABhhlodHRwOi8vb2NzcDIuZGlnaWNlcnQuY29tMEoGCCsGAQUF\n" +
                "BzAChj5odHRwOi8vY2FjZXJ0cy5kaWdpdGFsY2VydHZhbGlkYXRpb24uY29tL1Ry\n" +
                "dXN0QXNpYVRMU1JTQUNBLmNydDAJBgNVHRMEAjAAMIIBBQYKKwYBBAHWeQIEAgSB\n" +
                "9gSB8wDxAHYApLkJkLQYWBSHuxOizGdwCjw1mAT5G9+443fNDsgN3BAAAAFlaypt\n" +
                "AAAABAMARzBFAiApNn1TItjzNfP9aoWagk5Yyauy/r2xKiDcK7ZzbZpeHQIhAM6l\n" +
                "ATkyj+DAAa47xQa3HGvcuCj1aVPCUAqVxj8dIpcpAHcAh3W/51l8+IxDmV+9827/\n" +
                "Vo1HVjb/SrVgwbTq/16ggw8AAAFlaypt0AAABAMASDBGAiEA7vOUUXVVr/u1l5xX\n" +
                "32oGJ+wdcj0n8b6BfdMP13ak9MgCIQDE/xhRuQl2um/gzLKyY5r3DBcnu0HGmgH0\n" +
                "r9qvN5lXDDANBgkqhkiG9w0BAQsFAAOCAQEABkJJpNd4EuEJkUcAamBCRFUSu/rx\n" +
                "8P8fqZOxuBAmObZD1QgihNlPLCGHY+4Yixia+bw2ZlYuuxo2SPWhzmKgLmryJzbq\n" +
                "TxN0eDAusm+OH+HfYqv06fHhAzMrRmq6DDq5rVxbv9qnUc6b1LApkTDIPC2TqVN4\n" +
                "ePLFk+JJvNpbHSyH/DGt0RilHNABNJBmUCcgpdfEDX3YYR/B5xAT2rKKtJuvSbCG\n" +
                "rdu5JhuGZc1nB+uyDBz7iKwPNO0Bs/3LXj1pexqb3gO7wR43Za90Jv1KpiAKqxNg\n" +
                "Hl6bbOb0ZBV82T/aMZO45ZP05uz1nnqJ0UZIXXgeKfiArTMNPMbSNa54Ag==\n" +
                "-----END CERTIFICATE-----\n";
        return new Buffer()
                .writeUtf8(comodoRsaCertificationAuthority)
                .inputStream();
    }

    /**
     * Returns a trust manager that trusts {@code certificates} and none other. HTTPS services whose
     * certificates have not been signed by these certificates will fail with a {@code
     * SSLHandshakeException}.
     *
     * <p>This can be used to replace the host platform's built-in trusted certificates with a custom
     * set. This is useful in development where certificate authority-trusted certificates aren't
     * available. Or in production, to avoid reliance on third-party certificate authorities.
     *
     * <p>See also {@link CertificatePinner}, which can limit trusted certificates while still using
     * the host platform's built-in trust store.
     *
     * <h3>Warning: Customizing Trusted Certificates is Dangerous!</h3>
     *
     * <p>Relying on your own trusted certificates limits your server team's ability to update their
     * TLS certificates. By installing a specific set of trusted certificates, you take on additional
     * operational complexity and limit your ability to migrate between certificate authorities. Do
     * not use custom trusted certificates in production without the blessing of your server's TLS
     * administrator.
     */
    private X509TrustManager trustManagerForCertificates(InputStream in)
            throws GeneralSecurityException {
        CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
        Collection<? extends Certificate> certificates = certificateFactory.generateCertificates(in);
        if (certificates.isEmpty()) {
            throw new IllegalArgumentException("expected non-empty set of trusted certificates");
        }

        // Put the certificates a key store.
        char[] password = "password".toCharArray(); // Any password will work.
        KeyStore keyStore = newEmptyKeyStore(password);
        int index = 0;
        for (Certificate certificate : certificates) {
            String certificateAlias = Integer.toString(index++);
            keyStore.setCertificateEntry(certificateAlias, certificate);
        }

        // Use it to build an X509 trust manager.
        KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(
                KeyManagerFactory.getDefaultAlgorithm());
        keyManagerFactory.init(keyStore, password);
        TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(
                TrustManagerFactory.getDefaultAlgorithm());
        trustManagerFactory.init(keyStore);
        TrustManager[] trustManagers = trustManagerFactory.getTrustManagers();
        if (trustManagers.length != 1 || !(trustManagers[0] instanceof X509TrustManager)) {
            throw new IllegalStateException("Unexpected default trust managers:"
                    + Arrays.toString(trustManagers));
        }
        return (X509TrustManager) trustManagers[0];
    }

    private KeyStore newEmptyKeyStore(char[] password) throws GeneralSecurityException {
        try {
            KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            InputStream in = null; // By convention, 'null' creates an empty key store.
            keyStore.load(in, password);
            return keyStore;
        } catch (IOException e) {
            throw new AssertionError(e);
        }
    }
}
