package com.example.rfidstockpro.ui.activities

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
import androidx.appcompat.app.AppCompatActivity
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

            // Call the upload function
            AwsManager.uploadMediaToS3(
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
            )

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
            openImagePicker("image/*", IMAGE_PICKER_REQUEST_CODE)
        }

        binding.selectVideo.setOnClickListener {
            openImagePicker("video/*", VIDEO_PICKER_REQUEST_CODE)
        }
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
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