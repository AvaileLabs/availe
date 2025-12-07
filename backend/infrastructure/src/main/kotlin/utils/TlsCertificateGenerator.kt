package com.availelabs.utils

import org.bouncycastle.asn1.x500.X500Name
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.openssl.jcajce.JcaPEMWriter
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder
import java.io.StringWriter
import java.math.BigInteger
import java.security.KeyPairGenerator
import java.security.Security
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.*

internal object TlsCertificateGenerator {

    init {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(BouncyCastleProvider())
        }
    }

    fun generateSelfSignedCertificatePem(commonName: String): String {
        val keyPairGenerator = KeyPairGenerator.getInstance("RSA", BouncyCastleProvider.PROVIDER_NAME)
        keyPairGenerator.initialize(2048)
        val keyPair = keyPairGenerator.generateKeyPair()

        val notBefore = Date.from(Instant.now().minus(1, ChronoUnit.DAYS))
        val notAfter = Date.from(Instant.now().plus(365, ChronoUnit.DAYS))
        val serialNumber = BigInteger(64, Random())
        val issuer = X500Name("CN=$commonName")
        val subject = X500Name("CN=$commonName")

        val certificateBuilder = JcaX509v3CertificateBuilder(
            issuer,
            serialNumber,
            notBefore,
            notAfter,
            subject,
            keyPair.public
        )

        val contentSigner = JcaContentSignerBuilder("SHA256WithRSAEncryption")
            .setProvider(BouncyCastleProvider.PROVIDER_NAME)
            .build(keyPair.private)

        val certificateHolder = certificateBuilder.build(contentSigner)

        val stringWriter = StringWriter()
        JcaPEMWriter(stringWriter).use { pemWriter ->
            pemWriter.writeObject(keyPair.private)
            pemWriter.writeObject(certificateHolder)
        }

        return stringWriter.toString()
    }
}