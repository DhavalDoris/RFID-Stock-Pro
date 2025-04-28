package com.example.rfidstockpro.bulkupload.adapter

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import androidx.recyclerview.widget.RecyclerView
import com.example.rfidstockpro.R
import com.example.rfidstockpro.bulkupload.model.MappingItem
import com.example.rfidstockpro.databinding.ItemMappingBinding

class MappingAdapter(private val items: List<MappingItem>) : RecyclerView.Adapter<MappingAdapter.MappingViewHolder>() {

    // List to track selected headers
    private val selectedSystemHeaders = mutableListOf<String>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MappingViewHolder {
        val binding = ItemMappingBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MappingViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MappingViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount() = items.size

    inner class MappingViewHolder(private val binding: ItemMappingBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: MappingItem) {
            binding.tvImportedHeader.text = item.importedHeader
            binding.tvSample.text = "${item.sampleValue}"

            val context = binding.spinnerSystemHeader.context
            val systemHeaders = context.resources.getStringArray(R.array.system_headers)

            // Create the spinner adapter
            val adapter = ArrayAdapter.createFromResource(
                context,
                R.array.system_headers,
                R.layout.custom_spinner_item
            ).apply {
                setDropDownViewResource(R.layout.custom_spinner_item)
            }

            binding.spinnerSystemHeader.adapter = adapter

            // Auto select based on matching imported header with system headers
            val autoMatchPosition = findBestMatchPosition(item.importedHeader, systemHeaders)
            if (autoMatchPosition >= 0) {
                binding.spinnerSystemHeader.setSelection(autoMatchPosition)
                item.systemHeader = systemHeaders[autoMatchPosition]  // Save selected
            }

            // Pre-select if already selected
            item.systemHeader?.let { selected ->
                val position = adapter.getPosition(selected)
                if (position >= 0) {
                    binding.spinnerSystemHeader.setSelection(position)
                }
            }

            // Spinner item selected listener
            binding.spinnerSystemHeader.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>, view: View?, pos: Int, id: Long) {
                    val selectedSystemHeader = adapter.getItem(pos).toString()

                    // Remove old system header if it was previously selected
                    item.systemHeader?.let {
                        selectedSystemHeaders.remove(it)
                    }

                    // If the user selects a valid header, add it to the list of selected headers
                    if (selectedSystemHeader != "-- Select --") {
                        selectedSystemHeaders.add(selectedSystemHeader)
                        item.systemHeader = selectedSystemHeader
                    } else {
                        item.systemHeader = null
                    }

                    // Notify the adapter to update the views
                    notifyDataSetChanged()
                }

                override fun onNothingSelected(parent: AdapterView<*>) {}
            }

            // Clear selection button logic
            binding.btnClearSelection.apply {
                isClickable = true
                isFocusable = true
                setOnClickListener {
                    Log.d("MappingAdapter_TAG", "Clear button clicked at pos $adapterPosition")

                    // Clear the spinner selection for this item and reset the value
                    item.systemHeader = null
                    binding.spinnerSystemHeader.setSelection(0)  // Reset spinner to default ("-- Select --")

                    // Remove the header from selected list
                    item.systemHeader?.let {
                        selectedSystemHeaders.remove(it)
                    }

                    notifyDataSetChanged()
                }
            }
        }
    }

    // Helper function to find the best match position based on the imported header
    private fun findBestMatchPosition(importedHeader: String, systemHeaders: Array<String>): Int {
        val lowerHeader = importedHeader.lowercase()

        return systemHeaders.indexOfFirst { systemField ->
            val lowerSystemField = systemField.lowercase()

            lowerHeader == lowerSystemField || // Exact match
                    (lowerSystemField.contains("title") && lowerHeader.contains("title")) ||
                    (lowerSystemField.contains("image") && lowerHeader.contains("image")) ||
                    (lowerSystemField.contains("video") && lowerHeader.contains("video")) ||
                    (lowerSystemField.contains("price") && lowerHeader.contains("price")) ||
                    (lowerSystemField.contains("style no") && lowerHeader.contains("style")) ||
                    (lowerSystemField.contains("description") && lowerHeader.contains("description"))
        }
    }
}


