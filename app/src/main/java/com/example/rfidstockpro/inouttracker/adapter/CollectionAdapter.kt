package com.example.rfidstockpro.inouttracker.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.rfidstockpro.databinding.ItemCollectionBinding
import com.example.rfidstockpro.inouttracker.model.CollectionModel
import android.util.Log

class CollectionAdapter(
    private val onItemClick: (CollectionModel) -> Unit
) : RecyclerView.Adapter<CollectionAdapter.ViewHolder>() {

    private var list: List<CollectionModel> = emptyList()

    fun setData(newList: List<CollectionModel>) {
        Log.e("CollectionListVM", "New list size: ${newList.size}")
        list = newList
        notifyDataSetChanged()
    }

    inner class ViewHolder(val binding: ItemCollectionBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemCollectionBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun getItemCount(): Int = list.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = list[position]
        holder.binding.apply {
            Log.e("CollectionListVM", "onBindViewHolder: " + item.collectionName )
            tvCollectionName.text = item.collectionName
            tvCollectionDate.text = item.createdDateTime
            tvProductCount.text = "Products: ${item.productIds.size}"

            root.setOnClickListener {
                onItemClick(item)
            }
        }
    }
}

