package com.availelabs.utils

import org.bouncycastle.asn1.x500.X500Name
import org.bouncycastle.asn1.x509.*
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter
import org.bouncycastle.cert.jcajce.JcaX509ExtensionUtils
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.openssl.jcajce.JcaPEMWriter
import org.bouncycastle.openssl.jcajce.JcaPKCS8Generator
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder
import java.io.StringWriter
import java.math.BigInteger
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.Security
import java.security.cert.X509Certificate
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.*

internal object TlsCertificateGenerator {

    init {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(BouncyCastleProvider())
        }
    }

    data class CertificateBundle(
        val certificatePem: String,
        val privateKeyPem: String,
        val x509Certificate: X509Certificate,
        val keyPair: KeyPair
    )

    fun generateSelfSignedCertificatePem(commonName: String): String {
        val bundle = generateRootCertificate(commonName)
        return bundle.privateKeyPem + bundle.certificatePem
    }

    fun generateRootCertificate(commonName: String): CertificateBundle {
        val keyPair = generateKeyPair()
        val issuer = X500Name("CN=$commonName")
        val subject = X500Name("CN=$commonName")
        val serialNumber = BigInteger(64, Random())
        val notBefore = Date.from(Instant.now().minus(1, ChronoUnit.DAYS))
        val notAfter = Date.from(Instant.now().plus(3650, ChronoUnit.DAYS))

        val certificateBuilder = JcaX509v3CertificateBuilder(
            issuer,
            serialNumber,
            notBefore,
            notAfter,
            subject,
            keyPair.public
        )

        // Basic Constraints: CA=TRUE, PathLen=0
        certificateBuilder.addExtension(
            Extension.basicConstraints,
            true,
            BasicConstraints(true)
        )

        // Key Usage: KeyCertSign, cRLSign
        val keyUsage = KeyUsage(KeyUsage.keyCertSign or KeyUsage.cRLSign)
        certificateBuilder.addExtension(
            Extension.keyUsage,
            true,
            keyUsage
        )

        // Subject Key Identifier using JcaX509ExtensionUtils
        val utils = JcaX509ExtensionUtils()
        val subjectKeyIdentifier = utils.createSubjectKeyIdentifier(keyPair.public)
        certificateBuilder.addExtension(
            Extension.subjectKeyIdentifier,
            false,
            subjectKeyIdentifier
        )

        val contentSigner = JcaContentSignerBuilder("SHA256WithRSAEncryption")
            .setProvider(BouncyCastleProvider.PROVIDER_NAME)
            .build(keyPair.private) // Root signs itself

        val certificateHolder = certificateBuilder.build(contentSigner)
        val x509Certificate = JcaX509CertificateConverter()
            .setProvider(BouncyCastleProvider.PROVIDER_NAME)
            .getCertificate(certificateHolder)

        return CertificateBundle(
            certificatePem = convertToPem(x509Certificate),
            privateKeyPem = convertToPem(keyPair.private),
            x509Certificate = x509Certificate,
            keyPair = keyPair
        )
    }

    fun generateSignedCertificate(
        commonName: String,
        subjectAlternativeNames: List<String>,
        issuerBundle: CertificateBundle
    ): CertificateBundle {
        val keyPair = generateKeyPair()
        // Use the Issuer's Subject DN as the Issuer for this new cert
        val issuer = X500Name(issuerBundle.x509Certificate.subjectX500Principal.name)
        val subject = X500Name("CN=$commonName")
        val serialNumber = BigInteger(64, Random())
        val notBefore = Date.from(Instant.now().minus(1, ChronoUnit.DAYS))
        val notAfter = Date.from(Instant.now().plus(3650, ChronoUnit.DAYS))

        val certificateBuilder = JcaX509v3CertificateBuilder(
            issuer,
            serialNumber,
            notBefore,
            notAfter,
            subject,
            keyPair.public
        )

        // Basic Constraints: CA=FALSE
        certificateBuilder.addExtension(
            Extension.basicConstraints,
            true,
            BasicConstraints(false)
        )

        // Key Usage: DigitalSignature, KeyEncipherment
        val keyUsage = KeyUsage(KeyUsage.digitalSignature or KeyUsage.keyEncipherment)
        certificateBuilder.addExtension(
            Extension.keyUsage,
            true,
            keyUsage
        )

        // Extended Key Usage: ServerAuth, ClientAuth
        val extendedKeyUsage = ExtendedKeyUsage(
            arrayOf(KeyPurposeId.id_kp_serverAuth, KeyPurposeId.id_kp_clientAuth)
        )
        certificateBuilder.addExtension(
            Extension.extendedKeyUsage,
            false,
            extendedKeyUsage
        )

        // Subject Alternative Names (DNS and IP support)
        if (subjectAlternativeNames.isNotEmpty()) {
            val generalNames = subjectAlternativeNames.map { name ->
                if (isIpAddress(name)) {
                    GeneralName(GeneralName.iPAddress, name)
                } else {
                    GeneralName(GeneralName.dNSName, name)
                }
            }
            val generalNamesExtension = GeneralNames(generalNames.toTypedArray())
            certificateBuilder.addExtension(Extension.subjectAlternativeName, false, generalNamesExtension)
        }

        // Authority Key Identifier using JcaX509ExtensionUtils
        val utils = JcaX509ExtensionUtils()
        val authorityKeyIdentifier = utils.createAuthorityKeyIdentifier(issuerBundle.keyPair.public)
        certificateBuilder.addExtension(
            Extension.authorityKeyIdentifier,
            false,
            authorityKeyIdentifier
        )

        // CRITICAL FIX: Sign with the Issuer's Private Key, not the new KeyPair's private key.
        val contentSigner = JcaContentSignerBuilder("SHA256WithRSAEncryption")
            .setProvider(BouncyCastleProvider.PROVIDER_NAME)
            .build(issuerBundle.keyPair.private)

        val certificateHolder = certificateBuilder.build(contentSigner)
        val x509Certificate = JcaX509CertificateConverter()
            .setProvider(BouncyCastleProvider.PROVIDER_NAME)
            .getCertificate(certificateHolder)

        return CertificateBundle(
            certificatePem = convertToPem(x509Certificate),
            privateKeyPem = convertToPem(keyPair.private),
            x509Certificate = x509Certificate,
            keyPair = keyPair
        )
    }

    private fun generateKeyPair(): KeyPair {
        val keyPairGenerator = KeyPairGenerator.getInstance("RSA", BouncyCastleProvider.PROVIDER_NAME)
        keyPairGenerator.initialize(2048)
        return keyPairGenerator.generateKeyPair()
    }

    private fun convertToPem(anyObject: Any): String {
        val stringWriter = StringWriter()
        JcaPEMWriter(stringWriter).use { pemWriter ->
            if (anyObject is java.security.PrivateKey) {
                // Fix: OpenSearch requires PKCS#8 format for private keys (BEGIN PRIVATE KEY),
                // not PKCS#1 (BEGIN RSA PRIVATE KEY). Bouncy Castle writes PKCS#1 by default for KeyPair/PrivateKey.
                // We wrap it in JcaPKCS8Generator to force the correct format.
                val pkcs8Generator = JcaPKCS8Generator(anyObject, null)
                pemWriter.writeObject(pkcs8Generator)
            } else {
                pemWriter.writeObject(anyObject)
            }
        }
        return stringWriter.toString()
    }

    private fun isIpAddress(name: String): Boolean {
        return name.matches(Regex("^\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\$"))
    }
}