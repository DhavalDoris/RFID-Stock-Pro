package com.example.rfidstockpro.inouttracker.model

import java.util.UUID

data class CollectionModel(
    val collectionId : String = UUID.randomUUID().toString(),
    val collectionName: String = "",
    val description: String = "",
    val productIds: List<String> = emptyList(),
    val createdDateTime: String = "",
    val updatedDateTime: String = "",
    val userId: String = ""
)
