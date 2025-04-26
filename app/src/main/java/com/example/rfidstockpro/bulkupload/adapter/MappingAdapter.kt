package com.example.rfidstockpro.bulkupload.adapter

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

            val adapter = ArrayAdapter.createFromResource(
                context,
                R.array.system_headers,
                android.R.layout.simple_spinner_item
            ).apply {
                setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            }

            binding.spinnerSystemHeader.adapter = adapter

            // 👇 Auto select based on matching imported header with system headers
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

            binding.spinnerSystemHeader.setOnItemSelectedListener { _, _, pos, _ ->
                item.systemHeader = adapter.getItem(pos).toString()
            }
        }
    }

    // Helper function for finding best match position
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

    fun Spinner.setOnItemSelectedListener(listener: (parent: AdapterView<*>, view: View, position: Int, id: Long) -> Unit) {
        onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                listener(parent, view!!, position, id)
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }
}
