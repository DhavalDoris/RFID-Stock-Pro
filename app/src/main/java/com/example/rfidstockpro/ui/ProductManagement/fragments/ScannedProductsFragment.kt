package com.example.rfidstockpro.ui.ProductManagement.fragments

import ScannedProductsViewModel
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.rfidstockpro.aws.models.ProductModel
import com.example.rfidstockpro.databinding.FragmentInventoryBinding
import com.example.rfidstockpro.factores.UHFViewModelFactory
import com.example.rfidstockpro.repository.UHFRepository
import com.example.rfidstockpro.ui.ProductManagement.ProductPopupMenu
import com.example.rfidstockpro.ui.activities.DashboardActivity.Companion.uhfDevice
import com.example.rfidstockpro.ui.inventory.InventoryAdapter
import com.example.rfidstockpro.viewmodel.UHFReadViewModel
import com.rscja.deviceapi.interfaces.KeyEventCallback


class ScannedProductsFragment : Fragment() {

    private lateinit var binding: FragmentInventoryBinding
    private lateinit var scannedProductsViewModel: ScannedProductsViewModel
    private lateinit var uhfReadViewModel: UHFReadViewModel
    private lateinit var inventoryAdapter: InventoryAdapter

    var latestTotal = 0
    var latestIsLoading = false

    private val uniqueTagSet = mutableSetOf<String>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentInventoryBinding.inflate(inflater, container, false)
        scannedProductsViewModel = ViewModelProvider(this)[ScannedProductsViewModel::class.java]
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val repository = UHFRepository(uhfDevice)
        uhfReadViewModel = ViewModelProvider(this, UHFViewModelFactory(repository))[UHFReadViewModel::class.java]

        setupRecyclerView()
        observeProducts()
        setupRFIDGunTrigger()


    }

    private fun setupRecyclerView() {
        inventoryAdapter = InventoryAdapter(emptyList()) { product, anchorView ->
            ProductPopupMenu(requireContext(), anchorView, product, object : ProductPopupMenu.PopupActionListener {
                override fun onViewClicked(product: ProductModel) {
                    Toast.makeText(requireContext(), "View: ${product.productName}", Toast.LENGTH_SHORT).show()
                }

                override fun onEditClicked(product: ProductModel) {}
                override fun onLocateClicked(product: ProductModel) {}
                override fun onUpdateClicked(product: ProductModel) {}
                override fun onDeleteClicked(product: ProductModel) {}
            }).show()
        }

        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = inventoryAdapter
        }

        binding.btnLoadMore.setOnClickListener {
            binding.btnLoadMore.visibility = View.GONE
            binding.loadMoreProgress.visibility = View.VISIBLE
            scannedProductsViewModel.loadNextPage()
        }
    }

    private fun observeProducts() {
        uhfReadViewModel.tagList.observe(viewLifecycleOwner) { tagList ->
            var newTagsAdded = false

            tagList.forEach { tag ->
                val tagId = tag.generateTagString()
                if (uniqueTagSet.add(tagId)) {
                    // 👇 Only logs if it's a new tag
                    Log.d("ScannedProducts", "📦 Unique Tag Scanned: $tagId")
                    newTagsAdded = true
                }
            }

            if (newTagsAdded) {
                // 🔄 Pass the updated list to ViewModel
                scannedProductsViewModel.setMatchedTagIds(uniqueTagSet.toList())
            }
        }

        scannedProductsViewModel.pagedProducts.observe(viewLifecycleOwner) { products ->
            inventoryAdapter.updateList(products)

            val total = scannedProductsViewModel.totalCount.value ?: 0
            val currentCount = products.size
            binding.itemCountText.text = "$currentCount of $total"
        }


        scannedProductsViewModel.isPageLoading.observe(viewLifecycleOwner) { isLoading ->
            latestIsLoading = isLoading
            val currentCount = scannedProductsViewModel.pagedProducts.value?.size ?: 0
            updateLoadMoreUI(isLoading, currentCount, latestTotal)
        }

        scannedProductsViewModel.totalCount.observe(viewLifecycleOwner) { total ->
            latestTotal = total
            val currentCount = scannedProductsViewModel.pagedProducts.value?.size ?: 0
            updateLoadMoreUI(latestIsLoading, currentCount, total)
        }
    }

    private fun updateLoadMoreUI(isLoading: Boolean, currentCount: Int, total: Int) {
        val allItemsShown = currentCount >= total

        binding.itemCountText.text = "$currentCount of $total"

        if (allItemsShown || total == 0) {
            binding.loadMoreContainer.visibility = View.GONE
        } else {
            binding.loadMoreContainer.visibility = View.VISIBLE

            // Show loader for minimum 2 seconds
            if (isLoading) {
                binding.btnLoadMore.visibility = View.INVISIBLE
                binding.loadMoreProgress.visibility = View.VISIBLE

                // ⏳ Optional: Minimum 2-second loader experience
                Handler(Looper.getMainLooper()).postDelayed({
                    if (!latestIsLoading) {
                        binding.loadMoreProgress.visibility = View.GONE
                        binding.btnLoadMore.visibility = View.VISIBLE
                    }
                }, 2000)
            } else {
                binding.loadMoreProgress.visibility = View.GONE
                binding.btnLoadMore.visibility = View.VISIBLE
            }
        }
    }


    private fun setupRFIDGunTrigger() {

        uhfDevice.setKeyEventCallback(object : KeyEventCallback {
            override fun onKeyDown(keyCode: Int) {
                Log.d("ScannedProducts", "🔫 Trigger Down")
                uhfReadViewModel.handleKeyDown(keyCode)
            }

            override fun onKeyUp(keyCode: Int) {
                Log.d("ScannedProducts", "🔫 Trigger Up")
                uhfReadViewModel.handleKeyUp(keyCode)
            }
        })

        // Optional: handle regular key events from device
        requireActivity().window.decorView.setOnKeyListener { _, keyCode, event ->
            when (event.action) {
                KeyEvent.ACTION_DOWN -> {
                    uhfReadViewModel.handleKeyDown(keyCode)
                    true
                }

                KeyEvent.ACTION_UP -> {
                    uhfReadViewModel.handleKeyUp(keyCode)
                    true
                }

                else -> false
            }
        }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        uhfDevice.stopInventory()
    }

    private fun stopInventory() {
        uhfDevice.stopInventory()
    }

    override fun onResume() {
        super.onResume()
    }

    override fun onPause() {
        super.onPause()
    }

}
