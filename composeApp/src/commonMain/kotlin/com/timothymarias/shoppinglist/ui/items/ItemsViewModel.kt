package com.timothymarias.shoppinglist.ui.items

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.timothymarias.shoppinglist.data.ShoppingRepository
import com.timothymarias.shoppinglist.database.ShoppingListItem
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ItemsViewModel(
    private val repository: ShoppingRepository,
    private val listId: Long
) : ViewModel() {
    val items: StateFlow<List<ShoppingListItem>> = repository.getItemsByListId(listId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun createItem(name: String) {
        viewModelScope.launch {
            repository.insertItem(listId, name)
        }
    }

    fun toggleItemCompleted(id: Long, completed: Boolean) {
        viewModelScope.launch {
            repository.toggleItemCompleted(id, completed)
        }
    }

    fun deleteItem(id: Long) {
        viewModelScope.launch {
            repository.deleteItem(id)
        }
    }
}
