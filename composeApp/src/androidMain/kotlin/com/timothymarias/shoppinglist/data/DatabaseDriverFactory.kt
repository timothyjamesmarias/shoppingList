package com.timothymarias.shoppinglist.data

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.timothymarias.shoppinglist.database.ShoppingDatabase

actual class DatabaseDriverFactory(private val context: Context) {
    actual fun createDriver(): SqlDriver {
        return AndroidSqliteDriver(ShoppingDatabase.Schema, context, "shopping.db")
    }
}
