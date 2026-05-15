package com.timothymarias.shoppinglist.shared.models

import kotlinx.serialization.Serializable

@Serializable
data class CreateShoppingListRequest(val name: String)
