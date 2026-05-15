package com.timothymarias.shoppinglist.server.routes

import com.timothymarias.shoppinglist.server.NotFoundException
import com.timothymarias.shoppinglist.server.db.ShoppingLists
import com.timothymarias.shoppinglist.shared.models.CreateShoppingListRequest
import com.timothymarias.shoppinglist.shared.models.ShoppingListDto
import com.timothymarias.shoppinglist.shared.models.UpdateShoppingListRequest
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.datetime.Clock
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction

fun Route.listRoutes() {
    route("/lists") {
        get {
            val lists = transaction {
                ShoppingLists.selectAll()
                    .orderBy(ShoppingLists.createdAt, SortOrder.DESC)
                    .map { it.toShoppingListDto() }
            }
            call.respond(lists)
        }

        get("/{id}") {
            val id = call.parameters["id"]?.toLongOrNull()
                ?: throw IllegalArgumentException("Invalid list ID")
            val list = transaction {
                ShoppingLists.selectAll().where { ShoppingLists.id eq id }
                    .singleOrNull()?.toShoppingListDto()
            } ?: throw NotFoundException("List $id not found")
            call.respond(list)
        }

        post {
            val req = call.receive<CreateShoppingListRequest>()
            require(req.name.isNotBlank()) { "Name must not be blank" }
            val now = Clock.System.now()
            val created = transaction {
                val insertedId = ShoppingLists.insert {
                    it[name] = req.name
                    it[createdAt] = now
                    it[updatedAt] = now
                } get ShoppingLists.id
                ShoppingListDto(id = insertedId, name = req.name, createdAt = now, updatedAt = now)
            }
            call.respond(HttpStatusCode.Created, created)
        }

        put("/{id}") {
            val id = call.parameters["id"]?.toLongOrNull()
                ?: throw IllegalArgumentException("Invalid list ID")
            val req = call.receive<UpdateShoppingListRequest>()
            require(req.name.isNotBlank()) { "Name must not be blank" }
            val now = Clock.System.now()
            val updated = transaction {
                val count = ShoppingLists.update({ ShoppingLists.id eq id }) {
                    it[name] = req.name
                    it[updatedAt] = now
                }
                if (count == 0) throw NotFoundException("List $id not found")
                ShoppingLists.selectAll().where { ShoppingLists.id eq id }
                    .single().toShoppingListDto()
            }
            call.respond(updated)
        }

        delete("/{id}") {
            val id = call.parameters["id"]?.toLongOrNull()
                ?: throw IllegalArgumentException("Invalid list ID")
            transaction {
                val count = ShoppingLists.deleteWhere { ShoppingLists.id eq id }
                if (count == 0) throw NotFoundException("List $id not found")
            }
            call.respond(HttpStatusCode.NoContent)
        }
    }
}

private fun ResultRow.toShoppingListDto() = ShoppingListDto(
    id = this[ShoppingLists.id],
    name = this[ShoppingLists.name],
    createdAt = this[ShoppingLists.createdAt],
    updatedAt = this[ShoppingLists.updatedAt],
)
