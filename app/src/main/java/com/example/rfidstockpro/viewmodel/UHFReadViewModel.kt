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


    fun addOrUpdateProductToAWS(
        context: Context,
        product: ProductModel,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        Log.e("UPDATE_PRODUCT_TAG", "addOrUpdateProductToAWS:id " + product.id )
        Log.e("UPDATE_PRODUCT_TAG", "addOrUpdateProductToAWS:tag_id " + product.tagId )
        if (product.tagId!!.isNotEmpty()) {
            updateProductInAWS(context, product, onSuccess, onError)
        } else {
            addProductToAWS(context, product, onSuccess, onError)
        }
    }


    fun addProductToAWS(
        context: Context,
        product: ProductModel,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val scope = CoroutineScope(Dispatchers.IO)
        scope.launch {
            Log.d("AWS_PRODUCT", "Checking if tagId '${product.tagId}' already exists...")

            val (exists, message) = AwsManager.checkIfTagIdExists(PRODUCT_TABLE, product.tagId)
            if (exists) {
                Log.w("AWS_PRODUCT", "❌ Tag already exists: $message")
                withContext(Dispatchers.Main) { onError(message) }
                return@launch
            }

            Log.d("AWS_PRODUCT", "✅ TagId is unique. Proceeding...")

            val imageFiles = product.selectedImages.map { File(it) }
            val videoFile = product.selectedVideo?.let { File(it) }

            Log.d("AWS_PRODUCT", "Uploading media to S3...")
            AwsManager.uploadMediaToS3(
                scope = scope,
                context = context,
                imageFiles = imageFiles,
                videoFile = videoFile,
                onSuccess = { imageUrls, videoUrl ->
                    Log.d("AWS_PRODUCT", "✅ Media uploaded to S3 successfully.")
                    Log.d("AWS_PRODUCT", "Images: $imageUrls")
                    Log.d("AWS_PRODUCT", "Video: $videoUrl")

                    val updatedProduct = product.copy(
                        selectedImages = imageUrls,
                        selectedVideo = videoUrl
                    )

                    scope.launch {
                        Log.d("AWS_PRODUCT", "Saving product to DynamoDB...")
                        val (isSuccess, saveMessage) = AwsManager.saveProduct(PRODUCT_TABLE, updatedProduct)
                        withContext(Dispatchers.Main) {
                            if (isSuccess) {
                                Log.i("AWS_PRODUCT", "✅ Product saved to DynamoDB successfully.")
                                onSuccess()
                            } else {
                                Log.e("AWS_PRODUCT", "❌ Failed to save product to DynamoDB. Cleaning up S3 uploads.")
                                scope.launch {
                                    AwsManager.deleteMediaFromS3(
                                        imageUrls = imageUrls,
                                        videoUrl = videoUrl
                                    )
                                    Log.i("AWS_PRODUCT", "🧹 Cleaned up uploaded media from S3.")
                                }
                                onError("Failed to save to DynamoDB: $saveMessage")
                            }
                        }
                    }
                },
                onError = { errorMessage ->
                    Log.e("AWS_PRODUCT", "❌ Failed to upload media to S3: $errorMessage")
                    scope.launch {
                        withContext(Dispatchers.Main) { onError(errorMessage) }
                    }
                }
            )
        }
    }


   /* fun updateProductInAWS(
        context: Context,
        product: ProductModel,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val scope = CoroutineScope(Dispatchers.IO)

        scope.launch {
            Log.d("AWS_UPDATE", "Starting product update for ID: ${product.id}")

            // If no media changes, directly update product
            if (!product.isMediaUpdated) {
                Log.d("AWS_UPDATE", "No media updated. Directly updating product in DynamoDB...")

                val (isSuccess, saveMessage) = AwsManager.saveProduct(PRODUCT_TABLE, product)
                withContext(Dispatchers.Main) {
                    if (isSuccess) {
                        Log.i("AWS_UPDATE", "✅ Product updated successfully without media changes.")
                        onSuccess()
                    } else {
                        Log.e("AWS_UPDATE", "❌ Failed to update product in DynamoDB: $saveMessage")
                        onError("Failed to update product: $saveMessage")
                    }
                }
                return@launch
            }

            // Media has changed, so upload new and delete old after success
            val oldImageUrls = product.selectedImages
            val oldVideoUrl = product.selectedVideo

            val imageFiles = product.selectedImages.map { File(it) }
//            val videoFile = product.selectedVideo?.let { File(it) }

//            val imageFiles = product.newImageFiles.map { File(it) }
//            val videoFile = product.newVideoFile?.let { File(it) }


            val videoFile = product.selectedVideo?.takeIf {
                !it.startsWith("http") && File(it).exists() && File(it).length() > 0
            }?.let { File(it) }

            videoFile?.let {
                Log.d("UPLOAD_VIDEO", "Video Path: ${it.absolutePath}")
                Log.d("UPLOAD_VIDEO", "Video Exists: ${it.exists()}, Size: ${it.length()} bytes")
            } ?: Log.d("UPLOAD_VIDEO", "No valid video file to upload.")
            // ✅ Check for file existence and size BEFORE uploading

            val validImageFiles = imageFiles.filter { it.exists() && it.length() > 0 }
            val validVideoFile = videoFile?.takeIf { it.exists() && it.length() > 0 }

            // Log sizes for testing
            validImageFiles.forEachIndexed { index, file ->
                Log.d("UPLOAD_DEBUG", "Image $index: ${file.absolutePath}, Size: ${file.length()} bytes")
            }
            validVideoFile?.let {
                Log.d("UPLOAD_DEBUG", "Video: ${it.absolutePath}, Size: ${it.length()} bytes")
            }

            if (validImageFiles.isEmpty() && validVideoFile == null) {
                withContext(Dispatchers.Main) {
                    onError("❌ No valid media files to upload (files might be empty or missing)")
                }
                return@launch
            }

            Log.d("AWS_UPDATE", "Uploading new media to S3...")
            Log.d("AWS_UPDATE", "==>  $imageFiles")
            Log.d("AWS_UPDATE", "~~>  $videoFile")
            AwsManager.uploadMediaToS3(
                scope = scope,
                context = context,
                imageFiles = imageFiles,
                videoFile = videoFile,
                onSuccess = { newImageUrls, newVideoUrl ->
                    Log.d("AWS_UPDATE", "✅ New media uploaded to S3.")
                    Log.d("AWS_UPDATE", "New images: $newImageUrls")
                    Log.d("AWS_UPDATE", "New video: $newVideoUrl")

                    val updatedProduct = product.copy(
                        selectedImages = newImageUrls,
                        selectedVideo = newVideoUrl,
                        isMediaUpdated = false // Reset flag after update
                    )

                    scope.launch {
                        Log.d("AWS_UPDATE", "Updating product in DynamoDB...")
                        val (isSuccess, saveMessage) = AwsManager.saveProduct(PRODUCT_TABLE, updatedProduct)
                        withContext(Dispatchers.Main) {
                            if (isSuccess) {
                                Log.i("AWS_UPDATE", "✅ Product updated in DynamoDB.")
                                // Delete old media
                                scope.launch {
                                    AwsManager.deleteMediaFromS3(
                                        imageUrls = oldImageUrls,
                                        videoUrl = oldVideoUrl
                                    )
                                    Log.i("AWS_UPDATE", "🧹 Deleted old media from S3.")
                                }
                                onSuccess()
                            } else {
                                Log.e("AWS_UPDATE", "❌ DynamoDB update failed. Rolling back new media.")
                                // Clean up new uploads
                                scope.launch {
                                    AwsManager.deleteMediaFromS3(
                                        imageUrls = newImageUrls,
                                        videoUrl = newVideoUrl
                                    )
                                    Log.i("AWS_UPDATE", "🧹 Rolled back new media from S3.")
                                }
                                onError("Failed to update DynamoDB: $saveMessage")
                            }
                        }
                    }
                },
                onError = { errorMessage ->
                    Log.e("AWS_UPDATE", "❌ Failed to upload new media: $errorMessage")
                    scope.launch {
                        withContext(Dispatchers.Main) { onError(errorMessage) }
                    }
                }
            )
        }
    }*/


    fun updateProductInAWS(
        context: Context,
        product: ProductModel,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val scope = CoroutineScope(Dispatchers.IO)

        scope.launch {
            Log.d("AWS_UPDATE", "🔄 Starting product update for ID: ${product.id}")

            if (!product.isMediaUpdated) {
                // No media update, just update product
                val (isSuccess, saveMessage) = AwsManager.saveProduct(PRODUCT_TABLE, product)
                withContext(Dispatchers.Main) {
                    if (isSuccess) {
                        Log.i("AWS_UPDATE", "✅ Product updated without media changes.")
                        onSuccess()
                    } else {
                        Log.e("AWS_UPDATE", "❌ Failed to update product: $saveMessage")
                        onError("Failed to update product: $saveMessage")
                    }
                }
                return@launch
            }

            // Separate URLs and local files
            val oldImageUrls = product.selectedImages.filter { it.startsWith("http") }
            val oldVideoUrl = product.selectedVideo?.takeIf { it.startsWith("http") }

            val imageFiles = product.selectedImages
                .filter { it.startsWith("/") || it.startsWith("content://") }
                .map { File(it) }
                .filter { it.exists() && it.length() > 0 }

            val videoFile = product.selectedVideo?.takeIf {
                it.startsWith("/") && File(it).exists() && File(it).length() > 0
            }?.let { File(it) }

            if (imageFiles.isEmpty() && videoFile == null) {
                withContext(Dispatchers.Main) {
                    onError("❌ No valid new media selected to upload.")
                }
                return@launch
            }

            Log.d("AWS_UPDATE", "⬆️ Uploading media to S3...")

            AwsManager.uploadMediaToS3(
                scope = scope,
                context = context,
                imageFiles = imageFiles,
                videoFile = videoFile,
                onSuccess = { newImageUrls, newVideoUrl ->
                    Log.d("AWS_UPDATE", "✅ Media uploaded.")
                    Log.d("AWS_UPDATE", "🖼️ New image URLs: $newImageUrls")
                    Log.d("AWS_UPDATE", "🎥 New video URL: $newVideoUrl")

                    // Merge old + new media
                    val updatedProduct = product.copy(
                        selectedImages = oldImageUrls + newImageUrls,
                        selectedVideo = newVideoUrl ?: product.selectedVideo,
                        isMediaUpdated = false
                    )

                    scope.launch {
                        val (isSuccess, saveMessage) = AwsManager.saveProduct(PRODUCT_TABLE, updatedProduct)
                        withContext(Dispatchers.Main) {
                            if (isSuccess) {
                                Log.i("AWS_UPDATE", "✅ Product saved to DynamoDB.")

                                // 🧹 Delete old media only if replaced
                                scope.launch {
                                    if (newImageUrls.isNotEmpty() || newVideoUrl != null) {
                                        AwsManager.deleteMediaFromS3(
                                            imageUrls = if (newImageUrls.isNotEmpty()) oldImageUrls else emptyList(),
                                            videoUrl = if (newVideoUrl != null) oldVideoUrl else null
                                        )
                                        Log.i("AWS_UPDATE", "🧹 Cleaned old media from S3.")
                                    }
                                }
                                onSuccess()
                            } else {
                                Log.e("AWS_UPDATE", "❌ Product save failed. Rolling back media.")
                                scope.launch {
                                    AwsManager.deleteMediaFromS3(
                                        imageUrls = newImageUrls,
                                        videoUrl = newVideoUrl
                                    )
                                }
                                onError("Failed to update product: $saveMessage")
                            }
                        }
                    }
                },
                onError = { errorMessage ->
                    Log.e("AWS_UPDATE", "❌ Upload error: $errorMessage")
                    scope.launch {
                        onError(errorMessage)
                    }
                }
            )
        }
    }



    /*fun addProductToAWS(
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

            // ✅ If no new media selected, just update product in DynamoDB
            if (!product.isMediaUpdated) {
                val (isSuccess, saveMessage) = AwsManager.saveProduct(PRODUCT_TABLE, product)
                withContext(Dispatchers.Main) {
                    if (isSuccess) onSuccess() else onError("Failed to update product: $saveMessage")
                }
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
    }*/

    fun startRFIDScan() {
        viewModelScope.launch {
            uhfRepository.startInventory { tag ->
                RFIDTagManager.addTag(tag.epc ?: "")
            }
        }
    }

}