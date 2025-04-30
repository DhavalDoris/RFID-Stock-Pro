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

  val systemHeaders = listOf("Title", "Category", "Style No", "SKU", "Price", "Description", "Image", "Video")
  val mandatoryHeaders = systemHeaders.filterNot { it in listOf("Description", "Style No") }
  var fileUri: Uri? = null

  fun processExcelFile(context: Context, uri: Uri) {
    viewModelScope.launch(Dispatchers.IO) {
      val errors = mutableListOf<String>()
      val mappingItems = mutableListOf<MappingItem>()
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

        if (sheet.lastRowNum < 1) {
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
            mappingItems += MappingItem(headerName, sample)
            excelHeadersFound += headerName
          }
        }

        val missingHeaders = mandatoryHeaders.filterNot { hdr ->
          excelHeadersFound.any { it.equals(hdr, ignoreCase = true) }
        }

        if (missingHeaders.isNotEmpty()) {
          errors += "Missing mandatory headers: ${missingHeaders.joinToString()}"
        }

        // Check for duplicate SKUs
        val skuKey = headerMap.keys.firstOrNull { it.equals("SKU", ignoreCase = true) }
        if (skuKey != null) {
          val idx = headerMap[skuKey]!!
          val counts = mutableMapOf<String, Int>()
          for (r in 1..sheet.lastRowNum) {
            val value = sheet.getRow(r)?.getCell(idx)?.let { cell ->
              when (cell.cellType) {
                CellType.STRING -> cell.stringCellValue.trim()
                CellType.NUMERIC -> cell.numericCellValue.toString()
                else -> ""
              }
            }.orEmpty()
            if (value.isNotBlank()) counts[value] = counts.getOrDefault(value, 0) + 1
          }
          counts.filterValues { it > 1 }.forEach { (sku, times) ->
            errors += "SKU \"$sku\" appears $times times"
          }
        }

        // Check for empty mandatory fields
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
              errors += "$header is empty at row ${r + 1}"
            }
          }
        }

        workbook.close()
        fileNameLiveData.postValue(getFileName(uri, context))
        headersLiveData.postValue(mappingItems)
        validationErrorsLiveData.postValue(errors)

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
            val now = SimpleDateFormat("dd-MM-yyyy hh:mm:ss a", Locale.getDefault()).format(Date())

            // Check if SKU already exists
            val existing = skuVal.takeIf { it.isNotBlank() }
              ?.let { AwsManager.getProductBySku(PRODUCT_TABLE, it) }

            val product = ProductModel(
              id = existing?.id ?: UUID.randomUUID().toString(),
              selectedImages = extracted["Image"]?.takeIf { it.isNotBlank() }?.let { listOf(it) } ?: emptyList(),
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

            val (ok, _) = AwsManager.saveProduct(PRODUCT_TABLE, product)
            if (ok) successCount++ else failureCount++

          } catch (e: Exception) {
            e.printStackTrace()
            failureCount++
          }
        }

        val pct = (i * 100 / totalRows).coerceAtMost(100)
        withContext(Dispatchers.Main) { onProgress(pct) }
      }

      workbook.close()
      withContext(Dispatchers.Main) { onComplete(successCount, failureCount) }
    }
  }

}

