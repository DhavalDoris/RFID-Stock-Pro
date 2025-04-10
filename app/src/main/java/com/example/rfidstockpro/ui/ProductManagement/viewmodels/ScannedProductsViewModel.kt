import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.rfidstockpro.aws.AwsManager.getAllProducts
import com.example.rfidstockpro.aws.models.ProductModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import software.amazon.awssdk.services.dynamodb.model.AttributeValue

class ScannedProductsViewModel : ViewModel() {

    private val _pagedProducts = MutableLiveData<List<ProductModel>>()
    val pagedProducts: LiveData<List<ProductModel>> get() = _pagedProducts

    private val _totalCount = MutableLiveData<Int>()
    val totalCount: LiveData<Int> get() = _totalCount

    private val _isPageLoading = MutableLiveData<Boolean>(false)
    val isPageLoading: LiveData<Boolean> get() = _isPageLoading

    private val pageSize = 5
    private var currentPage = 0
    private var matchedTagIds: List<String> = emptyList()

    private var allMatchingProducts = mutableListOf<ProductModel>()
    private var lastEvaluatedKey: Map<String, AttributeValue>? = null
    private var isLastPageFromDB = false
    private var isFetchingFromDB = false

    fun setMatchedTagIds(tagIds: List<String>) {
        matchedTagIds = tagIds
        allMatchingProducts.clear()
        _pagedProducts.value = emptyList() // Clear UI items
        currentPage = 0
        lastEvaluatedKey = null
        isLastPageFromDB = false
        fetchMoreMatchingProducts()
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
