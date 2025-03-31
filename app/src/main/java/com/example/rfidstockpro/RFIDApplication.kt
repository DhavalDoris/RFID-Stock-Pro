package com.example.rfidstockpro

import android.app.Application
import android.util.Log
import com.example.rfidstockpro.aws.AwsManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


class RFIDApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        instance = this
        AwsManager.init(this)
        createDynamoDBTable()
    }

    companion object {
        private const val TAG = "AppContext"

        var instance: RFIDApplication? = null
            private set
    }

    private fun createDynamoDBTable() {
        CoroutineScope(Dispatchers.IO).launch {
            AwsManager.ensureTableExists { status ->
                when (status) {
                    "creating" -> Log.e("AWS_TAG", "Creating DynamoDB Table...")
                    "created", "exists" -> Log.e("AWS_TAG", "DynamoDB Table Ready!")
                    else -> Log.e("AWS_TAG", "Error: $status")
                }
            }
        }
    }
}
