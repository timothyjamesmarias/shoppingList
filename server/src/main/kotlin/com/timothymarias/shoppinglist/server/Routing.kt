package com.timothymarias.shoppinglist.server

import com.timothymarias.shoppinglist.server.routes.itemRoutes
import com.timothymarias.shoppinglist.server.routes.listRoutes
import io.ktor.server.application.*
import io.ktor.server.routing.*

fun Application.configureRouting() {
    routing {
        route("/api/v1") {
            listRoutes()
            itemRoutes()
        }
    }
}
