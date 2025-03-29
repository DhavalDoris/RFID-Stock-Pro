package com.example.rfidstockpro.aws

import android.content.Context
import android.util.Log
import com.amazonaws.auth.AWSCredentials
import com.amazonaws.auth.AWSCredentialsProvider
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
import com.amazonaws.services.s3.AmazonS3Client
import com.example.rfidstockpro.aws.models.UserModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request
import software.amazon.awssdk.services.s3.model.S3Object
import software.amazon.awssdk.services.ses.SesClient

object AwsManager {

    lateinit var dynamoDBMapper: DynamoDBMapper
    lateinit var dynamoDBClient: AmazonDynamoDBClient

    const val USER_TABLE = "user"
    val AWS_ACCESS_KEY = "AKIAU5LH6AA6PZMWLVGH" // Replace with IAM User Access Key
    val AWS_SECRET_KEY = "82uAgthAYF8t4Di5CNzJHtfS46BhKjnGhz9uWv7D" // Replace with IAM User Secret Key
    const val BUCKET_NAME = "rfid-stock-pro"
    const val AWS_BUCKET_REGION = "us-east-1"

    fun init(context: Context) {

        val awsRegion = Regions.US_EAST_1 // Change to your AWS region
        try {
            val credentials = BasicAWSCredentials(AWS_ACCESS_KEY, AWS_SECRET_KEY)
            dynamoDBClient = AmazonDynamoDBClient(credentials).apply {
                setRegion(com.amazonaws.regions.Region.getRegion(awsRegion))
            }
            dynamoDBMapper = DynamoDBMapper(dynamoDBClient)
            Log.e("AWS_TAG", "DynamoDB Initialized Successfully")
        } catch (e: Exception) {
            Log.e("AWS_TAG", "Error Initializing AWS: ${e.message}", e)
        }
    }

    // ✅ AWS SDK v1 AmazonS3Client (For TransferUtility)
     val awsS3Client = AmazonS3Client(
        object : AWSCredentialsProvider {
            override fun getCredentials(): AWSCredentials {
                return BasicAWSCredentials(AWS_ACCESS_KEY, AWS_SECRET_KEY)
            }

            override fun refresh() {}
        }
    ).apply {
        setRegion(com.amazonaws.regions.Region.getRegion(Regions.US_EAST_1)) // Change to your region
    }

     val sesClient: SesClient = SesClient.builder()
        .region(Region.US_EAST_1) // Change to your AWS region
        .httpClient(UrlConnectionHttpClient.builder().build()) // Use Android-compatible HTTP client
        .credentialsProvider(StaticCredentialsProvider.create(
            AwsBasicCredentials.create(AWS_ACCESS_KEY, AWS_SECRET_KEY) // Use your AWS credentials
        ))
        .build()

    fun getAllImageUrls(callback: (List<String>?) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val credentials = AwsBasicCredentials.create(AWS_ACCESS_KEY, AWS_SECRET_KEY)
                val s3Client = S3Client.builder()
                    .region(Region.US_EAST_1)
                    .credentialsProvider(StaticCredentialsProvider.create(credentials))
                    .httpClient(UrlConnectionHttpClient.create())
                    .build()

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

    fun ensureTableExists(callback: (String) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                Log.e("AWS_TAG", "Checking if table exists: $USER_TABLE")

                val existingTables = dynamoDBClient.listTables().tableNames
                Log.e("AWS_TAG", "Existing Tables: $existingTables")

                if (!existingTables.contains(USER_TABLE)) {
                    callback.invoke("creating")

                    val request = CreateTableRequest()
                        .withTableName(USER_TABLE)
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
            val tableStatus =
                dynamoDBClient.describeTable(DescribeTableRequest().withTableName(tableName)).table.tableStatus
            if (tableStatus == "ACTIVE") break
            Thread.sleep(2000) // Wait for 2 seconds before checking again
        }
    }

/*    fun getUserByEmail(email: String): UserModel? {
        return try {
            dynamoDBMapper.load(UserModel::class.java)
            dynamoDBMapper.load(UserModel::class.java, email)
        } catch (e: Exception) {
            Log.e("AWS_TAG", "Error fetching user: ${e.message}", e)
            null
        }
    }*/

    fun getUserByEmail(email: String): UserModel? {
        return try {
            val userKey = UserModel(email = email) // Create key object
            dynamoDBMapper.load(UserModel::class.java, userKey.email) // Load user data
        } catch (e: Exception) {
            Log.e("AWS_TAG", "Error fetching user: ${e.message}", e)
            null
        }
    }

    suspend fun fetchAllUsers() = withContext(Dispatchers.IO) {
        try {

            val scanRequest = com.amazonaws.services.dynamodbv2.model.ScanRequest()
                .withTableName(AwsManager.USER_TABLE)


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
                .withTableName(USER_TABLE)
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
