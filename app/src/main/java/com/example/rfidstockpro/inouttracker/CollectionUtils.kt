package com.example.rfidstockpro.inouttracker

import android.content.Context
import android.widget.Toast
import com.example.rfidstockpro.aws.AwsManager
import com.example.rfidstockpro.sharedpref.SessionManager
import com.example.rfidstockpro.R
import com.example.rfidstockpro.inouttracker.model.CollectionModel

object CollectionUtils {
    var selectedCollection: CollectionModel? = null
    fun handleCreateCollection(
        context: Context,
        collectionName: String,
        description: String,
        productIds: List<String>,
        onSuccess: () -> Unit,
        onFailure: () -> Unit
    ) {
        if (collectionName.isEmpty()) {
            Toast.makeText(context, "Collection name cannot be empty", Toast.LENGTH_SHORT).show()
            return
        }

        val sessionManager = SessionManager.getInstance(context)
        val userId = sessionManager.getUserName()



        AwsManager.checkIfCollectionNameExists(
            tableName = com.example.rfidstockpro.RFIDApplication.IN_OUT_COLLECTIONS_TABLE,
            collectionName = collectionName
        ) { exists ->

            if (exists) {
                Toast.makeText(context, "Collection “$collectionName” already exists", Toast.LENGTH_SHORT).show()

//                Toast.makeText(
//                    context,
//                    context.getString(R.string.collection_already_exists, collectionName),
//                    Toast.LENGTH_LONG
//                ).show()
                onFailure()
            } else {
                // Callback to proceed with creating the collection
                onSuccess()
            }
        }
    }
}
