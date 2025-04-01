package com.example.rfidstockpro.aws.models

data class ProductModel(
    val productName: String,
    val productCategory: String,
    val priceStr: String,
    val color: String,
    val jewelCode: String,
    val styleNo: String,
    val purity: String,
    val totalDiaWtStr: String,
    val totalGrossWtStr: String,
    val totalDiaStr: String,
    val description: String,
    val isImageSelected: Boolean
)
