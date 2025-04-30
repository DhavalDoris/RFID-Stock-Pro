package com.example.rfidstockpro.ui.inventory

import android.content.res.ColorStateList
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.rfidstockpro.R
import com.example.rfidstockpro.aws.models.ProductModel
import com.example.rfidstockpro.databinding.ItemInventoryBinding
import com.example.rfidstockpro.ui.activities.DashboardActivity.Companion.ShowCheckBoxinProduct

class InventoryAdapter(
    private var productList: List<ProductModel>,
    private val onItemClick: (product: ProductModel, anchorView: View) -> Unit,
    private val onItemViewClick: (product: ProductModel) -> Unit,
    private val onCheckboxClick: (selectedIds: Set<String>) -> Unit
) : RecyclerView.Adapter<InventoryAdapter.InventoryViewHolder>() {

    private val selectedProductIds = mutableSetOf<String>()

    inner class InventoryViewHolder(val binding: ItemInventoryBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(product: ProductModel) {
            binding.productName.text = product.productName
            binding.productSku.text = product.sku
            binding.productCategory.text = product.productCategory
            binding.productStatus.text = product.status
            binding.productPrice.text = "$" + product.price

            if (product.tagId.isNotEmpty()) {
                binding.tagIdTextView.text = product.tagId
            } else {
                binding.tagIdTextView.text = binding.root.context.getString(R.string.tag_id_not_added)
            }

            when (product.status.lowercase()) {
                "active" -> {
                    binding.productStatus.setTextColor(ContextCompat.getColor(binding.root.context, R.color.active_color))
                    binding.productStatus.backgroundTintList =
                        ColorStateList.valueOf(ContextCompat.getColor(binding.root.context, R.color.active_tint))
                }

                "pending" -> {
                    binding.productStatus.setTextColor(ContextCompat.getColor(binding.root.context, R.color.pending_status))
                    binding.productStatus.backgroundTintList =
                        ColorStateList.valueOf(ContextCompat.getColor(binding.root.context, R.color.pendingView_bg_2))
                }

                else -> {
                    binding.productStatus.setTextColor(ContextCompat.getColor(binding.root.context, R.color.active_color))
                    binding.productStatus.backgroundTintList =
                        ColorStateList.valueOf(ContextCompat.getColor(binding.root.context, R.color.active_tint))
                }
            }

            Log.e("SHOWCHECKBOXINPRODUCT_TAG", "bind: " + ShowCheckBoxinProduct)
            if (ShowCheckBoxinProduct!!) {
                binding.productCheckBox.visibility = View.VISIBLE
                binding.productCheckBox.isChecked = selectedProductIds.contains(product.id)
//                binding.productCheckBox.isVisible = ShowCheckBoxinProduct == true
            }
            if (product.selectedImages.isNotEmpty()) {
                Glide.with(binding.productImage.context)
                    .load(product.selectedImages.get(0))
                    .placeholder(R.drawable.app_icon)
                    .error(R.drawable.app_icon)
                    .into(binding.productImage)
            }

            binding.llAction.setOnClickListener {
                onItemClick(product, it)
            }

            binding.root.setOnClickListener {
                onItemViewClick(product) // 👈 Full item click
            }
            binding.productCheckBox.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    selectedProductIds.add(product.id!!)
                } else {
                    selectedProductIds.remove(product.id)
                }
                onCheckboxClick(selectedProductIds) // 👈 callback to fragment
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): InventoryViewHolder {
        val binding =
            ItemInventoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return InventoryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: InventoryViewHolder, position: Int) {
        holder.bind(productList[position])
    }

    override fun getItemCount(): Int = productList.size

    fun updateList(newList: List<ProductModel>) {
        productList = newList
        notifyDataSetChanged()
    }

    fun selectAllItems(selectAll: Boolean) {
        if (selectAll) {
            selectedProductIds.clear()
            selectedProductIds.addAll(productList.mapNotNull { it.id }) // assuming ProductModel has an `id` field
        } else {
            selectedProductIds.clear()
        }
        notifyDataSetChanged()
    }

    fun getAllItemIds(): List<String> {
        return productList.mapNotNull { it.id } // Assuming `productList` holds current items
    }

    /** Removes the item with the given ID from the list and notifies. */
    fun removeItemById(itemId: String) {
        // Find its position in the current list
        val mutable = productList.toMutableList()
        val index = mutable.indexOfFirst { it.id == itemId }
        if (index != -1) {
            mutable.removeAt(index)
            productList = mutable
            // Tell RecyclerView exactly which item vanished
            notifyItemRemoved(index)
        }
    }

}
