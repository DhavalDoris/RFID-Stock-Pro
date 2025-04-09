package com.example.rfidstockpro.ui.ProductManagement

object UHFService {
    fun startScan(onTagScanned: (String) -> Unit) {
        // Example using thread mock
        Thread {
            while (RFIDManager.isScanning) {
                Thread.sleep(1000)
                onTagScanned("TAG-${System.currentTimeMillis()}")
            }
        }.start()
    }

    fun stopScan() {
        // Stop scanning logic
    }
}
