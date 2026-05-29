package com.lumberyard.logscanner.ui.screens

import android.content.Context
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import com.huawei.hms.ml.scan.HmsScan
import com.huawei.hms.ml.scan.HmsScanAnalyzer
import com.huawei.hms.ml.scan.HmsScanAnalyzerOptions
import com.huawei.hms.mlsdk.common.MLFrame

class BarcodeAnalyzer(
    private val context: Context,
    private val onBarcodeDetected: (String) -> Unit
) : ImageAnalysis.Analyzer {

    private val mlKitScanner by lazy { BarcodeScanning.getClient() }
    
    private val isHuaweiDevice = android.os.Build.MANUFACTURER.equals("Huawei", ignoreCase = true)
    
    // Check if HMS is available by checking for the availability class
    private val isHmsAvailable: Boolean by lazy {
        try {
            val availability = Class.forName("com.huawei.hms.api.HuaweiApiAvailability")
            val method = availability.getMethod("getInstance")
            val instance = method.invoke(null)
            val checkMethod = availability.getMethod("isHuaweiMobileServicesAvailable", Context::class.java)
            val result = checkMethod.invoke(instance, context) as Int
            result == 0 // 0 is ConnectionResult.SUCCESS
        } catch (e: Exception) {
            false
        }
    }

    private val hmsScanner: HmsScanAnalyzer? by lazy {
        if (isHmsAvailable) {
            val options = HmsScanAnalyzerOptions.Creator()
                .setHmsScanTypes(HmsScan.ALL_SCAN_TYPE)
                .create()
            HmsScanAnalyzer(options)
        } else null
    }

    @OptIn(ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        if (isHuaweiDevice && isHmsAvailable) {
            analyzeWithHms(imageProxy)
        } else {
            analyzeWithMlKit(imageProxy)
        }
    }

    @OptIn(ExperimentalGetImage::class)
    private fun analyzeWithMlKit(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
            mlKitScanner.process(image)
                .addOnSuccessListener { barcodes ->
                    for (barcode in barcodes) {
                        barcode.rawValue?.let { value ->
                            onBarcodeDetected(value)
                        }
                    }
                }
                .addOnCompleteListener {
                    imageProxy.close()
                }
        } else {
            imageProxy.close()
        }
    }

    @OptIn(ExperimentalGetImage::class)
    private fun analyzeWithHms(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            try {
                val frame = MLFrame.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                val result = hmsScanner?.analyseFrame(frame)
                if (result != null && result.size() > 0) {
                    for (i in 0 until result.size()) {
                        result.valueAt(i).getOriginalValue()?.let { value ->
                            onBarcodeDetected(value)
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        imageProxy.close()
    }
}
