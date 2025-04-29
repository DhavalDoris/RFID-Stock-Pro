package com.example.rfidstockpro.bulkupload.activity

import android.app.ProgressDialog
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.rfidstockpro.R
import com.example.rfidstockpro.Utils.Comman.getFileName
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
import org.apache.poi.ss.usermodel.CellType
import org.apache.poi.xssf.usermodel.XSSFWorkbook


class BulkUploadActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBulkUploadBinding
    private val viewModel: BulkUploadViewModel by viewModels()
    val systemHeaders = listOf(
        "Title",
        "Category",
        "Style No",
        "SKU",
        "Price",
        "Description",
        "Image",
        "Video"
    )

    private val mandatoryHeaders = listOf(
        "Title",
        "Category",
        "SKU",
        "Price",
        "Image",
        "Video"
    )


    private var selectedFileUri: Uri? = null

    private val openDocumentLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            selectedFileUri = it
            loadMappings(it)
        }
    }

    private lateinit var adapter: MappingAdapter
    private val mappingItems = mutableListOf<MappingItem>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBulkUploadBinding.inflate(layoutInflater)
        setContentView(binding.root)


        initView()
        setupAdapter()
        initClicks()

    }

    private fun initView() {
        setStep(1)
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

    private fun setupAdapter() {
        adapter = MappingAdapter(mappingItems)
        binding.recyclerMappings.layoutManager = LinearLayoutManager(this)
        binding.recyclerMappings.adapter = adapter
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
            showProgressDialog(this)
            binding.btnUploadFile.isEnabled = false
            viewModel.uploadProductsFromMappings(
                context = this,
                fileUri = selectedFileUri!!, // keep your fileUri from when user picked file
                mappings = mappingItems,
                onProgress = { percent ->
                    binding.progressBar.progress = percent
                    binding.tvProgress.text = "$percent%"
                },
                onComplete = { success, failure ->
                    progressDialog.dismiss()
                    Toast.makeText(
                        this,
                        "Uploaded! Success: $success, Failed: $failure",
                        Toast.LENGTH_LONG
                    ).show()
                    binding.btnUploadFile.isEnabled = true
                }
            )
        }

        binding.btnCancel.setOnClickListener {
            finish()
        }
    }

   /* private fun loadMappings(fileUri: Uri) {

        CoroutineScope(Dispatchers.Main).launch {
            binding.llProgress.visibility = View.VISIBLE
            binding.progressBar.progress = 0
            binding.tvProgress.text = "0%"
            simulateFakeProgress()

            val fileName = getFileName(fileUri, this@BulkUploadActivity)
            binding.labelText.text = fileName
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val input = contentResolver.openInputStream(fileUri) ?: return@launch
                val workbook = XSSFWorkbook(input)
                val sheet = workbook.getSheetAt(0)

                // 1) Read headers
                val headerRow = sheet.getRow(0)
                val headersFound = mutableListOf<MappingItem>()
                val headerMap = mutableMapOf<String, Int>()
                for (ci in 0 until headerRow.lastCellNum) {
                    headerRow.getCell(ci)?.stringCellValue
                        ?.takeIf { it.isNotBlank() }
                        ?.trim()
                        ?.also { headerMap[it] = ci }
                }

                // 2) Build initial MappingItems (only system headers)
                val excelHeadersFound = mutableSetOf<String>()
                for ((headerName, ci) in headerMap) {
                    if (systemHeaders.any { it.equals(headerName, ignoreCase = true) }) {
                        // sample from first data row
                        val sampleCell = sheet.getRow(1)?.getCell(ci)
                        val sample = when (sampleCell?.cellType) {
                            CellType.STRING -> sampleCell.stringCellValue
                            CellType.NUMERIC -> sampleCell.numericCellValue.toString()
                            CellType.BOOLEAN -> sampleCell.booleanCellValue.toString()
                            else -> ""
                        }.trim()
                        headersFound += MappingItem(headerName, sample)
                        excelHeadersFound += headerName
                    }
                }

                // 3) Check for missing system headers
                val missingHeaders = systemHeaders.filter { sys ->
                    excelHeadersFound.none { it.equals(sys, ignoreCase = true) }
                }

                // 4) Check for duplicate SKUs
                //    Find the exact header key used in the sheet (case-insensitive)
                val skuKey = headerMap.keys.firstOrNull { it.equals("sku", ignoreCase = true) }
                val duplicateSkus = mutableMapOf<String, Int>()
                if (skuKey != null) {
                    val idx = headerMap[skuKey]!!
                    val counts = mutableMapOf<String, Int>()
                    for (r in 1..sheet.lastRowNum) {
                        val cell = sheet.getRow(r)?.getCell(idx)
                        val value = when (cell?.cellType) {
                            CellType.STRING -> cell.stringCellValue.trim()
                            CellType.NUMERIC -> cell.numericCellValue.toString()
                            else -> ""
                        }
                        if (value.isNotBlank()) counts[value] = counts.getOrDefault(value, 0) + 1
                    }
                    counts.filterValues { it > 1 }.forEach { (sku, times) ->
                        duplicateSkus[sku] = times
                    }
                }

                val missingValues = mutableListOf<String>()
                for (mandatory in mandatoryHeaders) {
                    val colIndex = headerMap.entries.firstOrNull { it.key.equals(mandatory, ignoreCase = true) }?.value
                    if (colIndex != null) {
                        for (r in 1..sheet.lastRowNum) {
                            val value = sheet.getRow(r)?.getCell(colIndex)?.let { cell ->
                                when (cell.cellType) {
                                    CellType.STRING -> cell.stringCellValue.trim()
                                    CellType.NUMERIC -> cell.numericCellValue.toString().trim()
                                    else -> ""
                                }
                            }.orEmpty()
                            if (value.isBlank()) {
                                missingValues += "Row ${r + 1} missing value for '$mandatory'"
                            }
                        }
                    }
                }
                workbook.close()

                runOnUiThread {
                    // Populate RecyclerView
                    mappingItems.clear()
                    mappingItems.addAll(headersFound)
                    adapter.notifyDataSetChanged()

                    // Build error message if needed
                    val errors = mutableListOf<String>()
                    if (missingHeaders.isNotEmpty()) {
                        errors += "Missing fields: ${missingHeaders.joinToString()}"
                    }
                    if (duplicateSkus.isNotEmpty()) {
                        duplicateSkus.forEach { (sku, times) ->
                            errors += "SKU \"$sku\" appears $times times"
                        }
                    }
                    if (missingValues.isNotEmpty()) {
                        errors += missingValues
                    }
                    if (errors.isNotEmpty()) {
                        binding.tvMissingMessage.visibility = View.VISIBLE
                        binding.tvMissingMessage.text = errors.joinToString("\n")
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
                        showCustomSnackbarBelowToolbar(this@BulkUploadActivity, findViewById(R.id.commonToolbar))
                        setStep(2)
                    }
                }

            } catch (e: Exception) {
                Log.e("BulkUpload", "Error reading Excel", e)
            }
        }
    }*/

    private fun loadMappings(fileUri: Uri) {
        CoroutineScope(Dispatchers.Main).launch {
            binding.llProgress.visibility = View.VISIBLE
            binding.progressBar.progress = 0
            binding.tvProgress.text = "0%"
            simulateFakeProgress()

            val fileName = getFileName(fileUri, this@BulkUploadActivity)
            binding.labelText.text = fileName
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val input = contentResolver.openInputStream(fileUri) ?: return@launch
                val workbook = XSSFWorkbook(input)
                val sheet = workbook.getSheetAt(0)

                val headerRow = sheet.getRow(0)
                if (headerRow == null || headerRow.lastCellNum <= 0) {
                    runOnUiThread {
                        binding.tvMissingMessage.visibility = View.VISIBLE
                        binding.tvMissingMessage.text = "The Excel file is empty or missing header row."
                        binding.btnUpload.isEnabled = false
                        binding.btnUpload.alpha = 0.5f
                    }
                    workbook.close()
                    return@launch
                }

                if (sheet.lastRowNum < 1) {
                    runOnUiThread {
                        binding.tvMissingMessage.visibility = View.VISIBLE
                        binding.tvMissingMessage.text = "The Excel file has no data rows. Please add product data."
                        binding.btnUpload.isEnabled = false
                        binding.btnUpload.alpha = 0.5f
                    }
                    workbook.close()
                    return@launch
                }

                val headerMap = mutableMapOf<String, Int>()
                for (ci in 0 until headerRow.lastCellNum) {
                    headerRow.getCell(ci)?.stringCellValue
                        ?.takeIf { it.isNotBlank() }
                        ?.trim()
                        ?.also { headerMap[it] = ci }
                }

                val headersFound = mutableListOf<MappingItem>()
                val excelHeadersFound = mutableSetOf<String>()

                for ((headerName, ci) in headerMap) {
                    if (systemHeaders.any { it.equals(headerName, ignoreCase = true) }) {
                        val sampleCell = sheet.getRow(1)?.getCell(ci)
                        val sample = when (sampleCell?.cellType) {
                            CellType.STRING -> sampleCell.stringCellValue
                            CellType.NUMERIC -> sampleCell.numericCellValue.toString()
                            CellType.BOOLEAN -> sampleCell.booleanCellValue.toString()
                            else -> ""
                        }.trim()
                        headersFound += MappingItem(headerName, sample)
                        excelHeadersFound += headerName
                    }
                }

                val missingHeaders = systemHeaders.filter { sys ->
                    excelHeadersFound.none { it.equals(sys, ignoreCase = true) }
                }

                val skuKey = headerMap.keys.firstOrNull { it.equals("sku", ignoreCase = true) }
                val duplicateSkus = mutableMapOf<String, Int>()
                if (skuKey != null) {
                    val idx = headerMap[skuKey]!!
                    val counts = mutableMapOf<String, Int>()
                    for (r in 1..sheet.lastRowNum) {
                        val cell = sheet.getRow(r)?.getCell(idx)
                        val value = when (cell?.cellType) {
                            CellType.STRING -> cell.stringCellValue.trim()
                            CellType.NUMERIC -> cell.numericCellValue.toString()
                            else -> ""
                        }
                        if (value.isNotBlank()) counts[value] = counts.getOrDefault(value, 0) + 1
                    }
                    counts.filterValues { it > 1 }.forEach { (sku, times) ->
                        duplicateSkus[sku] = times
                    }
                }

                val emptyMandatoryErrors = mutableListOf<String>()
                for (header in mandatoryHeaders) {
                    val headerCol = headerMap.keys.firstOrNull { it.equals(header, ignoreCase = true) }
                    val colIdx = headerMap[headerCol] ?: continue

                    for (r in 1..sheet.lastRowNum) {
                        val cell = sheet.getRow(r)?.getCell(colIdx)
                        val value = when (cell?.cellType) {
                            CellType.STRING -> cell.stringCellValue.trim()
                            CellType.NUMERIC -> cell.numericCellValue.toString()
                            else -> ""
                        }
                        if (value.isBlank()) {
                            emptyMandatoryErrors += "$header is empty at row ${r + 1}"
                        }
                    }
                }

                workbook.close()

                runOnUiThread {
                    mappingItems.clear()
                    mappingItems.addAll(headersFound)
                    adapter.notifyDataSetChanged()

                    val errors = mutableListOf<String>()
                    if (missingHeaders.isNotEmpty()) {
                        errors += "Missing fields: ${missingHeaders.joinToString()}"
                    }
                    if (duplicateSkus.isNotEmpty()) {
                        duplicateSkus.forEach { (sku, times) ->
                            errors += "SKU \"$sku\" appears $times times"
                        }
                    }
                    if (emptyMandatoryErrors.isNotEmpty()) {
                        errors += emptyMandatoryErrors
                    }

                    if (errors.isNotEmpty()) {
                        binding.tvMissingMessage.visibility = View.VISIBLE
                        /
                        binding.btnUploadFile.visibility = View.VISIBLE
                        binding.llProgress.visibility = View.GONE
                        binding.tvMissingMessage.text = errors.joinToString("\n")
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
                        showCustomSnackbarBelowToolbar(this@BulkUploadActivity, findViewById(R.id.commonToolbar))
                        setStep(2)
                    }
                }

            } catch (e: Exception) {
                Log.e("BulkUpload", "Error reading Excel", e)
            }
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


    private suspend fun simulateFakeProgress() {
        for (i in 1..100 step 5) {
            delay(30) // simulate work
            binding.progressBar.progress = i
            binding.tvProgress.text = "$i%"
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


}
