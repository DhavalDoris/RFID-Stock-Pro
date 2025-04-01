package com.example.rfidstockpro.aws

import android.content.Context
import android.util.Log
import com.example.rfidstockpro.aws.models.UserModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.dynamodb.DynamoDbClient
import software.amazon.awssdk.services.dynamodb.model.*
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request
import software.amazon.awssdk.services.s3.model.S3Object
import software.amazon.awssdk.services.ses.SesClient

object AwsManager {

    lateinit var dynamoDBClient: DynamoDbClient

    const val USER_TABLE = "user"
    const val PRODUCT_TABLE = "products"
    private const val AWS_ACCESS_KEY = "AKIAU5LH6AA6PZMWLVGH" // Replace with your IAM User Access Key
    private const val AWS_SECRET_KEY = "82uAgthAYF8t4Di5CNzJHtfS46BhKjnGhz9uWv7D" // Replace with your IAM User Secret Key
    const val BUCKET_NAME = "rfid-stock-pro"
    private val AWS_REGION = Region.US_EAST_1 // Change this to your AWS region

    fun init(context: Context) {
        try {
            val credentialsProvider = StaticCredentialsProvider.create(
                AwsBasicCredentials.create(AWS_ACCESS_KEY, AWS_SECRET_KEY)
            )

            dynamoDBClient = DynamoDbClient.builder()
                .region(AWS_REGION)
                .credentialsProvider(credentialsProvider)
                .httpClient(UrlConnectionHttpClient.create()) // Required for Android
                .build()

            Log.e("AWS_TAG", "DynamoDB Initialized Successfully")
        } catch (e: Exception) {
            Log.e("AWS_TAG", "Error Initializing AWS: ${e.message}", e)
        }
    }

    // ✅ AWS SDK v2 S3 Client
    val s3Client: S3Client = S3Client.builder()
        .region(AWS_REGION)
        .credentialsProvider(StaticCredentialsProvider.create(
            AwsBasicCredentials.create(AWS_ACCESS_KEY, AWS_SECRET_KEY)
        ))
        .httpClient(UrlConnectionHttpClient.create()) // Required for Android
        .build()

    // ✅ AWS SDK v2 SES Client
    private val sesClient: SesClient = SesClient.builder()
        .region(AWS_REGION)
        .credentialsProvider(StaticCredentialsProvider.create(
            AwsBasicCredentials.create(AWS_ACCESS_KEY, AWS_SECRET_KEY)
        ))
        .httpClient(UrlConnectionHttpClient.create()) // Required for Android
        .build()

    fun getAllImageUrls(callback: (List<String>?) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val listObjectsRequest = ListObjectsV2Request.builder()
                    .bucket(BUCKET_NAME)
                    .build()

                val listObjectsResponse = s3Client.listObjectsV2(listObjectsRequest)

                val imageUrls = listObjectsResponse.contents().map { obj: S3Object ->
                    "https://$BUCKET_NAME.s3.amazonaws.com/${obj.key()}"
                }

                withContext(Dispatchers.Main) {
                    callback(imageUrls) // Return the list of URLs to the UI thread
                }
            } catch (e: Exception) {
                Log.e("AWS_TAG", "Error fetching images: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    callback(null)
                }
            }
        }
    }

    fun close() {
        sesClient.close()
    }

    fun ensureUserTableExists(callback: (String) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                Log.e("AWS_TAG", "Checking if table exists: $USER_TABLE")

                val existingTables = dynamoDBClient.listTables().tableNames()
                Log.e("AWS_TAG", "Existing Tables: $existingTables")

                if (!existingTables.contains(USER_TABLE)) {
                    callback.invoke("creating")

                    val request = CreateTableRequest.builder()
                        .tableName(USER_TABLE)
                        .keySchema(KeySchemaElement.builder().attributeName("email").keyType(KeyType.HASH).build()) // Primary Key
                        .attributeDefinitions(
                            AttributeDefinition.builder().attributeName("email").attributeType(ScalarAttributeType.S).build()
                        ) // String Type
                        .provisionedThroughput(
                            ProvisionedThroughput.builder().readCapacityUnits(5L).writeCapacityUnits(5L).build()
                        ) // Read & Write Capacity
                        .build()

                    dynamoDBClient.createTable(request)
                    Log.e("AWS_TAG", "Table creation started")

                    // Wait for table to be active
                    waitForTableToBeActive(USER_TABLE)
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
            val tableStatus = dynamoDBClient.describeTable(
                DescribeTableRequest.builder().tableName(tableName).build()
            ).table().tableStatus().toString()

            if (tableStatus == "ACTIVE") break
            Thread.sleep(2000) // Wait for 2 seconds before checking again
        }
    }

    fun getUserByEmail(email: String): UserModel? {
        return try {
            val response = dynamoDBClient.getItem(
                GetItemRequest.builder()
                    .tableName(USER_TABLE)
                    .key(mapOf("email" to AttributeValue.builder().s(email).build()))
                    .build()
            )
            if (response.hasItem()) {
                UserModel.fromMap(response.item()) // Convert response to UserModel
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e("AWS_TAG", "Error fetching user: ${e.message}", e)
            null
        }
    }

    suspend fun fetchAllUsers() = withContext(Dispatchers.IO) {
        try {
            val scanRequest = ScanRequest.builder().tableName(USER_TABLE).build()
            val scanResponse = dynamoDBClient.scan(scanRequest)

            for (item in scanResponse.items()) {
                val email = item["email"]?.s() ?: "Unknown"
                val password = item["password"]?.s() ?: "Unknown"
                Log.d("AWS_TAG", "User: Email=$email, Password=$password")
            }

            if (scanResponse.items().isEmpty()) {
                Log.d("AWS_TAG", "No users found in the table")
            } else {
                Log.d("AWS_TAG", "users found in the table")
            }
        } catch (e: Exception) {
            Log.e("AWS_TAG", "Error fetching users: ${e.message}", e)
        }
    }

    fun saveUser(user: UserModel): Boolean {
        return try {
            val putItemRequest = PutItemRequest.builder()
                .tableName(USER_TABLE)
                .item(UserModel.toMap(user)) // Convert user object to DynamoDB map
                .build()

            dynamoDBClient.putItem(putItemRequest)
            Log.e("AWS_TAG", "User updated successfully: ${user.email}")
            true
        } catch (e: Exception) {
            Log.e("AWS_TAG", "Error updating user: ${e.message}", e)
            false
        }
    }

    fun deleteUser(email: String): Boolean {
        return try {
            val deleteRequest = DeleteItemRequest.builder()
                .tableName(USER_TABLE)
                .key(mapOf("email" to AttributeValue.builder().s(email).build()))
                .build()

            dynamoDBClient.deleteItem(deleteRequest)
            Log.e("AWS_TAG", "User deleted successfully: $email")
            true
        } catch (e: Exception) {
            Log.e("AWS_TAG", "Error deleting user: ${e.message}", e)
            false
        }
    }
}
