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

    fun loadMappings(fileUri: Uri) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val input = contentResolver.openInputStream(fileUri)
                val workbook = XSSFWorkbook(input)
                val sheet = workbook.getSheetAt(0)

                val headerRow = sheet.getRow(0)
                val firstDataRow = sheet.getRow(1) // First data example

                val headers = mutableListOf<MappingItem>()
                val excelHeadersFound = mutableSetOf<String>()

                for (ci in 0 until headerRow.lastCellNum) {
                    val headerCell = headerRow.getCell(ci)
                    val headerName = when (headerCell?.cellType) {
                        CellType.STRING -> headerCell.stringCellValue.trim()
                        else -> ""
                    }

                    if (headerName.isNotBlank()) {

                        val sampleCell = firstDataRow?.getCell(ci)
                        val sampleValue = when (sampleCell?.cellType) {
                            CellType.STRING -> sampleCell.stringCellValue
                            CellType.NUMERIC -> sampleCell.numericCellValue.toString()
                            CellType.BOOLEAN -> sampleCell.booleanCellValue.toString()
                            else -> ""
                        }.trim()

                        if (systemHeaders.any { it.equals(headerName, ignoreCase = true) }) {
                            headers.add(MappingItem(headerName, sampleValue))
                            excelHeadersFound.add(headerName)
                        }

                    }

                }

                workbook.close()

                val missingHeaders = systemHeaders.filter { systemField ->
                    excelHeadersFound.none { it.equals(systemField, ignoreCase = true) }
                }

                runOnUiThread {
                    mappingItems.clear()
                    mappingItems.addAll(headers)
                    adapter.notifyDataSetChanged()

                    if (missingHeaders.isNotEmpty()) {
                        binding.tvMissingMessage.visibility = View.VISIBLE
                        binding.tvMissingMessage.text =
                            "Missing fields in Excel: ${missingHeaders.joinToString(", ")}"
                    }
                    else{
                        binding.tvMissingMessage.visibility = View.GONE
                    }

                }

            } catch (e: Exception) {
                Log.e("BulkUpload", "Error reading Excel: ${e.message}")
                e.printStackTrace()
            }
        }
    }

}
