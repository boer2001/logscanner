package com.lumberyard.logscanner.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lumberyard.logscanner.data.local.LogEntity
import com.lumberyard.logscanner.data.local.ScanEntity
import com.lumberyard.logscanner.data.repository.LogRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface ScanUiState {
    data object Idle : ScanUiState
    data class Success(val log: LogEntity) : ScanUiState
    data class Error(val message: String) : ScanUiState
    data object Scanning : ScanUiState
}

class ScannerViewModel(private val repository: LogRepository) : ViewModel() {

    private val _uiState = MutableStateFlow<ScanUiState>(ScanUiState.Idle)
    val uiState: StateFlow<ScanUiState> = _uiState.asStateFlow()

    private var lastScannedBarcode: String? = null
    private var lastScanTime: Long = 0

    fun onBarcodeDetected(barcode: String) {
        val currentTime = System.currentTimeMillis()
        // Prevent duplicate scans within 2 seconds
        if (barcode == lastScannedBarcode && currentTime - lastScanTime < 2000) return

        lastScannedBarcode = barcode
        lastScanTime = currentTime

        _uiState.value = ScanUiState.Scanning

        viewModelScope.launch {
            val log = repository.getLogByBarcode(barcode)
            if (log != null) {
                repository.insertScan(
                    ScanEntity(
                        barcode = barcode,
                        scannedBy = "Current User", // Should come from a session/user setting
                        truckId = null // Could be set from UI
                    )
                )
                _uiState.value = ScanUiState.Success(log)
            } else {
                _uiState.value = ScanUiState.Error("Log not found: $barcode")
            }
        }
    }

    fun resetState() {
        _uiState.value = ScanUiState.Idle
    }
}
