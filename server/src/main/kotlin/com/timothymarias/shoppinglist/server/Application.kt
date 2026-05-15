package com.timothymarias.shoppinglist.server

import io.ktor.server.application.*
import io.ktor.server.netty.*

fun main(args: Array<String>): Unit = EngineMain.main(args)

fun Application.module() {
    val dbConfig = DatabaseConfig.fromEnvironment(environment)
    DatabaseFactory.init(dbConfig)
    configureSerialization()
    configureStatusPages()
    configureRouting()
}
