package com.availelabs.provisioning

internal object EjabberdSchemaLoader {
    fun loadSchemaSql(): String {
        val resourceStream = javaClass.getResourceAsStream("/pg.new.sql")
            ?: throw IllegalStateException("Could not find /pg.new.sql in resources. Please ensure backend/infrastructure/src/main/resources/pg.new.sql exists.")

        return resourceStream.bufferedReader().use { it.readText() }
    }
}