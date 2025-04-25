import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.rfidstockpro.aws.AwsManager
import com.example.rfidstockpro.aws.AwsManager.getAllProducts
import com.example.rfidstockpro.aws.models.ProductModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import software.amazon.awssdk.services.dynamodb.model.AttributeValue

class InventoryProductsViewModel : ViewModel() {

    private val _pagedProducts = MutableLiveData<List<ProductModel>>()
    val pagedProducts: LiveData<List<ProductModel>> get() = _pagedProducts

    private val _totalCount = MutableLiveData<Int>()
    val totalCount: LiveData<Int> get() = _totalCount

    private val _isPageLoading = MutableLiveData<Boolean>(false)
    val isPageLoading: LiveData<Boolean> get() = _isPageLoading

    val isDeleting = MutableLiveData<Boolean>(false) // To track delete loading
    val deletionError = MutableLiveData<String?>() // To track errors

    private val pageSize = 5
    private var currentPage = 0
    private var matchedTagIds: List<String> = emptyList()

    private var allMatchingProducts = mutableListOf<ProductModel>()
    private var lastEvaluatedKey: Map<String, AttributeValue>? = null
    private var isLastPageFromDB = false
    private var isFetchingFromDB = false

    fun filterOutProductsByIds(productIds: List<String>) {
        allMatchingProducts = allMatchingProducts.filterNot { it.id in productIds }.toMutableList()
        _pagedProducts.value = allMatchingProducts.take(pageSize) // Update the paged list based on filtered products
        _totalCount.value = allMatchingProducts.size
    }

    fun setMatchedTagIds(tagIds: List<String>) {
        matchedTagIds = tagIds
        allMatchingProducts.clear()
        _pagedProducts.value = emptyList() // Clear UI items
        currentPage = 0
        lastEvaluatedKey = null
        isLastPageFromDB = false
        fetchMoreMatchingProducts()
    }

    fun deleteProduct(product: ProductModel) {
        isDeleting.postValue(true)
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val result = AwsManager.deleteProduct(product)
                if (result) {
                    // Notify UI of successful deletion
                    loadNextPage() // Optionally refresh the data to reflect changes
                } else {
                    // Handle failure case (e.g., show a toast)
                    // You might also need to communicate failure to the UI
                    deletionError.postValue( "Failed to delete product")
                }
            } catch (e: Exception) {
                // Handle any exceptions, show error message, etc.
                deletionError.postValue("Error occurred: ${e.message}")
            }finally {
                isDeleting.postValue(false)
            }
        }
    }
    private fun fetchMoreMatchingProducts() {
        if (isFetchingFromDB || isLastPageFromDB) return
        isFetchingFromDB = true
        _isPageLoading.postValue(true)
        viewModelScope.launch(Dispatchers.IO) {
            do {
                val (products, newLastKey) = getAllProducts(lastEvaluatedKey)
                lastEvaluatedKey = newLastKey
                if (newLastKey == null) isLastPageFromDB = true

                val matched = products.filter { it.tagId in matchedTagIds }
                allMatchingProducts.addAll(matched)

                allMatchingProducts = allMatchingProducts.distinctBy { it.id }.toMutableList()

            } while (allMatchingProducts.size < (currentPage + 1) * pageSize && !isLastPageFromDB)

            val nextPage = allMatchingProducts.drop(currentPage * pageSize).take(pageSize)
            withContext(Dispatchers.Main) {
                viewModelScope.launch {
                    delay(2000)
                    _isPageLoading.value = false
                    val currentList = _pagedProducts.value.orEmpty()
                    val newList = (currentList + nextPage).distinctBy { it.id }
                    _pagedProducts.value = newList
                    _totalCount.value = allMatchingProducts.size
                    isFetchingFromDB = false
                }
                currentPage++
            }
        }
    }

    fun loadNextPage() {
        val nextPage = allMatchingProducts.drop(currentPage * pageSize).take(pageSize)
        _isPageLoading.value = true
        if (nextPage.isNotEmpty()) {
            val currentList = _pagedProducts.value.orEmpty()
            val newList = (currentList + nextPage).distinctBy { it.id }
            _pagedProducts.value = newList
            _isPageLoading.value = false
            currentPage++
        } else if (!isLastPageFromDB) {
            fetchMoreMatchingProducts()
        }
    }
}
