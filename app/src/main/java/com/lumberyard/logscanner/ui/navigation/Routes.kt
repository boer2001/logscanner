package com.lumberyard.logscanner.ui.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
sealed interface Route : NavKey {
    @Serializable
    data object Scanner : Route

    @Serializable
    data object History : Route

    @Serializable
    data object Inventory : Route

    @Serializable
    data object Sync : Route

    @Serializable
    data object StockTake : Route
}
