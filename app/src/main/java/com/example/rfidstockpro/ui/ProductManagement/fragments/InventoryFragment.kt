package com.example.rfidstockpro.ui.ProductManagement.fragments

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.rfidstockpro.R
import com.example.rfidstockpro.aws.models.ProductModel
import com.example.rfidstockpro.databinding.FragmentInventoryBinding
import com.example.rfidstockpro.ui.ProductManagement.ProductPopupMenu
import com.example.rfidstockpro.ui.ProductManagement.helper.ProductHolder
import com.example.rfidstockpro.ui.ProductManagement.viewmodels.InventoryViewModel
import com.example.rfidstockpro.ui.activities.AddProductActivity
import com.example.rfidstockpro.ui.inventory.InventoryAdapter


class InventoryFragment : Fragment() {
    private lateinit var binding: FragmentInventoryBinding
    private lateinit var viewModel: InventoryViewModel
    private lateinit var inventoryAdapter: InventoryAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentInventoryBinding.inflate(inflater, container, false)
        viewModel = ViewModelProvider(this)[InventoryViewModel::class.java]


        init()
        setupRecyclerView()
        observeViewModel()

        viewModel.loadNextPage()
        return binding.root
    }

    fun init() {
        binding.btnLoadMore.setOnClickListener {
            viewModel.loadNextPage()
        }
    }

    private fun setupRecyclerView() {

        inventoryAdapter = InventoryAdapter(emptyList()) { product, anchorView ->
            ProductHolder.selectedProduct = product
            ProductPopupMenu(requireContext(), anchorView, product, object : ProductPopupMenu.PopupActionListener {
                override fun onViewClicked(product: ProductModel) {
                    openProductDetails(product)
                }
                override fun onEditClicked(product: ProductModel) {
                    val intent = Intent(requireContext(), AddProductActivity::class.java)
                    intent.putExtra("source", "EditScreen")
                    startActivity(intent)
                }
                override fun onLocateClicked(product: ProductModel) {}
                override fun onUpdateClicked(product: ProductModel) {}
                override fun onDeleteClicked(product: ProductModel) {}
            }).show()
        }

        val layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.layoutManager = layoutManager
        binding.recyclerView.adapter = inventoryAdapter


    }

    private fun openProductDetails(productModel: ProductModel) {
        ProductHolder.selectedProduct = productModel
        val fragment = ProductDetailsFragment()
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragmentProductDetail, fragment)
            .addToBackStack(null)
            .commit()

    }

    private fun observeViewModel() {

        viewModel.isLoading.observe(viewLifecycleOwner) {                                                                                                          isLoading ->

            val productList = viewModel.products.value ?: emptyList()
            // Show progress only if loading and no data yet
            binding.scanProgressBar.visibility =
                if (isLoading && productList.isEmpty()) View.VISIBLE else View.GONE

            // Show "No items" only if not loading and list is empty
            binding.noItemsText.visibility =
                if (!isLoading && productList.isEmpty()) View.VISIBLE else View.GONE

            // Show RecyclerView only if list is not empty
            binding.recyclerView.visibility =
                if (productList.isNotEmpty()) View.VISIBLE else View.GONE

            binding.btnLoadMore.visibility =
                if (!isLoading && productList.isNotEmpty()) View.VISIBLE else View.GONE

            binding.loadMoreProgress.visibility =
                if (isLoading) View.VISIBLE else View.GONE

        }

        viewModel.products.observe(viewLifecycleOwner) { productList ->
            inventoryAdapter.updateList(productList)
            binding.recyclerView.visibility =
                if (productList.isNotEmpty()) View.VISIBLE else View.GONE
            binding.noItemsText.visibility = if (productList.isEmpty()) View.VISIBLE else View.GONE

            // Update count
            val total = viewModel.totalCount.value ?: 0
            Log.e("TOTAL_TAG", "observeViewModel:==>> $total")
            binding.itemCountText.text = "${productList.size} of $total"

            // Show/hide load more
            binding.loadMoreContainer.visibility =
                if (productList.size < total) View.VISIBLE else View.GONE
        }

        viewModel.totalCount.observe(viewLifecycleOwner) { total ->
            val currentCount = viewModel.products.value?.size ?: 0
            Log.e("TOTAL_TAG", "observeViewModel:~~>> $total")
            binding.itemCountText.text = "$currentCount of $total"
            binding.loadMoreContainer.visibility =
                if (currentCount < total) View.VISIBLE else View.GONE
        }

    }


}
