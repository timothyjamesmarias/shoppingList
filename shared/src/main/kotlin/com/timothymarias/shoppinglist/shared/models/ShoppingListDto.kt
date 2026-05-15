package com.timothymarias.shoppinglist.shared.models

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
data class ShoppingListDto(
    val id: Long = 0,
    val name: String,
    val createdAt: Instant,
    val updatedAt: Instant,
)
