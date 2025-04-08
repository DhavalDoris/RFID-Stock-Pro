package com.example.rfidstockpro.ui.ProductManagement

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
import com.example.rfidstockpro.R
import com.example.rfidstockpro.Utils.StatusBarUtils
import com.example.rfidstockpro.databinding.ActivityProductManagementBinding
import com.example.rfidstockpro.ui.ProductManagement.adapters.ProductPagerAdapter
import com.example.rfidstockpro.ui.activities.DeviceListActivity.TAG
import com.google.android.material.tabs.TabLayoutMediator

class ProductManagementActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProductManagementBinding


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProductManagementBinding.inflate(layoutInflater)
        setContentView(binding.root)
        StatusBarUtils.setStatusBarColor(this)
        updateToolbarTitleAddItem(getString(R.string.product_management))

        init()
    }

    fun init() {
        val adapter = ProductPagerAdapter(this)
        binding.viewPager.adapter = adapter

        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> "Inventory"
                1 -> "Scan"
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
}