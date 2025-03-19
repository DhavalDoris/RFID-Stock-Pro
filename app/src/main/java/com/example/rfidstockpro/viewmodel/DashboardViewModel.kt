package com.example.rfidstockpro.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.github.mikephil.charting.data.PieEntry

class DashboardViewModel : ViewModel() {
    private val _stockData = MutableLiveData<List<PieEntry>>()
    val stockData: LiveData<List<PieEntry>> get() = _stockData

    init {
        loadStaticData()
    }

    private fun loadStaticData() {
        val entries = listOf(
            PieEntry(15f, "Active"),
            PieEntry(10f, "Pending"),
            PieEntry(30f, "Inactive")
        )
        _stockData.value = entries
    }
}
