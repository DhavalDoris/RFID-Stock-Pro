package com.example.rfidstockpro.ui.ProductManagement.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.rfidstockpro.R
import com.example.rfidstockpro.RFIDApplication.Companion.PRODUCT_TABLE
import com.example.rfidstockpro.aws.AwsManager
import com.example.rfidstockpro.aws.models.ProductModel
import com.example.rfidstockpro.databinding.FragmentProductDetailBinding
import com.example.rfidstockpro.databinding.ItemProductRowBinding
import com.example.rfidstockpro.ui.ProductManagement.adapters.MediaPagerAdapter
import com.example.rfidstockpro.ui.ProductManagement.helper.ProductHolder
import com.google.android.material.snackbar.Snackbar


class ProductDetailsFragment : Fragment() {

    private lateinit var binding: FragmentProductDetailBinding


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentProductDetailBinding.inflate(inflater, container, false)


        init()

        return binding.root
    }

    fun init() {
        binding.commonToolbar.tvToolbarTitle.text = ""
        binding.commonToolbar.ivSearch.visibility = View.GONE

        val product = ProductHolder.selectedProduct

        val adapter = MediaPagerAdapter(
            mediaList = product!!.selectedImages, // List<String>
            videoUrl = product.selectedVideo // String? (nullable)
        )
        binding.mediaViewPager.adapter = adapter
        binding.dotsIndicator.setViewPager2(binding.mediaViewPager)




        binding.btnPrev.setOnClickListener {
            val current = binding.mediaViewPager.currentItem
            val newPos = (current - 1).coerceAtLeast(0)
            Log.e("BTN_TAG", "Prev button clicked, current: $current, newPos: $newPos")
            binding.mediaViewPager.setCurrentItem(newPos, true)
        }

        binding.btnNext.setOnClickListener {

            val current = binding.mediaViewPager.currentItem
            val maxIndex = binding.mediaViewPager.adapter?.itemCount?.minus(1) ?: 0
            val newPos = (current + 1).coerceAtMost(maxIndex)
            Log.e("BTN_TAG", "Next button clicked, current: $current, newPos: $newPos")
            binding.mediaViewPager.setCurrentItem(newPos, true)
        }

        product?.let {
            Log.d("ProductDetails", "ID: ${it.id}")
            Log.d("ProductDetails", "Name: ${it.productName}")
            Log.d("ProductDetails", "Category: ${it.productCategory}")
            Log.d("ProductDetails", "SKU: ${it.sku}")
            Log.d("ProductDetails", "Price: ${it.price}")
            Log.d("ProductDetails", "Description: ${it.description}")
            Log.d("ProductDetails", "Is Image Selected: ${it.isImageSelected}")
            Log.d("ProductDetails", "Tag ID: ${it.tagId}")
            Log.d("ProductDetails", "Status: ${it.status}")
            Log.d("ProductDetails", "Created At: ${it.createdAt}")

            Log.d("ProductDetails", "Selected Images:")
            it.selectedImages.forEachIndexed { index, image ->
                Log.d("ProductDetails", "Image $index: $image")
            }

            Log.d("ProductDetails", "Selected Video: ${it.selectedVideo ?: "None"}")
        } ?: run {
            Log.e("ProductDetails", "No product data available!")
        }

        binding.swipeRefreshLayout.setOnRefreshListener {
            refreshProductDetails()
        }

        refreshProductDetails()
    }

    private fun refreshProductDetails() {
        val tableName = PRODUCT_TABLE

        binding.shimmerLayout.visibility = View.VISIBLE
        binding.contentLayout.visibility = View.GONE
        binding.shimmerLayout.startShimmer()


        val productId =  ProductHolder.selectedProduct?.id
        if (productId.isNullOrEmpty()) {
            showError("Product ID not found")
            return
        }

        AwsManager.getProductById(tableName, productId, onSuccess = { product ->
            binding.swipeRefreshLayout.isRefreshing = false
            binding.shimmerLayout.stopShimmer()
            binding.shimmerLayout.visibility = View.GONE
            binding.contentLayout.visibility = View.VISIBLE

            showProductDetails(product)
        }, onError = {
            binding.swipeRefreshLayout.isRefreshing = false
            showError(getString(R.string.oops_product_id_not_found))
        })
    }

    private fun showError(message: String) {
        binding.contentLayout.visibility = View.GONE
        binding.shimmerLayout.stopShimmer()
        binding.shimmerLayout.visibility = View.GONE
        binding.errorMessage.visibility = View.VISIBLE
        binding.errorMessage.text = message

        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).apply {
            show()
        }

    }

    private fun showProductDetails(product: ProductModel) {
        binding.textStyleNo.text = product.tagId ?: "N/A"

        binding.rowProductName.set("Product Name:", product.productName ?: "N/A")
        binding.rowCategory.set("Category:", product.productCategory ?: "N/A")
        binding.rowPrice.set("Price:", "$${product.price ?: "0"}")
        binding.rowSku.set("SKU:", product.sku ?: "N/A")

        binding.textStatus.text = product.status ?: "Unknown"
        when (product.status?.lowercase()) {
            "active" -> {
                binding.textStatus.setBackgroundResource(R.drawable.bg_status_active)
                binding.textStatus.setTextColor(
                    ContextCompat.getColor(
                        requireContext(),
                        R.color.active_textColor
                    )
                )
            }

            "inactive" -> {
                binding.textStatus.setBackgroundResource(R.drawable.bg_status_inactive)
                binding.textStatus.setTextColor(
                    ContextCompat.getColor(
                        requireContext(),
                        R.color.red
                    )
                )
            }

            "pending" -> {
                binding.textStatus.setBackgroundResource(R.drawable.bg_status_pending)
                binding.textStatus.setTextColor(
                    ContextCompat.getColor(
                        requireContext(),
                        R.color.orange
                    )
                )
            }

            else -> {
            }
        }
    }


    fun ItemProductRowBinding.set(labelText: String, valueText: String) {
        label.text = labelText
        value.text = valueText
    }

    companion object {
        private const val ARG_PRODUCT = "product"
    }
}