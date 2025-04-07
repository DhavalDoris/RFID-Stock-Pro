package com.example.rfidstockpro

import android.app.Application
import android.util.Log
import com.example.rfidstockpro.aws.AwsManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


class RFIDApplication : Application() {


    companion object {
        private const val TAG = "AppContext"
        const val USER_TABLE = "user"
        const val PRODUCT_TABLE = "product"
        var instance: RFIDApplication? = null

        init {
            System.loadLibrary("rfidstockpro")
        }

        external fun getAwsAccessKeyFromNdk(): String
        external fun getAwsSecretKeyFromNdk(): String
        external fun getEncryptionKey(): String

        val ENCRYPTION_KEY = getEncryptionKey()
    }


    override fun onCreate() {
        super.onCreate()

        instance = this
        AwsManager.init(this)
        createDynamoDBTable()

    }

    private fun createDynamoDBTable() {

        CoroutineScope(Dispatchers.IO).launch {
            AwsManager.ensureUserTableExists(USER_TABLE, "email") { status ->
                when (status) {
                    "creating" -> Log.e("AWS_TAG", "$USER_TABLE Creating DynamoDB Table...")
                    "created", "exists" -> Log.e("AWS_TAG", "$USER_TABLE DynamoDB Table Ready!")
                    else -> Log.e("AWS_TAG", "$USER_TABLE Error: $status")
                }
            }
            AwsManager.ensureUserTableExists(PRODUCT_TABLE, "id") { status ->
                when (status) {
                    "creating" -> Log.e("AWS_TAG", "$PRODUCT_TABLE Creating DynamoDB Table...")
                    "created", "exists" -> {
                        Log.e("AWS_TAG", "$PRODUCT_TABLE DynamoDB Table Ready!")
                    }
                    else -> Log.e("AWS_TAG", "$PRODUCT_TABLE Error: $status")
                }
            }

        }
    }
}
