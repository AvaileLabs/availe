package com.availelabs.availe.domain.service

import kotlin.reflect.KClass

interface ICacheService {
    fun <T : Any> Get(key: String, type: KClass<T>): T?
    fun <T : Any> Set(key: String, value: T, timeToLiveSeconds: Int)
    fun Remove(key: String)
}