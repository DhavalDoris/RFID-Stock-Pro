package com.example.rfidstockpro.inouttracker.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.rfidstockpro.RFIDApplication.Companion.IN_OUT_COLLECTIONS_TABLE
import com.example.rfidstockpro.aws.AwsManager
import com.example.rfidstockpro.inouttracker.model.CollectionModel
import com.example.rfidstockpro.inouttracker.model.toCollectionModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import software.amazon.awssdk.services.dynamodb.model.AttributeValue
import software.amazon.awssdk.services.dynamodb.model.ScanRequest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CreateCollectionViewModel : ViewModel() {

    private val _isCollectionCreated = MutableLiveData<Boolean>()
    val isCollectionCreated: LiveData<Boolean> = _isCollectionCreated

    private val _collections = MutableLiveData<List<CollectionModel>>()
    val collections: LiveData<List<CollectionModel>> get() = _collections

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> get() = _isLoading

    fun createCollection(
        collectionName: String,
        description: String,
        productIds: List<String>,
        userId: String
    ) {
        if (collectionName.isBlank()) {
            _isCollectionCreated.postValue(false)
            return
        }

        val dateTimeFormat = SimpleDateFormat("dd/MM/yy hh:mm a", Locale.getDefault())
        val currentTime = dateTimeFormat.format(Date())

        val collection = CollectionModel(
            collectionName = collectionName,
            description = description,
            productIds = productIds,
            createdDateTime = currentTime,
            updatedDateTime = currentTime,
            userId = userId
        )

        viewModelScope.launch(Dispatchers.IO) {
            try {
                AwsManager.addCollectionToDynamoDB(collection)
                _isCollectionCreated.postValue(true)
            } catch (e: Exception) {
                Log.e("InOutTracker", "Failed to create collection", e)
                _isCollectionCreated.postValue(false)
            }
        }
    }

    fun fetchCollections(userId: String) {
        Log.d("CollectionListVM", "Fetching collections for userId: $userId")
        _isLoading.value = true

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val expressionAttributeValues = mapOf(
                    ":userId" to AttributeValue.builder().s(userId).build()
                )

                val scanRequest = ScanRequest.builder()
                    .tableName(IN_OUT_COLLECTIONS_TABLE)
                    .filterExpression("userId = :userId")
                    .expressionAttributeValues(expressionAttributeValues)
                    .build()

                val response = AwsManager.dynamoDBClient.scan(scanRequest)
                val items = response.items().map { it.toCollectionModel() }
                Log.d("CollectionListVM", "Fetched ${items.size} collections")
                withContext(Dispatchers.Main) {
                    _collections.value = items
                    _isLoading.value = false
                }
            } catch (e: Exception) {
                Log.e("CollectionListVM", "Error fetching collections: ${e.message}")
                withContext(Dispatchers.Main) {
                    _isLoading.value = false
                }
            }
        }
    }
}
