package com.example.rfidstockpro.ui.ProductManagement

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import com.example.rfidstockpro.R
import com.example.rfidstockpro.Utils.StatusBarUtils
import com.example.rfidstockpro.Utils.ToastUtils.showToast
import com.example.rfidstockpro.databinding.ActivityProductManagementBinding
import com.example.rfidstockpro.ui.ProductManagement.adapters.ProductPagerAdapter
import com.example.rfidstockpro.ui.activities.DashboardActivity
import com.example.rfidstockpro.ui.activities.DeviceListActivity.TAG
import com.example.rfidstockpro.viewmodel.DashboardViewModel
import com.google.android.material.tabs.TabLayoutMediator
import com.rscja.deviceapi.interfaces.ConnectionStatus

class ProductManagementActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProductManagementBinding
    private lateinit var dashboardViewModel: DashboardViewModel

    @SuppressLint("MissingPermission")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProductManagementBinding.inflate(layoutInflater)
        setContentView(binding.root)
        StatusBarUtils.setStatusBarColor(this)
        updateToolbarTitleAddItem(getString(R.string.product_management))

        init()
    }

    @SuppressLint("MissingPermission")
    fun init() {
        dashboardViewModel = ViewModelProvider(this)[DashboardViewModel::class.java]
        BluetoothConnectionManager.registerLaunchers(
            this,  // Use 'this' instead of requireActivity() to ensure proper lifecycle
            onDeviceConnected = { device ->
                Log.d("Bluetooth", "Connected to device: ${device.name}")
                dashboardViewModel.notifyDeviceConnected(device)
                UHFConnectionManager.updateConnectionStatus( ConnectionStatus.CONNECTED, device)
            },
            onStatusUpdate = { status, _ ->
                if (status == ConnectionStatus.DISCONNECTED) {
                    showToast(this, "Disconnected")
                    dashboardViewModel.notifyConnectionStatus(status)
                    UHFConnectionManager.updateConnectionStatus( ConnectionStatus.DISCONNECTED, null)
                }
            }
        )

        val adapter = ProductPagerAdapter(this)
        binding.viewPager.adapter = adapter

        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> "Stock"
                1 -> "Inventory"
                else -> ""
            }
        }.attach()
    }

    fun updateToolbarTitleAddItem(title: String) {
        val toolbarTitle = findViewById<AppCompatTextView>(R.id.tvToolbarTitle)
        val toolbarSearch = findViewById<AppCompatImageView>(R.id.ivSearch)
        val toolbarFilter = findViewById<AppCompatImageView>(R.id.ivFilter)
        Log.e(TAG, "updateToolbarTitle: ")
        toolbarTitle!!.text = title

        toolbarSearch.visibility = View.GONE
        toolbarFilter.visibility = View.VISIBLE

        toolbarFilter.setOnClickListener {
            Toast.makeText(this, "Filter", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        startActivity(Intent(this, DashboardActivity::class.java))
    }
}