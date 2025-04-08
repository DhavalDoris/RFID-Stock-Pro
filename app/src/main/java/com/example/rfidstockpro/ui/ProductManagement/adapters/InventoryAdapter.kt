package com.example.rfidstockpro.ui.inventory

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.rfidstockpro.R
import com.example.rfidstockpro.aws.models.ProductModel
import com.example.rfidstockpro.databinding.ItemInventoryBinding

class InventoryAdapter(
    private var productList: List<ProductModel>,
    private val onItemClick: (product: ProductModel, anchorView: View) -> Unit
) : RecyclerView.Adapter<InventoryAdapter.InventoryViewHolder>() {

    inner class InventoryViewHolder(val binding: ItemInventoryBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(product: ProductModel) {
            binding.productName.text = product.productName
            binding.productSku.text = product.sku
            binding.productCategory.text = product.productCategory
            binding.productStatus.text = product.status
            binding.productPrice.text = product.price
            binding.tagIdTextView.text = product.tagId

            Glide.with(binding.productImage.context)
                .load(product.selectedImages.get(0))
                .placeholder(R.drawable.app_icon)
                .error(R.drawable.app_icon)
                .into(binding.productImage)

            binding.llAction.setOnClickListener {
                onItemClick(product,it)
            }


        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): InventoryViewHolder {
        val binding = ItemInventoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
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
}
