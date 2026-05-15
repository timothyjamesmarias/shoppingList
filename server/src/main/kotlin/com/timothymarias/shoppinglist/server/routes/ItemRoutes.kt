package com.timothymarias.shoppinglist.server.routes

import com.timothymarias.shoppinglist.server.NotFoundException
import com.timothymarias.shoppinglist.server.db.ShoppingItems
import com.timothymarias.shoppinglist.server.db.ShoppingLists
import com.timothymarias.shoppinglist.shared.models.CreateItemRequest
import com.timothymarias.shoppinglist.shared.models.ShoppingListItemDto
import com.timothymarias.shoppinglist.shared.models.UpdateItemRequest
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.datetime.Clock
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction

fun Route.itemRoutes() {
    route("/lists/{listId}/items") {
        get {
            val listId = call.parameters["listId"]?.toLongOrNull()
                ?: throw IllegalArgumentException("Invalid list ID")
            val items = transaction {
                ShoppingItems.selectAll().where { ShoppingItems.listId eq listId }
                    .orderBy(ShoppingItems.createdAt, SortOrder.ASC)
                    .map { it.toShoppingListItemDto() }
            }
            call.respond(items)
        }

        get("/{id}") {
            val listId = call.parameters["listId"]?.toLongOrNull()
                ?: throw IllegalArgumentException("Invalid list ID")
            val id = call.parameters["id"]?.toLongOrNull()
                ?: throw IllegalArgumentException("Invalid item ID")
            val item = transaction {
                ShoppingItems.selectAll()
                    .where { (ShoppingItems.id eq id) and (ShoppingItems.listId eq listId) }
                    .singleOrNull()?.toShoppingListItemDto()
            } ?: throw NotFoundException("Item $id not found in list $listId")
            call.respond(item)
        }

        post {
            val listId = call.parameters["listId"]?.toLongOrNull()
                ?: throw IllegalArgumentException("Invalid list ID")
            val req = call.receive<CreateItemRequest>()
            require(req.name.isNotBlank()) { "Name must not be blank" }
            val now = Clock.System.now()
            val created = transaction {
                val listExists = ShoppingLists.selectAll()
                    .where { ShoppingLists.id eq listId }
                    .count() > 0
                if (!listExists) throw NotFoundException("List $listId not found")

                val insertedId = ShoppingItems.insert {
                    it[ShoppingItems.listId] = listId
                    it[name] = req.name
                    it[completed] = false
                    it[createdAt] = now
                    it[updatedAt] = now
                } get ShoppingItems.id
                ShoppingListItemDto(
                    id = insertedId,
                    listId = listId,
                    name = req.name,
                    completed = false,
                    createdAt = now,
                    updatedAt = now,
                )
            }
            call.respond(HttpStatusCode.Created, created)
        }

        put("/{id}") {
            val listId = call.parameters["listId"]?.toLongOrNull()
                ?: throw IllegalArgumentException("Invalid list ID")
            val id = call.parameters["id"]?.toLongOrNull()
                ?: throw IllegalArgumentException("Invalid item ID")
            val req = call.receive<UpdateItemRequest>()
            val now = Clock.System.now()
            val updated = transaction {
                val count = ShoppingItems.update({
                    (ShoppingItems.id eq id) and (ShoppingItems.listId eq listId)
                }) {
                    req.name?.let { value -> it[name] = value }
                    req.completed?.let { value -> it[completed] = value }
                    it[updatedAt] = now
                }
                if (count == 0) throw NotFoundException("Item $id not found in list $listId")
                ShoppingItems.selectAll()
                    .where { (ShoppingItems.id eq id) and (ShoppingItems.listId eq listId) }
                    .single().toShoppingListItemDto()
            }
            call.respond(updated)
        }

        delete("/{id}") {
            val listId = call.parameters["listId"]?.toLongOrNull()
                ?: throw IllegalArgumentException("Invalid list ID")
            val id = call.parameters["id"]?.toLongOrNull()
                ?: throw IllegalArgumentException("Invalid item ID")
            transaction {
                val count = ShoppingItems.deleteWhere {
                    (ShoppingItems.id eq id) and (ShoppingItems.listId eq listId)
                }
                if (count == 0) throw NotFoundException("Item $id not found in list $listId")
            }
            call.respond(HttpStatusCode.NoContent)
        }
    }
}

private fun ResultRow.toShoppingListItemDto() = ShoppingListItemDto(
    id = this[ShoppingItems.id],
    listId = this[ShoppingItems.listId],
    name = this[ShoppingItems.name],
    completed = this[ShoppingItems.completed],
    createdAt = this[ShoppingItems.createdAt],
    updatedAt = this[ShoppingItems.updatedAt],
)
