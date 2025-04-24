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
import com.example.rfidstockpro.RFIDApplication.Companion.IN_OUT_COLLECTIONS_TABLE
import com.example.rfidstockpro.Utils.StatusBarUtils
import com.example.rfidstockpro.aws.AwsManager
import com.example.rfidstockpro.databinding.ActivityCreateCollectionBinding
import com.example.rfidstockpro.inouttracker.viewmodel.CreateCollectionViewModel
import com.example.rfidstockpro.ui.ProductManagement.activity.ProductManagementActivity
import com.example.rfidstockpro.ui.activities.DeviceListActivity.TAG

class CreateCollectionActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCreateCollectionBinding
    private val viewModel: CreateCollectionViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCreateCollectionBinding.inflate(layoutInflater)
        setContentView(binding.root)
        StatusBarUtils.setStatusBarColor(this)
        updateToolbarTitleAddItem(getString(R.string.in_out_ntracker_header))

        init()
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
        binding.btnCreateCollction.setOnClickListener {
            val collectionName = binding.etCollectionName.text.toString().trim()
            val description = binding.etDescription.text.toString().trim()

         /*   val sessionManager = SessionManager.getInstance(this) // Get Singleton Instance
            val userName = sessionManager.getUserName()

            val userId = userName

            if (collectionName.isEmpty()) {
                Toast.makeText(this, "Collection name cannot be empty", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            binding.btnCreateCollction.isEnabled = false
            AwsManager.checkIfCollectionNameExists(
                tableName       = IN_OUT_COLLECTIONS_TABLE,
                collectionName  = collectionName
            ) { exists ->
                // Back on the main thread already
                binding.btnCreateCollction.isEnabled = true

                if (exists) {
                    Toast.makeText(this, "Collection “$collectionName” already exists", Toast.LENGTH_LONG).show()
                } else {
                    // not a duplicate — go ahead and create it
                    viewModel.createCollection(collectionName, description, productIds, userId)
                }
            }*/

           /* CollectionUtils.handleCreateCollection(
                context = this,
                collectionName = collectionName,
                description = description,
                productIds = productIds,
                onStart = { binding.btnCreateCollction.isEnabled = false },
                onEnd = { binding.btnCreateCollction.isEnabled = true },
                onSuccess = {
                    val userId = SessionManager.getInstance(this).getUserName()
                    viewModel.createCollection(collectionName, description, productIds, userId)
                },
                onFailure = {
                    // Optional: log or UI changes
                }
            )*/

            if (collectionName.isEmpty()) {
                Toast.makeText(this, "Collection name cannot be empty", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            AwsManager.checkIfCollectionNameExists(
                tableName       = IN_OUT_COLLECTIONS_TABLE,
                collectionName  = collectionName
            ) { exists ->
                // Back on the main thread already
                binding.btnCreateCollction.isEnabled = true

                if (exists) {
                    Toast.makeText(this, "Collection “$collectionName” already exists", Toast.LENGTH_LONG).show()
                } else {
                    // not a duplicate — go ahead and create it
                    val intent = Intent(this, ProductManagementActivity::class.java).apply {
                        putExtra("comesFrom", "collection")
                        putExtra("collection_name", collectionName)
                        putExtra("description", description)
                    }
                    startActivity(intent)
                }
            }

        }

        viewModel.isCollectionCreated.observe(this) { success ->
            if (success) {
                Toast.makeText(this, "Collection created successfully", Toast.LENGTH_SHORT).show()
                finish()
            } else {
                Toast.makeText(this, "Failed to create collection", Toast.LENGTH_SHORT).show()
            }
        }
    }
}