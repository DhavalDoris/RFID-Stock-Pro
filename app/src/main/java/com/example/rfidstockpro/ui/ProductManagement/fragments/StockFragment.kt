package com.example.rfidstockpro.ui.ProductManagement.fragments

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.rfidstockpro.R
import com.example.rfidstockpro.aws.models.ProductModel
import com.example.rfidstockpro.databinding.FragmentStockBinding
import com.example.rfidstockpro.ui.ProductManagement.ProductPopupMenu
import com.example.rfidstockpro.ui.ProductManagement.helper.ProductHolder
import com.example.rfidstockpro.ui.ProductManagement.viewmodels.StockViewModel
import com.example.rfidstockpro.ui.activities.AddProductActivity
import com.example.rfidstockpro.ui.inventory.InventoryAdapter


class StockFragment : Fragment() {
    private lateinit var binding: FragmentStockBinding
    private lateinit var viewModel: StockViewModel
    private lateinit var inventoryAdapter: InventoryAdapter
    private lateinit var editProductLauncher: ActivityResultLauncher<Intent>


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        editProductLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == AppCompatActivity.RESULT_OK) {
                viewModel.refreshData() // You can create this method to re-fetch products from the beginning
            }
        }
    }


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentStockBinding.inflate(inflater, container, false)
        viewModel = ViewModelProvider(this)[StockViewModel::class.java]

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
        binding.swipeRefreshLayout.setOnRefreshListener {
            viewModel.refreshData()
        }

        viewModel.isDeleting.observe(viewLifecycleOwner) { isDeleting ->
            binding.scanProgressBar.visibility = if (isDeleting) View.VISIBLE else View.GONE
        }

        viewModel.deletionError.observe(viewLifecycleOwner) { errorMessage ->
            if (errorMessage != null) {
                Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupRecyclerView() {

        inventoryAdapter = InventoryAdapter(
            emptyList(),
            onItemClick = { product, anchorView ->
                ProductPopupMenu(
                    requireContext(),
                    anchorView,
                    product,
                    object : ProductPopupMenu.PopupActionListener {
                        override fun onViewClicked(product: ProductModel) {
                            openProductDetails(product)
                        }

                        override fun onEditClicked(product: ProductModel) {
                            val intent = Intent(requireContext(), AddProductActivity::class.java)
                            intent.putExtra("source", "EditScreen")
                            editProductLauncher.launch(intent)
//                          startActivity(intent)
                        }
                        override fun onLocateClicked(product: ProductModel) {}
                        override fun onUpdateClicked(product: ProductModel) {}
                        override fun onDeleteClicked(product: ProductModel) {
                            AlertDialog.Builder(requireContext())
                                .setTitle(getString(R.string.delete_product))
                                .setMessage(getString(R.string.are_you_sure_you_want_to_delete_this_product_and_its_media))
                                .setPositiveButton(getString(R.string.delete)) { _, _ ->
                                    viewModel.deleteProduct(product)
                                }
                                .setNegativeButton(getString(R.string.cancel), null)
                                .show()
                        }
                    }
                ).show()
            },
            onItemViewClick = { product -> // 👈 This gets called on full item click
                // Or: do something like navigate to product details, etc.
                openProductDetails(product)
            }
        )

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

            binding.recyclerView.visibility = if (productList.isNotEmpty()) View.VISIBLE else View.GONE
            binding.noItemsText.visibility = if (productList.isEmpty()) View.VISIBLE else View.GONE
            binding.swipeRefreshLayout.isRefreshing = false // ✅ Stop refresh here


            // Update count
            val total = viewModel.totalCount.value ?: 0
            Log.e("TOTAL_TAG", "observeViewModel:==>> $total")
            binding.itemCountText.text = "${productList.size} of $total"
            // Show/hide load more
            binding.loadMoreContainer.visibility = if (productList.size < total) View.VISIBLE else View.GONE
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
