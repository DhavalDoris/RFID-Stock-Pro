package com.example.rfidstockpro.repository

import com.rscja.deviceapi.RFIDWithUHFBLE
import com.rscja.deviceapi.entity.InventoryParameter
import com.rscja.deviceapi.entity.UHFTAGInfo
import com.rscja.deviceapi.interfaces.ConnectionStatus

class UHFRepository(private val uhfDevice: RFIDWithUHFBLE) {
    fun startInventory(
        maxRunTime: Int = 30000,
        needPhase: Boolean = false,
        onTagFound: (UHFTAGInfo) -> Unit
    ): Boolean {
        val inventoryParameter = InventoryParameter().apply {
            resultData = InventoryParameter.ResultData().setNeedPhase(needPhase)
        }

        uhfDevice.setInventoryCallback { uhfTagInfo ->
            onTagFound(uhfTagInfo)
        }

        return uhfDevice.startInventoryTag(inventoryParameter)
    }

    fun stopInventory(): Boolean {
        return uhfDevice.stopInventory()
    }

    fun inventorySingleTag(): UHFTAGInfo? {
        return uhfDevice.inventorySingleTag()
    }

    fun setFilter(
        filterBank: Int,
        ptr: Int,
        len: Int,
        data: String
    ): Boolean {
        return uhfDevice.setFilter(filterBank, ptr, len, data)
    }

    val connectStatus: ConnectionStatus
        get() = uhfDevice.connectStatus
}