package com.availelabs.orchestration.opensearch

import com.availelabs.availe.configuration.SharedOpenSearchConstants
import com.availelabs.utils.TlsCertificateGenerator
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo
import org.bouncycastle.cert.X509CertificateHolder
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.openssl.PEMKeyPair
import org.bouncycastle.openssl.PEMParser
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter
import org.slf4j.LoggerFactory
import java.io.File
import java.io.FileReader
import java.security.KeyPair
import java.security.Security

internal object OpenSearchCertGen {
    private val logger = LoggerFactory.getLogger(javaClass)

    init {
        // Essential: Register Bouncy Castle provider explicitly.
        // This ensures the "BC" provider is available even if we take the "Load Existing Certs" path,
        // which might bypass the init block of TlsCertificateGenerator.
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(BouncyCastleProvider())
        }
    }

    fun generateCertificates() {
        val projectRootDirectory = System.getProperty("user.dir")
        val generatedCertDirectory = File(projectRootDirectory, "build/generated/opensearch/certs")

        if (!generatedCertDirectory.exists()) {
            generatedCertDirectory.mkdirs()
        }

        // Ensure the directory itself is accessible
        setFileReadableByAll(generatedCertDirectory)

        val rootCaFile = File(generatedCertDirectory, "root-ca.pem")
        val rootKeyFile = File(generatedCertDirectory, "root-ca-key.pem")

        // Idempotency check: If Root CA exists, load it. If not, generate it.
        // This prevents "SignatureException" where nodes reject each other because they were signed by different CA generations.
        val rootBundle = if (rootCaFile.exists() && rootKeyFile.exists()) {
            logger.info("Existing OpenSearch Root CA found. Loading...")
            loadRootCertificate(rootCaFile, rootKeyFile)
        } else {
            logger.info("Generating New OpenSearch Root CA...")
            val newBundle = TlsCertificateGenerator.generateRootCertificate("OpenSearch-Root-CA")
            writeAndPermitFile(rootCaFile, newBundle.certificatePem)
            writeAndPermitFile(rootKeyFile, newBundle.privateKeyPem)
            newBundle
        }

        logger.info("Generating OpenSearch Admin Certificate...")
        val adminBundle = TlsCertificateGenerator.generateSignedCertificate(
            commonName = "admin",
            subjectAlternativeNames = emptyList(),
            issuerBundle = rootBundle
        )
        writeAndPermitFile(File(generatedCertDirectory, "admin.pem"), adminBundle.certificatePem)
        writeAndPermitFile(File(generatedCertDirectory, "admin-key.pem"), adminBundle.privateKeyPem)

        logger.info("Generating OpenSearch Node1 Certificate...")
        val node1Bundle = TlsCertificateGenerator.generateSignedCertificate(
            commonName = SharedOpenSearchConstants.NODE_1_NAME,
            // Include localhost and 127.0.0.1 for health checks
            subjectAlternativeNames = listOf(SharedOpenSearchConstants.NODE_1_NAME, "localhost", "127.0.0.1"),
            issuerBundle = rootBundle
        )
        writeAndPermitFile(File(generatedCertDirectory, "node1.pem"), node1Bundle.certificatePem)
        writeAndPermitFile(File(generatedCertDirectory, "node1-key.pem"), node1Bundle.privateKeyPem)

        logger.info("Generating OpenSearch Node2 Certificate...")
        val node2Bundle = TlsCertificateGenerator.generateSignedCertificate(
            commonName = SharedOpenSearchConstants.NODE_2_NAME,
            subjectAlternativeNames = listOf(SharedOpenSearchConstants.NODE_2_NAME, "localhost", "127.0.0.1"),
            issuerBundle = rootBundle
        )
        writeAndPermitFile(File(generatedCertDirectory, "node2.pem"), node2Bundle.certificatePem)
        writeAndPermitFile(File(generatedCertDirectory, "node2-key.pem"), node2Bundle.privateKeyPem)

        logger.info("Generating OpenSearch Dashboards Certificate...")
        val dashboardsBundle = TlsCertificateGenerator.generateSignedCertificate(
            commonName = SharedOpenSearchConstants.DASHBOARDS_NAME,
            subjectAlternativeNames = listOf(SharedOpenSearchConstants.DASHBOARDS_NAME, "localhost", "127.0.0.1"),
            issuerBundle = rootBundle
        )
        writeAndPermitFile(File(generatedCertDirectory, "dashboards.pem"), dashboardsBundle.certificatePem)
        writeAndPermitFile(File(generatedCertDirectory, "dashboards-key.pem"), dashboardsBundle.privateKeyPem)

        logger.info("OpenSearch certificates generated at ${generatedCertDirectory.absolutePath}")
    }

    private fun loadRootCertificate(certFile: File, keyFile: File): TlsCertificateGenerator.CertificateBundle {
        // Read Certificate
        val certParser = PEMParser(FileReader(certFile))
        val certHolder = certParser.readObject() as X509CertificateHolder
        val x509Certificate = JcaX509CertificateConverter().setProvider("BC").getCertificate(certHolder)
        certParser.close()

        // Read Private Key
        val keyParser = PEMParser(FileReader(keyFile))
        val keyObject = keyParser.readObject()
        keyParser.close()

        // Robust key loading: Handle both PKCS#1 (PEMKeyPair) and PKCS#8 (PrivateKeyInfo)
        // This ensures that if we have an old Root CA generated with PKCS#1, we can still load it
        // to sign new certs, while new generation will use PKCS#8.
        val privateKey = when (keyObject) {
            is PEMKeyPair -> JcaPEMKeyConverter().setProvider("BC").getKeyPair(keyObject).private
            is PrivateKeyInfo -> JcaPEMKeyConverter().setProvider("BC").getPrivateKey(keyObject)
            else -> throw IllegalStateException("Unknown private key format in ${keyFile.name}: ${keyObject?.javaClass?.name}")
        }

        // Reconstruct keypair (public key is extracted from certificate)
        val keyPair = KeyPair(x509Certificate.publicKey, privateKey)

        // Reconstruct the contents as text for the Bundle
        val certPem = certFile.readText()
        val keyPem = keyFile.readText()

        return TlsCertificateGenerator.CertificateBundle(
            certificatePem = certPem,
            privateKeyPem = keyPem,
            x509Certificate = x509Certificate,
            keyPair = keyPair
        )
    }

    private fun writeAndPermitFile(file: File, content: String) {
        file.writeText(content)
        setFileReadableByAll(file)
    }

    private fun setFileReadableByAll(file: File) {
        // Essential for Docker bind mounts: Host user owns the file, but Container user (uid 1000) needs to read it.
        val success = file.setReadable(true, false) // readable=true, ownerOnly=false
        if (!success) {
            logger.warn("Failed to set readable permissions on ${file.absolutePath}. Docker container might fail to read this file.")
        }
    }
}