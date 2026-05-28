package com.lumberyard.logscanner

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Inventory
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material.icons.rounded.Warehouse
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import com.lumberyard.logscanner.data.local.AppDatabase
import com.lumberyard.logscanner.data.local.LogEntity
import com.lumberyard.logscanner.data.remote.ApiService
import com.lumberyard.logscanner.data.repository.LogRepository
import com.lumberyard.logscanner.ui.MainViewModel
import com.lumberyard.logscanner.ui.navigation.NavigationState
import com.lumberyard.logscanner.ui.navigation.Navigator
import com.lumberyard.logscanner.ui.navigation.Route
import com.lumberyard.logscanner.ui.navigation.rememberNavigationState
import com.lumberyard.logscanner.ui.navigation.toEntries
import com.lumberyard.logscanner.ui.screens.HistoryScreen
import com.lumberyard.logscanner.ui.screens.InventoryScreen
import com.lumberyard.logscanner.ui.screens.ScannerScreen
import com.lumberyard.logscanner.ui.screens.ScannerViewModel
import com.lumberyard.logscanner.ui.screens.StockTakeScreen
import com.lumberyard.logscanner.ui.screens.SyncScreen
import com.lumberyard.logscanner.ui.theme.LogScannerTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val database = AppDatabase.getDatabase(this)
        val retrofit = Retrofit.Builder()
            .baseUrl("https://api.lumberyard.com/")
            .addConverterFactory(MoshiConverterFactory.create())
            .client(OkHttpClient.Builder().build())
            .build()
        val apiService = retrofit.create(ApiService::class.java)
        val repository = LogRepository(database.logDao(), database.scanDao(), apiService)

        // Seed some test data
        CoroutineScope(Dispatchers.IO).launch {
            if (database.logDao().getLogCount() == 0) {
                database.logDao().insertLogs(listOf(
                    LogEntity("LOG001", 6.0, 30.0, 0.42, "Available"),
                    LogEntity("LOG002", 6.0, 35.0, 0.58, "Available"),
                    LogEntity("QR12345", 4.8, 25.0, 0.24, "Available")
                ))
            }
        }

        setContent {
            val viewModel: MainViewModel = viewModel(
                factory = object : ViewModelProvider.Factory {
                    @Suppress("UNCHECKED_CAST")
                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
                        return MainViewModel(repository) as T
                    }
                }
            )

            LogScannerTheme {
                val navigationState = rememberNavigationState(
                    startRoute = Route.Scanner,
                    topLevelRoutes = setOf(Route.Scanner, Route.History, Route.Inventory, Route.Sync, Route.StockTake)
                )
                val navigator = remember { Navigator(navigationState) }

                AppShell(navigationState, navigator, viewModel, repository)
            }
        }
    }
}

@Composable
fun AppShell(
    navigationState: NavigationState,
    navigator: Navigator,
    viewModel: MainViewModel,
    repository: LogRepository
) {
    val entryProvider: (NavKey) -> NavEntry<NavKey> = entryProvider {
        entry<Route.Scanner> {
            val scannerViewModel: ScannerViewModel = viewModel(
                factory = object : ViewModelProvider.Factory {
                    @Suppress("UNCHECKED_CAST")
                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
                        return ScannerViewModel(repository) as T
                    }
                }
            )
            ScannerScreen(scannerViewModel, onHistoryClick = { navigator.navigate(Route.History) })
        }
        entry<Route.History> { HistoryScreen(viewModel) }
        entry<Route.Inventory> { InventoryScreen(viewModel) }
        entry<Route.Sync> { SyncScreen(viewModel) }
        entry<Route.StockTake> { StockTakeScreen(viewModel) }
    }

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = navigationState.topLevelRoute == Route.Scanner,
                    onClick = { navigator.navigate(Route.Scanner) },
                    icon = { Icon(Icons.Rounded.QrCodeScanner, contentDescription = "Scan") },
                    label = { Text("Scan") }
                )
                NavigationBarItem(
                    selected = navigationState.topLevelRoute == Route.History,
                    onClick = { navigator.navigate(Route.History) },
                    icon = { Icon(Icons.Rounded.History, contentDescription = "History") },
                    label = { Text("History") }
                )
                NavigationBarItem(
                    selected = navigationState.topLevelRoute == Route.Inventory,
                    onClick = { navigator.navigate(Route.Inventory) },
                    icon = { Icon(Icons.Rounded.Warehouse, contentDescription = "Inventory") },
                    label = { Text("Inventory") }
                )
                NavigationBarItem(
                    selected = navigationState.topLevelRoute == Route.StockTake,
                    onClick = { navigator.navigate(Route.StockTake) },
                    icon = { Icon(Icons.Rounded.Inventory, contentDescription = "Stock Take") },
                    label = { Text("Stock") }
                )
                NavigationBarItem(
                    selected = navigationState.topLevelRoute == Route.Sync,
                    onClick = { navigator.navigate(Route.Sync) },
                    icon = { Icon(Icons.Rounded.Sync, contentDescription = "Sync") },
                    label = { Text("Sync") }
                )
            }
        }
    ) { innerPadding ->
        NavDisplay(
            modifier = Modifier.padding(innerPadding),
            entries = navigationState.toEntries(entryProvider),
            onBack = { navigator.goBack() }
        )
    }
}
