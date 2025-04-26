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

  fun uploadProductsFromExcel(
    context: Context,
    fileUri: Uri,
    onProgress: (current: Int, total: Int) -> Unit,
    onComplete: (successCount: Int, failureCount: Int) -> Unit
  ) {
    viewModelScope.launch(Dispatchers.IO) {
      val input = context.contentResolver.openInputStream(fileUri)
      if (input == null) {
        withContext(Dispatchers.Main) {
          onComplete(0, 0)
        }
        return@launch
      }

      val workbook = XSSFWorkbook(input)
      val sheet = workbook.getSheetAt(0)
      val headerRow = sheet.getRow(0)

      // build header→columnIndex map
      val headerMap = mutableMapOf<String, Int>()
      for (ci in 0 until headerRow.lastCellNum) {
        headerRow.getCell(ci)?.stringCellValue
          ?.takeIf { it.isNotBlank() }
          ?.trim()
          ?.let { headerMap[it] = ci }
      }

      val totalRows = sheet.lastRowNum
      var success = 0
      var fail = 0

      fun getCellValue(row: org.apache.poi.ss.usermodel.Row, header: String): String {
        val idx = headerMap[header] ?: return ""
        val cell = row.getCell(idx) ?: return ""
        return when (cell.cellType) {
          CellType.STRING  -> cell.stringCellValue
          CellType.NUMERIC -> cell.numericCellValue.toString()
          CellType.BOOLEAN -> cell.booleanCellValue.toString()
          CellType.FORMULA -> cell.cellFormula
          else             -> ""
        }.trim()
      }

      // iterate data rows
      for (i in 1..totalRows) {
        val row = sheet.getRow(i)
        if (row == null) {
          fail++
          withContext(Dispatchers.Main) { onProgress(i, totalRows) }
          continue
        }

        // map to your fields
        val dataMap = mapOf(
          "Title"            to getCellValue(row, "Title"),
          "Category"         to getCellValue(row, "Category"),
          "Style No"         to getCellValue(row, "Style No"),
          "10K Price"        to getCellValue(row, "10K Price"),
          "Description"      to getCellValue(row, "Description"),
          "Compressed Image" to getCellValue(row, "Compressed Image"),
          "Compressed Video" to getCellValue(row, "Compressed Video")
        )

        // build ProductModel
        val product = mapRowToProductModel(dataMap)

        // directly save to DynamoDB
        val (wasSaved, _) = AwsManager.saveProduct(PRODUCT_TABLE, product)
        if (wasSaved) success++ else fail++

        // update progress on Main
        withContext(Dispatchers.Main) {
          onProgress(i, totalRows)
        }
      }

      workbook.close()

      // final callback
      withContext(Dispatchers.Main) {
        onComplete(success, fail)
      }
    }
  }

  /*private fun mapRowToProductModel(data: Map<String,String>): ProductModel {
    val now = SimpleDateFormat("dd-MM-yyyy hh:mm:ss a", Locale.getDefault()).format(Date())
    val img = data["Compressed Image"].orEmpty()
    val vid = data["Compressed Video"].orEmpty().takeIf { it.isNotBlank() }

    return ProductModel(
      id               = UUID.randomUUID().toString(),
      selectedImages   = if (img.isNotEmpty()) listOf(img) else emptyList(),
      selectedVideo    = vid,
      productName      = data["Title"].orEmpty(),
      productCategory  = data["Category"].orEmpty(),
      styleNo          = data["Style No"].orEmpty(),
      sku              = "SKU-${System.currentTimeMillis()}",
      price            = data["10K Price"].orEmpty(),
      description      = data["Description"].orEmpty(),
      isImageSelected  = img.isNotEmpty(),
      isMediaUpdated   = false,
      tagId            = "",
      status           = "in",
      createdAt        = now,
      updatedAt        = now,
      previewImageUrls = if (img.isNotEmpty()) listOf(img) else null,
      previewVideoUrl  = vid
    )
  }*/

  private fun mapRowToProductModel(data: Map<String,String>): ProductModel {
    // 1) Generate a UUID for the PK…
    val id = UUID.randomUUID().toString()


    // 3) Timestamp
    val now = SimpleDateFormat("dd-MM-yyyy hh:mm:ss a", Locale.getDefault())
      .format(Date())

    // 4) Pull media URLs from Excel…
    val img = data["Compressed Image"].orEmpty()
    val vid = data["Compressed Video"]
      .orEmpty()
      .takeIf { it.isNotBlank() }

    val rawPrice = data["10K Price"].orEmpty()
    val cleanedPrice = rawPrice.replace(Regex("[$,]"), "")  // Remove $ and commas

    return ProductModel(
      id               = id,
      selectedImages   = if (img.isNotEmpty()) listOf(img) else emptyList(),
      selectedVideo    = vid,
      productName      = data["Title"].orEmpty(),
      productCategory  = data["Category"].orEmpty(),
      styleNo          = data["Style No"].orEmpty(),
      sku              = "SKU-${System.currentTimeMillis()}",
      price            = cleanedPrice,
      description      = data["Description"].orEmpty(),
      isImageSelected  = img.isNotEmpty(),
      isMediaUpdated   = false,

      // ← use the generated, non-empty tagId here:
      tagId = "",

      status           = "in",
      createdAt        = now,
      updatedAt        = now,
      previewImageUrls = if (img.isNotEmpty()) listOf(img) else null,
      previewVideoUrl  = vid
    )
  }

}
