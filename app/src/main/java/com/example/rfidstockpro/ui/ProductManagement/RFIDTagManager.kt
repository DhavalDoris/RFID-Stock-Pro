package com.example.rfidstockpro.ui.ProductManagement

object RFIDTagManager {
    private val tagList = mutableListOf<String>()

    fun addTag(tag: String) {
        if (tag !in tagList) tagList.add(tag)
    }

    fun clearTags() {
        tagList.clear()
    }

    fun getTags(): List<String> = tagList.toList()
}
