package com.timothymarias.shoppinglist.data

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.timothymarias.shoppinglist.database.ShoppingDatabase
import com.timothymarias.shoppinglist.database.ShoppingList
import com.timothymarias.shoppinglist.database.ShoppingListItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ShoppingRepository(
    private val database: ShoppingDatabase,
    private val syncEngine: SyncEngine,
) {
    private val listQueries = database.shoppingListQueries
    private val itemQueries = database.shoppingListItemQueries

    private val syncScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val syncTrigger = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    init {
        @OptIn(kotlinx.coroutines.FlowPreview::class)
        syncScope.launch {
            syncTrigger
                .debounce(500)
                .collect { syncEngine.sync() }
        }
    }

    private fun requestSync() {
        syncTrigger.tryEmit(Unit)
    }

    suspend fun syncNow() {
        syncEngine.sync()
    }

    // Reads

    fun getAllLists(): Flow<List<ShoppingList>> {
        return listQueries.getAllLists().asFlow().mapToList(Dispatchers.Default)
    }

    fun getItemsByListId(listId: Long): Flow<List<ShoppingListItem>> {
        return itemQueries.getItemsByListId(listId).asFlow().mapToList(Dispatchers.Default)
    }

    // Writes

    suspend fun insertList(name: String) = withContext(Dispatchers.Default) {
        val now = System.currentTimeMillis()
        listQueries.insertList(name, now, now)
        requestSync()
    }

    suspend fun updateList(id: Long, name: String) = withContext(Dispatchers.Default) {
        listQueries.updateList(name, System.currentTimeMillis(), id)
        requestSync()
    }

    suspend fun deleteList(id: Long) = withContext(Dispatchers.Default) {
        listQueries.softDeleteList(System.currentTimeMillis(), id)
        requestSync()
    }

    suspend fun insertItem(listId: Long, name: String) = withContext(Dispatchers.Default) {
        val now = System.currentTimeMillis()
        itemQueries.insertItem(listId, name, false, now, now)
        requestSync()
    }

    suspend fun updateItem(id: Long, name: String, completed: Boolean) = withContext(Dispatchers.Default) {
        itemQueries.updateItem(name, completed, System.currentTimeMillis(), id)
        requestSync()
    }

    suspend fun toggleItemCompleted(id: Long, completed: Boolean) = withContext(Dispatchers.Default) {
        itemQueries.toggleCompleted(completed, System.currentTimeMillis(), id)
        requestSync()
    }

    suspend fun deleteItem(id: Long) = withContext(Dispatchers.Default) {
        itemQueries.softDeleteItem(System.currentTimeMillis(), id)
        requestSync()
    }
}
