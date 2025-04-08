package com.example.rfidstockpro.ui.ProductManagement.viewmodels

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.rfidstockpro.aws.AwsManager.getPaginatedProducts
import com.example.rfidstockpro.aws.models.ProductModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import software.amazon.awssdk.services.dynamodb.model.AttributeValue

class InventoryViewModel : ViewModel() {

    private val _products = MutableLiveData<List<ProductModel>>(mutableListOf())
    val products: LiveData<List<ProductModel>> get() = _products

    private val allProducts = mutableListOf<ProductModel>()
    private var lastEvaluatedKey: Map<String, AttributeValue>? = null
    private var isLoading = false
    private var isLastPage = false

    fun loadNextPage() {
        if (isLoading || isLastPage) return
        isLoading = true

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val (newItems, newLastKey) = getPaginatedProducts(lastEvaluatedKey)
                Log.d("DynamoDB", "Fetched items: ${newItems.size}")
                newItems.forEach { product ->
                    Log.d("DynamoDB", "Product: ${product.productName}, SKU: ${product.sku}")
                }
                if (newItems.isNotEmpty()) {
                    allProducts.addAll(newItems)
                    _products.postValue(allProducts)
                    lastEvaluatedKey = newLastKey
                    isLastPage = newLastKey == null
                }else {
                    isLastPage = true
                    Log.d("DynamoDB", "No more products to load.")
                }
            } catch (e: Exception) {
                Log.e("DynamoDB", "Error loading products", e)
            } finally {
                isLoading = false
            }
        }
    }


}
