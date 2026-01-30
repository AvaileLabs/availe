package com.availelabs.availe.data.cache

import kotlinx.serialization.Serializable
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.testcontainers.containers.GenericContainer
import org.testcontainers.utility.DockerImageName

@Serializable
data class TestData(val name: String, val value: Int)

class ValkeyCacheServiceTest {

    private val redisPassword = "testpassword"

    private val valkeyContainer = GenericContainer(DockerImageName.parse("valkey/valkey:8.0"))
        .withExposedPorts(6379)
        .withCommand("valkey-server", "--requirepass", redisPassword)

    @Test
    fun `Set and Get works correctly with running container`() {
        valkeyContainer.start()
        try {
            val host = valkeyContainer.host
            val port = valkeyContainer.getMappedPort(6379)

            val service = ValkeyCacheService(host, port, redisPassword)

            val testData = TestData("test", 123)
            service.Set("test_key", testData, 60)

            val result = service.Get("test_key", TestData::class)
            assertEquals(testData, result)
        } finally {
            valkeyContainer.stop()
        }
    }

    @Test
    fun `Get returns null and swallows exception when Valkey is down`() {
        // Point to localhost with a random port where nothing is running
        val service = ValkeyCacheService("localhost", 12345, redisPassword)

        val result = service.Get("test_key", TestData::class)

        assertNull(result, "Should return null on connection failure")
    }

    @Test
    fun `Set swallows exception when Valkey is down`() {
        val service = ValkeyCacheService("localhost", 12345, redisPassword)

        assertDoesNotThrow {
            service.Set("test_key", TestData("test", 1), 60)
        }
    }

    @Test
    fun `Remove swallows exception when Valkey is down`() {
        val service = ValkeyCacheService("localhost", 12345, redisPassword)

        assertDoesNotThrow {
            service.Remove("test_key")
        }
    }
}