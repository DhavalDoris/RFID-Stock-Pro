package com.example.rfidstockpro.aws.models

data class ProductModel(
    val selectedImages: List<String>,  // New: List of image paths
    val selectedVideo: String?,
    val productName: String,
    val productCategory: String,
    val sku: String,                   // SKU (Stock Keeping Unit)
    val price: String,
    val description: String,
    val isImageSelected: Boolean,
    val tagId: String,  // Added tagId
    val status: String,  // Added status
)
