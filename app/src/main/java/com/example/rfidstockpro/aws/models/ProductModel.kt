package com.example.rfidstockpro.aws.models

import software.amazon.awssdk.services.dynamodb.model.AttributeValue

data class ProductModel(
    var id: String?,
    val selectedImages: List<String>,  // New: List of image paths
    val selectedVideo: String?,
    val productName: String,
    val productCategory: String,
    val sku: String,                   // SKU (Stock Keeping Unit)
    val price: String,
    val description: String,
    val isImageSelected: Boolean,
    var tagId: String,  // Added tagId
    val status: String,  // Added status
    val createdAt: String  // 🔥 New field
)


fun ProductModel.toMap(): Map<String, AttributeValue> {
    return mapOf(
        "id" to AttributeValue.builder().s(id).build(),
        "productName" to AttributeValue.builder().s(productName).build(),
        "productCategory" to AttributeValue.builder().s(productCategory).build(),
        "sku" to AttributeValue.builder().s(sku).build(),
        "price" to AttributeValue.builder().s(price).build(),
        "description" to AttributeValue.builder().s(description).build(),
        "isImageSelected" to AttributeValue.builder().bool(isImageSelected).build(),
        "tagId" to AttributeValue.builder().s(tagId).build(),
        "status" to AttributeValue.builder().s(status).build(),
        "createdAt" to AttributeValue.builder().s(createdAt).build(),
        "selectedImages" to AttributeValue.builder().l(
            selectedImages.map { AttributeValue.builder().s(it).build() }
        ).build(),
        "selectedVideo" to AttributeValue.builder().s(selectedVideo ?: "").build()
    )
}

