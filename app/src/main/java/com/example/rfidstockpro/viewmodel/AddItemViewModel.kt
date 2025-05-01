package com.example.rfidstockpro.viewmodel

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import android.view.View
import android.webkit.MimeTypeMap
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.rfidstockpro.aws.models.ProductModel
import com.google.android.material.snackbar.Snackbar
import java.io.File
import java.io.FileOutputStream

class AddItemViewModel : ViewModel() {

    // LiveData to hold a validation error message, if any.
    private val _validationError = MutableLiveData<String?>()
    val validationError: LiveData<String?> = _validationError

    fun clearValidationError() {
        _validationError.value = null
    }

    /**
     * Validates the product input.
     * Returns true if validation passes; false otherwise.
     */
    fun validateProductInput(input: ProductModel): Boolean {

        if (!input.isImageSelected) {
            _validationError.value = "Please select at least one image"
            return false
        }

        if (input.productName.isEmpty()) {
            _validationError.value = "Product Name is required"
            return false
        }


        if (input.productCategory.trim().isEmpty()) {
            _validationError.value = "Category is required"
            return false
        }

        if (input.sku.isEmpty()) {
            _validationError.value = "sku is required"
            return false
        }

        val price = input.price.toDoubleOrNull()
        if (input.price.isEmpty() || price == null ) {
            _validationError.value = "Price is required"
            return false
        }

        if (price <= 0) {
            _validationError.value = "Price must be greater than 0"
            return false
        }

        // Validate Description
       /* if (input.description.isEmpty()) {
            _validationError.value = "Description is required"
            return false
        }*/


        if (!input.isImageSelected) {
            _validationError.value = "Please select image"
            return false
        }

        // If all validations pass, clear any previous error and return true.
        _validationError.value = null
        return true
    }

    /**
     * Gets the file size in MB
     */
    private fun getFileSize(context: Context, uri: Uri): Long {
        val cursor = context.contentResolver.query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)
        var size: Long = 0
        cursor?.use {
            if (it.moveToFirst()) {
                val sizeIndex = it.getColumnIndex(OpenableColumns.SIZE)
                size = it.getLong(sizeIndex)
            }
        }
        return size
    }

    /**
     * Validates if the selected file is less than 10MB
     */

    fun validateFileSize(
        context: Context,
        uri: Uri,
        isImage: Boolean,
        rootView: View,
        maxSizeMB: Int = 10
    ): Boolean {
        val fileSize = getFileSize(context, uri)
        val maxSizeBytes = maxSizeMB * 1024 * 1024 // Convert MB to Bytes

        return if (fileSize > maxSizeBytes) {
            val fileType = if (isImage) "Image" else "Video"
            Snackbar.make(rootView, "$fileType size should not exceed ${maxSizeMB}MB", Snackbar.LENGTH_LONG).show()
            Log.e("FILE_VALIDATION", "$fileType size is too large: ${fileSize / (1024 * 1024)} MB")
            false
        } else {
            Log.d("FILE_VALIDATION", "File size is valid: ${fileSize / (1024 * 1024)} MB")
            true
        }
    }

    fun getRealPathFromUriNew(context: Context,uri: Uri): String {
        // Get MIME type from content resolver
        val mimeType = context.contentResolver?.getType(uri)
        val extension = MimeTypeMap.getSingleton()
            .getExtensionFromMimeType(mimeType)
            ?: run {
                // Fallback: Try to get from URI if mimeType is null
                MimeTypeMap.getFileExtensionFromUrl(uri.toString()).ifEmpty { "tmp" }
            }

        // Create a new file in cache with correct extension
        val file = File(context.cacheDir, "${System.currentTimeMillis()}.$extension")

        context.contentResolver?.openInputStream(uri)?.use { inputStream ->
            FileOutputStream(file).use { outputStream ->
                inputStream.copyTo(outputStream)
            }
        }

        return file.absolutePath
    }
}
