package com.example.rfidstockpro.bulkupload.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.rfidstockpro.RFIDApplication.Companion.PRODUCT_TABLE
import com.example.rfidstockpro.aws.AwsManager
import com.example.rfidstockpro.aws.models.ProductModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.apache.poi.ss.usermodel.CellType
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.text.SimpleDateFormat
import java.util.*

class BulkUploadViewModel : ViewModel() {

  // Mapping user Excel headers to your app fields
  private val systemHeaderMap = mapOf(
    "Title"             to "productName",
    "Category"          to "productCategory",
    "Style No"          to "styleNo",
    "10K Price"         to "price",
    "Description"       to "description",
    "Compressed Image"  to "selectedImages",
    "Compressed Video"  to "selectedVideo"
  )

  fun uploadProductsFromExcel(
    context: Context,
    fileUri: Uri,
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

      // Build dynamic header → column index map
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

      // Loop over data rows
      for (i in 1..totalRows) {
        val row = sheet.getRow(i)
        if (row == null) {
          failureCount++
          updateProgress(i, totalRows, onProgress)
          continue
        }

        // Build extracted data map dynamically
        val extractedData = mutableMapOf<String, String>()
        for ((excelHeader, appField) in systemHeaderMap) {
          extractedData[appField] = getCellValue(row, excelHeader)
        }

        // Validate required fields
        if (extractedData["productName"].isNullOrBlank() ||
          extractedData["price"].isNullOrBlank() ||
          extractedData["productCategory"].isNullOrBlank()
        ) {
          failureCount++
          updateProgress(i, totalRows, onProgress)
          continue
        }

        // Create ProductModel
        val product = mapExtractedDataToProduct(extractedData)

        val (wasSaved, _) = AwsManager.saveProduct(PRODUCT_TABLE, product)
        if (wasSaved) successCount++ else failureCount++

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

    val rawPrice = data["price"].orEmpty()
    val cleanedPrice = rawPrice.replace(Regex("[$,]"), "") // clean price

    return ProductModel(
      id = id,
      selectedImages = data["selectedImages"]?.takeIf { it.isNotBlank() }?.let { listOf(it) } ?: emptyList(),
      selectedVideo = data["selectedVideo"].takeIf { it?.isNotBlank() == true },
      productName = data["productName"].orEmpty(),
      productCategory = data["productCategory"].orEmpty(),
      styleNo = data["styleNo"].orEmpty(),
      sku = "SKU-${System.currentTimeMillis()}",
      price = cleanedPrice,
      description = data["description"].orEmpty(),
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
