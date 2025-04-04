package com.example.rfidstockpro.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.rfidstockpro.aws.models.ProductModel

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
        if (input.price.isEmpty() || price == null || price <= 0) {
            _validationError.value = "Price is required"
            return false
        }


        // Validate Description
        if (input.description.isEmpty()) {
            _validationError.value = "Description is required"
            return false
        }


        if (!input.isImageSelected) {
            _validationError.value = "Please select image"
            return false
        }

        // If all validations pass, clear any previous error and return true.
        _validationError.value = null
        return true
    }
}
