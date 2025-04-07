package com.example.rfidstockpro.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.rfidstockpro.aws.models.ProductModel

class SharedProductViewModel : ViewModel() {
    private val _product = MutableLiveData<ProductModel>()
    val product: LiveData<ProductModel> get() = _product

    fun setProduct(productModel: ProductModel) {
        _product.value = productModel
    }

    fun updateTagId(tagId: String) {
        _product.value = _product.value?.copy(tagId = tagId)
    }
}
