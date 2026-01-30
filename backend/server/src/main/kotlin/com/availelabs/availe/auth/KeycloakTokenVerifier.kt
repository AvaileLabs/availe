package com.availelabs.availe.auth

import com.auth0.jwk.JwkProviderBuilder
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.availelabs.availe.configuration.SharedAuthenticationConstants
import com.availelabs.availe.domain.model.*
import com.availelabs.availe.observability.TokenVerificationFailed
import com.availelabs.availe.observability.TokenVerificationSuccess
import org.http4k.events.Events
import java.net.URI
import java.security.interfaces.RSAPublicKey
import java.util.concurrent.TimeUnit
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

interface ITokenVerifier {
    fun verify(token: String): VerifiedKeycloakToken?
}

@OptIn(ExperimentalUuidApi::class)
class KeycloakTokenVerifier(
    private val events: Events
) : ITokenVerifier {
    private val jsonWebKeyProvider =
        JwkProviderBuilder(URI.create(AuthenticationConfiguration.JSON_WEB_KEY_SET_UNIFORM_RESOURCE_IDENTIFIER).toURL())
            .cached(10, 24, TimeUnit.HOURS)
            .rateLimited(10, 1, TimeUnit.MINUTES)
            .build()

    override fun verify(token: String): VerifiedKeycloakToken? {
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
                val userIdClaim = verifiedToken.getClaim(SharedAuthenticationConstants.Claims.USER_ID)
                if (!userIdClaim.isNull) userIdClaim.asString() else null
            }

            if (userIdString == null) {
                throw IllegalStateException("Token is missing both '${SharedAuthenticationConstants.Claims.SUBJECT}' and '${SharedAuthenticationConstants.Claims.USER_ID}' claims")
            }

            val emailClaim = verifiedToken.getClaim(SharedAuthenticationConstants.Claims.EMAIL)
            if (emailClaim.isNull || emailClaim.asString().isBlank()) {
                throw IllegalStateException("Token is missing '${SharedAuthenticationConstants.Claims.EMAIL}' claim")
            }

            val nameClaim = verifiedToken.getClaim("name")
            val givenNameClaim = verifiedToken.getClaim("given_name")
            val familyNameClaim = verifiedToken.getClaim("family_name")

            val displayNameString = when {
                !nameClaim.isNull && !nameClaim.asString().isBlank() -> nameClaim.asString()
                !givenNameClaim.isNull -> "${givenNameClaim.asString()} ${familyNameClaim?.asString().orEmpty()}".trim()
                else -> emailClaim.asString().substringBefore("@")
            }

            val pictureClaim = verifiedToken.getClaim("picture")
            val avatarUrlString =
                if (!pictureClaim.isNull && !pictureClaim.asString().isBlank()) pictureClaim.asString() else null

            val jidClaim = verifiedToken.getClaim("jid")
            val jidString = if (!jidClaim.isNull && !jidClaim.asString().isBlank()) jidClaim.asString() else null

            val userId = UserId(Uuid.parse(userIdString))
            val email = Email(emailClaim.asString())
            val displayName = UserDisplayName(displayNameString)
            val avatarUrl = avatarUrlString?.let { ImageUrl(it) }
            val jid = jidString?.let { Jid(it) }

            events(
                TokenVerificationSuccess(
                    userId = userId,
                    issuer = tokenIssuer
                )
            )

            VerifiedKeycloakToken(
                userId = userId,
                email = email,
                displayName = displayName,
                avatarUrl = avatarUrl,
                jid = jid
            )
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