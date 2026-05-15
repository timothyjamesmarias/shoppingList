package com.timothymarias.shoppinglist.shared.models

import kotlinx.serialization.Serializable

@Serializable
data class CreateItemRequest(val name: String)
