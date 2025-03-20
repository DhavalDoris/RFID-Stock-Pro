package com.example.rfidstockpro.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.core.content.res.ResourcesCompat
import com.example.rfidstockpro.R
import com.example.rfidstockpro.databinding.ItemSpinnerBinding

class CustomSpinnerAdapter(
    context: Context,
    private val items: List<String>
) : ArrayAdapter<String>(context, R.layout.item_spinner, items) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        return createView(position, convertView, parent)
    }

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
        return createView(position, convertView, parent)
    }

    private fun createView(position: Int, convertView: View?, parent: ViewGroup): View {
        val binding = ItemSpinnerBinding.inflate(LayoutInflater.from(context), parent, false)
        binding.tvSpinnerItem.text = items[position]

        // Set custom font
        val typeface = ResourcesCompat.getFont(context, R.font.rethinksans_variablefont_wght)
        binding.tvSpinnerItem.typeface = typeface

        return binding.root
    }
}
