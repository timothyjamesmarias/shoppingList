package com.timothymarias.shoppinglist.server.db

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp

object ShoppingLists : Table("shopping_lists") {
    val id = long("id").autoIncrement()
    val name = text("name")
    val createdAt = timestamp("created_at")
    val updatedAt = timestamp("updated_at")

    override val primaryKey = PrimaryKey(id)
}
