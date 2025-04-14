package com.example.rfidstockpro.viewmodel

import android.content.Context
import android.os.SystemClock
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.rfidstockpro.RFIDApplication.Companion.PRODUCT_TABLE
import com.example.rfidstockpro.aws.AwsManager
import com.example.rfidstockpro.aws.models.ProductModel
import com.example.rfidstockpro.data.UHFTagModel
import com.example.rfidstockpro.repository.UHFRepository
import com.example.rfidstockpro.ui.ProductManagement.RFIDTagManager
import com.example.rfidstockpro.ui.activities.DashboardActivity.Companion.uhfDevice
import com.example.rfidstockpro.ui.activities.DeviceListActivity.TAG
import com.rscja.deviceapi.entity.UHFTAGInfo
import com.rscja.deviceapi.interfaces.ConnectionStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.CopyOnWriteArrayList

class UHFReadViewModel(private val uhfRepository: UHFRepository) : ViewModel() {
    var isKeyDownUP = false
    private val _tagList = MutableLiveData<List<UHFTagModel>>(CopyOnWriteArrayList())
    val tagList: LiveData<List<UHFTagModel>> = _tagList

    private val _totalTagCount = MutableLiveData(0)
    val totalTagCount: LiveData<Int> = _totalTagCount

    private val _isScanning = MutableLiveData(false)
    val isScanning: LiveData<Boolean> = _isScanning

    private val _useTime = MutableLiveData(0.0)
    val useTime: LiveData<Double> = _useTime

    private val _connectionStatus = MutableLiveData<ConnectionStatus>()
    val connectionStatus: LiveData<ConnectionStatus> = _connectionStatus

    fun startInventory(maxRunTime: Int = 30000, needPhase: Boolean = false) {
        viewModelScope.launch {
            _isScanning.value = true

            clearData()

            uhfRepository.startInventory(
                maxRunTime = maxRunTime,
                needPhase = needPhase
            ) { uhfTagInfo ->
                // Use viewModelScope to switch to main thread
                viewModelScope.launch {
                    addTagToList(uhfTagInfo)
                }
            }

        }
    }

    fun stopInventory() {
        viewModelScope.launch {
            val result = uhfRepository.stopInventory()
            _isScanning.value = false
        }
    }

    fun singleInventory() {
        viewModelScope.launch {
            val tagInfo = uhfRepository.inventorySingleTag()
            tagInfo?.let {
                viewModelScope.launch {
                    addTagToList(it)
                }
            }
        }
    }

    fun handleKeyDown(keyCode: Int) {
        Log.d("KEY_TAG", "1")
        if (uhfDevice.connectStatus == ConnectionStatus.CONNECTED) {
            Log.d("KEY_TAG", "2")
            when (keyCode) {
                3 -> {  // Example key for continuous scan
                    isKeyDownUP = true
                    startInventory()
                    Log.d("KEY_TAG", "3")
                }
                1 -> {  // Example key for start/stop scan
                    if (!isKeyDownUP) {
                        if (_isScanning.value == true) {
                            stopInventory()
                        } else {
                            startInventory()
                        }
                    }
                    Log.d("KEY_TAG", "2")
                }
                2 -> {  // Example key for single inventory
                    if (_isScanning.value == true) {
                        stopInventory()
                        SystemClock.sleep(100)
                    }
                    Log.d("KEY_TAG", "3")
                    singleInventory()
                }
            }
        }
    }

    fun handleKeyUp(keyCode: Int) {
        if (keyCode == 4) { // Stop scanning on key release
            stopInventory()
        }
    }

    private suspend  fun addTagToList(uhfTagInfo: UHFTAGInfo) {
        val epc = uhfTagInfo.epc ?: return

        // Check with DynamoDB if this tag already exists
        val (exists, message) = AwsManager.checkIfTagIdExists(PRODUCT_TABLE, epc)
        if (exists) {
            Log.d("TAG_FILTER", "Skipping tag $epc: $message")
            return
        }

        // Ensure this runs on the main thread
        withContext(Dispatchers.Main) {
            val currentList = _tagList.value?.toMutableList() ?: mutableListOf()

            val existingTag = currentList.find { it.epc == uhfTagInfo.epc }

            if (existingTag != null) {
                val updatedList = currentList.map { tag ->
                    if (tag.epc == uhfTagInfo.epc) {
                        tag.copy(
                            rssi = uhfTagInfo.rssi,
                            phase = uhfTagInfo.phase.toDouble(),
                            count = tag.count + 1
                        )
                    } else tag
                }
                _tagList.value = updatedList
            } else {
                val newTag = UHFTagModel(
                    reserved = uhfTagInfo.reserved ?: "",
                    epc = uhfTagInfo.epc ?: "",
                    tid = uhfTagInfo.tid ?: "",
                    user = uhfTagInfo.user ?: "",
                    rssi = uhfTagInfo.rssi,
                    phase = uhfTagInfo.phase.toDouble()
                )
                currentList.add(newTag)
                _tagList.value = currentList
            }

            _totalTagCount.value = (_totalTagCount.value ?: 0) + 1
        }
    }

    fun clearData() {
        _tagList.value = mutableListOf()
        _totalTagCount.value = 0
        _useTime.value = 0.0
    }

    fun setFilter(
        filterBank: Int,
        ptr: Int,
        len: Int,
        data: String
    ): Boolean {
        return uhfRepository.setFilter(filterBank, ptr, len, data)
    }

    fun addProductToAWS(
        context: Context,
        product: ProductModel, // or whatever your product class is
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val scope = CoroutineScope(Dispatchers.IO)

        scope.launch {
            val (exists, message) = AwsManager.checkIfTagIdExists(PRODUCT_TABLE, product.tagId)
            if (exists) {
                withContext(Dispatchers.Main) { onError(message) }
                return@launch
            }

            val imageFiles = product.selectedImages.map { File(it) }
            val videoFile = product.selectedVideo?.let { File(it) }

            AwsManager.uploadMediaToS3(
                scope = scope,
                context = context,
                imageFiles = imageFiles,
                videoFile = videoFile,
                onSuccess = { imageUrls, videoUrl ->
                    val updatedProduct = product.copy(
                        selectedImages = imageUrls,
                        selectedVideo = videoUrl
                    )
                    Log.e("AWS_TAG", "imageUrls: " + imageUrls)
                    Log.e("AWS_TAG", "videoUrl: " + videoUrl)

                    scope.launch {
                        val (isSuccess, saveMessage) = AwsManager.saveProduct(PRODUCT_TABLE, updatedProduct)
                        withContext(Dispatchers.Main) {
                            if (isSuccess) {
                                onSuccess()
                            } else {
                                // 🔥 Clean up uploaded media if save fails
                                scope.launch {
                                    AwsManager.deleteMediaFromS3(
                                        imageUrls = imageUrls,
                                        videoUrl = videoUrl
                                    )
                                }

                                onError("Failed to save to DynamoDB: $saveMessage")
//                                onError(saveMessage)

                            }
                        }
                    }
                },
                onError = { errorMessage ->
                    scope.launch {
                        withContext(Dispatchers.Main) { onError(errorMessage) }
                    }
                }
            )
        }
    }

    fun startRFIDScan() {
        viewModelScope.launch {
            uhfRepository.startInventory { tag ->
                RFIDTagManager.addTag(tag.epc ?: "")
            }
        }
    }

}