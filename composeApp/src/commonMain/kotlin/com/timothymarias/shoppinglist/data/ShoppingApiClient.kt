package com.timothymarias.shoppinglist.data

import com.timothymarias.shoppinglist.shared.models.*
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

class ShoppingApiClient(baseUrl: String) {

    private val client = HttpClient {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
            })
        }
        install(Logging) {
            level = LogLevel.NONE
        }
        defaultRequest {
            url(baseUrl)
            contentType(ContentType.Application.Json)
        }
    }

    // Lists

    suspend fun getLists(): List<ShoppingListDto> =
        client.get("/api/v1/lists").body()

    suspend fun getList(id: Long): ShoppingListDto =
        client.get("/api/v1/lists/$id").body()

    suspend fun createList(request: CreateShoppingListRequest): ShoppingListDto =
        client.post("/api/v1/lists") { setBody(request) }.body()

    suspend fun updateList(id: Long, request: UpdateShoppingListRequest): ShoppingListDto =
        client.put("/api/v1/lists/$id") { setBody(request) }.body()

    suspend fun deleteList(id: Long) {
        client.delete("/api/v1/lists/$id")
    }

    // Items

    suspend fun getItems(listId: Long): List<ShoppingListItemDto> =
        client.get("/api/v1/lists/$listId/items").body()

    suspend fun getItem(listId: Long, id: Long): ShoppingListItemDto =
        client.get("/api/v1/lists/$listId/items/$id").body()

    suspend fun createItem(listId: Long, request: CreateItemRequest): ShoppingListItemDto =
        client.post("/api/v1/lists/$listId/items") { setBody(request) }.body()

    suspend fun updateItem(listId: Long, id: Long, request: UpdateItemRequest): ShoppingListItemDto =
        client.put("/api/v1/lists/$listId/items/$id") { setBody(request) }.body()

    suspend fun deleteItem(listId: Long, id: Long) {
        client.delete("/api/v1/lists/$listId/items/$id")
    }
}
