package com.example.rfidstockpro.adapter

import android.content.Context
import android.util.SparseBooleanArray
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.CheckBox
import android.widget.TextView
import com.example.rfidstockpro.R
import com.example.rfidstockpro.data.UHFTagModel

class UHFTagAdapter(context: Context) : BaseAdapter() {
    private val mInflater: LayoutInflater = LayoutInflater.from(context)
    private var tagList: List<UHFTagModel> = listOf()
    private val checkedItems = SparseBooleanArray()

    fun updateTags(newTagList: List<UHFTagModel>) {
        tagList = newTagList
        notifyDataSetChanged()
    }

    override fun getCount(): Int = tagList.size
    override fun getItem(position: Int): UHFTagModel = tagList[position]
    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: createViewHolder(parent)
        val holder = view.tag as ViewHolder
        val tag = tagList[position]

        holder.apply {
            tvTag.text = tag.generateTagString()
            tvTagCount.text = tag.count.toString()
            tvTagRssi.text = tag.rssi
            tvPhase.text = tag.phase.toString()

            checkBox.setOnCheckedChangeListener(null)
            checkBox.isChecked = checkedItems[position, false]
            checkBox.setOnCheckedChangeListener { _, isChecked ->
                checkedItems.put(position, isChecked)
            }
        }

        return view
    }

    private fun createViewHolder(parent: ViewGroup): View {
        val view = mInflater.inflate(R.layout.listtag_items, parent, false)
        val holder = ViewHolder(
            tvTag = view.findViewById(R.id.TvTag),
            tvTagCount = view.findViewById(R.id.TvTagCount),
            tvTagRssi = view.findViewById(R.id.TvTagRssi),
            tvPhase = view.findViewById(R.id.TvPhase),
            checkBox = view.findViewById(R.id.customCheckBox)
        )
        view.tag = holder
        return view
    }

    data class ViewHolder(
        val tvTag: TextView,
        val tvTagCount: TextView,
        val tvTagRssi: TextView,
        val tvPhase: TextView,
        val checkBox: CheckBox
    )
}