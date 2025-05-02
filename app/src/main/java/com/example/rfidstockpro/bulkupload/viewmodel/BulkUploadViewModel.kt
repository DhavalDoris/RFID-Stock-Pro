package com.example.rfidstockpro.bulkupload.viewmodel

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.rfidstockpro.RFIDApplication.Companion.PRODUCT_TABLE
import com.example.rfidstockpro.aws.AwsManager
import com.example.rfidstockpro.aws.models.ProductModel
import com.example.rfidstockpro.bulkupload.model.MappingItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.apache.poi.ss.usermodel.CellType
import org.apache.poi.ss.usermodel.Row
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.text.SimpleDateFormat
import java.util.*

class BulkUploadViewModel : ViewModel() {

    val headersLiveData = MutableLiveData<List<MappingItem>>()
    val validationErrorsLiveData = MutableLiveData<List<String>>()
    val fileNameLiveData = MutableLiveData<String>()
    val parsedProductsLiveData = MutableLiveData<List<ProductModel>>()

    var fileUri: Uri? = null

    fun processExcelFile(context: Context, uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            val errors = mutableListOf<String>()
            val mappingItems = mutableListOf<MappingItem>()
            val parsedProducts = mutableListOf<ProductModel>()
            val contentResolver = context.contentResolver

            try {
                val input = contentResolver.openInputStream(uri) ?: return@launch
                val workbook = XSSFWorkbook(input)
                val sheet = workbook.getSheetAt(0)

                if (sheet.physicalNumberOfRows == 0) {
                    errors += "The Excel file is completely empty."
                    validationErrorsLiveData.postValue(errors)
                    workbook.close()
                    return@launch
                }

                val headerRow = sheet.getRow(0)
                if (headerRow == null || headerRow.lastCellNum <= 0) {
                    errors += "The Excel file is missing a header row."
                    validationErrorsLiveData.postValue(errors)
                    workbook.close()
                    return@launch
                }

                // Check if all rows below header are null or empty
                val hasDataRows = (1..sheet.lastRowNum).any { rowIndex ->
                    val row = sheet.getRow(rowIndex)
                    row != null && row.any { cell ->
                        cell != null && cell.toString().trim().isNotEmpty()
                    }
                }

                if (!hasDataRows) {
                    errors += "The Excel file has no data rows."
                    validationErrorsLiveData.postValue(errors)
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

                val normalizedHeaderMap = headerMap.mapKeys { it.key.lowercase() }
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

                for ((headerName, ci) in headerMap) {
                    if (systemHeaders.any { it.equals(headerName, ignoreCase = true) }) {
                        val sampleCell = sheet.getRow(1)?.getCell(ci)
                        val sample = when (sampleCell?.cellType) {
                            CellType.STRING -> sampleCell.stringCellValue
                            CellType.NUMERIC -> sampleCell.numericCellValue.toString()
                            CellType.BOOLEAN -> sampleCell.booleanCellValue.toString()
                            else -> ""
                        }.trim()
                        mappingItems += MappingItem(headerName, sample)
                    }
                }

                // ✅ VALIDATION SECTION
                val mandatoryHeaders = listOf("Category", "SKU", "Price")

                // Check for missing mandatory headers
                val missingHeaders = mandatoryHeaders.filterNot { hdr ->
                    normalizedHeaderMap.keys.any { it.equals(hdr, ignoreCase = true) }
                }

                if (missingHeaders.isNotEmpty()) {
                    errors += "Missing mandatory headers: ${missingHeaders.joinToString()}"
                }

                // Check for duplicate SKUs
                val skuColIndex = normalizedHeaderMap["sku"]
                if (skuColIndex != null) {
                    val counts = mutableMapOf<String, Int>()
                    for (r in 1..sheet.lastRowNum) {
                        val value =
                            sheet.getRow(r)?.getCell(skuColIndex)?.toString()?.trim().orEmpty()
                        if (value.isNotBlank()) counts[value] = counts.getOrDefault(value, 0) + 1
                    }
                    counts.filterValues { it > 1 }.forEach { (sku, times) ->
                        errors += "SKU \"$sku\" appears $times times"
                    }
                }

                // Check for empty mandatory field values
                for (header in mandatoryHeaders) {
                    val colIdx = normalizedHeaderMap[header.lowercase()] ?: continue
                    val emptyRows = mutableListOf<Int>()
                    for (r in 1..sheet.lastRowNum) {
                        val cellValue =
                            sheet.getRow(r)?.getCell(colIdx)?.toString()?.trim().orEmpty()
                        if (cellValue.isBlank()) {
                            emptyRows += (r + 1)
                        }
                    }
                    if (emptyRows.isNotEmpty()) {
                        errors += "$header is empty at row ${emptyRows.joinToString(",")}"
                    }
                }

                // ✅ BUILD PRODUCT PREVIEW
                for (r in 1..sheet.lastRowNum) {
                    val row = sheet.getRow(r) ?: continue

                    val title =
                        headerMap["Title"]?.let { row.getCell(it)?.toString()?.trim() } ?: ""
                    val category =
                        headerMap["Category"]?.let { row.getCell(it)?.toString()?.trim() } ?: ""
                    val styleNo =
                        headerMap["Style No"]?.let { row.getCell(it)?.toString()?.trim() } ?: ""
                    val sku =
                        normalizedHeaderMap["sku"]?.let { row.getCell(it)?.toString()?.trim() }
                            ?: ""
                    val price =
                        headerMap["Price"]?.let { row.getCell(it)?.toString()?.trim() } ?: ""
                    val description =
                        headerMap["Description"]?.let { row.getCell(it)?.toString()?.trim() } ?: ""
                    val image =
                        headerMap["Image"]?.let { row.getCell(it)?.toString()?.trim() } ?: ""
                    val video =
                        headerMap["Video"]?.let { row.getCell(it)?.toString()?.trim() } ?: ""

                    val product = ProductModel(
                        id = UUID.randomUUID().toString(),
                        selectedImages = image.takeIf { it.isNotBlank() }?.let { listOf(it) }
                            ?: emptyList(),
                        selectedVideo = video.takeIf { it.isNotBlank() },
                        productName = title,
                        productCategory = category,
                        styleNo = styleNo,
                        sku = sku,
                        price = price,
                        description = description,
                        isImageSelected = image.isNotBlank(),
                        isMediaUpdated = false,
                        tagId = "",
                        status = "Pending",
                        createdAt = "",
                        updatedAt = "",
                        previewImageUrls = null,
                        previewVideoUrl = null
                    )

                    parsedProducts += product
                }

                workbook.close()
                fileNameLiveData.postValue(getFileName(uri, context))
                headersLiveData.postValue(mappingItems)
                validationErrorsLiveData.postValue(errors)
                parsedProductsLiveData.postValue(parsedProducts)

            } catch (e: Exception) {
                Log.e("BulkUploadViewModel", "Excel parsing error", e)
                errors += "An error occurred while reading the Excel file."
                validationErrorsLiveData.postValue(errors)
            }
        }
    }


    private fun getFileName(uri: Uri, context: Context): String {
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        return cursor?.use {
            val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            it.moveToFirst()
            it.getString(nameIndex)
        } ?: "Unknown"
    }

    fun uploadProductsFromMappings(
        context: Context,
        fileUri: Uri,
        mappings: List<MappingItem>,
        onProgress: (percent: Int) -> Unit,
        onComplete: (successCount: Int, failureCount: Int) -> Unit
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val input = context.contentResolver.openInputStream(fileUri)
            if (input == null) {
                withContext(Dispatchers.Main) { onComplete(0, 0) }
                return@launch
            }

            val workbook = XSSFWorkbook(input)
            val sheet = workbook.getSheetAt(0)
            val headerRow = sheet.getRow(0)
            if (headerRow == null) {
                withContext(Dispatchers.Main) {
                    onComplete(0, 0)
                }
                return@launch
            }

            // Build column-index map
            val headerMap = mutableMapOf<String, Int>()
            for (ci in 0 until headerRow.lastCellNum) {
                headerRow.getCell(ci)?.stringCellValue
                    ?.takeIf { it.isNotBlank() }
                    ?.trim()
                    ?.let { headerMap[it] = ci }
            }

            val totalRows = sheet.lastRowNum
            var successCount = 0
            var failureCount = 0
            val parsedProducts = mutableListOf<ProductModel>() // ✅ List of uploaded products

            suspend fun getCell(row: Row, hdr: String): String {
                val idx = headerMap[hdr] ?: return ""
                val c = row.getCell(idx) ?: return ""
                return when (c.cellType) {
                    CellType.STRING -> c.stringCellValue
                    CellType.NUMERIC -> c.numericCellValue.toString()
                    CellType.BOOLEAN -> c.booleanCellValue.toString()
                    else -> ""
                }.trim()
            }

            for (i in 1..totalRows) {
                val row = sheet.getRow(i)
                if (row == null) {
                    failureCount++
                } else {
                    try {
                        // Extract fields using mapping
                        val extracted = mutableMapOf<String, String>()
                        for (m in mappings) {
                            val value = getCell(row, m.importedHeader)
                            if (!m.systemHeader.isNullOrBlank()) {
                                extracted[m.systemHeader!!] = value
                            }
                        }

                        // Normalize data
                        val skuVal = extracted["SKU"].orEmpty().trim()
                        val rawPrice = extracted["Price"].orEmpty()
                        val cleanPrice = rawPrice.replace(Regex("[$,]"), "")
                        val now =
                            SimpleDateFormat("dd-MM-yyyy hh:mm:ss a", Locale.getDefault()).format(
                                Date()
                            )

                        // Check if SKU already exists
                        val existing = skuVal.takeIf { it.isNotBlank() }
                            ?.let { AwsManager.getProductBySku(PRODUCT_TABLE, it) }

                        val product = ProductModel(
                            id = existing?.id ?: UUID.randomUUID().toString(),
                            selectedImages = extracted["Image"]?.takeIf { it.isNotBlank() }
                                ?.let { listOf(it) } ?: emptyList(),
                            selectedVideo = extracted["Video"].takeIf { it?.isNotBlank() == true },
                            productName = extracted["Title"].orEmpty(),
                            productCategory = extracted["Category"].orEmpty(),
                            styleNo = extracted["Style No"].orEmpty(),
                            sku = skuVal,
                            price = cleanPrice,
                            description = extracted["Description"].orEmpty(),
                            isImageSelected = !extracted["Image"].isNullOrBlank(),
                            isMediaUpdated = false,
                            tagId = existing?.tagId ?: "",
                            status = existing?.status ?: "Pending",
                            createdAt = existing?.createdAt ?: now,
                            updatedAt = now,
                            previewImageUrls = null,
                            previewVideoUrl = null
                        )
                        parsedProducts += product
                        val (ok, _) = AwsManager.saveProduct(PRODUCT_TABLE, product)
                        if (ok) {
                            successCount++
//                      parsedProducts.add(product) // ✅ add to list
                        } else {
                            failureCount++
                        }

                    } catch (e: Exception) {
                        e.printStackTrace()
                        failureCount++
                    }
                }

                val pct = (i * 100 / totalRows).coerceAtMost(100)
                withContext(Dispatchers.Main) { onProgress(pct) }
            }

            workbook.close()
            parsedProductsLiveData.postValue(parsedProducts)
            withContext(Dispatchers.Main) { onComplete(successCount, failureCount) }
        }
    }

}

