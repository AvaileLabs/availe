package com.availelabs.orchestration.opensearch

import org.bouncycastle.asn1.pkcs.PrivateKeyInfo
import org.bouncycastle.cert.X509CertificateHolder
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.openssl.PEMKeyPair
import org.bouncycastle.openssl.PEMParser
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter
import java.io.File
import java.io.FileReader
import java.security.KeyStore
import java.security.Security
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManagerFactory

internal object OpenSearchSslContextFactory {

    init {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(BouncyCastleProvider())
        }
    }

    fun createAdminSslContext(
        rootCaFile: File,
        adminCertificateFile: File,
        adminKeyFile: File
    ): SSLContext {
        val trustStore = createTrustStore(rootCaFile)
        val keyStore = createKeyStore(adminCertificateFile, adminKeyFile)

        val trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
        trustManagerFactory.init(trustStore)

        val keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm())
        keyManagerFactory.init(keyStore, "password".toCharArray())

        val sslContext = SSLContext.getInstance("TLS")
        sslContext.init(keyManagerFactory.keyManagers, trustManagerFactory.trustManagers, null)

        return sslContext
    }

    private fun createTrustStore(rootCaFile: File): KeyStore {
        val certificate = readCertificate(rootCaFile)
        val keyStore = KeyStore.getInstance(KeyStore.getDefaultType())
        keyStore.load(null, null)
        keyStore.setCertificateEntry("root-ca", certificate)
        return keyStore
    }

    private fun createKeyStore(certificateFile: File, keyFile: File): KeyStore {
        val certificate = readCertificate(certificateFile)
        val privateKey = readPrivateKey(keyFile)

        val keyStore = KeyStore.getInstance(KeyStore.getDefaultType())
        keyStore.load(null, null)
        keyStore.setKeyEntry(
            "admin",
            privateKey,
            "password".toCharArray(),
            arrayOf(certificate)
        )
        return keyStore
    }

    private fun readCertificate(file: File): java.security.cert.Certificate {
        PEMParser(FileReader(file)).use { parser ->
            val certificateHolder = parser.readObject() as X509CertificateHolder
            return JcaX509CertificateConverter()
                .setProvider(BouncyCastleProvider.PROVIDER_NAME)
                .getCertificate(certificateHolder)
        }
    }

    private fun readPrivateKey(file: File): java.security.PrivateKey {
        PEMParser(FileReader(file)).use { parser ->
            val objectInFile = parser.readObject()
            val converter = JcaPEMKeyConverter().setProvider(BouncyCastleProvider.PROVIDER_NAME)

            return when (objectInFile) {
                is PEMKeyPair -> converter.getKeyPair(objectInFile).private
                is PrivateKeyInfo -> converter.getPrivateKey(objectInFile)
                else -> throw IllegalArgumentException("Unknown private key format in ${file.absolutePath}")
            }
        }
    }
}