package com.example.rfidstockpro.aws

import android.content.Context
import android.util.Log
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBMapper
import com.amazonaws.regions.Regions
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient
import com.amazonaws.services.dynamodbv2.model.AttributeDefinition
import com.amazonaws.services.dynamodbv2.model.CreateTableRequest
import com.amazonaws.services.dynamodbv2.model.DescribeTableRequest
import com.amazonaws.services.dynamodbv2.model.KeySchemaElement
import com.amazonaws.services.dynamodbv2.model.KeyType
import com.amazonaws.services.dynamodbv2.model.ProvisionedThroughput
import com.amazonaws.services.dynamodbv2.model.ScalarAttributeType
import com.example.rfidstockpro.aws.models.UserModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


object AwsManager {

    lateinit var dynamoDBMapper: DynamoDBMapper
    lateinit var dynamoDBClient: AmazonDynamoDBClient

    const val TABLE_NAME = "user"

    fun init(context: Context) {

        try {
            val awsAccessKey = "AKIAU5LH6AA6PZMWLVGH" // Replace with IAM User Access Key
            val awsSecretKey =
                "82uAgthAYF8t4Di5CNzJHtfS46BhKjnGhz9uWv7D" // Replace with IAM User Secret Key
            val awsRegion = Regions.US_EAST_1 // Change to your AWS region

            val credentials = BasicAWSCredentials(awsAccessKey, awsSecretKey)
            dynamoDBClient = AmazonDynamoDBClient(credentials).apply {
                setRegion(com.amazonaws.regions.Region.getRegion(awsRegion))
            }
            dynamoDBMapper = DynamoDBMapper(dynamoDBClient)

            Log.e("AWS_TAG", "DynamoDB Initialized Successfully")
        } catch (e: Exception) {
            Log.e("AWS_TAG", "Error Initializing AWS: ${e.message}", e)
        }
    }

    fun ensureTableExists(callback: (String) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                Log.e("AWS_TAG", "Checking if table exists: $TABLE_NAME")

                val existingTables = dynamoDBClient.listTables().tableNames
                Log.e("AWS_TAG", "Existing Tables: $existingTables")

                if (!existingTables.contains(TABLE_NAME)) {
                    callback.invoke("creating")

                    val request = CreateTableRequest()
                        .withTableName(TABLE_NAME)
                        .withKeySchema(KeySchemaElement("email", KeyType.HASH)) // Primary Key
                        .withAttributeDefinitions(
                            AttributeDefinition(
                                "email",
                                ScalarAttributeType.S
                            )
                        ) // String Type
                        .withProvisionedThroughput(
                            ProvisionedThroughput(
                                5L,
                                5L
                            )
                        ) // Read & Write Capacity

                    dynamoDBClient.createTable(request)
                    Log.e("AWS_TAG", "Table creation started")

                    // Wait for table to be active
                    waitForTableToBeActive(TABLE_NAME)
                    callback.invoke("created")
                } else {
                    callback.invoke("exists")
                }
            } catch (e: Exception) {
                Log.e("AWS_TAG", "Error Checking/Creating Table: ${e.message}", e)
                callback.invoke("error: ${e.message}")
            }
        }
    }

    private fun waitForTableToBeActive(tableName: String) {
        while (true) {
            val tableStatus =
                dynamoDBClient.describeTable(DescribeTableRequest().withTableName(tableName)).table.tableStatus
            if (tableStatus == "ACTIVE") break
            Thread.sleep(2000) // Wait for 2 seconds before checking again
        }
    }

    fun getUserByEmail(email: String): UserModel? {
        return try {
            dynamoDBMapper.load(UserModel::class.java)
            dynamoDBMapper.load(UserModel::class.java, email)
        } catch (e: Exception) {
            Log.e("AWS_TAG", "Error fetching user: ${e.message}", e)
            null
        }
    }

    suspend fun fetchAllUsers() = withContext(Dispatchers.IO) {
        try {

            val scanRequest = com.amazonaws.services.dynamodbv2.model.ScanRequest()
                .withTableName(AwsManager.TABLE_NAME)


            val scanResponse = AwsManager.dynamoDBClient.scan(scanRequest)

            for (item in scanResponse.items) {
                val email = item["email"]?.s ?: "Unknown"
                val password = item["password"]?.s ?: "Unknown"
                Log.d("AWS_TAG", "User: Email=$email, Password=$password")
            }

            if (scanResponse.items.isEmpty()) {
                Log.d("AWS_TAG", "No users found in the table")
            } else {
                Log.d("AWS_TAG", "ELSE")
            }
        } catch (e: Exception) {
            Log.e("AWS_TAG", "Error fetching users: ${e.message}", e)
        }
    }

    fun saveUser(user: UserModel): Boolean {
        return try {
            val updateItemRequest = com.amazonaws.services.dynamodbv2.model.UpdateItemRequest()
                .withTableName(TABLE_NAME)
                .withKey(
                    mapOf(
                        "email" to com.amazonaws.services.dynamodbv2.model.AttributeValue(
                            user.email
                        )
                    )
                )
                .withAttributeUpdates(
                    mapOf(
                        "password" to com.amazonaws.services.dynamodbv2.model.AttributeValueUpdate()
                            .withValue(com.amazonaws.services.dynamodbv2.model.AttributeValue(user.password))
                            .withAction(com.amazonaws.services.dynamodbv2.model.AttributeAction.PUT)
                    )
                )

            dynamoDBClient.updateItem(updateItemRequest) // Update item in DynamoDB
            Log.e("AWS_TAG", "User updated successfully: ${user.email}")
            true
        } catch (e: Exception) {
            Log.e("AWS_TAG", "Error updating user: ${e.message}", e)
            false
        }
    }


    fun deleteUser(email: String): Boolean {
        return try {
            val user = getUserByEmail(email)
            if (user != null) {
                dynamoDBMapper.delete(user)
                true
            } else {
                Log.e("AWS_TAG", "User not found with email: $email")
                false
            }
        } catch (e: Exception) {
            Log.e("AWS_TAG", "Error deleting user: ${e.message}", e)
            false
        }
    }

}
