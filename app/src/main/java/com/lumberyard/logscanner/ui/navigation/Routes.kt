package com.lumberyard.logscanner.ui.navigation

import android.os.Parcelable
import androidx.navigation3.runtime.NavKey
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Serializable
sealed interface Route : NavKey, Parcelable {
    @Serializable
    @Parcelize
    data object Scanner : Route

    @Serializable
    @Parcelize
    data object History : Route

    @Serializable
    @Parcelize
    data object Inventory : Route

    @Serializable
    @Parcelize
    data object Sync : Route

    @Serializable
    @Parcelize
    data object StockTake : Route
}
