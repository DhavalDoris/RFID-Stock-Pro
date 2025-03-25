package com.example.rfidstockpro.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.rfidstockpro.data.UHFTagModel
import com.example.rfidstockpro.repository.UHFRepository
import com.rscja.deviceapi.entity.UHFTAGInfo
import com.rscja.deviceapi.interfaces.ConnectionStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.CopyOnWriteArrayList

class UHFReadViewModel(private val uhfRepository: UHFRepository) : ViewModel() {
    private val _tagList = MutableLiveData<List<UHFTagModel>>(CopyOnWriteArrayList())
    val tagList: LiveData<List<UHFTagModel>> = _tagList

    private val _totalTagCount = MutableLiveData(0)
    val totalTagCount: LiveData<Int> = _totalTagCount

    private val _isScanning = MutableLiveData(false)
    val isScanning: LiveData<Boolean> = _isScanning

    private val _useTime = MutableLiveData(0.0)
    val useTime: LiveData<Double> = _useTime

    private val _connectionStatus = MutableLiveData<ConnectionStatus>()
    val connectionStatus: LiveData<ConnectionStatus> = _connectionStatus

    fun startInventory(maxRunTime: Int = 30000, needPhase: Boolean = false) {
        viewModelScope.launch {
            _isScanning.value = true

            clearData()

            uhfRepository.startInventory(
                maxRunTime = maxRunTime,
                needPhase = needPhase
            ) { uhfTagInfo ->
                // Use viewModelScope to switch to main thread
                viewModelScope.launch {
                    addTagToList(uhfTagInfo)
                }
            }

        }
    }

    fun stopInventory() {
        viewModelScope.launch {
            val result = uhfRepository.stopInventory()
            _isScanning.value = false
        }
    }

    fun singleInventory() {
        viewModelScope.launch {
            val tagInfo = uhfRepository.inventorySingleTag()
            tagInfo?.let {
                viewModelScope.launch {
                    addTagToList(it)
                }
            }
        }
    }

    private suspend  fun addTagToList(uhfTagInfo: UHFTAGInfo) {
        // Ensure this runs on the main thread
        withContext(Dispatchers.Main) {
            val currentList = _tagList.value?.toMutableList() ?: mutableListOf()

            val existingTag = currentList.find { it.epc == uhfTagInfo.epc }

            if (existingTag != null) {
                val updatedList = currentList.map { tag ->
                    if (tag.epc == uhfTagInfo.epc) {
                        tag.copy(
                            rssi = uhfTagInfo.rssi,
                            phase = uhfTagInfo.phase.toDouble(),
                            count = tag.count + 1
                        )
                    } else tag
                }
                _tagList.value = updatedList
            } else {
                val newTag = UHFTagModel(
                    reserved = uhfTagInfo.reserved ?: "",
                    epc = uhfTagInfo.epc ?: "",
                    tid = uhfTagInfo.tid ?: "",
                    user = uhfTagInfo.user ?: "",
                    rssi = uhfTagInfo.rssi,
                    phase = uhfTagInfo.phase.toDouble()
                )
                currentList.add(newTag)
                _tagList.value = currentList
            }

            _totalTagCount.value = (_totalTagCount.value ?: 0) + 1
        }
    }

    fun clearData() {
        _tagList.value = mutableListOf()
        _totalTagCount.value = 0
        _useTime.value = 0.0
    }

    fun setFilter(
        filterBank: Int,
        ptr: Int,
        len: Int,
        data: String
    ): Boolean {
        return uhfRepository.setFilter(filterBank, ptr, len, data)
    }
}