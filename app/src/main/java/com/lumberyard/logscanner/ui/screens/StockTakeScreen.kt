package com.lumberyard.logscanner.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Inventory
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lumberyard.logscanner.data.local.LogEntity
import com.lumberyard.logscanner.data.local.ScanEntity
import com.lumberyard.logscanner.ui.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StockTakeScreen(viewModel: MainViewModel) {
    val logs by viewModel.allLogs.collectAsState(initial = emptyList())
    val scans by viewModel.allScans.collectAsState(initial = emptyList())

    // Group scans by barcode to get actual physical counts
    val scanCounts = scans.groupBy { it.barcode }.mapValues { it.value.size }
    
    // Reconciliation logic: Compare logs (on hand) vs scans (physical)
    val reconciliationList = logs.map { log ->
        StockReconciliation(
            barcode = log.barcode,
            expected = 1, // In this app, each unique barcode is usually one log
            actual = scanCounts[log.barcode] ?: 0
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Stock Take Reconciliation", fontWeight = FontWeight.Bold) }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Summary Header
            StockSummaryHeader(reconciliationList)

            LazyColumn(modifier = Modifier.fillMaxSize()) {
                item {
                    Text(
                        "Discrepancy Report",
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                items(reconciliationList) { item ->
                    ReconciliationItem(item)
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                }
            }
        }
    }
}

data class StockReconciliation(
    val barcode: String,
    val expected: Int,
    val actual: Int
) {
    val status: String
        get() = when {
            actual == expected -> "Match"
            actual < expected -> "Short"
            else -> "Over"
        }
}

@Composable
fun StockSummaryHeader(list: List<StockReconciliation>) {
    val totalExpected = list.size
    val totalActual = list.count { it.actual > 0 }
    val matches = list.count { it.status == "Match" }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Row(
            modifier = Modifier.padding(24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Overall Progress", style = MaterialTheme.typography.labelLarge)
                Text(
                    "$matches / $totalExpected",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Black
                )
                Text("Logs Verified", style = MaterialTheme.typography.bodyMedium)
            }
            
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(MaterialTheme.colorScheme.primary, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Rounded.Inventory,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    }
}

@Composable
fun ReconciliationItem(item: StockReconciliation) {
    val statusColor = when (item.status) {
        "Match" -> Color(0xFF4CAF50)
        "Short" -> Color(0xFFF44336)
        else -> Color(0xFFFF9800)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(item.barcode, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            Text(
                "Status: ${item.status}",
                color = statusColor,
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.bodySmall
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("${item.actual}", fontWeight = FontWeight.Black, fontSize = 20.sp)
                Text("Actual", style = MaterialTheme.typography.labelSmall)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("${item.expected}", fontWeight = FontWeight.Black, fontSize = 20.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Nett", style = MaterialTheme.typography.labelSmall)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Icon(
                imageVector = if (item.status == "Match") Icons.Rounded.Check else Icons.Rounded.ErrorOutline,
                contentDescription = null,
                tint = statusColor
            )
        }
    }
}
