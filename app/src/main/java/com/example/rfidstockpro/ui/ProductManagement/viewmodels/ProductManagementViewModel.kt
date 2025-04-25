package com.example.rfidstockpro.ui.ProductManagement.viewmodels

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.rfidstockpro.aws.AwsManager
import com.example.rfidstockpro.inouttracker.model.CollectionModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ProductManagementViewModel(application: Application) : AndroidViewModel(application) {

    private val _isCollectionCreated = MutableLiveData<Boolean>()
    val isCollectionCreated: LiveData<Boolean> = _isCollectionCreated


    fun createCollection(
        collectionName: String,
        description: String,
        productIds: List<String>,
        userId: String
    ) {
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
}
