package com.example.rfidstockpro.bulkupload.model

data class MappingItem(
    val importedHeader: String,  // From Excel
    val sampleValue: String,      // Example value from Excel
    var systemHeader: String? = null // Selected App Field (user will choose)
)
