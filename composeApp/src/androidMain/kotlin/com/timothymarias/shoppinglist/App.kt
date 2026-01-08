package com.timothymarias.shoppinglist

import android.app.Application
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.timothymarias.shoppinglist.data.DatabaseDriverFactory
import com.timothymarias.shoppinglist.data.ShoppingRepository
import com.timothymarias.shoppinglist.ui.items.ItemsScreen
import com.timothymarias.shoppinglist.ui.items.ItemsViewModel
import com.timothymarias.shoppinglist.ui.lists.ListsScreen
import com.timothymarias.shoppinglist.ui.lists.ListsViewModel

sealed class Screen {
    object Lists : Screen()
    data class Items(val listId: Long, val listName: String) : Screen()
}

@Composable
fun App() {
    val context = LocalContext.current
    val repository = remember(context) {
        ShoppingRepository(DatabaseDriverFactory(context.applicationContext as Application))
    }

    var currentScreen by remember { mutableStateOf<Screen>(Screen.Lists) }

    MaterialTheme {
        when (val screen = currentScreen) {
            is Screen.Lists -> {
                val viewModel: ListsViewModel = viewModel {
                    ListsViewModel(repository)
                }
                ListsScreen(
                    viewModel = viewModel,
                    onListClick = { listId ->
                        val list = viewModel.lists.value.find { it.id == listId }
                        list?.let {
                            currentScreen = Screen.Items(listId, it.name)
                        }
                    }
                )
            }
            is Screen.Items -> {
                val viewModel: ItemsViewModel = viewModel(key = screen.listId.toString()) {
                    ItemsViewModel(repository, screen.listId)
                }
                ItemsScreen(
                    viewModel = viewModel,
                    listName = screen.listName,
                    onBack = { currentScreen = Screen.Lists }
                )
            }
        }
    }
}