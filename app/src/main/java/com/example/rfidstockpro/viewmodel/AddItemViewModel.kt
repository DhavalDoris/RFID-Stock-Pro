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
        if (input.productCategory.isEmpty() || input.productCategory == "Select Category") {
            _validationError.value = "Please select a Product Category"
            return false
        }
        val price = input.priceStr.toDoubleOrNull()
        if (input.priceStr.isEmpty() || price == null || price <= 0) {
            _validationError.value = "Enter a valid Price"
            return false
        }
        if (input.color.isEmpty()) {
            _validationError.value = "Color is required"
            return false
        }
        if (input.jewelCode.isEmpty()) {
            _validationError.value = "Jewel Code is required"
            return false
        }
        if (input.styleNo.isEmpty()) {
            _validationError.value = "Style No. is required"
            return false
        }
        if (input.purity.isEmpty()) {
            _validationError.value = "Please select a Purity option (10K, 14K, 18K)"
            return false
        }
        if (input.totalDiaWtStr.isEmpty() && input.totalDiaWtStr.toDoubleOrNull() == null) {
            _validationError.value = "Enter a valid Total Dia Weight"
            return false
        }
        val totalGrossWt = input.totalGrossWtStr.toDoubleOrNull()
        if (input.totalGrossWtStr.isEmpty() || totalGrossWt == null || totalGrossWt <= 0) {
            _validationError.value = "Enter a valid Total Gross Weight"
            return false
        }
        if (input.totalDiaStr.isEmpty() && input.totalDiaStr.toIntOrNull() == null) {
            _validationError.value = "Enter a valid Total Dia number"
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
