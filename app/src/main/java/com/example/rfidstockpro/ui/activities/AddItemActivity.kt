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
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.ActivityResultLauncher

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

    private lateinit var pickImageLauncher: ActivityResultLauncher<Intent>
    private lateinit var pickVideoLauncher: ActivityResultLauncher<Intent>
    private lateinit var captureImageLauncher: ActivityResultLauncher<Intent>
    private lateinit var captureVideoLauncher: ActivityResultLauncher<Intent>
    private lateinit var enableBluetoothLauncher: ActivityResultLauncher<Intent>
    private lateinit var selectDeviceLauncher: ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddItemBinding.inflate(layoutInflater)
        setContentView(binding.root)
        StatusBarUtils.setStatusBarColor(this)

        registerActivityResultLaunchers() // Register the launchers
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
            validateAndLogFields()
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
        pickImageLauncher.launch(intent)
    }

    private fun pickVideoFromGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
        intent.type = "video/*"
        pickVideoLauncher.launch(intent)
    }

    private fun captureImage() {
        val imageIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        imageFile = File(externalCacheDir,  "image_${System.currentTimeMillis()}.jpg")
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
        pickImageLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data = result.data
                selectedImageFiles.clear()
                val uris = when {
                    data?.clipData != null -> List(data.clipData!!.itemCount) { i -> data.clipData!!.getItemAt(i).uri }
                    data?.data != null -> listOf(data.data!!)
                    else -> emptyList()
                }

                uris.forEach { uri ->
                    val filePath = addItemViewModel.getRealPathFromUriNew(this, uri)
                    val file = File(filePath)
                    val isValid = addItemViewModel.validateFileSize(this, uri, true, binding.root)
                    if (isValid) {
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
        }

        pickVideoLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
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
                        binding.changeVideo.visibility = View.VISIBLE
                    } else {
                        selectedVideo = null
                    }
                }
            }
        }

        captureImageLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == Activity.RESULT_OK) {
                selectedImage?.let { uri ->
                    val filePath = addItemViewModel.getRealPathFromUriNew(this, uri)
                    val file = File(filePath)
                    val isValid = addItemViewModel.validateFileSize(this, uri, true, binding.root)
                    if (isValid) {
                        selectedImageFiles.add(file)
                        binding.selectedImagesContainer.setImageURI(Uri.fromFile(file))
                        isImageSelected = true
                        binding.changeImage.visibility = View.VISIBLE
                    }
                }
            }
        }

        captureVideoLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == Activity.RESULT_OK) {
                selectedVideo?.let { uri ->
                    val isValid = addItemViewModel.validateFileSize(this, uri, false, binding.root)
                    if (isValid) {
                        val retriever = MediaMetadataRetriever()
                        retriever.setDataSource(this, uri)
                        val bitmap = retriever.frameAtTime
                        binding.selectedVideoContainer.setImageBitmap(bitmap)
                        isImageSelected = true
                        binding.changeVideo.visibility = View.VISIBLE
                    }
                }
            }
        }

        enableBluetoothLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == Activity.RESULT_OK) {
                showBluetoothDevice()
            }
        }

        selectDeviceLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data = result.data
                data?.getStringExtra(BluetoothDevice.EXTRA_DEVICE)?.let { deviceAddress ->
                    if (deviceAddress.isNotEmpty()) {
                        mDevice = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(deviceAddress)
                        connectToDevice(deviceAddress)
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
        val selectedVideoPath = selectedVideo?.let { addItemViewModel.getRealPathFromUriNew(this,it) }

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
            Log.d("ADD_ITEM", "Selected Video: ${addItemViewModel.getRealPathFromUriNew(this,it)}")
        } ?: Log.d("ADD_ITEM", "No video selected")

        if (isValid) {
            Log.d("ADD_ITEM", "✅ Validation Passed! Ready to upload.")

            if (uhfDevice.connectStatus == ConnectionStatus.CONNECTED) {
                openTagListFragment(input)
            }
            else{
                binding.rlStatScan.visibility = View.VISIBLE
                Log.d("ADD_ITEM", "Show Connecting screen===>>>===>>>")
            }
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
            enableBluetoothLauncher.launch(enableIntent)
        } else {
            val newIntent = Intent(this@AddItemActivity, DeviceListActivity::class.java)
            newIntent.putExtra(SHOW_HISTORY_CONNECTED_LIST, false)
            selectDeviceLauncher.launch(newIntent)
            dashboardViewModel.cancelDisconnectTimer()
        }
    }
}