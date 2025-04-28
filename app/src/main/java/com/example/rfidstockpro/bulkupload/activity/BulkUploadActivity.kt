package com.example.rfidstockpro.bulkupload.activity

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.rfidstockpro.aws.models.ProductModel
import com.example.rfidstockpro.bulkupload.adapter.MappingAdapter
import com.example.rfidstockpro.bulkupload.model.MappingItem
import com.example.rfidstockpro.bulkupload.viewmodel.BulkUploadViewModel
import com.example.rfidstockpro.databinding.ActivityBulkUploadBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
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
    private var selectedFileUri: Uri? = null

    private val openDocumentLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            selectedFileUri = it
            Log.e("STARTBULKUPLOAD_TAG", ": loadMappings() " + it)
            loadMappings(it)
        }
    }

    private lateinit var adapter: MappingAdapter
    private val mappingItems = mutableListOf<MappingItem>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBulkUploadBinding.inflate(layoutInflater)
        setContentView(binding.root)

        adapter = MappingAdapter(mappingItems)
        binding.recyclerMappings.layoutManager = LinearLayoutManager(this)
        binding.recyclerMappings.adapter = adapter


        binding.btnUploadFile.setOnClickListener {
            openDocumentLauncher.launch(
                arrayOf(
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    "application/vnd.ms-excel"
                )
            )
        }

        binding.btnUpload.setOnClickListener {
            viewModel.uploadProductsFromMappings(
                context = this,
                fileUri = selectedFileUri!!, // keep your fileUri from when user picked file
                mappings = mappingItems,
                onProgress = { percent ->
                    binding.progressBar.progress = percent
                    binding.tvProgress.text = "$percent%"
                },
                onComplete = { success, failure ->
                    Toast.makeText(
                        this,
                        "Uploaded! Success: $success, Failed: $failure",
                        Toast.LENGTH_LONG
                    ).show()
                    binding.btnUploadFile.isEnabled = true
                }
            )
        }
    }

    private fun loadMappings(fileUri: Uri) {
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
                            CellType.STRING  -> sampleCell.stringCellValue
                            CellType.NUMERIC -> sampleCell.numericCellValue.toString()
                            CellType.BOOLEAN -> sampleCell.booleanCellValue.toString()
                            else             -> ""
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
                            CellType.STRING  -> cell.stringCellValue.trim()
                            CellType.NUMERIC -> cell.numericCellValue.toString()
                            else             -> ""
                        }
                        if (value.isNotBlank()) counts[value] = counts.getOrDefault(value, 0) + 1
                    }
                    counts.filterValues { it > 1 }.forEach { (sku, times) ->
                        duplicateSkus[sku] = times
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

                    if (errors.isNotEmpty()) {
                        binding.tvMissingMessage.visibility = View.VISIBLE
                        binding.tvMissingMessage.text = errors.joinToString("\n")
                        binding.btnUpload.isEnabled = false
                        binding.btnUpload.alpha = 0.5f
                    } else {
                        binding.tvMissingMessage.visibility = View.GONE
                        binding.btnUpload.isEnabled = true
                        binding.btnUpload.alpha = 1f
                    }
                }

            } catch (e: Exception) {
                Log.e("BulkUpload", "Error reading Excel", e)
            }
        }
    }


}
