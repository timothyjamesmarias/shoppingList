package com.timothymarias.shoppinglist.server.db

import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp

object ShoppingItems : Table("shopping_items") {
    val id = long("id").autoIncrement()
    val listId = long("list_id").references(ShoppingLists.id, onDelete = ReferenceOption.CASCADE)
    val name = text("name")
    val completed = bool("completed").default(false)
    val createdAt = timestamp("created_at")
    val updatedAt = timestamp("updated_at")

    override val primaryKey = PrimaryKey(id)

    init {
        index(isUnique = false, listId)
    }
}
