package com.example.rfidstockpro.ui.ProductManagement.fragments

import InventoryProductsViewModel
import UHFConnectionManager
import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.rfidstockpro.R
import com.example.rfidstockpro.Utils.ToastUtils.showToast
import com.example.rfidstockpro.aws.models.ProductModel
import com.example.rfidstockpro.databinding.FragmentStockBinding
import com.example.rfidstockpro.factores.UHFViewModelFactory
import com.example.rfidstockpro.repository.UHFRepository
import com.example.rfidstockpro.ui.ProductManagement.BluetoothConnectionManager
import com.example.rfidstockpro.ui.ProductManagement.ProductPopupMenu
import com.example.rfidstockpro.ui.ProductManagement.helper.ProductHolder
import com.example.rfidstockpro.ui.activities.AddProductActivity
import com.example.rfidstockpro.ui.activities.DashboardActivity.Companion.uhfDevice
import com.example.rfidstockpro.ui.inventory.InventoryAdapter
import com.example.rfidstockpro.viewmodel.DashboardViewModel
import com.example.rfidstockpro.viewmodel.UHFReadViewModel
import com.rscja.deviceapi.interfaces.ConnectionStatus
import com.rscja.deviceapi.interfaces.KeyEventCallback


class InventoryProductsFragment : Fragment() {

    private lateinit var binding: FragmentStockBinding
    private lateinit var inventoryProductsViewModel: InventoryProductsViewModel
    private lateinit var uhfReadViewModel: UHFReadViewModel
    private lateinit var inventoryAdapter: InventoryAdapter
    var latestTotal = 0
    var latestIsLoading = false
    private lateinit var dashboardViewModel: DashboardViewModel
    private val uniqueTagSet = mutableSetOf<String>()


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentStockBinding.inflate(inflater, container, false)
        inventoryProductsViewModel = ViewModelProvider(this)[InventoryProductsViewModel::class.java]
        return binding.root
    }

    @SuppressLint("MissingPermission")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val repository = UHFRepository(uhfDevice)
        uhfReadViewModel =
            ViewModelProvider(this, UHFViewModelFactory(repository))[UHFReadViewModel::class.java]
        dashboardViewModel = ViewModelProvider(requireActivity())[DashboardViewModel::class.java]


        initView()
        setupRecyclerView()
        observeActions()
        setupRFIDGunTrigger()


    }

    @SuppressLint("MissingPermission")
    private fun initView() {

        binding.swipeRefreshLayout.isRefreshing = false

        val rfidConnectionView = binding.connectRFID.rlStatScan
        if (UHFConnectionManager.getConnectionStatus() == ConnectionStatus.CONNECTED) {
            rfidConnectionView.visibility = View.GONE
        } else {
            rfidConnectionView.visibility = View.VISIBLE
        }

        val btnConnectScanner = binding.connectRFID.btnConnectScannerAdd
        btnConnectScanner.setOnClickListener {
            if (dashboardViewModel.isConnected.value == true) {
                dashboardViewModel.disconnect(true)
            } else {
                BluetoothConnectionManager.showBluetoothDevice(requireActivity())
            }
        }
    }

    private fun openProductDetails(productModel: ProductModel) {
        ProductHolder.selectedProduct = productModel
        val fragment = ProductDetailsFragment()
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragmentProductDetail, fragment)
            .addToBackStack(null)
            .commit()

    }


    private fun setupRecyclerView() {
        binding.noItemsText.visibility = View.VISIBLE

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
//                            editProductLauncher.launch(intent)
                            startActivity(intent)
                        }
                        override fun onLocateClicked(product: ProductModel) {}
                        override fun onUpdateClicked(product: ProductModel) {}
                        override fun onDeleteClicked(product: ProductModel) {}
                    }
                ).show()
            },
            onItemViewClick = { product -> // 👈 This gets called on full item click
                // Or: do something like navigate to product details, etc.
                openProductDetails(product)
            }
        )

        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = inventoryAdapter
        }

        binding.btnLoadMore.setOnClickListener {
            binding.btnLoadMore.visibility = View.GONE
            binding.loadMoreProgress.visibility = View.VISIBLE
            inventoryProductsViewModel.loadNextPage()
        }
    }

    @SuppressLint("MissingPermission")
    private fun observeActions() {
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
                // Show progress while scanning & matching
                inventoryProductsViewModel.setMatchedTagIds(uniqueTagSet.toList())
            }
        }

        inventoryProductsViewModel.pagedProducts.observe(viewLifecycleOwner) { products ->
            inventoryAdapter.updateList(products)

            // Hide loading when items are received
//            binding.scanProgressBar.visibility = View.VISIBLE

            val total = inventoryProductsViewModel.totalCount.value ?: 0
            val currentCount = products.size
            binding.itemCountText.text = "$currentCount of $total"

            if (products.isNullOrEmpty()) {
                binding.noItemsText.visibility = View.VISIBLE
            } else {
                binding.noItemsText.visibility = View.GONE
            }

        }


        inventoryProductsViewModel.isPageLoading.observe(viewLifecycleOwner) { isLoading ->
            latestIsLoading = isLoading
            val currentCount = inventoryProductsViewModel.pagedProducts.value?.size ?: 0
            updateLoadMoreUI(isLoading, currentCount, latestTotal)
        }

        inventoryProductsViewModel.totalCount.observe(viewLifecycleOwner) { total ->
            latestTotal = total
            val currentCount = inventoryProductsViewModel.pagedProducts.value?.size ?: 0
            updateLoadMoreUI(latestIsLoading, currentCount, total)
        }

        dashboardViewModel.deviceConnected.observe(viewLifecycleOwner) { device ->
            device?.let {
                Log.d("Bluetooth", "Connected to: ${device.name}")
                binding.connectRFID.rlStatScan.visibility = View.GONE
            }
        }

        dashboardViewModel.connectionStatus.observe(viewLifecycleOwner) { status ->
            if (status == ConnectionStatus.DISCONNECTED) {
                showToast(requireContext(), "Disconnected")
                binding.connectRFID.rlStatScan.visibility = View.VISIBLE
            }
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
