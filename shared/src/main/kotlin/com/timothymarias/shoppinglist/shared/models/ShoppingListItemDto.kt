package com.timothymarias.shoppinglist.shared.models

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
data class ShoppingListItemDto(
    val id: Long = 0,
    val listId: Long,
    val name: String,
    val completed: Boolean = false,
    val createdAt: Instant,
    val updatedAt: Instant,
)
