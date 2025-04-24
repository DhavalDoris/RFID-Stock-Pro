package com.example.rfidstockpro.inouttracker.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.rfidstockpro.databinding.ItemCollectionBinding
import com.example.rfidstockpro.inouttracker.model.CollectionModel
import android.util.Log
import com.example.rfidstockpro.inouttracker.SelectionState

class CollectionAdapter(
    private val onItemClick: (CollectionModel) -> Unit,
    private var onSelectionChanged: (SelectionState) -> Unit // 🔄 Send full info
) : RecyclerView.Adapter<CollectionAdapter.ViewHolder>() {

    var list: List<CollectionModel> = emptyList()

    fun setData(newList: List<CollectionModel>) {
        Log.e("CollectionListVM", "New list size: ${newList.size}")
        list = newList
        notifyDataSetChanged()
    }

    fun selectAllItems(selectAll: Boolean) {
        list = list.map { it.copy(isSelected = selectAll) }
        notifyDataSetChanged()
        onSelectionChanged?.invoke(
            SelectionState(
                allSelected = selectAll,
                selectedItems = getSelectedCollections()
            )
        )
    }


    inner class ViewHolder(val binding: ItemCollectionBinding) :
        RecyclerView.ViewHolder(binding.root) {
        init {
            binding.CollectionCheckBox.setOnCheckedChangeListener { _, isChecked ->
                val currentItem  = list[adapterPosition]
                list = list.mapIndexed { index, item ->
                    if (index == adapterPosition) item.copy(isSelected = isChecked) else item
                }
                notifyItemChanged(adapterPosition)

                onSelectionChanged.invoke(
                    SelectionState(
                        allSelected = list.all { it.isSelected },
                        selectedItems = getSelectedCollections()
                    )
                )
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding =
            ItemCollectionBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun getItemCount(): Int = list.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = list[position]
        holder.binding.apply {
            Log.e("CollectionListVM", "onBindViewHolder: " + item.collectionName)
            tvCollectionName.text = item.collectionName
            tvCollectionDate.text = item.createdDateTime
            tvProductCount.text = "Products: ${item.productIds.size}"

            CollectionCheckBox.setOnCheckedChangeListener(null)
            CollectionCheckBox.isChecked = item.isSelected

            CollectionCheckBox.setOnCheckedChangeListener { _, isChecked ->
                val updatedItem = item.copy(isSelected = isChecked)
                val updatedList = list.toMutableList()
                updatedList[position] = updatedItem
                list = updatedList
                notifyItemChanged(position)

                onSelectionChanged(
                    SelectionState(
                        allSelected = list.all { it.isSelected },
                        selectedItems = list.filter { it.isSelected }
                    )
                )
            }

            root.setOnClickListener {
                onItemClick(item)
            }
        }
    }

    fun getSelectedCollections(): List<CollectionModel> {
        return list.filter { it.isSelected }
    }

}

