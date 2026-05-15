package com.timothymarias.shoppinglist.server

import io.ktor.server.application.*

data class DatabaseConfig(
    val url: String,
    val user: String,
    val password: String,
    val maxPoolSize: Int,
) {
    companion object {
        fun fromEnvironment(environment: ApplicationEnvironment): DatabaseConfig {
            val config = environment.config
            return DatabaseConfig(
                url = System.getenv("DB_URL") ?: config.property("database.url").getString(),
                user = System.getenv("DB_USER") ?: config.property("database.user").getString(),
                password = System.getenv("DB_PASSWORD") ?: config.property("database.password").getString(),
                maxPoolSize = config.property("database.maxPoolSize").getString().toInt(),
            )
        }
    }
}
