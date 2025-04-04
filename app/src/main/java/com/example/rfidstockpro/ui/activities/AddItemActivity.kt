package com.example.rfidstockpro.ui.activities

import android.app.Activity
import android.content.Intent
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.util.Log
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.example.rfidstockpro.R
import com.example.rfidstockpro.Utils.StatusBarUtils
import com.example.rfidstockpro.aws.models.ProductModel
import com.example.rfidstockpro.databinding.ActivityAddItemBinding
import com.example.rfidstockpro.viewmodel.AddItemViewModel
import com.google.android.material.snackbar.Snackbar
import java.io.File
import java.io.FileOutputStream

class AddItemActivity : AppCompatActivity() {

    private val selectedImageFiles = mutableListOf<File>()


    private lateinit var binding: ActivityAddItemBinding
    private val addItemViewModel: AddItemViewModel by viewModels()
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

        binding.commonToolbar.tvToolbarTitle.text = getString(R.string.add_item)


        // Button Click Listeners
        binding.btnAdd.setOnClickListener {
            validateAndLogFields()

            /*Log.d("ADD_ITEM", "Button Clicked - Logging Selected Data")

            if (selectedImageFiles.isEmpty() && selectedVideo == null) {
                Log.d("ADD_ITEM", "No media selected")
            } else {
                // Log multiple images if selected
                if (selectedImageFiles.isNotEmpty()) {
                    Log.d("ADD_ITEM", "Selected Images:")
                    selectedImageFiles.forEachIndexed { index, file ->
                        Log.d("ADD_ITEM", "Image $index: ${file.absolutePath}")
                    }
                } else {
                    Log.d("ADD_ITEM", "No images selected")
                }

                // Log video if selected
                selectedVideo?.let {
                    Log.d("ADD_ITEM", "Selected Video: ${getRealPathFromUriNew(it)}")
                } ?: Log.d("ADD_ITEM", "No video selected")
            }*/

            /*val imageFile = AwsManager.uriToFile(this, selectedImage!!)

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
                imageFiles  = selectedImageFiles,
                videoFile = videoFile,
                onSuccess = { imageUrls, videoUrl ->

                    imageUrls.forEach { Log.d("AWS_UPLOAD", "Image URL: $it") }
                    videoUrl?.let { Log.d("AWS_UPLOAD", "Video URL: $it") }
                },
                onError = { errorMessage ->
                    Log.e("AWS_UPLOAD", "Upload failed: $errorMessage")
                }
            )*/
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
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
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
            }
        }
    }


    private fun getRealPathFromUriNew(uri: Uri): String {
        val file = File(cacheDir, "${System.currentTimeMillis()}.jpg")
        contentResolver.openInputStream(uri)?.use { inputStream ->
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

        val input = ProductModel(
            selectedImages = selectedImagePaths,
            selectedVideo = selectedVideoPath,
            productName = productName,
            productCategory = productCategory,
            sku = etSku,
            price = priceStr,
            description = description,
            isImageSelected = isImageSelected,
            tagId = tagId,
            status = status
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
        } else {
            Log.d("ADD_ITEM", "❌ Validation Failed! Fix errors before proceeding.")
        }
    }
}