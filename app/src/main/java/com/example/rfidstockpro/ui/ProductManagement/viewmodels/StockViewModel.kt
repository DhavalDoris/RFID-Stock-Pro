package com.example.rfidstockpro.ui.ProductManagement.viewmodels

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.rfidstockpro.RFIDApplication.Companion.PRODUCT_TABLE
import com.example.rfidstockpro.aws.AwsManager
import com.example.rfidstockpro.aws.AwsManager.getPaginatedProducts
import com.example.rfidstockpro.aws.AwsManager.getProductById
import com.example.rfidstockpro.aws.AwsManager.getTotalProductCount
import com.example.rfidstockpro.aws.models.ProductModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import software.amazon.awssdk.core.internal.http.pipeline.RequestPipelineBuilder.async
import software.amazon.awssdk.services.dynamodb.model.AttributeValue
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll

class StockViewModel : ViewModel() {

    private var filterIds: List<String>? = null

    private val _products = MutableLiveData<List<ProductModel>>(mutableListOf())
    val products: LiveData<List<ProductModel>> get() = _products

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> get() = _isLoading

    private val _totalCount = MutableLiveData<Int>()
    val totalCount: LiveData<Int> get() = _totalCount

    val isDeleting = MutableLiveData<Boolean>(false) // To track delete loading
    val deletionError = MutableLiveData<String?>() // To track errors

    private val allProducts = mutableListOf<ProductModel>()
    private var lastEvaluatedKey: Map<String, AttributeValue>? = null
    private var isLastPage = false

    private val _filteredProducts = MutableLiveData<List<ProductModel>>()
    val filteredProducts: LiveData<List<ProductModel>> = _filteredProducts

    private val currentList = mutableListOf<ProductModel>()
    private var currentPage = 0
    private val pageSize = 10

    fun setTotalItemCount(count: Int) {
        _totalCount.postValue(count)
    }

    fun setFilterProductIds(ids: List<String>) {
        filterIds = ids
        refreshData()
    }

    fun loadFilteredPage(productIds: List<String>) {
        viewModelScope.launch {
            val startIndex = currentPage * pageSize
            val endIndex = minOf(startIndex + pageSize, productIds.size)

            if (startIndex >= productIds.size) return@launch // No more data
            Log.e("productIds_TAG", "loadFilteredPage: " + productIds )
            val pageIds = productIds.subList(startIndex, endIndex)

            val newProducts = getProductsByIds(PRODUCT_TABLE,pageIds)
            currentList.addAll(newProducts)
            _filteredProducts.postValue(currentList)
            currentPage++
        }
    }
    suspend fun getProductsByIds(
        tableName: String,
        productIds: List<String>
    ): List<ProductModel> = coroutineScope {
        productIds.map { id ->
            async {
                getProductById(tableName, id)
            }
        }.awaitAll().filterNotNull()
    }


    fun loadNextPage() {
        if (isLoading.value == true || isLastPage) return
        _isLoading.postValue(true)

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val (newItems, newLastKey) = getPaginatedProducts(lastEvaluatedKey)
                val filteredItems = filterIds?.let {
                    newItems.filter { it.id in filterIds!! }
                } ?: newItems

                Log.d("DynamoDB", "Fetched items: ${newItems.size}")
                newItems.forEach { product ->
                    Log.d("DynamoDB", "Product: ${product.productName}, SKU: ${product.sku}")
                }
                if (filteredItems.isNotEmpty()) {
                    allProducts.addAll(filteredItems)
                    _products.postValue(allProducts)
                    _totalCount.postValue(filterIds?.size ?: getTotalProductCount())
                    lastEvaluatedKey = newLastKey
                    isLastPage = newLastKey == null
                }
                /*if (newItems.isNotEmpty()) {
                    allProducts.addAll(newItems)
                    _products.postValue(allProducts)
                    _totalCount.postValue(getTotalProductCount()) // 👈 add this line
                    lastEvaluatedKey = newLastKey
                    isLastPage = newLastKey == null
                }*/else {
                    isLastPage = true
                    Log.d("DynamoDB", "No more products to load.")
                }
            } catch (e: Exception) {
                Log.e("DynamoDB", "Error loading products", e)
            } finally {
                _isLoading.postValue(false)
            }
        }
    }
    fun deleteProduct(product: ProductModel) {
        isDeleting.value = true
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val result = AwsManager.deleteProduct(product)
                if (result) {
                    // Notify UI of successful deletion
                    refreshData() // Optionally refresh the data to reflect changes
                } else {
                    // Handle failure case (e.g., show a toast)
                    // You might also need to communicate failure to the UI
                    deletionError.value = "Failed to delete product"
                }
            } catch (e: Exception) {
                // Handle any exceptions, show error message, etc.
                isDeleting.value = false
                deletionError.value = "Error occurred: ${e.message}"
            }
        }
    }
    fun resetFilteredPagination() {
        currentPage = 0
        currentList.clear()
        _filteredProducts.postValue(emptyList())
    }
    fun refreshData() {
        lastEvaluatedKey = null
        isLastPage = false
        allProducts.clear()

        // ✅ Reset both product list and total count
        _products.postValue(emptyList())
        _totalCount.postValue(0)

        // Load first page again
        loadNextPage()
    }
}
