package com.timothymarias.shoppinglist.shared.models

import kotlinx.serialization.Serializable

@Serializable
data class UpdateItemRequest(
    val name: String? = null,
    val completed: Boolean? = null,
)
