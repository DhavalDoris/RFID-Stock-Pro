package com.example.rfidstockpro.inouttracker

import com.example.rfidstockpro.inouttracker.model.CollectionModel

data class SelectionState(
    val allSelected: Boolean,
    val selectedItems: List<CollectionModel>
)