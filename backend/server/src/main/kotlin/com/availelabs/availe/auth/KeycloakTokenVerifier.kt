@file:OptIn(kotlin.uuid.ExperimentalUuidApi::class)

package com.availelabs.availe.auth

import com.auth0.jwk.JwkProviderBuilder
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.availelabs.availe.domain.model.UserId
import java.net.URL
import java.security.interfaces.RSAPublicKey
import java.util.concurrent.TimeUnit
import kotlin.uuid.Uuid

class KeycloakTokenVerifier {
    private val jsonWebKeyProvider =
        JwkProviderBuilder(URL(AuthenticationConfiguration.JSON_WEB_KEY_SET_UNIFORM_RESOURCE_IDENTIFIER))
            .cached(10, 24, TimeUnit.HOURS)
            .rateLimited(10, 1, TimeUnit.MINUTES)
            .build()

    fun verify(token: String): AuthenticatedUser? {
        return try {
            val decodedJsonWebToken = JWT.decode(token)
            val keyIdentifier = decodedJsonWebToken.keyId
            val jsonWebKey = jsonWebKeyProvider.get(keyIdentifier)
            val rsaPublicKey = jsonWebKey.publicKey as RSAPublicKey
            val algorithm = Algorithm.RSA256(rsaPublicKey, null)

            val verifier = JWT.require(algorithm)
                .withIssuer(AuthenticationConfiguration.EXPECTED_TOKEN_ISSUER)
                .build()

            val verifiedToken = verifier.verify(token)
            val subjectClaim = verifiedToken.subject

            AuthenticatedUser(UserId(Uuid.parse(subjectClaim)))
        } catch (exception: Exception) {
            null
        }
    }
}