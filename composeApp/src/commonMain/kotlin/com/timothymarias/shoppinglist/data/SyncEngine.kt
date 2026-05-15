package com.timothymarias.shoppinglist.data

import com.timothymarias.shoppinglist.database.ShoppingDatabase
import com.timothymarias.shoppinglist.shared.models.*
import io.ktor.client.plugins.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

sealed class SyncState {
    data object Idle : SyncState()
    data object Syncing : SyncState()
    data class Error(val message: String) : SyncState()
}

class SyncEngine(
    private val database: ShoppingDatabase,
    private val apiClient: ShoppingApiClient,
) {
    private val listQueries = database.shoppingListQueries
    private val itemQueries = database.shoppingListItemQueries
    private val syncMutex = Mutex()

    private val _syncState = MutableStateFlow<SyncState>(SyncState.Idle)
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

    suspend fun sync() {
        syncMutex.withLock {
            _syncState.value = SyncState.Syncing
            try {
                pushChanges()
                pullChanges()
                _syncState.value = SyncState.Idle
            } catch (e: Exception) {
                _syncState.value = SyncState.Error(e.message ?: "Sync failed")
            }
        }
    }

    private suspend fun pushChanges() {
        pushDeletedLists()
        pushDirtyLists()
        pushDeletedItems()
        pushDirtyItems()
    }

    private suspend fun pushDeletedLists() {
        val deletedLists = listQueries.getDeletedLists().executeAsList()
        for (list in deletedLists) {
            val sid = list.serverId
            if (sid == null) {
                // Never pushed to server, just hard-delete locally
                listQueries.deleteList(list.id)
                continue
            }
            try {
                apiClient.deleteList(sid)
            } catch (e: ClientRequestException) {
                if (e.response.status.value != 404) throw e
                // 404 = already deleted on server, fine
            }
            listQueries.deleteList(list.id)
        }
    }

    private suspend fun pushDirtyLists() {
        val dirtyLists = listQueries.getDirtyLists().executeAsList()
        for (list in dirtyLists) {
            if (list.serverId == null) {
                val serverList = apiClient.createList(CreateShoppingListRequest(list.name))
                listQueries.setListServerId(serverList.id, list.id)
            } else {
                apiClient.updateList(list.serverId, UpdateShoppingListRequest(list.name))
                listQueries.markListClean(list.id)
            }
        }
    }

    private suspend fun pushDeletedItems() {
        val deletedItems = itemQueries.getDeletedItems().executeAsList()
        for (item in deletedItems) {
            val itemSid = item.serverId
            if (itemSid == null) {
                itemQueries.deleteItem(item.id)
                continue
            }
            val parentList = listQueries.getListById(item.listId).executeAsOneOrNull() ?: continue
            val listSid = parentList.serverId ?: continue
            try {
                apiClient.deleteItem(listSid, itemSid)
            } catch (e: ClientRequestException) {
                if (e.response.status.value != 404) throw e
            }
            itemQueries.deleteItem(item.id)
        }
    }

    private suspend fun pushDirtyItems() {
        val dirtyItems = itemQueries.getDirtyItems().executeAsList()
        for (item in dirtyItems) {
            val parentList = listQueries.getListById(item.listId).executeAsOneOrNull() ?: continue
            val listSid = parentList.serverId ?: continue // parent not synced yet, skip

            if (item.serverId == null) {
                val serverItem = apiClient.createItem(listSid, CreateItemRequest(item.name))
                itemQueries.setItemServerId(serverItem.id, item.id)
            } else {
                apiClient.updateItem(
                    listSid, item.serverId,
                    UpdateItemRequest(name = item.name, completed = item.completed)
                )
                itemQueries.markItemClean(item.id)
            }
        }
    }

    private suspend fun pullChanges() {
        val serverLists = apiClient.getLists()

        for (serverList in serverLists) {
            pullList(serverList)
            pullItemsForList(serverList)
        }

        detectDeletedLists(serverLists)
    }

    private fun pullList(serverList: ShoppingListDto) {
        val localList = listQueries.getListByServerId(serverList.id).executeAsOneOrNull()
        val serverTime = serverList.updatedAt.toEpochMilliseconds()

        if (localList == null) {
            listQueries.insertListFromServer(
                serverList.name,
                serverList.createdAt.toEpochMilliseconds(),
                serverList.id,
                serverTime,
            )
        } else if (!localList.dirty || serverTime > localList.updatedAt) {
            listQueries.updateListFromServer(serverList.name, serverTime, serverList.id)
        }
        // else: local is dirty and newer, keep local version
    }

    private suspend fun pullItemsForList(serverList: ShoppingListDto) {
        val localParent = listQueries.getListByServerId(serverList.id).executeAsOneOrNull() ?: return
        val serverItems = apiClient.getItems(serverList.id)

        for (serverItem in serverItems) {
            val localItem = itemQueries.getItemByServerId(serverItem.id).executeAsOneOrNull()
            val serverTime = serverItem.updatedAt.toEpochMilliseconds()

            if (localItem == null) {
                itemQueries.insertItemFromServer(
                    localParent.id,
                    serverItem.name,
                    serverItem.completed,
                    serverItem.createdAt.toEpochMilliseconds(),
                    serverItem.id,
                    serverTime,
                )
            } else if (!localItem.dirty || serverTime > localItem.updatedAt) {
                itemQueries.updateItemFromServer(
                    serverItem.name,
                    serverItem.completed,
                    serverTime,
                    serverItem.id,
                )
            }
        }

        // Detect server-side item deletions
        val serverItemIds = serverItems.map { it.id }.toSet()
        val localItemServerIds = itemQueries.getServerIdsByListServerId(serverList.id).executeAsList()
        for (localSid in localItemServerIds) {
            if (localSid != null && localSid !in serverItemIds) {
                val orphan = itemQueries.getItemByServerId(localSid).executeAsOneOrNull()
                if (orphan != null && !orphan.dirty) {
                    itemQueries.deleteItem(orphan.id)
                }
            }
        }
    }

    private fun detectDeletedLists(serverLists: List<ShoppingListDto>) {
        val serverListIds = serverLists.map { it.id }.toSet()
        val localListServerIds = listQueries.getAllServerIds().executeAsList()
        for (localSid in localListServerIds) {
            if (localSid != null && localSid !in serverListIds) {
                val orphan = listQueries.getListByServerId(localSid).executeAsOneOrNull()
                if (orphan != null && !orphan.dirty) {
                    listQueries.deleteList(orphan.id)
                }
            }
        }
    }
}
