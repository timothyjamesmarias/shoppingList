package com.timothymarias.shoppinglist.shared

object ApiRoutes {
    const val LISTS = "/api/v1/lists"
    const val LIST_BY_ID = "/api/v1/lists/{id}"
    const val ITEMS = "/api/v1/lists/{listId}/items"
    const val ITEM_BY_ID = "/api/v1/lists/{listId}/items/{id}"
}
