package com.example.myapplication.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import com.example.myapplication.ui.dashboard.DashboardScreen
import com.example.myapplication.ui.history.HistoryScreen
import com.example.myapplication.ui.history.HistoryViewModel
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun KavachNavGraph(backStack: NavBackStack<NavKey>) {
    NavDisplay(
        backStack = backStack,
        onBack = { if (backStack.size > 1) backStack.removeAt(backStack.size - 1) },
        entryProvider = entryProvider {
            entry<Destination.Dashboard> {
                DashboardScreen(
                    onNavigateToHistory = { backStack.add(Destination.History) }
                )
            }
            entry<Destination.History> {
                val historyViewModel: HistoryViewModel = viewModel()
                HistoryScreen(
                    viewModel = historyViewModel,
                    onBack = { backStack.removeAt(backStack.size - 1) }
                )
            }
        }
    )
}
