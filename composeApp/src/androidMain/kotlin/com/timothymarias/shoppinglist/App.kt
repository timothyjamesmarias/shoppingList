package com.timothymarias.shoppinglist

import android.app.Application
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.timothymarias.shoppinglist.data.DatabaseDriverFactory
import com.timothymarias.shoppinglist.data.ShoppingApiClient
import com.timothymarias.shoppinglist.data.ShoppingRepository
import com.timothymarias.shoppinglist.data.SyncEngine
import com.timothymarias.shoppinglist.database.ShoppingDatabase
import com.timothymarias.shoppinglist.ui.items.ItemsScreen
import com.timothymarias.shoppinglist.ui.items.ItemsViewModel
import com.timothymarias.shoppinglist.ui.lists.ListsScreen
import com.timothymarias.shoppinglist.ui.lists.ListsViewModel

private const val BASE_URL = "http://10.0.2.2:8080"

sealed class Screen {
    object Lists : Screen()
    data class Items(val listId: Long, val listName: String) : Screen()
}

@Composable
fun App() {
    val context = LocalContext.current
    val repository = remember(context) {
        val driver = DatabaseDriverFactory(context.applicationContext as Application).createDriver()
        val database = ShoppingDatabase(driver)
        val apiClient = ShoppingApiClient(BASE_URL)
        val syncEngine = SyncEngine(database, apiClient)
        ShoppingRepository(database, syncEngine)
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            repository.syncNow()
        }
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
