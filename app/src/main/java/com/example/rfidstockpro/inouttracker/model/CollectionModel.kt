package com.example.rfidstockpro.inouttracker.model

import software.amazon.awssdk.services.dynamodb.model.AttributeValue
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
fun Map<String, AttributeValue>.toCollectionModel(): CollectionModel {
    return CollectionModel(
        collectionId = this["collectionId"]?.s() ?: "",
        collectionName = this["collectionName"]?.s() ?: "",
        description = this["description"]?.s() ?: "",
        productIds = this["productIds"]?.l()?.mapNotNull { it.s() } ?: emptyList(),
        createdDateTime = this["createdDateTime"]?.s() ?: "",
        updatedDateTime = this["updatedDateTime"]?.s() ?: "",
        userId = this["userId"]?.s() ?: ""
    )
}
fun CollectionModel.toMap(): Map<String, AttributeValue> {
    return mapOf(
        "collectionId" to AttributeValue.builder().s(collectionId).build(),
        "collectionName" to AttributeValue.builder().s(collectionName).build(),
        "description" to AttributeValue.builder().s(description).build(),
        "productIds" to AttributeValue.builder().l(
            productIds.map { AttributeValue.builder().s(it).build() }
        ).build(),
        "createdDateTime" to AttributeValue.builder().s(createdDateTime).build(),
        "updatedDateTime" to AttributeValue.builder().s(updatedDateTime).build(),
        "userId" to AttributeValue.builder().s(userId).build()
    )
}

