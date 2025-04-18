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
import com.example.rfidstockpro.R
import com.example.rfidstockpro.Utils.StatusBarUtils
import com.example.rfidstockpro.databinding.ActivityInOutBinding
import com.example.rfidstockpro.inouttracker.viewmodel.CreateCollectionViewModel
import com.example.rfidstockpro.ui.activities.DeviceListActivity.TAG

class InOutTrackerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityInOutBinding
    private val viewModel: CreateCollectionViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityInOutBinding.inflate(layoutInflater)
        setContentView(binding.root)
        StatusBarUtils.setStatusBarColor(this)
        updateToolbarTitleAddItem(getString(R.string.in_out_ntracker_header))

        init()
        setupRecyclerView()
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

    fun init() {

        binding.ListOfCollections

        binding.fabAdd.setOnClickListener {
            startActivity(Intent(this, CreateCollectionActivity::class.java))
        }
    }
}