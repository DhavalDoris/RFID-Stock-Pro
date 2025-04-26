package com.example.rfidstockpro.bulkupload.activity

import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.rfidstockpro.bulkupload.viewmodel.BulkUploadViewModel
import com.example.rfidstockpro.databinding.ActivityBulkUploadBinding

class BulkUploadActivity : AppCompatActivity() {

  private lateinit var binding: ActivityBulkUploadBinding
  private val viewModel: BulkUploadViewModel by viewModels()

  private val openDocumentLauncher = registerForActivityResult(
    ActivityResultContracts.OpenDocument()
  ) { uri: Uri? ->
    uri?.let { startBulkUpload(it) }
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    binding = ActivityBulkUploadBinding.inflate(layoutInflater)
    setContentView(binding.root)

    binding.btnSelectExcel.setOnClickListener {
      openDocumentLauncher.launch(arrayOf(
        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
        "application/vnd.ms-excel"
      ))
    }
  }

  private fun startBulkUpload(fileUri: Uri) {
    binding.btnSelectExcel.isEnabled = false
    viewModel.uploadProductsFromExcel(
      context   = this,
      fileUri   = fileUri,
      onProgress = { current, total ->
        runOnUiThread {
          binding.tvProgress.text = "Uploading $current of $total"
          binding.progressBar.max = total
          binding.progressBar.progress = current
        }
      },
      onComplete = { successCount, failureCount ->
        runOnUiThread {
          binding.btnSelectExcel.isEnabled = true
          Toast.makeText(
            this,
            "Upload complete: $successCount succeeded, $failureCount failed",
            Toast.LENGTH_LONG
          ).show()
        }
      }
    )
  }
}
