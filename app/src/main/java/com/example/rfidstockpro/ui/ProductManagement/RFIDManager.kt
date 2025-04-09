package com.example.rfidstockpro.ui.ProductManagement

import android.util.Log

object RFIDManager {
    var isScanning = false
    var onTagScanned: ((String) -> Unit)? = null

    fun startScan() {
        isScanning = true
        // Replace with SDK start trigger listener or polling
        UHFService.startScan { tag ->
            Log.d("RFIDScan", "Tag scanned: $tag")
            RFIDTagManager.addTag(tag)
            onTagScanned?.invoke(tag)
        }
    }

    fun stopScan() {
        isScanning = false
        UHFService.stopScan()
    }
}
