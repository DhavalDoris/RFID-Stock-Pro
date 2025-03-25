package com.example.rfidstockpro.factores

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.rfidstockpro.repository.UHFRepository
import com.example.rfidstockpro.viewmodel.UHFReadViewModel

class UHFViewModelFactory(private val uhfRepository: UHFRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(UHFReadViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return UHFReadViewModel(uhfRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}