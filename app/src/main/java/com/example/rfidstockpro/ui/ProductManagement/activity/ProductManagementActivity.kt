package com.example.rfidstockpro.ui.ProductManagement.activity

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.lifecycle.ViewModelProvider
import com.example.rfidstockpro.R
import com.example.rfidstockpro.Utils.StatusBarUtils
import com.example.rfidstockpro.Utils.ToastUtils.showToast
import com.example.rfidstockpro.databinding.ActivityProductManagementBinding
import com.example.rfidstockpro.inouttracker.CollectionUtils
import com.example.rfidstockpro.inouttracker.activity.CreateCollectionActivity
import com.example.rfidstockpro.inouttracker.activity.InOutTrackerActivity
import com.example.rfidstockpro.inouttracker.model.CollectionModel
import com.example.rfidstockpro.sharedpref.SessionManager
import com.example.rfidstockpro.ui.ProductManagement.BluetoothConnectionManager
import com.example.rfidstockpro.ui.ProductManagement.adapters.ProductPagerAdapter
import com.example.rfidstockpro.ui.ProductManagement.fragments.InventoryProductsFragment
import com.example.rfidstockpro.ui.ProductManagement.viewmodels.ProductManagementViewModel
import com.example.rfidstockpro.ui.activities.DashboardActivity.Companion.ShowCheckBoxinProduct
import com.example.rfidstockpro.ui.activities.DashboardActivity.Companion.isShowDuplicateTagId
import com.example.rfidstockpro.ui.activities.DeviceListActivity.TAG
import com.example.rfidstockpro.viewmodel.DashboardViewModel
import com.google.android.material.tabs.TabLayoutMediator
import com.rscja.deviceapi.interfaces.ConnectionStatus

class ProductManagementActivity : AppCompatActivity(), InventoryProductsFragment.ToolbarCallback {

    private lateinit var binding: ActivityProductManagementBinding
    private lateinit var dashboardViewModel: DashboardViewModel
    var collectionName: String = ""
    var collectionId: String = ""
    var description: String = ""
    private var selectedItems: ArrayList<CollectionModel>? = null

    //    var productIds: MutableList<String> = mutableListOf()
    private var productIds: List<String>? = null
    private lateinit var viewModel: ProductManagementViewModel

    @SuppressLint("MissingPermission")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProductManagementBinding.inflate(layoutInflater)
        setContentView(binding.root)
        StatusBarUtils.setStatusBarColor(this)

        init()
    }

    @SuppressLint("MissingPermission")
    fun init() {

        isShowDuplicateTagId = true
        dashboardViewModel = ViewModelProvider(this)[DashboardViewModel::class.java]
        BluetoothConnectionManager.registerLaunchers(
            this,  // Use 'this' instead of requireActivity() to ensure proper lifecycle
            onDeviceConnected = { device ->
                Log.d("Bluetooth", "Connected to device: ${device.name}")
                dashboardViewModel.notifyDeviceConnected(device)
                UHFConnectionManager.updateConnectionStatus(ConnectionStatus.CONNECTED, device)
            },
            onStatusUpdate = { status, _ ->
                if (status == ConnectionStatus.DISCONNECTED) {
                    showToast(this, "Disconnected")
                    dashboardViewModel.notifyConnectionStatus(status)
                    UHFConnectionManager.updateConnectionStatus(ConnectionStatus.DISCONNECTED, null)
                }
            }
        )
        var isFromCollection = false
//        val adapter = ProductPagerAdapter(this, showBothTabs = !isFromCollection)
        val comesFrom = intent.getStringExtra("comesFrom")
        Log.d("IntentCheck", "Came ~~~> " + comesFrom)
        if (comesFrom == "collection") {
            isFromCollection = true
            ShowCheckBoxinProduct = true
            // Hide tabs and disable swiping
            binding.tabLayout.visibility = View.GONE
            binding.viewPager.setCurrentItem(0, false)
            binding.viewPager.isUserInputEnabled = false
            updateToolbarTitleAddItem(getString(R.string.add_to_collection), null)
            collectionName = intent.getStringExtra("collection_name")!!
            description = intent.getStringExtra("description")!!
        } else if (comesFrom == "InOutTracker") {
            isFromCollection = false
            // 👉 Handle logic specifically for InOutTracker case
            val comesFrom = intent.getStringExtra("comesFrom")
            collectionName = intent.getStringExtra("collectionName").toString()
            collectionId = intent.getStringExtra("collectionId").toString()
            description = intent.getStringExtra("description")!!
            productIds = intent.getStringArrayListExtra("productIds") ?: arrayListOf()
            updateToolbarTitleAddItem(getString(R.string.list_of_products), true)
        } else if (comesFrom == "TrackCollection") {
            isFromCollection = true // For Showing Only Scanning Tab
            selectedItems = intent.getSerializableExtra("selected_items") as? ArrayList<CollectionModel>
            Log.d("INVENTORYPRODUCTSFRAGMENT_TAG", "selectedItems " + selectedItems)
            binding.tabLayout.visibility = View.GONE
            binding.viewPager.setCurrentItem(0, false)
            binding.viewPager.isUserInputEnabled = false
            updateToolbarTitleAddItem("Track Collection", null)
        }

        val adapter = ProductPagerAdapter(
            this,
            showBothTabs = !isFromCollection,
            comesFrom = comesFrom,
            collectionName = collectionName,
            collectionId = collectionId,
            productIds = productIds,
            selectedItems = selectedItems
        )
        binding.viewPager.adapter = adapter
        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> "Stock"
                1 -> "Inventory"
                else -> ""
            }
        }.attach()
        binding.viewPager.isUserInputEnabled = true
        viewModel = ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory.getInstance(application)
        )[ProductManagementViewModel::class.java]

        observeViewModel()
    }

    private fun observeViewModel() {
        viewModel.isCollectionCreated.observe(this) { isCreated ->
            if (isCreated) {
                Toast.makeText(this, "Collection created successfully", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, InOutTrackerActivity::class.java))
                finish() // or navigate back
            } else {
                Toast.makeText(this, "Failed to create collection", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun updateToolbarTitleAddItem(title: String, showFilter: Boolean?) {
        val toolbarTitle = findViewById<AppCompatTextView>(R.id.tvToolbarTitle)
        val toolbarSearch = findViewById<AppCompatImageView>(R.id.ivSearch)
        val toolbarFilter = findViewById<AppCompatImageView>(R.id.ivFilter)
        val toolbarDone = findViewById<AppCompatImageView>(R.id.ivDone)
        Log.e(TAG, "updateToolbarTitle: ")
        toolbarTitle!!.text = title

        toolbarSearch.visibility = View.GONE
        if (showFilter != null) {
            if (showFilter) {
                toolbarFilter.visibility = View.VISIBLE
            } else {
                toolbarDone.visibility = View.VISIBLE
            }
        } else {
            toolbarFilter.visibility = View.GONE
        }

        toolbarFilter.setOnClickListener {
            Toast.makeText(this, "Filter", Toast.LENGTH_SHORT).show()
        }

        toolbarDone.setOnClickListener {

            Log.e("ToolbarCallback", "updateToolbarTitleAddItem: " + productIds)

            CollectionUtils.handleCreateCollection(
                context = this,
                collectionName = collectionName,
                description = description,
                productIds = productIds!!,
                onSuccess = {
                    val userId = SessionManager.getInstance(this).getUserName()
                    viewModel.createCollection(collectionName, description, productIds!!, userId)
                },
                onFailure = {
                    // Optional: log or UI changes
                }
            )
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
//        startActivity(Intent(this, DashboardActivity::class.java))
    }

    override fun updateToolbar(selectedIds: List<String>) {
        updateToolbarTitleAddItem(getString(R.string.add_to_collection), false)
        // 👇 Here you receive the selected IDs from fragment
        Log.d("ToolbarCallback", "Selected IDs from fragment: $selectedIds")

        // Update your toolbar or selection UI
        if (selectedIds.isNotEmpty()) {
            // Show toolbar actions (edit/delete/etc.)
            productIds = selectedIds
        } else {
            // Hide toolbar actions
        }
    }
}