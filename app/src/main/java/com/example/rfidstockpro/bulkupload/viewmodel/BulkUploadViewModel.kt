package com.example.rfidstockpro.bulkupload.viewmodel

import android.content.Context
import android.net.Uri
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
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.text.SimpleDateFormat
import java.util.*

class BulkUploadViewModel : ViewModel() {

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

      // Build header → column index map
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

      fun getCellValue(row: org.apache.poi.ss.usermodel.Row, headerName: String): String {
        val idx = headerMap[headerName] ?: return ""
        val cell = row.getCell(idx) ?: return ""
        return when (cell.cellType) {
          CellType.STRING  -> cell.stringCellValue
          CellType.NUMERIC -> cell.numericCellValue.toString()
          CellType.BOOLEAN -> cell.booleanCellValue.toString()
          CellType.FORMULA -> cell.cellFormula
          else             -> ""
        }.trim()
      }

      for (i in 1..totalRows) {
        val row = sheet.getRow(i)
        if (row == null) {
          failureCount++
          updateProgress(i, totalRows, onProgress)
          continue
        }

        try {
          val extractedData = mutableMapOf<String, String>()

          for (mapping in mappings) {
            val excelHeader = mapping.importedHeader
            val systemField = mapping.systemHeader ?: continue
            val value = getCellValue(row, excelHeader)
            extractedData[systemField] = value
          }

          val product = mapExtractedDataToProduct(extractedData)

          val (wasSaved, _) = AwsManager.saveProduct(PRODUCT_TABLE, product)
          if (wasSaved) successCount++ else failureCount++

        } catch (e: Exception) {
          e.printStackTrace()
          failureCount++
        }

        updateProgress(i, totalRows, onProgress)
      }

      workbook.close()

      withContext(Dispatchers.Main) {
        onComplete(successCount, failureCount)
      }
    }
  }

  // --- Progress update helper
  private suspend fun updateProgress(current: Int, total: Int, onProgress: (percent: Int) -> Unit) {
    val percent = (current * 100 / total)
    withContext(Dispatchers.Main) {
      onProgress(percent)
    }
  }


  // --- Data Mapping Helper
  private fun mapExtractedDataToProduct(data: Map<String, String>): ProductModel {
    val id = UUID.randomUUID().toString()

    val now = SimpleDateFormat("dd-MM-yyyy hh:mm:ss a", Locale.getDefault())
      .format(Date())

    val rawPrice = data["Price"].orEmpty()
    val cleanedPrice = rawPrice.replace(Regex("[$,]"), "") // clean price

    return ProductModel(
      id = id,
      selectedImages = data["Image"]?.takeIf { it.isNotBlank() }?.let { listOf(it) } ?: emptyList(),
      selectedVideo = data["Video"].takeIf { it?.isNotBlank() == true },
      productName = data["Title"].orEmpty(),
      productCategory = data["Category"].orEmpty(),
      styleNo = data["Style No"].orEmpty(),
      sku = data["SKU"].orEmpty() ,
      price = cleanedPrice,
      description = data["Description"].orEmpty(),
      isImageSelected = !data["selectedImages"].isNullOrBlank(),
      isMediaUpdated = false,
      tagId = "", // empty, because bulk upload
      status = "in",
      createdAt = now,
      updatedAt = now,
      previewImageUrls = data["selectedImages"]?.let { listOf(it) },
      previewVideoUrl = data["selectedVideo"]
    )
  }
}
