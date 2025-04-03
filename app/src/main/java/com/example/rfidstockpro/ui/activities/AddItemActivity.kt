package com.example.rfidstockpro.ui.activities

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.example.rfidstockpro.R
import com.example.rfidstockpro.Utils.StatusBarUtils
import com.example.rfidstockpro.adapter.CustomSpinnerAdapter
import com.example.rfidstockpro.aws.AwsManager
import com.example.rfidstockpro.aws.AwsManager.getFileExtension
import com.example.rfidstockpro.aws.AwsManager.uploadMediaToS3
import com.example.rfidstockpro.aws.models.ProductModel
import com.example.rfidstockpro.databinding.ActivityAddItemBinding
import com.example.rfidstockpro.viewmodel.AddItemViewModel
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import java.io.File

class AddItemActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddItemBinding
    private val CategoryFilterOptions = listOf(
        "Diamond Bracelets",
        "Diamond Rings",
        "Diamond Earrings",
        "Diamond Pendants",
        "Chains"
    )
    private val addItemViewModel: AddItemViewModel by viewModels()
    private val IMAGE_PICKER_REQUEST_CODE = 1001
    private val VIDEO_PICKER_REQUEST_CODE = 1002
    private var isImageSelected: Boolean = false // Add this flag
    private var selectedImage: Uri? = null  // Global Image URI
    private var selectedVideo: Uri? = null  // Global Video URI

    private lateinit var imageFile: File
    private lateinit var videoFile: File

    companion object {
        private const val REQUEST_PICK_IMAGE = 1001
        private const val REQUEST_PICK_VIDEO = 1002
        private const val REQUEST_CAPTURE_IMAGE = 1003
        private const val REQUEST_CAPTURE_VIDEO = 1004
        private const val MAX_FILE_SIZE_MB = 10.0
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddItemBinding.inflate(layoutInflater)
        setContentView(binding.root)
        StatusBarUtils.setStatusBarColor(this)
        setupSpinner()
        initUI()
        observeValidationErrors()
    }

    private fun initUI() {

        binding.commonToolbar.tvToolbarTitle.text = getString(R.string.add_item)

        binding.button10k.isSelected = true
        binding.button10k.setOnClickListener { selectButton(binding.button10k) }
        binding.button14k.setOnClickListener { selectButton(binding.button14k) }
        binding.button18k.setOnClickListener { selectButton(binding.button18k) }

        // Button Click Listeners
        binding.btnAdd.setOnClickListener {

            val imageFile = AwsManager.uriToFile(this, selectedImage!!)

            val videoFile = selectedVideo?.let { uri ->
                AwsManager.uriToFile(this, uri) // Only convert if not null
            }

            // Log Image File Details
            Log.d("AWS_UPLOAD", "Image File Details:")
            Log.d("AWS_UPLOAD", "Path: ${imageFile.absolutePath}")
            Log.d("AWS_UPLOAD", "Name: ${imageFile.name}")
            Log.d(
                "AWS_UPLOAD",
                "Size: ${imageFile.length()} bytes (${imageFile.length() / (1024 * 1024)} MB)"
            )
            Log.d("AWS_UPLOAD", "Exists: ${imageFile.exists()}")


            if (videoFile != null) {
                // Log Video File Details
                Log.d("AWS_UPLOAD", "Video File Details:")
                Log.d("AWS_UPLOAD", "Path: ${videoFile!!.absolutePath}")
                Log.d("AWS_UPLOAD", "Name: ${videoFile.name}")
                Log.d(
                    "AWS_UPLOAD",
                    "Size: ${videoFile.length()} bytes (${videoFile.length() / (1024 * 1024)} MB)"
                )
                Log.d("AWS_UPLOAD", "Exists: ${videoFile.exists()}")

            }


            val scope = CoroutineScope(Dispatchers.Main)

            AwsManager.uploadMediaToS3(
                scope = scope, // ✅ Pass a coroutine scope
                context = this,
                imageFile = imageFile,
                videoFile = videoFile,
                onSuccess = { imageUrl, videoUrl ->
                    Log.d("AWS_UPLOAD", "Image URL: $imageUrl")
                    videoUrl?.let { Log.d("AWS_UPLOAD", "Video URL: $it") }
                },
                onError = { errorMessage ->
                    Log.e("AWS_UPLOAD", "Upload failed: $errorMessage")
                }
            )

            // Call the upload function
            /*AwsManager.uploadMediaToS3(
                context = this,  // 'this' refers to the Activity or Fragment context
                imageFile = imageFile,
                videoFile = videoFile,  // Pass null if no video file
                onSuccess = { imageUrl, videoUrl ->
                    // This block will be called on successful upload
                    Log.d("AWS_UPLOAD", "Image URL: $imageUrl")
                    if (videoUrl != null) {
                        Log.d("AWS_UPLOAD", "Video URL: $videoUrl")
                    } else {
                        Log.d("AWS_UPLOAD", "No video uploaded")
                    }
                },
                onError = { errorMessage ->
                    // This block will be called if there's an error during the upload
                    Log.e("AWS_UPLOAD", "Error occurred: $errorMessage")
                }
            )*/

            // Proceed with adding the product
            /*  AwsManager.uploadMediaToS3(
                  context = this,
                  imageFile = imageFile,
                  videoFile = videoFile,
                  onSuccess = { imageUrl, videoUrl ->
                      Log.d("AWS_UPLOAD", "Image uploaded: $imageUrl")
                      videoUrl?.let { Log.d("AWS_UPLOAD", "Video uploaded: $it") }
                      // Save URLs to database or UI
                  },
                  onError = { errorMessage ->
                      Log.e("AWS_UPLOAD", "Failed: $errorMessage")
                  }
              )*/

            /* if (validateFields()) {
                 val imageFile = AwsManager.uriToFile(this, selectedImage!!)

                 val videoFile = selectedVideo?.let { uri ->
                     AwsManager.uriToFile(this, uri) // Only convert if not null
                 }

                 // Proceed with adding the product
                 AwsManager.uploadMediaToS3(
                     context = this,
                     imageFile = imageFile,
                     videoFile = videoFile,
                     onSuccess = { imageUrl, videoUrl ->
                         Log.d("AWS_UPLOAD", "Image uploaded: $imageUrl")
                         videoUrl?.let { Log.d("Upload", "Video uploaded: $it") }
                         // Save URLs to database or UI
                     },
                     onError = { errorMessage ->
                         Log.e("AWS_UPLOAD", "Failed: $errorMessage")
                     }
                 )
                 Toast.makeText(
                     this,
                     "Validation Passed! Proceeding with Add...",
                     Toast.LENGTH_SHORT
                 ).show()
                 // You can call your ViewModel's addProduct() here.
             }*/
        }

        binding.btnAddScan.setOnClickListener {
            if (validateFields()) {
                // Proceed with adding & scanning
                Toast.makeText(
                    this,
                    "Validation Passed! Proceeding with Add & Scan...",
                    Toast.LENGTH_SHORT
                ).show()
                // You can call your ViewModel's addProduct() here.
            }
        }

        binding.selectedImagesContainer.setOnClickListener {
//            openImagePicker("image/*", IMAGE_PICKER_REQUEST_CODE)
            showMediaPickerDialog(isImage = true)
        }

        binding.selectVideo.setOnClickListener {
//            openImagePicker("video/*", VIDEO_PICKER_REQUEST_CODE)
            showMediaPickerDialog(isImage = false)
        }
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

    private fun openImagePicker(type: String, request_code: Int) {
        // Create an intent to pick an image
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        intent.type = type
        startActivityForResult(intent, request_code)
    }

    private fun selectButton(selectedButton: androidx.appcompat.widget.AppCompatTextView) {
        // Deselect all buttons
        binding.button10k.isSelected = false
        binding.button14k.isSelected = false
        binding.button18k.isSelected = false

        // Select the clicked button
        selectedButton.isSelected = true
    }

    private fun setupSpinner() {
        val adapter = CustomSpinnerAdapter(this, CategoryFilterOptions)
        binding.spinnerCategoryFilter.adapter = adapter
        // Set default selection to "Monthly"
        binding.spinnerCategoryFilter.setSelection(1)
        // Handle item selection
        binding.spinnerCategoryFilter.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    val selectedOption = CategoryFilterOptions[position]
                    // Handle selection if required
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }
    }


    private fun observeValidationErrors() {
        addItemViewModel.validationError.observe(this) { error ->
            error?.let {
                Snackbar.make(binding.main, it, Snackbar.LENGTH_SHORT).show()
                addItemViewModel.clearValidationError()
            }
        }
    }

    /*override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == IMAGE_PICKER_REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            val selectedImageUri: Uri? = data.data
            selectedImage = data.data
            selectedImageUri?.let {
                binding.selectedImagesContainer.setImageURI(it)
                isImageSelected = true
                binding.changeImage.visibility = View.VISIBLE
            }
        }

        if (requestCode == VIDEO_PICKER_REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            val selectedVideoUri: Uri? = data.data
            selectedVideo = data.data
            selectedVideoUri?.let {
                val retriever = MediaMetadataRetriever()
                retriever.setDataSource(this, it)
                val bitmap = retriever.frameAtTime
                binding.selectedVideoContainer.setImageBitmap(bitmap)
//                isImageSelected = true
                binding.changeVideo.visibility = View.VISIBLE
            }
        }
    }*/


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                REQUEST_PICK_IMAGE, REQUEST_CAPTURE_IMAGE -> {
                    selectedImage = data?.data ?: selectedImage
                    selectedImage?.let {
                        if (validateFileSize(it, isImage = true)) {
                            binding.selectedImagesContainer.setImageURI(it)
                        } else {
                            selectedImage = null
                        }
                    }
                }

                REQUEST_PICK_VIDEO, REQUEST_CAPTURE_VIDEO -> {
                    selectedVideo = data?.data ?: selectedVideo
                    selectedVideo?.let {
                        if (validateFileSize(it, isImage = false)) {
                            val retriever = MediaMetadataRetriever()
                            retriever.setDataSource(this, it)
                            val bitmap = retriever.frameAtTime
                            binding.selectedVideoContainer.setImageBitmap(bitmap)
                        } else {
                            selectedVideo = null
                        }
                    }
                }
            }
        }

    }

    /**
     * Validates if the selected file is less than 10MB
     */
    private fun validateFileSize(uri: Uri, isImage: Boolean): Boolean {
        val fileSize = getFileSize(this, uri)
        if (fileSize > MAX_FILE_SIZE_MB) {
            Toast.makeText(
                this,
                "${if (isImage) "Image" else "Video"} size should be less than 10MB",
                Toast.LENGTH_SHORT
            ).show()
            return false
        }
        return true
    }

    /**
     * Gets the file size in MB
     */
    private fun getFileSize(context: Context, uri: Uri): Double {
        val cursor = context.contentResolver.query(uri, arrayOf(MediaStore.MediaColumns.SIZE), null, null, null)
        var size = 0L
        cursor?.use {
            if (it.moveToFirst()) {
                size = it.getLong(0)
            }
        }
        return (size / (1024.0 * 1024.0)) // Convert to MB
    }

    private fun validateFields(): Boolean {
        val productName = binding.etProductName.text.toString().trim()
        val productCategory = binding.spinnerCategoryFilter.selectedItem.toString()
        val priceStr = binding.etPrice.text.toString().trim()
        val color = binding.etColor.text.toString().trim()
        val jewelCode = binding.etJewelCode.text.toString().trim()
        val styleNo = binding.etStyleNo.text.toString().trim()

        // Purity selection based on button state.
        val purity = when {
            binding.button10k.isSelected -> "10K"
            binding.button14k.isSelected -> "14K"
            binding.button18k.isSelected -> "18K"
            else -> ""
        }

        val totalDiaWtStr = binding.etTotalDiaWt.text.toString().trim()
        val totalGrossWtStr = binding.etTotalGrossWt.text.toString().trim()
        val totalDiaStr = binding.etTotalDia.text.toString().trim()
        val description = binding.etDescription.text.toString().trim()


        val input = ProductModel(
            productName = productName,
            productCategory = productCategory,
            priceStr = priceStr,
            color = color,
            jewelCode = jewelCode,
            styleNo = styleNo,
            purity = purity,
            totalDiaWtStr = totalDiaWtStr,
            totalGrossWtStr = totalGrossWtStr,
            totalDiaStr = totalDiaStr,
            description = description,
            isImageSelected = isImageSelected
        )

        return addItemViewModel.validateProductInput(input)
    }
}