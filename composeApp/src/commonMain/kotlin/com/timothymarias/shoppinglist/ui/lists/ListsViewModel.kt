package com.timothymarias.shoppinglist.ui.lists

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.timothymarias.shoppinglist.data.ShoppingRepository
import com.timothymarias.shoppinglist.database.ShoppingList
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ListsViewModel(private val repository: ShoppingRepository) : ViewModel() {
    val lists: StateFlow<List<ShoppingList>> = repository.getAllLists()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun createList(name: String) {
        viewModelScope.launch {
            repository.insertList(name)
        }
    }

    fun updateList(id: Long, name: String) {
        viewModelScope.launch {
            repository.updateList(id, name)
        }
    }

    fun deleteList(id: Long) {
        viewModelScope.launch {
            repository.deleteList(id)
        }
    }
}
