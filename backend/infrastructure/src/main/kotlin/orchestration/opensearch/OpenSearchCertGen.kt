package com.availelabs.orchestration.opensearch

import com.availelabs.orchestration.InfrastructureRegistry
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
import java.security.cert.X509Certificate

internal object OpenSearchCertGen {
    private val logger = LoggerFactory.getLogger(javaClass)

    init {
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

        setFileReadableByAll(generatedCertDirectory)

        val rootCaFile = File(generatedCertDirectory, "root-ca.pem")
        val rootKeyFile = File(generatedCertDirectory, "root-ca-key.pem")

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

        ensureSignedCertificate(
            rootBundle,
            File(generatedCertDirectory, "admin.pem"),
            File(generatedCertDirectory, "admin-key.pem"),
            "admin",
            emptyList()
        )

        ensureSignedCertificate(
            rootBundle,
            File(generatedCertDirectory, "node1.pem"),
            File(generatedCertDirectory, "node1-key.pem"),
            InfrastructureRegistry.Topology.OPENSEARCH_NODE_1_NAME,
            listOf(InfrastructureRegistry.Topology.OPENSEARCH_NODE_1_NAME, "localhost", "127.0.0.1")
        )

        ensureSignedCertificate(
            rootBundle,
            File(generatedCertDirectory, "node2.pem"),
            File(generatedCertDirectory, "node2-key.pem"),
            InfrastructureRegistry.Topology.OPENSEARCH_NODE_2_NAME,
            listOf(InfrastructureRegistry.Topology.OPENSEARCH_NODE_2_NAME, "localhost", "127.0.0.1")
        )

        ensureSignedCertificate(
            rootBundle,
            File(generatedCertDirectory, "dashboards.pem"),
            File(generatedCertDirectory, "dashboards-key.pem"),
            InfrastructureRegistry.Topology.OPENSEARCH_DASHBOARDS_NAME,
            listOf(InfrastructureRegistry.Topology.OPENSEARCH_DASHBOARDS_NAME, "localhost", "127.0.0.1")
        )

        logger.info("OpenSearch certificates check complete at ${generatedCertDirectory.absolutePath}")
    }

    private fun ensureSignedCertificate(
        issuerBundle: TlsCertificateGenerator.CertificateBundle,
        certificateFile: File,
        keyFile: File,
        commonName: String,
        subjectAlternativeNames: List<String>
    ) {
        if (certificateFile.exists() && keyFile.exists()) {
            try {
                val existingCertificate = loadCertificate(certificateFile)
                existingCertificate.verify(issuerBundle.x509Certificate.publicKey)
                logger.info("Certificate for '$commonName' exists and is valid. Skipping generation.")
                return
            } catch (exception: Exception) {
                logger.warn("Certificate for '$commonName' exists but failed verification against current Root CA: ${exception.message}. Regenerating...")
            }
        }

        logger.info("Generating OpenSearch '$commonName' Certificate...")
        val bundle = TlsCertificateGenerator.generateSignedCertificate(
            commonName = commonName,
            subjectAlternativeNames = subjectAlternativeNames,
            issuerBundle = issuerBundle
        )
        writeAndPermitFile(certificateFile, bundle.certificatePem)
        writeAndPermitFile(keyFile, bundle.privateKeyPem)
    }

    private fun loadCertificate(certificateFile: File): X509Certificate {
        PEMParser(FileReader(certificateFile)).use { parser ->
            val certificateHolder = parser.readObject() as? X509CertificateHolder
                ?: throw IllegalStateException("File ${certificateFile.name} does not contain a certificate")
            return JcaX509CertificateConverter()
                .setProvider(BouncyCastleProvider.PROVIDER_NAME)
                .getCertificate(certificateHolder)
        }
    }

    private fun loadRootCertificate(certificateFile: File, keyFile: File): TlsCertificateGenerator.CertificateBundle {
        val x509Certificate = loadCertificate(certificateFile)

        val keyParser = PEMParser(FileReader(keyFile))
        val keyObject = keyParser.readObject()
        keyParser.close()

        val privateKey = when (keyObject) {
            is PEMKeyPair -> JcaPEMKeyConverter().setProvider(BouncyCastleProvider.PROVIDER_NAME)
                .getKeyPair(keyObject).private

            is PrivateKeyInfo -> JcaPEMKeyConverter().setProvider(BouncyCastleProvider.PROVIDER_NAME)
                .getPrivateKey(keyObject)

            else -> throw IllegalStateException("Unknown private key format in ${keyFile.name}: ${keyObject?.javaClass?.name}")
        }

        val keyPair = KeyPair(x509Certificate.publicKey, privateKey)
        val certificatePem = certificateFile.readText()
        val privateKeyPem = keyFile.readText()

        return TlsCertificateGenerator.CertificateBundle(
            certificatePem = certificatePem,
            privateKeyPem = privateKeyPem,
            x509Certificate = x509Certificate,
            keyPair = keyPair
        )
    }

    private fun writeAndPermitFile(file: File, content: String) {
        file.writeText(content)
        setFileReadableByAll(file)
    }

    private fun setFileReadableByAll(file: File) {
        val success = file.setReadable(true, false)
        if (!success) {
            logger.warn("Failed to set readable permissions on ${file.absolutePath}. Docker container might fail to read this file.")
        }
    }
}