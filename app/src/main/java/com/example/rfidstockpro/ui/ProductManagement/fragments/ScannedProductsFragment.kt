package com.example.rfidstockpro.ui.ProductManagement.fragments

import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.rfidstockpro.databinding.FragmentInventoryBinding
import com.example.rfidstockpro.factores.UHFViewModelFactory
import com.example.rfidstockpro.repository.UHFRepository
import com.example.rfidstockpro.ui.ProductManagement.viewmodels.InventoryViewModel
import com.example.rfidstockpro.ui.ProductManagement.viewmodels.ScannedProductsViewModel
import com.example.rfidstockpro.ui.activities.DashboardActivity.Companion.uhfDevice
import com.example.rfidstockpro.ui.inventory.InventoryAdapter
import com.example.rfidstockpro.viewmodel.UHFReadViewModel
import com.rscja.deviceapi.interfaces.KeyEventCallback


class ScannedProductsFragment : Fragment() {

    private lateinit var binding: FragmentInventoryBinding
    private lateinit var scannedProductsViewModel: ScannedProductsViewModel
    private lateinit var uhfReadViewModel: UHFReadViewModel
    private lateinit var inventoryViewModel: InventoryViewModel

    private lateinit var inventoryAdapter: InventoryAdapter
    private val scannedTagIds = mutableSetOf<String>()
    private var isScanning = false

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
        binding.btnLoadMore.setOnClickListener {
            scannedProductsViewModel.loadNextPage()
        }

    }

    private fun setupRecyclerView() {
        inventoryAdapter = InventoryAdapter(emptyList()) { product, anchorView ->
            // You can reuse showCustomPopupMenu if needed
        }

        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = inventoryAdapter
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
                scannedProductsViewModel.setTagFilters(uniqueTagSet.toList())
            }
        }

        scannedProductsViewModel.isLoading.observe(viewLifecycleOwner) { loading ->
            binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        }

        scannedProductsViewModel.products.observe(viewLifecycleOwner) { productList  ->
            inventoryAdapter.updateList(productList)
            binding.btnLoadMore.visibility =
                if (scannedProductsViewModel.hasMoreData()) View.VISIBLE else View.GONE
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
