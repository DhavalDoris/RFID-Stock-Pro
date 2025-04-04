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

class UHFTagAdapter(context: Context,private val onTagSelected: (UHFTagModel) -> Unit) : BaseAdapter() {
    private val mInflater: LayoutInflater = LayoutInflater.from(context)
    private var tagList: List<UHFTagModel> = listOf()
    private val checkedItems = SparseBooleanArray()
    private var lastCheckedPosition: Int = -1 // Track last checked position

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
            checkBox.isChecked = position == lastCheckedPosition // Ensure only one is checked
            checkBox.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    // Uncheck previous selection
                    if (lastCheckedPosition != -1) {
                        checkedItems.put(lastCheckedPosition, false)
                        notifyDataSetChanged() // Refresh UI
                    }
                    lastCheckedPosition = position
                    checkedItems.put(position, true)
                    onTagSelected.invoke(tag) // Invoke callback with selected tag
                } else {
                    checkedItems.put(position, false)
                    if (lastCheckedPosition == position) {
                        lastCheckedPosition = -1
                    }
                }
            }
            /*checkBox.isChecked = checkedItems[position, false]
            checkBox.setOnCheckedChangeListener { _, isChecked ->
                checkedItems.put(position, isChecked)
            }*/
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