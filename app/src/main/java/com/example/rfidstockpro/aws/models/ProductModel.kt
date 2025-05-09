package com.example.rfidstockpro.aws.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import software.amazon.awssdk.services.dynamodb.model.AttributeValue
import java.io.Serializable
@Parcelize
data class ProductModel(
    var id: String?,
    val selectedImages: List<String>,  // New: List of image paths
    val selectedVideo: String?,
    val productName: String,
    val productCategory: String,
    val styleNo: String,
    val sku: String,                   // SKU (Stock Keeping Unit)
    val price: String,
    val description: String,
    var isImageSelected: Boolean,
    val isMediaUpdated: Boolean,
    var tagId: String,  // Added tagId
    val status: String,  // Added status
    val createdAt: String, // 🔥 New field
    val updatedAt: String,  // 🔥 New field
    // Temporary fields - not to be stored
    var previewImageUrls: List<String>? = null,
    var previewVideoUrl: String? = null,
    var isUploaded: Boolean = false
): Parcelable

fun ProductModel.toMap(): Map<String, AttributeValue> {
    return mapOf(
        "id" to AttributeValue.builder().s(id).build(),
        "productName" to AttributeValue.builder().s(productName).build(),
        "productCategory" to AttributeValue.builder().s(productCategory).build(),
        "sku" to AttributeValue.builder().s(sku).build(),
        "styleNo" to AttributeValue.builder().s(styleNo).build(),
        "price" to AttributeValue.builder().s(price).build(),
        "description" to AttributeValue.builder().s(description).build(),
        "isImageSelected" to AttributeValue.builder().bool(isImageSelected).build(),
        "isMediaUpdated" to AttributeValue.builder().bool(isImageSelected).build(),
        "tagId" to AttributeValue.builder().s(tagId).build(),
        "status" to AttributeValue.builder().s(status).build(),
        "createdAt" to AttributeValue.builder().s(createdAt).build(),
        "updatedAt" to AttributeValue.builder().s(updatedAt).build(),
        "selectedImages" to AttributeValue.builder().l(
            selectedImages.map { AttributeValue.builder().s(it).build() }
        ).build(),
        "selectedVideo" to AttributeValue.builder().s(selectedVideo ?: "").build()
    )
}

fun Map<String, AttributeValue>.toProductModel(): ProductModel {
    return ProductModel(
        id = this["id"]?.s(),
        selectedImages = this["selectedImages"]?.l()?.mapNotNull { it.s() } ?: emptyList(),
        selectedVideo = this["selectedVideo"]?.s()?.takeIf { it.isNotEmpty() },
        productName = this["productName"]?.s() ?: "",
        productCategory = this["productCategory"]?.s() ?: "",
        sku = this["sku"]?.s() ?: "",
        styleNo = this["styleNo"]?.s() ?: "",
        price = this["price"]?.s() ?: "",
        description = this["description"]?.s() ?: "",
        isImageSelected = this["isImageSelected"]?.bool() ?: false,
        isMediaUpdated = this["isMediaUpdated"]?.bool() ?: false,
        tagId = this["tagId"]?.s() ?: "",
        status = this["status"]?.s() ?: "",
        createdAt = this["createdAt"]?.s() ?: "",
        updatedAt = this["updatedAt"]?.s() ?: ""
    )
}



