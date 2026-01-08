package com.timothymarias.shoppinglist.data

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.timothymarias.shoppinglist.database.ShoppingDatabase
import com.timothymarias.shoppinglist.database.ShoppingList
import com.timothymarias.shoppinglist.database.ShoppingListItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class ShoppingRepository(databaseDriverFactory: DatabaseDriverFactory) {
    private val database = ShoppingDatabase(databaseDriverFactory.createDriver())
    private val listQueries = database.shoppingListQueries
    private val itemQueries = database.shoppingListItemQueries

    fun getAllLists(): Flow<List<ShoppingList>> {
        return listQueries.getAllLists().asFlow().mapToList(Dispatchers.Default)
    }

    suspend fun insertList(name: String) = withContext(Dispatchers.Default) {
        listQueries.insertList(name, System.currentTimeMillis())
    }

    suspend fun updateList(id: Long, name: String) = withContext(Dispatchers.Default) {
        listQueries.updateList(name, id)
    }

    suspend fun deleteList(id: Long) = withContext(Dispatchers.Default) {
        listQueries.deleteList(id)
    }

    fun getItemsByListId(listId: Long): Flow<List<ShoppingListItem>> {
        return itemQueries.getItemsByListId(listId).asFlow().mapToList(Dispatchers.Default)
    }

    suspend fun insertItem(listId: Long, name: String) = withContext(Dispatchers.Default) {
        itemQueries.insertItem(listId, name, false, System.currentTimeMillis())
    }

    suspend fun updateItem(id: Long, name: String, completed: Boolean) = withContext(Dispatchers.Default) {
        itemQueries.updateItem(name, completed, id)
    }

    suspend fun toggleItemCompleted(id: Long, completed: Boolean) = withContext(Dispatchers.Default) {
        itemQueries.toggleCompleted(completed, id)
    }

    suspend fun deleteItem(id: Long) = withContext(Dispatchers.Default) {
        itemQueries.deleteItem(id)
    }
}
