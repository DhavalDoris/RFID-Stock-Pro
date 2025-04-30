package com.example.rfidstockpro.bulkupload.adapter

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.rfidstockpro.R
import com.example.rfidstockpro.aws.models.ProductModel
import com.example.rfidstockpro.databinding.ExcelProductItemBinding

class ExcelProductAdapter(
    private val items: List<ProductModel>
) : RecyclerView.Adapter<ExcelProductAdapter.ProductViewHolder>() {

    inner class ProductViewHolder(
        val binding: ExcelProductItemBinding
    ) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val binding = ExcelProductItemBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ProductViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        val product = items[position]
        with(holder.binding) {
            tvTitle.text = product.productName
            tvCategory.text = product.productCategory
            tvStatus.text = product.status
            tvPrice.text = "${product.price}"
            if (product.sku.isNotEmpty()) {
                tvSku.text = product.sku
            } else {
                tvSku.text = product.styleNo
            }
            Log.e("Product_TAG", "onBindViewHolder: " + product.toString())
            // Load image (first in list) with Glide
            val imgUrl = product.selectedImages.firstOrNull()
            Glide.with(imgProduct.context)
                .load(imgUrl)
                .placeholder(R.drawable.app_icon)
                .error(R.drawable.app_icon)
                .centerCrop()
                .into(imgProduct)
        }
    }

    override fun getItemCount(): Int = items.size
}
