package com.timothymarias.shoppinglist.server

import com.timothymarias.shoppinglist.server.db.ShoppingItems
import com.timothymarias.shoppinglist.server.db.ShoppingLists
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction

object DatabaseFactory {
    fun init(config: DatabaseConfig) {
        val dataSource = HikariDataSource(HikariConfig().apply {
            jdbcUrl = config.url
            username = config.user
            password = config.password
            maximumPoolSize = config.maxPoolSize
            driverClassName = "org.postgresql.Driver"
            isAutoCommit = false
            transactionIsolation = "TRANSACTION_REPEATABLE_READ"
        })
        Database.connect(dataSource)
        transaction {
            SchemaUtils.createMissingTablesAndColumns(ShoppingLists, ShoppingItems)
        }
    }
}
