package com.example.rfidstockpro.inouttracker.activity

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.rfidstockpro.R
import com.example.rfidstockpro.Utils.StatusBarUtils
import com.example.rfidstockpro.databinding.ActivityInOutBinding
import com.example.rfidstockpro.inouttracker.adapter.CollectionAdapter
import com.example.rfidstockpro.inouttracker.viewmodel.CreateCollectionViewModel
import com.example.rfidstockpro.sharedpref.SessionManager
import com.example.rfidstockpro.ui.ProductManagement.activity.ProductManagementActivity
import com.example.rfidstockpro.ui.activities.DashboardActivity.Companion.ShowCheckBoxinProduct
import com.example.rfidstockpro.ui.activities.DashboardActivity.Companion.isShowDuplicateTagId
import com.example.rfidstockpro.ui.activities.DeviceListActivity.TAG

class InOutTrackerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityInOutBinding
    private val viewModel: CreateCollectionViewModel by viewModels()
    private var adapter: CollectionAdapter? = null
    private var userId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityInOutBinding.inflate(layoutInflater)
        setContentView(binding.root)
        StatusBarUtils.setStatusBarColor(this)
        updateToolbarTitleAddItem(getString(R.string.in_out_ntracker_header))

        init()
        setupRecyclerView()
    }

    fun init() {
        isShowDuplicateTagId = true
        ShowCheckBoxinProduct = true
        viewModel.isLoading.observe(this) { isLoading ->
            binding.swipeRefreshLayout.isRefreshing = isLoading
            Log.d("CollectionListVM", "Loading: $isLoading")
        }
        binding.swipeRefreshLayout.setOnRefreshListener {
            viewModel.fetchCollections(userId!!)
        }
        binding.fabAdd.setOnClickListener {
            startActivity(Intent(this, CreateCollectionActivity::class.java))
        }
    }

    private fun setupRecyclerView() {

        adapter = CollectionAdapter { selectedCollection ->
            // You can open a detail screen or show a toast etc.
          /*  Toast.makeText(
                this,
                "Clicked: ${selectedCollection.collectionName}",
                Toast.LENGTH_SHORT
            ).show()*/

            // Example: Navigate to CollectionDetailsActivity
             /* val intent = Intent(this, ProductManagementActivity::class.java).apply {
                  putExtra("collection_id", selectedCollection.collectionId)
              }
              startActivity(intent)*/
        }
        binding.ListOfCollections.layoutManager = LinearLayoutManager(this)
        binding.ListOfCollections.adapter = adapter

        viewModel.collections.observe(this) {
            Log.d("CollectionListVM", "Observed ${it.size} collections")
            adapter!!.setData(it)
        }

        viewModel.isLoading.observe(this) {
            binding.progressBar.isVisible = it
            binding.ListOfCollections.isVisible = !it
            Log.d("CollectionListVM", "Loading: $it")
        }

        val sessionManager = SessionManager.getInstance(this)
        val userName = sessionManager.getUserName()
        userId = userName

        viewModel.fetchCollections(userId!!)
    }

    fun updateToolbarTitleAddItem(title: String) {
        val toolbarTitle = findViewById<AppCompatTextView>(R.id.tvToolbarTitle)
        val toolbarSearch = findViewById<AppCompatImageView>(R.id.ivSearch)
        val toolbarFilter = findViewById<AppCompatImageView>(R.id.ivFilter)
        Log.e(TAG, "updateToolbarTitle: ")
        toolbarTitle!!.text = title

        toolbarSearch.visibility = View.GONE
        toolbarFilter.visibility = View.GONE

        toolbarFilter.setOnClickListener {
            Toast.makeText(this, "Filter", Toast.LENGTH_SHORT).show()
        }
    }


}