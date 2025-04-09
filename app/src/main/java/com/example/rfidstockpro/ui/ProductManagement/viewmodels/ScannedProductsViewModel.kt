package com.example.rfidstockpro.ui.ProductManagement.viewmodels

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.rfidstockpro.aws.AwsManager.getAllProducts
import com.example.rfidstockpro.aws.models.ProductModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import software.amazon.awssdk.services.dynamodb.model.AttributeValue

class ScannedProductsViewModel : ViewModel() {

    private val _products = MutableLiveData<List<ProductModel>>(mutableListOf())
    val products: LiveData<List<ProductModel>> get() = _products

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> get() = _isLoading

    private val _totalCount = MutableLiveData<Int>()
    val totalCount: LiveData<Int> get() = _totalCount


    private var scannedTagList: List<String> = emptyList()
    private var allMatchedProducts = mutableListOf<ProductModel>()
    private var currentPageMatchedProducts = mutableListOf<ProductModel>()
    private val allFilteredProducts = mutableListOf<ProductModel>()
    private var lastEvaluatedKey: Map<String, AttributeValue>? = null
    private var isLastPage = false
    private val pageSize = 5

    fun setTagFilters(tagList: List<String>) {
        scannedTagList = tagList
        allMatchedProducts.clear()
        currentPageMatchedProducts.clear()
        lastEvaluatedKey = null
        isLastPage = false
        _products.postValue(emptyList())
        loadNextPage()
    }

    private fun clearPagination() {
        allFilteredProducts.clear()
        _products.postValue(emptyList())
        lastEvaluatedKey = null
        isLastPage = false
    }

    fun loadNextPage() {
        if (_isLoading.value == true || isLastPage) return
        _isLoading.postValue(true)

        viewModelScope.launch(Dispatchers.IO) {

            val matched = mutableListOf<ProductModel>()
            while (matched.size < pageSize && !isLastPage) {
                val (items, newLastKey) = getAllProducts(lastEvaluatedKey)
                lastEvaluatedKey = newLastKey
                isLastPage = newLastKey == null

                val filtered = items.filter { it.tagId in scannedTagList }
                matched.addAll(filtered)

                if (filtered.isNotEmpty()) {
                    Log.d("ScannedProductsVM", "🎯 Matched so far: ${matched.size}")
                }
            }

            currentPageMatchedProducts.addAll(matched.take(pageSize))
            allMatchedProducts.addAll(matched.take(pageSize))
            _products.postValue(allMatchedProducts)
            _isLoading.postValue(false)

        }
    }

    fun hasMoreData(): Boolean {
        return !isLastPage
    }
}

