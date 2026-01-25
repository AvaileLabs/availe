package com.availelabs.orchestration.ejabberd

import com.fasterxml.jackson.databind.ObjectMapper
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class GenerateEjabberdJwtKeyFileContentTest {
    private val objectMapper = ObjectMapper()

    @Test
    fun returnsJwksWithOnlySigningKeysWhenPresent() {
        val keycloakJwks =
            """{"keys":[{"kid":"sigKid","use":"sig","kty":"RSA","alg":"RS256","n":"abc","e":"AQAB"},{"kid":"encKid","use":"enc","kty":"RSA","alg":"RSA-OAEP","n":"def","e":"AQAB"}]}"""

        val result = generateEjabberdJwtKeyFileContent(keycloakJwks)

        val resultNode = objectMapper.readTree(result)
        val keysNode = resultNode.get("keys")
        assertNotNull(keysNode)
        assertEquals(1, keysNode.size())
        assertEquals("sigKid", keysNode[0].get("kid").asText())
        assertEquals("sig", keysNode[0].get("use").asText())
    }

    @Test
    fun returnsJwksWithAllKeysWhenNoSigningKeysAreMarked() {
        val keycloakJwks =
            """{"keys":[{"kid":"first","kty":"RSA"},{"kid":"second","kty":"RSA"}]}"""

        val result = generateEjabberdJwtKeyFileContent(keycloakJwks)

        val resultNode = objectMapper.readTree(result)
        val keysNode = resultNode.get("keys")
        assertNotNull(keysNode)
        assertEquals(2, keysNode.size())
        assertEquals("first", keysNode[0].get("kid").asText())
        assertEquals("second", keysNode[1].get("kid").asText())
    }
}