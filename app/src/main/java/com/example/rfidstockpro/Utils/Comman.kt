package com.example.rfidstockpro.Utils

import android.content.Context
import android.net.Uri
import com.example.rfidstockpro.aws.AwsManager.getFileExtension
import java.util.UUID

object Comman {

    fun generateUniqueFileName(originalUri: Uri, context: Context): String {
        val extension = getFileExtension(context, originalUri)  // Get the file extension
        val uuid = UUID.randomUUID().toString()  // Generate a UUID
        val timestamp = System.currentTimeMillis()  // Get current timestamp

        return "media_${uuid}_$timestamp.$extension"  // Create unique filename
    }


}