package com.example.rfidstockpro.Utils

import android.content.Context
import android.net.Uri
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import com.example.rfidstockpro.R
import com.example.rfidstockpro.aws.AwsManager.getFileExtension
import java.util.UUID

object Comman {

    fun generateUniqueFileName(originalUri: Uri, context: Context): String {
        val extension = getFileExtension(context, originalUri)  // Get the file extension
        val uuid = UUID.randomUUID().toString()  // Generate a UUID
        val timestamp = System.currentTimeMillis()  // Get current timestamp

        return "media_${uuid}_$timestamp.$extension"  // Create unique filename
    }

    public fun getFileName(uri: Uri,context: Context): String {
        var name = ""
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
            if (cursor.moveToFirst() && nameIndex >= 0) {
                name = cursor.getString(nameIndex)
            }
        }
        return name
    }
    fun showCustomSnackbarBelowToolbar(activity: AppCompatActivity, anchor: View) {
        val inflater = activity.layoutInflater
        val customView = inflater.inflate(R.layout.custom_snackbar, null)

        val parent = activity.findViewById<ViewGroup>(android.R.id.content)
        val container = FrameLayout(activity)
        container.addView(customView)

        // Add just below the toolbar
        val layoutParams = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        layoutParams.topMargin = anchor.bottom // place below toolbar

        parent.addView(container, layoutParams)

        // Auto-dismiss after 2.5 seconds
        customView.postDelayed({
            parent.removeView(container)
        }, 2500)
    }

}