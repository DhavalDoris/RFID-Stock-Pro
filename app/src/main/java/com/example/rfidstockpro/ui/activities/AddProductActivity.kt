package com.example.rfidstockpro.ui.activities

import UHFConnectionManager
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.example.rfidstockpro.R
import com.example.rfidstockpro.Utils.FragmentManagerHelper
import com.example.rfidstockpro.Utils.StatusBarUtils
import com.example.rfidstockpro.Utils.ToastUtils.showToast
import com.example.rfidstockpro.aws.models.ProductModel
import com.example.rfidstockpro.databinding.ActivityAddItemBinding
import com.example.rfidstockpro.ui.ProductManagement.BluetoothConnectionManager
import com.example.rfidstockpro.ui.ProductManagement.helper.ProductHolder
import com.example.rfidstockpro.ui.ProductManagement.helper.ProductHolder.selectedProduct
import com.example.rfidstockpro.ui.activities.DashboardActivity.Companion.uhfDevice
import com.example.rfidstockpro.ui.activities.DeviceListActivity.TAG
import com.example.rfidstockpro.ui.fragments.UHFReadFragment
import com.example.rfidstockpro.viewmodel.AddItemViewModel
import com.example.rfidstockpro.viewmodel.DashboardViewModel
import com.example.rfidstockpro.viewmodel.SharedProductViewModel
import com.google.android.material.snackbar.Snackbar
import com.rscja.deviceapi.RFIDWithUHFBLE
import com.rscja.deviceapi.interfaces.ConnectionStatus
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class AddProductActivity : AppCompatActivity(), UHFReadFragment.UHFDeviceProvider {

    private val selectedImageFiles = mutableListOf<File>()

    private lateinit var binding: ActivityAddItemBinding
    private val addItemViewModel: AddItemViewModel by viewModels()
    private var isImageSelected: Boolean = false // Add this flag
    private var selectedImage: Uri? = null  // Global Image URI
    private var selectedVideo: Uri? = null  // Global Video URI

    private lateinit var imageFile: File
    private lateinit var videoFile: File

    private lateinit var dashboardViewModel: DashboardViewModel

    private lateinit var pickImageLauncher: ActivityResultLauncher<Intent>
    private lateinit var pickVideoLauncher: ActivityResultLauncher<Intent>
    private lateinit var captureImageLauncher: ActivityResultLauncher<Intent>
    private lateinit var captureVideoLauncher: ActivityResultLauncher<Intent>
    var isMediaUpdated: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddItemBinding.inflate(layoutInflater)
        setContentView(binding.root)
        StatusBarUtils.setStatusBarColor(this)

        registerActivityResultLaunchers() // Register the launchers
        initUI()
        observeValidationErrors()
    }

    @SuppressLint("MissingPermission")
    private fun initUI() {

        val product = ProductHolder.selectedProduct
        product?.let {
            Log.d("ProductAdd", "ID: ${it.id}")
            Log.d("ProductAdd", "Name: ${it.productName}")
            Log.d("ProductAdd", "Category: ${it.productCategory}")
            Log.d("ProductAdd", "SKU: ${it.sku}")
            Log.d("ProductAdd", "Price: ${it.price}")
            Log.d("ProductAdd", "Description: ${it.description}")
            Log.d("ProductAdd", "Is Image Selected: ${it.isImageSelected}")
            Log.d("ProductAdd", "Tag ID: ${it.tagId}")
            Log.d("ProductAdd", "Status: ${it.status}")
            Log.d("ProductAdd", "Created At: ${it.createdAt}")
            Log.d("ProductAdd", "Images List At: ${it.selectedImages}")

            Log.d("ProductAdd", "Selected Images:")
            it.selectedImages.forEachIndexed { index, image ->
                Log.d("ProductAdd", "Image $index: $image")
            }

            Log.d("ProductAdd", "Selected Video: ${it.selectedVideo ?: "None"}")
        } ?: run {
            Log.e("ProductAdd", "No product data available!")
        }

        val source = intent.getStringExtra("source")
        if (source == "EditScreen") {

            product?.let {
                Glide.with(this).load(it.selectedImages.get(0)).into(binding.selectedImagesContainer)
                val videoToLoad = it.selectedVideo.takeIf { !it.isNullOrEmpty() } ?: R.drawable.select_video

                Glide.with(this)
                    .load(videoToLoad)
                    .into(binding.selectedVideoContainer)

                isImageSelected = true
                binding.etProductName.setText(it.productName)
                binding.etCategory.setText(it.productCategory)
                binding.etSku.setText(it.sku)
                binding.etPrice.setText(it.price)
                binding.etDescription.setText(it.description)
            }

        }

//        mBtAdapter = BluetoothAdapter.getDefaultAdapter()
        binding.commonToolbar.tvToolbarTitle.text = getString(R.string.add_item)

        dashboardViewModel = ViewModelProvider(this)[DashboardViewModel::class.java]
        BluetoothConnectionManager.registerLaunchers(
            this,  // Use 'this' instead of requireActivity() to ensure proper lifecycle
            onDeviceConnected = { device ->
                Log.d("Bluetooth", "Connected to device: ${device.name}")
                dashboardViewModel.notifyDeviceConnected(device)
                UHFConnectionManager.updateConnectionStatus(ConnectionStatus.CONNECTED, device)
            },
            onStatusUpdate = { status, _ ->
                if (status == ConnectionStatus.DISCONNECTED) {
                    showToast(this, "Disconnected")
                    dashboardViewModel.notifyConnectionStatus(status)
                    UHFConnectionManager.updateConnectionStatus(ConnectionStatus.DISCONNECTED, null)
                }
            }
        )

        // Button Click Listeners
        binding.btnAdd.setOnClickListener {
            validateAndLogFields()
        }

        binding.btnAddScan.setOnClickListener {
            validateAndLogFields()
        }

        binding.selectedImagesContainer.setOnClickListener {
            showMediaPickerDialog(isImage = true)
        }

        binding.selectVideo.setOnClickListener {
            showMediaPickerDialog(isImage = false)
        }
        val btnConnectScanner = binding.connectRFID.btnConnectScannerAdd
        btnConnectScanner.setOnClickListener {
            if (dashboardViewModel.isConnected.value == true) {
                dashboardViewModel.disconnect(true)
            } else {
                BluetoothConnectionManager.showBluetoothDevice(this)
            }
        }


        dashboardViewModel.deviceConnected.observe(this) { device ->
            device?.let {
                Log.d("Bluetooth", "Connected to: ${device.name}")
                binding.connectRFID.rlStatScan.visibility = View.GONE
            }
        }

        dashboardViewModel.connectionStatus.observe(this) { status ->
            if (status == ConnectionStatus.DISCONNECTED) {
                showToast(this, "Disconnected")
                binding.connectRFID.rlStatScan.visibility = View.VISIBLE
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
        pickImageLauncher.launch(intent)
    }

    private fun pickVideoFromGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
        intent.type = "video/*"
        pickVideoLauncher.launch(intent)
    }

    private fun captureImage() {
        val imageIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        imageFile = File(externalCacheDir, "image_${System.currentTimeMillis()}.jpg")
        selectedImage = FileProvider.getUriForFile(this, "$packageName.fileprovider", imageFile)
        imageIntent.putExtra(MediaStore.EXTRA_OUTPUT, selectedImage)
        captureImageLauncher.launch(imageIntent)
    }

    private fun recordVideo() {
        val videoIntent = Intent(MediaStore.ACTION_VIDEO_CAPTURE)
        videoFile = File(externalCacheDir, "video_${System.currentTimeMillis()}.mp4")
        selectedVideo = FileProvider.getUriForFile(this, "$packageName.fileprovider", videoFile)
        videoIntent.putExtra(MediaStore.EXTRA_OUTPUT, selectedVideo)
        videoIntent.putExtra(MediaStore.EXTRA_DURATION_LIMIT, 60) // 60 sec max
        captureVideoLauncher.launch(videoIntent)
    }

    private fun observeValidationErrors() {
        addItemViewModel.validationError.observe(this) { error ->
            error?.let {
                Snackbar.make(binding.main, it, Snackbar.LENGTH_SHORT).show()
                addItemViewModel.clearValidationError()
            }
        }
    }

    private fun registerActivityResultLaunchers() {
        pickImageLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == Activity.RESULT_OK) {
                    val data = result.data
                    selectedImageFiles.clear()
                    val uris = when {
                        data?.clipData != null -> List(data.clipData!!.itemCount) { i ->
                            data.clipData!!.getItemAt(
                                i
                            ).uri
                        }

                        data?.data != null -> listOf(data.data!!)
                        else -> emptyList()
                    }

                    uris.forEach { uri ->
                        val filePath = addItemViewModel.getRealPathFromUriNew(this, uri)
                        val file = File(filePath)
                        val isValid =
                            addItemViewModel.validateFileSize(this, uri, true, binding.root)
                        if (isValid) {
                            selectedImageFiles.add(file)
                            Log.d("IMAGE_SELECTION", "Selected Image: ${file.absolutePath}")
                        }
                    }

                    if (selectedImageFiles.isNotEmpty()) {
                        binding.selectedImagesContainer.setImageURI(Uri.fromFile(selectedImageFiles[0]))
                        isImageSelected = true
                        isMediaUpdated = true
                        binding.changeImage.visibility = View.VISIBLE
                    }
                }
            }

        pickVideoLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == Activity.RESULT_OK) {
                    val data = result.data
                    selectedVideo = data?.data ?: selectedVideo
                    selectedVideo?.let { uri ->
                        val isValid = addItemViewModel.validateFileSize(this, uri, false, binding.root)
                        if (isValid) {
                            val retriever = MediaMetadataRetriever()
                            retriever.setDataSource(this, uri)
                            val bitmap = retriever.frameAtTime
                            binding.selectedVideoContainer.setImageBitmap(bitmap)
                            isImageSelected = true
                            isMediaUpdated = true
                            binding.changeVideo.visibility = View.VISIBLE
                        } else {
                            selectedVideo = null
                        }
                    }
                }
            }

        captureImageLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                if (it.resultCode == Activity.RESULT_OK) {
                    selectedImage?.let { uri ->
                        val filePath = addItemViewModel.getRealPathFromUriNew(this, uri)
                        val file = File(filePath)
                        val isValid =
                            addItemViewModel.validateFileSize(this, uri, true, binding.root)
                        if (isValid) {
                            selectedImageFiles.add(file)
                            binding.selectedImagesContainer.setImageURI(Uri.fromFile(file))
                            isImageSelected = true
                            isMediaUpdated = true
                            binding.changeImage.visibility = View.VISIBLE
                        }
                    }
                }
            }

        captureVideoLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                if (it.resultCode == Activity.RESULT_OK) {
                    selectedVideo?.let { uri ->
                        val isValid =
                            addItemViewModel.validateFileSize(this, uri, false, binding.root)
                        if (isValid) {
                            val retriever = MediaMetadataRetriever()
                            retriever.setDataSource(this, uri)
                            val bitmap = retriever.frameAtTime
                            binding.selectedVideoContainer.setImageBitmap(bitmap)
                            isImageSelected = true
                            isMediaUpdated = true
                            binding.changeVideo.visibility = View.VISIBLE
                        }
                    }
                }
            }
    }

    /*private fun validateAndLogFields() {
        val productName = binding.etProductName.text.toString().trim()
        val productCategory = binding.etCategory.text.toString().trim()
        val priceStr = binding.etPrice.text.toString().trim()
        val etSku = binding.etSku.text.toString().trim()
        val tagId = "" // Replace this with actual tag ID logic
        val status = "Active"

        var selectedImagePaths: List<String> = emptyList()
        var selectedVideoPath: String? = null
        val selectedProduct = ProductHolder.selectedProduct
        val source = intent.getStringExtra("source")

        if (source == "EditScreen") {

//            selectedImagePaths = product!!.selectedImages
            selectedImagePaths = selectedProduct?.selectedImages ?: emptyList()
            if (selectedImageFiles.isNotEmpty()) {
                selectedImageFiles.forEachIndexed { index, file ->
                    Log.d("ADD_ITEM", "Image $index: ${file.absolutePath}")
                }
            } else {
                Log.d("ADD_ITEM", "No images selected")
                selectedImagePaths.forEachIndexed { index, image ->
                    Log.d("ProductDetails", "Image $index: $image")
                }
            }
            selectedVideoPath = selectedVideo?.let { addItemViewModel.getRealPathFromUriNew(this, it) }
        } else {
            // Extract image paths
            selectedImagePaths = selectedImageFiles.map { it.absolutePath }

            // Extract video path (if selected)
            selectedVideoPath = selectedVideo?.let { addItemViewModel.getRealPathFromUriNew(this, it) }

        }
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
            isMediaUpdated = isMediaUpdated,
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
        Log.d("ADD_ITEM", "selectedImageFiles: $selectedImagePaths")
        Log.d("ADD_ITEM", "input: $input")


        if (selectedImageFiles.isNotEmpty()) {
            selectedImageFiles.forEachIndexed { index, file ->
                Log.d("ADD_ITEM", "Image $index: ${file.absolutePath}")
            }
        } else {
            Log.d("ADD_ITEM", "No images selected")
        }

        selectedVideo?.let {
            Log.d("ADD_ITEM", "Selected Video: ${addItemViewModel.getRealPathFromUriNew(this, it)}")
        } ?: Log.d("ADD_ITEM", "No video selected")

        if (isValid) {
            Log.d("ADD_ITEM", "✅ Validation Passed! Ready to upload.")

            if (uhfDevice.connectStatus == ConnectionStatus.CONNECTED) {
                openTagListFragment(input)
            } else {
                binding.connectRFID.rlStatScan.visibility = View.VISIBLE
                Log.d("ADD_ITEM", "Show Connecting screen===>>>===>>>")
            }
        } else {
            Log.d("ADD_ITEM", "❌ Validation Failed! Fix errors before proceeding.")
        }
    }*/

    /*private fun validateAndLogFields() {
        val productName = binding.etProductName.text.toString().trim()
        val productCategory = binding.etCategory.text.toString().trim()
        val priceStr = binding.etPrice.text.toString().trim()
        val etSku = binding.etSku.text.toString().trim()
        val tagId = "" // TODO: Update with actual tagId from UHF scanner
        val status = "Active"
        val description = binding.etDescription.text.toString().trim()
        val currentTime = SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.getDefault()).format(Date())
        val source = intent.getStringExtra("source")
        val isEditMode = source == "EditScreen"

        val product = ProductHolder.selectedProduct

        val existingImageUrls = product?.selectedImages ?: emptyList()
        val newImagePaths = selectedImageFiles.map { it.absolutePath }
        val newVideoPath = selectedVideo?.let { addItemViewModel.getRealPathFromUriNew(this, it) }
        val existingVideoUrl = product?.selectedVideo

        val finalImageUrls = if (newImagePaths.isNotEmpty()) emptyList() else existingImageUrls
        val finalVideoUrl = if (newVideoPath != null) null else existingVideoUrl

        val updatedProduct = ProductModel(
            id = product?.id ?: UUID.randomUUID().toString(),
            selectedImages = finalImageUrls,
            selectedVideo = finalVideoUrl,
            newImageFiles = newImagePaths,
            newVideoFile = newVideoPath,
            productName = productName,
            productCategory = productCategory,
            sku = etSku,
            price = priceStr,
            description = description,
            isImageSelected = isImageSelected,
            isMediaUpdated = newImagePaths.isNotEmpty() || newVideoPath != null,
            tagId = tagId,
            status = status,
            createdAt = currentTime
        )

        val isValid = addItemViewModel.validateProductInput(updatedProduct)

        // ✅ Logging for testing
        Log.d("ADD_ITEM", "---- Logging Entered Data ----")
        Log.d("ADD_ITEM", "Edit Mode: $isEditMode")
        Log.d("ADD_ITEM", "Product Name: $productName")
        Log.d("ADD_ITEM", "Category: $productCategory")
        Log.d("ADD_ITEM", "SKU: $etSku")
        Log.d("ADD_ITEM", "Price: $priceStr")
        Log.d("ADD_ITEM", "Description: $description")
        Log.d("ADD_ITEM", "TagId: $tagId")
        Log.d("ADD_ITEM", "Status: $status")
        Log.d("ADD_ITEM", "New Image Files: $newImagePaths")
        Log.d("ADD_ITEM", "Old Image URLs: $existingImageUrls")
        Log.d("ADD_ITEM", "New Video File: $newVideoPath")
        Log.d("ADD_ITEM", "Old Video URL: $existingVideoUrl")
        Log.d("ADD_ITEM", "Final Product Model: $updatedProduct")

        if (isValid) {
            Log.d("ADD_ITEM", "✅ Validation Passed! Ready to upload.")

            if (uhfDevice.connectStatus == ConnectionStatus.CONNECTED) {
                openTagListFragment(updatedProduct)
            } else {
                binding.connectRFID.rlStatScan.visibility = View.VISIBLE
                Log.d("ADD_ITEM", "🔄 RFID not connected — showing connect UI")
            }
        } else {
            Log.d("ADD_ITEM", "❌ Validation Failed! Fix errors before proceeding.")
        }
    }*/

    private fun validateAndLogFields() {
        val productName = binding.etProductName.text.toString().trim()
        val productCategory = binding.etCategory.text.toString().trim()
        val priceStr = binding.etPrice.text.toString().trim()
        val etSku = binding.etSku.text.toString().trim()
        val description = binding.etDescription.text.toString().trim()

        val source = intent.getStringExtra("source")
        val selectedProduct = ProductHolder.selectedProduct

        var selectedImagePaths: List<String> = emptyList()
        var selectedVideoPath: String? = null

        if (source == "EditScreen") {
            selectedImagePaths = selectedProduct?.selectedImages ?: emptyList()
            if (selectedImageFiles.isNotEmpty()) {
                selectedImagePaths = selectedImageFiles.map { it.absolutePath }
            }
            selectedVideoPath = selectedVideo?.let {
                addItemViewModel.getRealPathFromUriNew(this, it)
            } ?: selectedProduct?.selectedVideo
        } else {
            selectedImagePaths = selectedImageFiles.map { it.absolutePath }
            selectedVideoPath = selectedVideo?.let {
                addItemViewModel.getRealPathFromUriNew(this, it)
            }
        }
        val tagId = ""
        val currentTime = SimpleDateFormat("dd-MM-yyyy hh:mm:ss a", Locale.getDefault()).format(Date())
        val createdAt = selectedProduct?.createdAt ?: currentTime
        val productModel = ProductModel(
            id = selectedProduct?.id ?: UUID.randomUUID().toString(),
            productName = productName,
            productCategory = productCategory,
            sku = etSku,
            price = priceStr,
            description = description,
            selectedImages = selectedImagePaths,
            selectedVideo = selectedVideoPath,
            tagId = tagId, // Will be updated in UHFReadFragment
            status = "Active",
            createdAt = createdAt,
            isImageSelected = selectedProduct?.isImageSelected ?: selectedImagePaths.isNotEmpty(),
            isMediaUpdated = selectedProduct?.isMediaUpdated ?: true,
            updatedAt = currentTime
        )

        Log.d("ADD_ITEM", "ProductModel: $productModel")

        val isValid = addItemViewModel.validateProductInput(productModel)
        if (isValid) {
            if (uhfDevice.connectStatus == ConnectionStatus.CONNECTED) {
                openTagListFragment(productModel)
            } else {
                binding.connectRFID.rlStatScan.visibility = View.VISIBLE
                Log.d("ADD_ITEM", "🔄 RFID not connected — showing connect UI")
            }
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

}