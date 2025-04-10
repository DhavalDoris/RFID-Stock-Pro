package com.example.rfidstockpro.ui.ProductManagement.fragments

import ScannedProductsViewModel
import UHFConnectionManager
import android.annotation.SuppressLint
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
import com.example.rfidstockpro.Utils.ToastUtils.showToast
import com.example.rfidstockpro.aws.models.ProductModel
import com.example.rfidstockpro.databinding.FragmentInventoryBinding
import com.example.rfidstockpro.factores.UHFViewModelFactory
import com.example.rfidstockpro.repository.UHFRepository
import com.example.rfidstockpro.ui.ProductManagement.BluetoothConnectionManager
import com.example.rfidstockpro.ui.ProductManagement.ProductPopupMenu
import com.example.rfidstockpro.ui.activities.DashboardActivity.Companion.uhfDevice
import com.example.rfidstockpro.ui.inventory.InventoryAdapter
import com.example.rfidstockpro.viewmodel.DashboardViewModel
import com.example.rfidstockpro.viewmodel.UHFReadViewModel
import com.rscja.deviceapi.interfaces.ConnectionStatus
import com.rscja.deviceapi.interfaces.KeyEventCallback


class ScannedProductsFragment : Fragment() {

    private lateinit var binding: FragmentInventoryBinding
    private lateinit var scannedProductsViewModel: ScannedProductsViewModel
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
        binding = FragmentInventoryBinding.inflate(inflater, container, false)
        scannedProductsViewModel = ViewModelProvider(this)[ScannedProductsViewModel::class.java]
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

   /* private fun registerActivityResultLaunchers() {
        enableBluetoothLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                if (it.resultCode == Activity.RESULT_OK) {
                    showBluetoothDevice()
                }
            }

        selectDeviceLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == Activity.RESULT_OK) {
                    val data = result.data
                    data?.getStringExtra(BluetoothDevice.EXTRA_DEVICE)?.let { deviceAddress ->
                        if (deviceAddress.isNotEmpty()) {
                            mDevice =
                                BluetoothAdapter.getDefaultAdapter().getRemoteDevice(deviceAddress)
                            connectToDevice(deviceAddress)
                        }
                    }
                }
            }
    }

    private fun connectToDevice(deviceAddress: String) {
        if (uhfDevice.connectStatus == ConnectionStatus.CONNECTING) {
            showToast(requireActivity(), getString(R.string.connecting))
        } else {
            uhfDevice.connect(deviceAddress, object : ConnectionStatusCallback<Any?> {
                override fun getStatus(connectionStatus: ConnectionStatus, device: Any?) {
                    lifecycleScope.launch {
                        if (connectionStatus == ConnectionStatus.CONNECTED) {
                            Log.e("ConetionTAG", "getStatus: " + "IF")
                            UHFConnectionManager.updateConnectionStatus(connectionStatus, device)
                            val rfidConnectionView = binding.connectRFID.rlStatScan
                            rfidConnectionView.visibility = View.GONE
                        } else {
                            UHFConnectionManager.updateConnectionStatus(ConnectionStatus.DISCONNECTED, device)
                            Log.e("ConetionTAG", "getStatus: " + "ELSE")
                        }
                    }
                }
            })
        }
    }

    var mBtAdapter: BluetoothAdapter? = null
    private lateinit var enableBluetoothLauncher: ActivityResultLauncher<Intent>
    private lateinit var selectDeviceLauncher: ActivityResultLauncher<Intent>
    private var mDevice: BluetoothDevice? = null

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private fun showBluetoothDevice() {
        if (mBtAdapter == null) {
            return
        }
        if (!mBtAdapter!!.isEnabled) {
            val enableIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            enableBluetoothLauncher.launch(enableIntent)
        } else {
            val newIntent = Intent(requireActivity(), DeviceListActivity::class.java)
            newIntent.putExtra(SHOW_HISTORY_CONNECTED_LIST, false)
            selectDeviceLauncher.launch(newIntent)
            dashboardViewModel.cancelDisconnectTimer()
        }
    }*/

    private fun setupRecyclerView() {
        binding.noItemsText.visibility = View.VISIBLE

        inventoryAdapter = InventoryAdapter(emptyList()) { product, anchorView ->
            ProductPopupMenu(
                requireContext(),
                anchorView,
                product,
                object : ProductPopupMenu.PopupActionListener {
                    override fun onViewClicked(product: ProductModel) {
                        Toast.makeText(
                            requireContext(),
                            "View: ${product.productName}",
                            Toast.LENGTH_SHORT
                        ).show()
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
                scannedProductsViewModel.setMatchedTagIds(uniqueTagSet.toList())
            }
        }

        scannedProductsViewModel.pagedProducts.observe(viewLifecycleOwner) { products ->
            inventoryAdapter.updateList(products)

            // Hide loading when items are received
//            binding.scanProgressBar.visibility = View.VISIBLE

            val total = scannedProductsViewModel.totalCount.value ?: 0
            val currentCount = products.size
            binding.itemCountText.text = "$currentCount of $total"

            if (products.isNullOrEmpty()) {
                binding.noItemsText.visibility = View.VISIBLE
            } else {
                binding.noItemsText.visibility = View.GONE
            }

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
