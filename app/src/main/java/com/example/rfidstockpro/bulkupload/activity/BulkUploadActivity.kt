package com.example.rfidstockpro.bulkupload.activity

import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.rfidstockpro.R
import com.example.rfidstockpro.RFIDApplication.Companion.PRODUCT_TABLE
import com.example.rfidstockpro.Utils.Comman.showCustomSnackbarBelowToolbar
import com.example.rfidstockpro.Utils.StatusBarUtils
import com.example.rfidstockpro.aws.AwsManager
import com.example.rfidstockpro.aws.models.ProductModel
import com.example.rfidstockpro.bulkupload.adapter.ExcelProductAdapter
import com.example.rfidstockpro.bulkupload.adapter.MappingAdapter
import com.example.rfidstockpro.bulkupload.model.MappingItem
import com.example.rfidstockpro.bulkupload.viewmodel.BulkUploadViewModel
import com.example.rfidstockpro.databinding.ActivityBulkUploadBinding
import com.example.rfidstockpro.ui.ToolbarConfig
import com.example.rfidstockpro.ui.ToolbarUtils
import com.example.rfidstockpro.ui.activities.AddProductActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class BulkUploadActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBulkUploadBinding
    private lateinit var viewModel: BulkUploadViewModel
    private val mappingItems = mutableListOf<MappingItem>()
    private lateinit var adapter: MappingAdapter
    private var currentProductIndex = 0 // To keep track of the product being reviewed
    private val REQUEST_CODE_REVIEW = 1001

    //    var selectedFileUri: Uri? = null
    private lateinit var excelProductAdapter: ExcelProductAdapter

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

        // Trigger the Excel processing when clicking Next
       /* binding.btnNext.setOnClickListener {
            *//*  val excelFileUri = viewModel.fileUri
              Log.e("Product_TAG", "initClicks: " + excelFileUri )
              if (excelFileUri != null) {
                  // Process Excel file and show products
                  viewModel.processExcelFile(this, excelFileUri)
              }*//*

            val products = viewModel.parsedProductsLiveData.value
            if (!products.isNullOrEmpty()) {
                binding.recyclerMappings.apply {
                    layoutManager = LinearLayoutManager(this@BulkUploadActivity)

                    val products = viewModel.parsedProductsLiveData.value ?: mutableListOf()
                    excelProductAdapter = ExcelProductAdapter(products)
                    binding.recyclerMappings.adapter = excelProductAdapter

                    visibility = View.VISIBLE
                }

                binding.btnNext.visibility = View.GONE
                binding.btnCancel.visibility = View.VISIBLE
                binding.btnReviewUpload.visibility = View.VISIBLE
                setStep(2)
            } else {
                Toast.makeText(this, "No products to preview.", Toast.LENGTH_SHORT).show()
            }
        }*/

        binding.btnNext.setOnClickListener {
           /* val products = viewModel.parsedProductsLiveData.value.orEmpty()
            if (products.isEmpty()) {
                Toast.makeText(this, "No products to preview.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Disable the Next button and show a progress dialog
            binding.btnNext.isEnabled = false
            binding.btnNext.alpha = 0.5f

            // 1) Check for existing SKUs on a background thread
            CoroutineScope(Dispatchers.Main).launch {
                val duplicates = withContext(Dispatchers.IO) {
                    products.filter { AwsManager.getProductBySku(PRODUCT_TABLE, it.sku) != null }
                }
                Log.e("CHECK_DUPLICATE_TAG", "initClicks: " +  duplicates )

                if (duplicates.isNotEmpty()) {
                    // 2) Show one dialog listing all duplicates
                    AlertDialog.Builder(this@BulkUploadActivity)
                        .setTitle("Duplicate SKUs")
                        .setMessage("The following SKUs already exist and will be skipped:\n\n" +
                                duplicates.joinToString("\n") { "• ${it.sku}" })
                        .setPositiveButton("Continue") { _, _ ->
                            // 3) Remove duplicates and show the rest
                            displayPreview(products - duplicates.toSet())
                        }
                        .setNegativeButton("Cancel", null)
                        .show()
                } else {
                    // No duplicates → just proceed
                    displayPreview(products)
                }
            }*/


          /*  val products = viewModel.parsedProductsLiveData.value.orEmpty()
            if (products.isEmpty()) return@setOnClickListener

            // disable UI
            binding.btnNext.isEnabled = false
            binding.btnNext.alpha = 0.5f
//            showProgressDialog("Checking duplicates…")

            lifecycleScope.launch {
                // 1) Batch-get all existing SKUs in one shot
                val existing = withContext(Dispatchers.IO) {
                    AwsManager.batchGetProductsBySku(PRODUCT_TABLE, products.map { it.sku })
                        .map { it.sku }
                        .toSet()
                }

                if (existing.isNotEmpty()) {
                    AlertDialog.Builder(this@BulkUploadActivity)
                        .setTitle("Duplicate SKUs")
                        .setMessage(
                            "These SKUs already exist and will be skipped:\n\n" +
                                    existing.joinToString("\n") { "• $it" }
                        )
                        .setPositiveButton("Continue") { _, _ ->
                            displayPreview(products.filter { it.sku !in existing })
                        }
                        .setOnDismissListener { binding.btnNext.isEnabled = true }
                        .show()
                } else {
                    displayPreview(products)
                }
            }*/


            lifecycleScope.launch {
                val products = viewModel.parsedProductsLiveData.value.orEmpty()

                binding.btnNext.isEnabled = false
                binding.btnNext.alpha = 0.5f
                val progressDialog = ProgressDialog(this@BulkUploadActivity)
                progressDialog.setMessage("Checking for Excel...")
                progressDialog.setCancelable(false) // Prevent user from dismissing during check
                progressDialog.show()
                // Kick off *all* queries in parallel:
                val checks = products.map { p ->
                    async(Dispatchers.IO) {
                        AwsManager.getProductBySku(PRODUCT_TABLE, p.sku) != null
                    }
                }
                val existsFlags = checks.awaitAll()
                progressDialog.dismiss()

                val duplicates = products
                    .zip(existsFlags)
                    .filter { it.second }
                    .map { it.first.sku }

                if (duplicates.isNotEmpty()) {

                    AlertDialog.Builder(this@BulkUploadActivity)
                        .setTitle("Duplicate SKUs")
                        .setMessage("These SKUs exist and will be skipped:\n\n" +
                                duplicates.joinToString("\n") { "• $it" })
                        .setPositiveButton("Continue") { _, _ ->
                            displayPreview(products.filter { it.sku !in duplicates })
                        }
                        .setCancelable(false)
                        .setOnDismissListener { binding.btnNext.isEnabled = true }
                        .show()
                } else {
                    displayPreview(products)
                }
            }

        }


        binding.btnUploadFile.setOnClickListener {
            openDocumentLauncher.launch(
                arrayOf(
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    "application/vnd.ms-excel"
                )
            )
            binding.btnUploadFile.visibility = View.GONE
        }

        binding.btnReviewUpload.setOnClickListener {
//            uploadeProduct()
            /*  val selectedProduct = getSelectedProduct() // You need to implement this method
              if (selectedProduct != null) {
                  navigateToAddProductScreen(selectedProduct)
              } else {
                  /
                  Toast.makeText(this, "Please select a product to review.", Toast.LENGTH_SHORT).show()
              }*/


            val productList = viewModel.parsedProductsLiveData.value
            if (productList != null && productList.isNotEmpty()) {
                // Check if any products are left to review
                if (currentProductIndex < productList.size) {
                    val currentProduct = productList[currentProductIndex]
                    val total = productList.size
                    val index =
                        currentProductIndex // or currentProductIndex + 1 if you want 1-based in UI
                    navigateToAddProductScreen(currentProduct, index, total)
                } else {
                    Toast.makeText(this, "All products have been reviewed.", Toast.LENGTH_SHORT)
                        .show()
                }
            } else {
                Toast.makeText(this, "Please select products first.", Toast.LENGTH_SHORT).show()
            }
        }


        binding.btnCancel.setOnClickListener {
            finish()
        }
    }



    private fun displayPreview(list: List<ProductModel>) {
        excelProductAdapter = ExcelProductAdapter(list)
        binding.recyclerMappings.apply {
            layoutManager = LinearLayoutManager(this@BulkUploadActivity)
            adapter = excelProductAdapter
            visibility = View.VISIBLE
        }

        binding.btnNext.visibility = View.GONE
        binding.btnCancel.visibility = View.VISIBLE
        binding.btnReviewUpload.visibility = View.VISIBLE
        setStep(2)
    }

    private fun getSelectedProduct(): ProductModel? {
        val adapter = binding.recyclerMappings.adapter as ExcelProductAdapter
        // Assuming you are storing selected items in a list, or you can get the selected item by its position
        return adapter.getSelectedProduct() // Implement this method in your adapter to return the selected product
    }

    private fun navigateToAddProductScreen(product: ProductModel, index: Int, total: Int) {

        val intent = Intent(this, AddProductActivity::class.java).apply {
            putExtra("productData", product) // Pass the selected product data
            putExtra("index", index + 1) // to show 1-based count
            putExtra("total", total)
        }
        startActivityForResult(intent, REQUEST_CODE_REVIEW)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_CODE_REVIEW && resultCode == RESULT_OK) {

            val sku = data?.getStringExtra("uploadedSku")
            val productList = viewModel.parsedProductsLiveData.value
            val updatedIndex = productList?.indexOfFirst { it.sku == sku } ?: -1

//            productList?.find { it.sku == sku }?.isUploaded = true

            if (updatedIndex != -1) {
                productList?.get(updatedIndex)!!.isUploaded = true
                excelProductAdapter.notifyItemChanged(updatedIndex)
            }
//            binding.recyclerMappings.adapter?.notifyDataSetChanged()
            Log.e("CallBackTAG", "onSuccess: 3")
            // Move to the next product after the user finishes reviewing
            currentProductIndex++

            // If there are more products to review, pass the next product to the Add Product screen

            if (productList != null && currentProductIndex < productList.size) {
                val nextProduct = productList[currentProductIndex]
                val total = productList.size
                val index =
                    currentProductIndex
                navigateToAddProductScreen(nextProduct, index, total)
            } else {
                // All products reviewed
                Toast.makeText(this, "All products have been reviewed.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun uploadeProduct() {
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
                binding.btnReviewUpload.isEnabled = false
                binding.btnReviewUpload.alpha = 0.5f
            } else {
                binding.tvMissingMessage.visibility = View.GONE
                binding.btnReviewUpload.isEnabled = true
                binding.btnReviewUpload.alpha = 1f
                binding.llProgress.visibility = View.VISIBLE
                binding.llPickFile.visibility = View.GONE
                binding.recyclerMappings.visibility = View.VISIBLE
                binding.footerView.visibility = View.VISIBLE
                binding.btnCancel.visibility = View.GONE
                binding.btnReviewUpload.visibility = View.GONE
                binding.btnNext.visibility = View.VISIBLE
                showCustomSnackbarBelowToolbar(this, findViewById(R.id.commonToolbar))
                setStep(2)

            }
        }

        viewModel.fileNameLiveData.observe(this) { name ->
            binding.labelText.text = name
        }

        /*  viewModel.parsedProductsLiveData.observe(this) { products ->
              Log.d("UploadedProduct", products.toString())
              if (!products.isNullOrEmpty()) {
                  products.forEach { Log.d("UploadedProduct", it.toString()) }
              }
              else{
                  Log.d("UploadedProduct" , " ELSE" )
              }
          }*/
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


    private fun simulateFakeProgress() {

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

        /*       viewModel.parsedProductsLiveData.observe(this) { products ->
                   if (!products.isNullOrEmpty()) {
                       products.forEach { Log.d("UploadedProduct", it.toString()) }
                   }

                   if (products.isNotEmpty()) {
                       binding.recyclerMappings.apply {
                           layoutManager = LinearLayoutManager(this@BulkUploadActivity)
                           adapter = ExcelProductAdapter(products)
                           visibility = View.VISIBLE
                       }
                       // hide Next, show Cancel & Upload
                       binding.btnNext.visibility = View.GONE
                       binding.btnCancel.visibility = View.VISIBLE
                       binding.btnReviewUpload.visibility = View.VISIBLE
                   }
               }*/
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
