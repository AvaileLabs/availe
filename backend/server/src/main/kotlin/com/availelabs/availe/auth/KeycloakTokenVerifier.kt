package com.availelabs.availe.auth

import com.auth0.jwk.JwkProviderBuilder
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.availelabs.availe.domain.model.UserId
import com.availelabs.availe.observability.TokenVerificationFailed
import com.availelabs.availe.observability.TokenVerificationSuccess
import org.http4k.events.Events
import java.net.URI
import java.security.interfaces.RSAPublicKey
import java.util.concurrent.TimeUnit
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class KeycloakTokenVerifier(
    private val events: Events
) {
    private val jsonWebKeyProvider =
        JwkProviderBuilder(URI.create(AuthenticationConfiguration.JSON_WEB_KEY_SET_UNIFORM_RESOURCE_IDENTIFIER).toURL())
            .cached(10, 24, TimeUnit.HOURS)
            .rateLimited(10, 1, TimeUnit.MINUTES)
            .build()

    fun verify(token: String): AuthenticatedUser? {
        var keyIdentifier: String? = null
        var tokenIssuer: String? = null

        return try {
            val decodedJsonWebToken = JWT.decode(token)
            keyIdentifier = decodedJsonWebToken.keyId
            tokenIssuer = decodedJsonWebToken.issuer

            val jsonWebKey = jsonWebKeyProvider.get(keyIdentifier)
            val publicKey = jsonWebKey.publicKey
                ?: throw IllegalStateException("Public key is null for keyId: $keyIdentifier")

            if (publicKey !is RSAPublicKey) {
                throw IllegalStateException("Public key is not an RSA key for keyId: $keyIdentifier. Type: ${publicKey::class.java.name}")
            }

            val algorithm = Algorithm.RSA256(publicKey, null)

            val verifier = JWT.require(algorithm)
                .withIssuer(AuthenticationConfiguration.EXPECTED_TOKEN_ISSUER)
                .build()

            val verifiedToken = verifier.verify(token)

            val subjectClaim = verifiedToken.subject
            val userIdString = if (!subjectClaim.isNullOrBlank()) {
                subjectClaim
            } else {
                val userIdClaim = verifiedToken.getClaim("user_id")
                if (!userIdClaim.isNull) userIdClaim.asString() else null
            }

            if (userIdString == null) {
                throw IllegalStateException("Token is missing both 'sub' and 'user_id' claims")
            }

            val userId = UserId(Uuid.parse(userIdString))

            events(
                TokenVerificationSuccess(
                    userId = userId,
                    issuer = tokenIssuer
                )
            )

            AuthenticatedUser(userId)
        } catch (exception: Exception) {
            val debugClaims = try {
                JWT.decode(token).claims.mapValues { it.value.toString() }
            } catch (e: Exception) {
                mapOf("error" to "Could not decode token claims: ${e.message}")
            }

            events(
                TokenVerificationFailed(
                    reason = exception.message ?: "Unknown error during token verification",
                    exceptionType = exception::class.qualifiedName ?: "UnknownException",
                    stackTrace = exception.stackTrace.map { it.toString() }.take(10),
                    keyIdentifier = keyIdentifier,
                    tokenIssuer = tokenIssuer,
                    expectedIssuer = AuthenticationConfiguration.EXPECTED_TOKEN_ISSUER,
                    jsonWebKeySetUniformResourceIdentifier = AuthenticationConfiguration.JSON_WEB_KEY_SET_UNIFORM_RESOURCE_IDENTIFIER,
                    availableClaims = debugClaims
                )
            )
            null
        }
    }
}