package com.example.rfidstockpro.inouttracker.activity

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.rfidstockpro.R
import com.example.rfidstockpro.Utils.StatusBarUtils
import com.example.rfidstockpro.databinding.ActivityInOutBinding
import com.example.rfidstockpro.inouttracker.CollectionUtils
import com.example.rfidstockpro.inouttracker.adapter.CollectionAdapter
import com.example.rfidstockpro.inouttracker.fragment.CollectionDetailListFragment
import com.example.rfidstockpro.inouttracker.model.CollectionModel
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
    private var isUpdatingSelectAll = false // prevent recursive trigger
    var selectedCollectionItems: List<CollectionModel> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityInOutBinding.inflate(layoutInflater)
        setContentView(binding.root)
        StatusBarUtils.setStatusBarColor(this)

        updateToolbarTitleAddItem(getString(R.string.in_out_ntracker_header))
        init()
        setupRecyclerView()
        Log.e("INOUt_TAG", "onCreate: ", )
    }

    fun init() {
        binding.btnNext.isEnabled = false
        binding.btnNext.alpha = 0.5f
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
        binding.selectAllCheckBox.setOnCheckedChangeListener { _, isChecked ->
            if (!isUpdatingSelectAll) {  // Avoid recursion
                isUpdatingSelectAll = true
                adapter?.selectAllItems(isChecked)  // Update selection in the adapter
                isUpdatingSelectAll = false
            }
        }
        binding.btnNext.setOnClickListener {
            val intent = Intent(this, ProductManagementActivity::class.java).apply {
                putExtra("comesFrom", "TrackCollection")
                putExtra("selected_items", ArrayList(selectedCollectionItems)) // Must be Serializable or Parcelable
            }
            startActivity(intent)
        }

    }

    private fun setupRecyclerView() {

        adapter = CollectionAdapter(
            onItemClick = { selectedCollection ->
                ShowCheckBoxinProduct = false
                val intent = Intent(this, ProductManagementActivity::class.java).apply {
                    putExtra("comesFrom", "InOutTracker")
                    putExtra("collection_name", selectedCollection.collectionName)
                    putExtra("description", selectedCollection.description)
                    putExtra("collectionId", selectedCollection.collectionId)
                    putStringArrayListExtra("productIds", ArrayList(selectedCollection.productIds))
                }
                startActivity(intent)
            },
            onSelectionChanged = { selectionState ->
                updateSelectAllCheckbox()
                Log.d("SelectedItems", "Selected Items: ${selectionState.selectedItems}")

                selectedCollectionItems = selectionState.selectedItems // store here

                val isListNotEmpty = selectedCollectionItems.isNotEmpty()
                binding.btnNext.isEnabled = isListNotEmpty
                binding.btnNext.alpha = if (isListNotEmpty) 1.0f else 0.5f // Optional for visual feedback

            },
            onDeleteClick = { collectionId ->
                AlertDialog.Builder(this)
                    .setTitle(getString(R.string.delete_collection))
                    .setMessage(getString(R.string.are_you_sure_you_want_to_delete_this_collection))
                    .setPositiveButton(getString(R.string.delete)) { _, _ ->
                        // 🔥 Proceed to delete after confirmation
                        viewModel.deleteCollection(
                            collectionId = collectionId,
                            onSuccess = {
                                adapter!!.removeItemById(collectionId) // Instant removal
                            },
                            onError = { error ->
                                Toast.makeText(this, "Error: ${error.localizedMessage}", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
        )

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

    fun updateSelectAllCheckbox() {
        val allSelected = adapter?.list?.all { it.isSelected } == true
        if (!isUpdatingSelectAll) {
            isUpdatingSelectAll = true
            binding.selectAllCheckBox.isChecked = allSelected
            isUpdatingSelectAll = false
        }
    }

    private fun openFragment(selectedCollection: CollectionModel) {
        CollectionUtils.selectedCollection = selectedCollection
        supportFragmentManager.beginTransaction()
            .replace(R.id.FrameForFragment, CollectionDetailListFragment())
            .addToBackStack(null)
            .commit()
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

    override fun onResume() {
        super.onResume()
        Log.e("INOUt_TAG", "onResume: ", )
        viewModel.fetchCollections(userId!!)
    }

    override fun onBackPressed() {
        super.onBackPressed()
    }


}