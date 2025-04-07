package com.example.rfidstockpro.ui.activities

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Intent
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.util.Log
import android.view.View
import android.webkit.MimeTypeMap
import androidx.activity.viewModels
import androidx.annotation.RequiresPermission
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModelProvider
import com.example.rfidstockpro.R
import com.example.rfidstockpro.Utils.FragmentManagerHelper
import com.example.rfidstockpro.Utils.StatusBarUtils
import com.example.rfidstockpro.Utils.ToastUtils.showToast
import com.example.rfidstockpro.aws.models.ProductModel
import com.example.rfidstockpro.databinding.ActivityAddItemBinding
import com.example.rfidstockpro.ui.activities.DashboardActivity.Companion.uhfDevice
import com.example.rfidstockpro.ui.activities.DeviceListActivity.TAG
import com.example.rfidstockpro.ui.fragments.UHFReadFragment
import com.example.rfidstockpro.viewmodel.AddItemViewModel
import com.example.rfidstockpro.viewmodel.DashboardViewModel
import com.example.rfidstockpro.viewmodel.DashboardViewModel.Companion.SHOW_HISTORY_CONNECTED_LIST
import com.example.rfidstockpro.viewmodel.SharedProductViewModel
import com.google.android.material.snackbar.Snackbar
import com.rscja.deviceapi.RFIDWithUHFBLE
import com.rscja.deviceapi.interfaces.ConnectionStatus
import com.rscja.deviceapi.interfaces.ConnectionStatusCallback
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class AddItemActivity : AppCompatActivity(), UHFReadFragment.UHFDeviceProvider {

    private val selectedImageFiles = mutableListOf<File>()


    private lateinit var binding: ActivityAddItemBinding
    private val addItemViewModel: AddItemViewModel by viewModels()
    private var isImageSelected: Boolean = false // Add this flag
    private var selectedImage: Uri? = null  // Global Image URI
    private var selectedVideo: Uri? = null  // Global Video URI

    private lateinit var imageFile: File
    private lateinit var videoFile: File

    private val dashboardViewModel: DashboardViewModel by viewModels()
    var mBtAdapter: BluetoothAdapter? = null
    private var mDevice: BluetoothDevice? = null

    companion object {
        private const val REQUEST_PICK_IMAGE = 1001
        private const val REQUEST_PICK_VIDEO = 1002
        private const val REQUEST_CAPTURE_IMAGE = 1003
        private const val REQUEST_CAPTURE_VIDEO = 1004
        private val REQUEST_ENABLE_BT: Int = 22
        private val REQUEST_SELECT_DEVICE: Int = 11
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddItemBinding.inflate(layoutInflater)
        setContentView(binding.root)
        StatusBarUtils.setStatusBarColor(this)
        initUI()
        observeValidationErrors()
    }

    private fun initUI() {
        mBtAdapter = BluetoothAdapter.getDefaultAdapter()
        binding.commonToolbar.tvToolbarTitle.text = getString(R.string.add_item)

        // Button Click Listeners
        binding.btnAdd.setOnClickListener {
            validateAndLogFields()
        }

        binding.btnAddScan.setOnClickListener {

            if (uhfDevice.connectStatus == ConnectionStatus.CONNECTED) {
                validateAndLogFields()
            }
            else{
                binding.rlStatScan.visibility = View.VISIBLE
                Log.d("ADD_ITEM", "Show Connecting screen===>>>===>>>")
            }

        }

        binding.selectedImagesContainer.setOnClickListener {
            showMediaPickerDialog(isImage = true)
        }

        binding.selectVideo.setOnClickListener {
            showMediaPickerDialog(isImage = false)
        }

        binding.btnConnectScannerAdd.setOnClickListener {
            if (dashboardViewModel.isConnected.value == true) {
                dashboardViewModel.disconnect(true)
            } else {
                showBluetoothDevice()
            }
        }
    }

    fun updateToolbarTitleAddItem(title: String) {
        val toolbarTitle = findViewById<AppCompatTextView>(R.id.tvToolbarTitle)
        val toolbarSearch = findViewById<AppCompatImageView>(R.id.ivSearch)
        val toolbarInvoice = findViewById<AppCompatImageView>(R.id.ivInvoice)
        Log.e(TAG, "updateToolbarTitle: ")
        toolbarTitle!!.text = title

        toolbarSearch.visibility = View.GONE
        toolbarInvoice.visibility = View.VISIBLE
    }

    /**
     * Shows a dialog to choose between Camera or Gallery
     */
    private fun showMediaPickerDialog(isImage: Boolean) {
        val options = arrayOf("Capture with Camera", "Select from Gallery")
        AlertDialog.Builder(this)
            .setTitle(if (isImage) "Select Image" else "Select Video")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> if (isImage) captureImage() else recordVideo()
                    1 -> if (isImage) pickImageFromGallery() else pickVideoFromGallery()
                }
            }.show()
    }

    private fun pickImageFromGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
//        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        intent.type = "image/*"
        startActivityForResult(intent, REQUEST_PICK_IMAGE)
    }

    private fun pickVideoFromGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
        intent.type = "video/*"
        startActivityForResult(intent, REQUEST_PICK_VIDEO)
    }

    private fun captureImage() {
        val imageIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        imageFile = File(externalCacheDir,  "image_${System.currentTimeMillis()}.jpg")
        selectedImage = FileProvider.getUriForFile(this, "$packageName.fileprovider", imageFile)
        imageIntent.putExtra(MediaStore.EXTRA_OUTPUT, selectedImage)
        startActivityForResult(imageIntent, REQUEST_CAPTURE_IMAGE)
    }

    private fun recordVideo() {
        val videoIntent = Intent(MediaStore.ACTION_VIDEO_CAPTURE)
        videoFile = File(externalCacheDir, "video_${System.currentTimeMillis()}.mp4")
        selectedVideo = FileProvider.getUriForFile(this, "$packageName.fileprovider", videoFile)

        videoIntent.putExtra(MediaStore.EXTRA_OUTPUT, selectedVideo)
        videoIntent.putExtra(MediaStore.EXTRA_DURATION_LIMIT, 60) // 60 sec max
        startActivityForResult(videoIntent, REQUEST_CAPTURE_VIDEO)
    }

    private fun observeValidationErrors() {
        addItemViewModel.validationError.observe(this) { error ->
            error?.let {
                Snackbar.make(binding.main, it, Snackbar.LENGTH_SHORT).show()
                addItemViewModel.clearValidationError()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                REQUEST_PICK_IMAGE -> {
                    selectedImageFiles.clear()
                    if (data?.clipData != null) {
                        for (i in 0 until data.clipData!!.itemCount) {
                            val uri = data.clipData!!.getItemAt(i).uri
                            val filePath = getRealPathFromUriNew(uri)
                            val file = File(filePath)
                            if (validateFileSize(uri, isImage = true)) {
                                selectedImageFiles.add(file)
                                Log.d("IMAGE_SELECTION", "Selected Image: ${file.absolutePath}")
                            }
                        }
                    } else if (data?.data != null) {
                        val uri = data.data!!
                        val filePath = getRealPathFromUriNew(uri)
                        val file = File(filePath)
                        if (validateFileSize(uri, isImage = true)) {
                            selectedImageFiles.add(file)
                            Log.d("IMAGE_SELECTION", "Selected Image: ${file.absolutePath}")
                        }
                    }

                    if (selectedImageFiles.isNotEmpty()) {
                        binding.selectedImagesContainer.setImageURI(Uri.fromFile(selectedImageFiles[0]))
                        isImageSelected = true
                        binding.changeImage.visibility = View.VISIBLE
                    }
                }

                REQUEST_CAPTURE_IMAGE -> {
                    selectedImage?.let {
                        val filePath = getRealPathFromUriNew(it)
                        val file = File(filePath)
                        if (validateFileSize(it, isImage = true)) {
                            selectedImageFiles.add(file)
                            Log.d("IMAGE_SELECTION", "Captured Image: ${file.absolutePath}")
                            binding.selectedImagesContainer.setImageURI(Uri.fromFile(file))
                            isImageSelected = true
                            binding.changeImage.visibility = View.VISIBLE
                        }
                    }
                }

                REQUEST_PICK_VIDEO, REQUEST_CAPTURE_VIDEO -> {
                    selectedVideo = data?.data ?: selectedVideo
                    selectedVideo?.let {
                        if (validateFileSize(it, isImage = false)) {
                            val videoPath = getRealPathFromUriNew(it)
                            Log.d("VIDEO_SELECTION", "Selected Video: $videoPath")
                            val retriever = MediaMetadataRetriever()
                            retriever.setDataSource(this, it)
                            val bitmap = retriever.frameAtTime
                            binding.selectedVideoContainer.setImageBitmap(bitmap)
                            isImageSelected = true
                            binding.changeVideo.visibility = View.VISIBLE
                        } else {
                            selectedVideo = null
                        }
                    }
                }

                REQUEST_SELECT_DEVICE -> {
                    if (resultCode == Activity.RESULT_OK && data != null) {
                        val deviceAddress = data.getStringExtra(BluetoothDevice.EXTRA_DEVICE)
                        if (!deviceAddress.isNullOrEmpty()) {
                            mDevice = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(deviceAddress)
                            connectToDevice(deviceAddress)
                        }
                    }
                }

                REQUEST_ENABLE_BT -> {
                    if (resultCode == Activity.RESULT_OK) {
                        showBluetoothDevice()
//                    showToast(dashboardActivity!!, "Bluetooth has turned on")
                    } else {
//                    showToast(dashboardActivity!!, "Problem in BT Turning ON")
                    }
                }
            }
        }
    }

    private fun connectToDevice(deviceAddress: String) {
        if (uhfDevice.connectStatus == ConnectionStatus.CONNECTING) {
            showToast(this, getString(R.string.connecting))
        } else {
//            showToast(this, "Connecting to $deviceAddress")
            uhfDevice.connect(deviceAddress, object : ConnectionStatusCallback<Any?> {
                override fun getStatus(connectionStatus: ConnectionStatus, device: Any?) {
                    runOnUiThread {
                        if (connectionStatus == ConnectionStatus.CONNECTED) {
                            Log.e("ConetionTAG", "getStatus: " + "IF")
                            UHFConnectionManager.updateConnectionStatus(connectionStatus, device)
                            binding.rlStatScan.visibility = View.GONE
                            validateAndLogFields()

                        } else {
                            UHFConnectionManager.updateConnectionStatus(ConnectionStatus.DISCONNECTED, device)
                            Log.e("ConetionTAG", "getStatus: " + "ELSE")
                        }
                    }
                }
            })
        }
    }

    private fun getRealPathFromUriNew(uri: Uri): String {
        // Get MIME type from content resolver
        val mimeType = contentResolver?.getType(uri)
        val extension = MimeTypeMap.getSingleton()
            .getExtensionFromMimeType(mimeType)
            ?: run {
                // Fallback: Try to get from URI if mimeType is null
                MimeTypeMap.getFileExtensionFromUrl(uri.toString()).ifEmpty { "tmp" }
            }

        // Create a new file in cache with correct extension
        val file = File(cacheDir, "${System.currentTimeMillis()}.$extension")

        contentResolver?.openInputStream(uri)?.use { inputStream ->
            FileOutputStream(file).use { outputStream ->
                inputStream.copyTo(outputStream)
            }
        }

        return file.absolutePath
    }

    /**
     * Validates if the selected file is less than 10MB
     */
    private fun validateFileSize(uri: Uri, isImage: Boolean): Boolean {
        val fileSize = getFileSize( uri)
        val maxSize = 10 * 1024 * 1024 // 10MB in bytes
        return if (fileSize > maxSize) {
            val fileType = if (isImage) "Image" else "Video"
            Snackbar.make(binding.root, "$fileType size should not exceed 10MB", Snackbar.LENGTH_LONG).show()
            Log.e("FILE_VALIDATION", "$fileType size is too large: ${fileSize / (1024 * 1024)} MB")
            false
        } else {
            Log.d("FILE_VALIDATION", "File size is valid: ${fileSize / (1024 * 1024)} MB")
            true
        }
    }
    /**
     * Gets the file size in MB
     */
    private fun getFileSize(uri: Uri): Long {
        val cursor = contentResolver.query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)
        var size: Long = 0
        cursor?.use {
            if (it.moveToFirst()) {
                val sizeIndex = it.getColumnIndex(OpenableColumns.SIZE)
                size = it.getLong(sizeIndex)
            }
        }
        return size
    }

    private fun validateAndLogFields() {
        val productName = binding.etProductName.text.toString().trim()
        val productCategory = binding.etCategory.text.toString().trim()
        val priceStr = binding.etPrice.text.toString().trim()
        val etSku = binding.etSku.text.toString().trim()
        val tagId = "" // Replace this with actual tag ID logic
        val status = "Pending"

        // Extract image paths
        val selectedImagePaths = selectedImageFiles.map { it.absolutePath }

        // Extract video path (if selected)
        val selectedVideoPath = selectedVideo?.let { getRealPathFromUriNew(it) }

        val description = binding.etDescription.text.toString().trim()
        val currentTime = SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.getDefault()).format(Date())

        val input = ProductModel(
            id = UUID.randomUUID().toString(),
            selectedImages = selectedImagePaths,
            selectedVideo = selectedVideoPath,
            productName = productName,
            productCategory = productCategory,
            sku = etSku,
            price = priceStr,
            description = description,
            isImageSelected = isImageSelected,
            tagId = tagId,
            status = status,
            currentTime
        )

        val isValid = addItemViewModel.validateProductInput(input)

        // Logging entered data
        Log.d("ADD_ITEM", "---- Logging Entered Data ----")
        Log.d("ADD_ITEM", "Product Name: $productName")
        Log.d("ADD_ITEM", "Category: $productCategory")
        Log.d("ADD_ITEM", "sku: $etSku")
        Log.d("ADD_ITEM", "Price: $priceStr")
        Log.d("ADD_ITEM", "Description: $description")
        Log.d("ADD_ITEM", "TagId: $tagId")
        Log.d("ADD_ITEM", "Status: $status")

        // Log selected media

        if (selectedImageFiles.isNotEmpty()) {
            selectedImageFiles.forEachIndexed { index, file ->
                Log.d("ADD_ITEM", "Image $index: ${file.absolutePath}")
            }
        } else {
            Log.d("ADD_ITEM", "No images selected")
        }

        selectedVideo?.let {
            Log.d("ADD_ITEM", "Selected Video: ${getRealPathFromUriNew(it)}")
        } ?: Log.d("ADD_ITEM", "No video selected")

        if (isValid) {
            Log.d("ADD_ITEM", "✅ Validation Passed! Ready to upload.")
            openTagListFragment(input)

        } else {
            Log.d("ADD_ITEM", "❌ Validation Failed! Fix errors before proceeding.")
        }
    }

    private fun openTagListFragment(input: ProductModel) {
        val viewModel = ViewModelProvider(this).get(SharedProductViewModel::class.java)
        viewModel.setProduct(input)
        FragmentManagerHelper.setFragment(this, UHFReadFragment(), R.id.rfidFrame)
    }

    override fun provideUHFDevice(): RFIDWithUHFBLE {
        return uhfDevice
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private fun showBluetoothDevice() {
        if (mBtAdapter == null) {
            return
        }
        if (!mBtAdapter!!.isEnabled) {
            val enableIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT)
        } else {
            val newIntent = Intent(this@AddItemActivity, DeviceListActivity::class.java)
            newIntent.putExtra(SHOW_HISTORY_CONNECTED_LIST, false)
            startActivityForResult(newIntent, REQUEST_SELECT_DEVICE)
            dashboardViewModel.cancelDisconnectTimer()
        }
    }
}