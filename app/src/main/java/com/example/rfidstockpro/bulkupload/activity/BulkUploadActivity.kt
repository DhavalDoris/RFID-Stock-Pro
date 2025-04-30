package com.example.rfidstockpro.bulkupload.activity

import android.app.ProgressDialog
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.rfidstockpro.R
import com.example.rfidstockpro.Utils.Comman.showCustomSnackbarBelowToolbar
import com.example.rfidstockpro.Utils.StatusBarUtils
import com.example.rfidstockpro.bulkupload.adapter.MappingAdapter
import com.example.rfidstockpro.bulkupload.model.MappingItem
import com.example.rfidstockpro.bulkupload.viewmodel.BulkUploadViewModel
import com.example.rfidstockpro.databinding.ActivityBulkUploadBinding
import com.example.rfidstockpro.ui.ToolbarConfig
import com.example.rfidstockpro.ui.ToolbarUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


class BulkUploadActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBulkUploadBinding
    private lateinit var viewModel: BulkUploadViewModel
    private val mappingItems = mutableListOf<MappingItem>()
    private lateinit var adapter: MappingAdapter
//    var selectedFileUri: Uri? = null

    private val openDocumentLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            viewModel.fileUri = it
            simulateFakeProgress()
            binding.llProgress.visibility = View.VISIBLE
            viewModel.processExcelFile(this, it)
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBulkUploadBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel = ViewModelProvider(this)[BulkUploadViewModel::class.java]
        setupToolbar()
        initUI()
        initClicks()
        setupObservers()
    }

    private fun initUI() {
        setStep(1)
        adapter = MappingAdapter(mappingItems)
        binding.recyclerMappings.layoutManager = LinearLayoutManager(this)
        binding.recyclerMappings.adapter = adapter

    }


    private fun setupToolbar() {
        StatusBarUtils.setStatusBarColor(this)
        ToolbarUtils.setupToolbar(
            this,
            ToolbarConfig(
                title = getString(R.string.import_title),
                showSearch = false,
                showFilter = false,
                onFilterClick = {
                    Toast.makeText(this, "Custom Filter Action", Toast.LENGTH_SHORT).show()
                }
            )
        )
    }

    private fun initClicks() {

        binding.btnUploadFile.setOnClickListener {
            openDocumentLauncher.launch(
                arrayOf(
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    "application/vnd.ms-excel"
                )
            )
            binding.btnUploadFile.visibility = View.GONE
        }

        binding.btnUpload.setOnClickListener {

            AlertDialog.Builder(this)
                .setTitle("Confirm Upload")
                .setMessage("Are you sure you want to upload these products?")
                .setPositiveButton("Yes") { _, _ ->
                    showProgressDialog(this)
                    val fileUri = viewModel.fileUri ?: return@setPositiveButton
                    val finalMappings = mappingItems.map {
                        val selected = it.systemHeader ?: ""
                        MappingItem(it.importedHeader, it.sampleValue, selected)
                    }


                    viewModel.uploadProductsFromMappings(
                        context = this,
                        fileUri = fileUri,
                        mappings = finalMappings,
                        onProgress = { pct ->
                            runOnUiThread {
                                binding.progressBar.progress = pct
                                binding.tvProgress.text = "$pct%"
                            }
                        },
                        onComplete = { success, failure ->
                            progressDialog.dismiss()
                            runOnUiThread {
                                Toast.makeText(
                                    this,
                                    "Uploaded! Success: $success, Failed: $failure",
                                    Toast.LENGTH_LONG
                                ).show()
                                showUploadResultDialog(success, failure)
                                finish()
                            }
                        }
                    )
                }
                .setNegativeButton("Cancel", null)
                .show()


        }

        binding.btnCancel.setOnClickListener {
            finish()
        }
    }

    private fun setupObservers() {
        viewModel.headersLiveData.observe(this) { headers ->
            mappingItems.clear()
            mappingItems.addAll(headers)
            adapter.notifyDataSetChanged()
        }

        viewModel.validationErrorsLiveData.observe(this) { errors ->
            if (errors.isNotEmpty()) {
                binding.tvMissingMessage.apply {
                    visibility = View.VISIBLE
                    text = errors.joinToString("\n")
                }
                binding.btnUploadFile.visibility = View.VISIBLE
                binding.llProgress.visibility = View.GONE
                binding.btnUpload.isEnabled = false
                binding.btnUpload.alpha = 0.5f
            } else {
                binding.tvMissingMessage.visibility = View.GONE
                binding.btnUpload.isEnabled = true
                binding.btnUpload.alpha = 1f
                binding.llProgress.visibility = View.VISIBLE
                binding.llPickFile.visibility = View.GONE
                binding.recyclerMappings.visibility = View.VISIBLE
                binding.footerView.visibility = View.VISIBLE
//                binding.btnCancel.visibility = View.GONE
//                binding.btnUpload.visibility = View.GONE
//                binding.btnNext.visibility = View.VISIBLE
                showCustomSnackbarBelowToolbar(this, findViewById(R.id.commonToolbar))
                setStep(2)
            }
        }

        viewModel.fileNameLiveData.observe(this) { name ->
            binding.labelText.text = name
        }
    }

    fun setStep(step: Int) {

        when (step) {
            1 -> {
                // Step 1 active
                binding.tvOne.background.setTint(ContextCompat.getColor(this, R.color.appMainColor))
                binding.tvOne.setTextColor(ContextCompat.getColor(this, R.color.white))
                binding.tvUploadFile.setTextColor(ContextCompat.getColor(this, R.color.black))

                // Step 2 inactive
                binding.tvTwo.background.setTint(ContextCompat.getColor(this, R.color.gray_line))
                binding.tvTwo.setTextColor(ContextCompat.getColor(this, R.color.gray))
                binding.tvReviewData.setTextColor(ContextCompat.getColor(this, R.color.gray))
            }

            2 -> {
                // Step 1 inactive
                binding.tvOne.background.setTint(ContextCompat.getColor(this, R.color.gray_line))
                binding.tvOne.setTextColor(ContextCompat.getColor(this, R.color.gray))
                binding.tvUploadFile.setTextColor(ContextCompat.getColor(this, R.color.gray))

                // Step 2 active
                binding.tvTwo.background.setTint(ContextCompat.getColor(this, R.color.appMainColor))
                binding.tvTwo.setTextColor(ContextCompat.getColor(this, R.color.white))
                binding.tvReviewData.setTextColor(ContextCompat.getColor(this, R.color.black))
            }
        }
    }


    private fun simulateFakeProgress()  {

        binding.llProgress.visibility = View.VISIBLE
        binding.progressBar.progress = 0
        binding.tvProgress.text = "0%"

        val job = CoroutineScope(Dispatchers.Main).launch {
            var progress = 0
            while (progress < 100) {
                delay(30) // Adjust speed here
                progress += 2
                binding.progressBar.progress = progress
                binding.tvProgress.text = "$progress%"
            }
        }

        // Observe ViewModel for end conditions
        viewModel.headersLiveData.observe(this) {
            job.cancel()
            binding.llProgress.visibility = View.GONE
        }

        viewModel.validationErrorsLiveData.observe(this) { errors ->
            if (errors.isNotEmpty()) {
                job.cancel()
                binding.llProgress.visibility = View.GONE
            }
        }

    }

    private lateinit var progressDialog: ProgressDialog

    private fun showProgressDialog(context: Context) {
        progressDialog = ProgressDialog(context).apply {
            setMessage("Uploading, please wait...")
            setProgressStyle(ProgressDialog.STYLE_HORIZONTAL)
            isIndeterminate = false
            setCancelable(false)
            max = 100
            progress = 0
            show()
        }
    }

    private fun showUploadResultDialog(success: Int, failure: Int) {
        AlertDialog.Builder(this)
            .setTitle("Upload Completed")
            .setMessage("Uploaded successfully: $success\nFailed: $failure")
            .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
            .show()
    }


}
