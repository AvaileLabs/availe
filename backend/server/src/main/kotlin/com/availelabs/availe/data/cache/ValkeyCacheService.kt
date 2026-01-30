package com.availelabs.availe.data.cache

import com.availelabs.availe.domain.service.ICacheService
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import org.slf4j.LoggerFactory
import redis.clients.jedis.DefaultJedisClientConfig
import redis.clients.jedis.RedisClient
import kotlin.reflect.KClass

class ValkeyCacheService(
    host: String,
    port: Int,
    password: String
) : ICacheService {

    private val logger = LoggerFactory.getLogger(javaClass)
    private val redisClient: RedisClient

    init {
        val clientConfig = DefaultJedisClientConfig.builder()
            .password(password)
            .build()

        redisClient = RedisClient.builder()
            .hostAndPort(host, port)
            .clientConfig(clientConfig)
            .build()
    }

    override fun <T : Any> Get(key: String, type: KClass<T>): T? {
        return try {
            val jsonValue = redisClient.get(key) ?: return null
            val serializer = serializer(type.java)
            Json.decodeFromString(serializer, jsonValue) as T
        } catch (exception: Exception) {
            logger.warn("Cache Get failed for key: $key. Reason: ${exception.message}")
            null
        }
    }

    override fun <T : Any> Set(key: String, value: T, timeToLiveSeconds: Int) {
        try {
            val serializer = serializer(value::class.java)
            val jsonValue = Json.encodeToString(serializer, value)
            redisClient.setex(key, timeToLiveSeconds.toLong(), jsonValue)
        } catch (exception: Exception) {
            logger.warn("Cache Set failed for key: $key. Reason: ${exception.message}")
        }
    }

    override fun Remove(key: String) {
        try {
            redisClient.del(key)
        } catch (exception: Exception) {
            logger.warn("Cache Remove failed for key: $key. Reason: ${exception.message}")
        }
    }
}