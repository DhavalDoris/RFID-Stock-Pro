package com.example.rfidstockpro.aws

import android.app.ProgressDialog
import android.content.Context
import android.net.Uri
import android.os.Build
import android.util.Log
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.annotation.RequiresApi
import com.example.rfidstockpro.R
import com.example.rfidstockpro.RFIDApplication.Companion.USER_TABLE
import com.example.rfidstockpro.aws.models.UserModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.core.async.AsyncRequestBody
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.dynamodb.DynamoDbClient
import software.amazon.awssdk.services.dynamodb.model.*
import software.amazon.awssdk.services.s3.S3AsyncClient
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import software.amazon.awssdk.services.s3.model.PutObjectResponse
import software.amazon.awssdk.services.s3.model.S3Object
import software.amazon.awssdk.services.ses.SesClient
import java.io.File
import java.util.UUID
import java.util.concurrent.CompletableFuture

object AwsManager {

    lateinit var dynamoDBClient: DynamoDbClient


    const val PRODUCT_TABLE = "products"
    private const val AWS_ACCESS_KEY = "AKIAU5LH6AA6PZMWLVGH" // Replace with your IAM User Access Key
    private const val AWS_SECRET_KEY = "82uAgthAYF8t4Di5CNzJHtfS46BhKjnGhz9uWv7D" // Replace with your IAM User Secret Key
    const val BUCKET_NAME = "rfid-stock-pro"
    const val FOLDER_NAME = "rfid-uploads"
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

        private var instance: AwsManager? = null

        fun getInstance(): AwsManager {
            if (instance == null) {
                instance = AwsManager
            }
            return instance!!
        }

        // Static method to upload a file without parameters in the AwsManager constructor
        fun uploadFileToS3(imageFile: File, bucketName: String, listener: UploadListener) {
            val awsManager = getInstance() // Get the instance of AwsManager
            val key = "uploads/${imageFile.name}" // S3 object key

            val putObjectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build()

            try {
                val response: PutObjectResponse = awsManager.s3Client.putObject(
                    putObjectRequest,
                    RequestBody.fromFile(imageFile)
                )

                // After successful upload, return the URL
                val imageUrl = "https://$bucketName.s3.amazonaws.com/$key"
                listener.onUploadComplete(imageUrl)

            } catch (exception: Exception) {
                listener.onUploadError(exception)
            }
        }


    // Callback interface to track upload status
    interface UploadListener {
        fun onUploadComplete(imageUrl: String)
        fun onUploadError(exception: Exception?)
    }


    /*fun uploadMediaToS3(
        context: Context,
        imageFile: File,
        videoFile: File?, // Video file is optional
        onSuccess: (String, String?) -> Unit, // Callback with both URLs
        onError: (String) -> Unit
    ) {
        val progressDialog = ProgressDialog(context).apply {
            setMessage(context.getString(R.string.uploading_please_wait))
            setCancelable(false)
            show()
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Upload Image
                val imageKey = "$FOLDER_NAME/images/${UUID.randomUUID()}_${imageFile.name}"
                val imageRequest = PutObjectRequest.builder()
                    .bucket(BUCKET_NAME)
                    .key(imageKey)
                    .build()
                s3Client.putObject(imageRequest, RequestBody.fromFile(imageFile))
                val imageUrl = "https://$BUCKET_NAME.s3.amazonaws.com/$imageKey"

                // Upload Video (if available)
                var videoUrl: String? = null
                if (videoFile != null) {
                    val videoKey = "$FOLDER_NAME/videos/${UUID.randomUUID()}_${videoFile.name}"
                    val videoRequest = PutObjectRequest.builder()
                        .bucket(BUCKET_NAME)
                        .key(videoKey)
                        .build()
                    s3Client.putObject(videoRequest, RequestBody.fromFile(videoFile))
                    videoUrl = "https://$BUCKET_NAME.s3.amazonaws.com/$videoKey"
                }

                withContext(Dispatchers.Main) {
                    progressDialog.dismiss()
                    Toast.makeText(context, context.getString(R.string.upload_successful), Toast.LENGTH_SHORT).show()
                    onSuccess(imageUrl, videoUrl)
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    progressDialog.dismiss()
                    Toast.makeText(context, context.getString(R.string.upload_failed, e.message), Toast.LENGTH_SHORT).show()
                    onError(e.message ?: "Unknown error")
                }
            }
        }
    }*/

    fun uploadMediaToS3(
        context: Context,
        imageFile: File,
        videoFile: File?, // Video file is optional
        onSuccess: (String, String?) -> Unit, // Callback with both URLs
        onError: (String) -> Unit
    ) {
        val progressDialog = ProgressDialog(context).apply {
            setMessage(context.getString(R.string.uploading_please_wait))
            setCancelable(false)
            show()
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                Log.d("AWS_UPLOAD", "Upload started...")

                // Upload Image
                val imageKey = "$FOLDER_NAME/images/${UUID.randomUUID()}_${imageFile.name}"
                Log.d("AWS_UPLOAD", "Uploading image: $imageKey")
                val imageRequest = PutObjectRequest.builder()
                    .bucket(BUCKET_NAME)
                    .key(imageKey)
                    .build()

                // Use RequestBody.fromFile() for synchronous upload
                s3Client.putObject(imageRequest, RequestBody.fromFile(imageFile))
                val imageUrl = "https://$BUCKET_NAME.s3.amazonaws.com/$imageKey"
                Log.d("AWS_UPLOAD", "Image uploaded successfully: $imageUrl")

                // Upload Video (if available)
                var videoUrl: String? = null
                if (videoFile != null) {
                    val videoKey = "$FOLDER_NAME/videos/${UUID.randomUUID()}_${videoFile.name}"
                    Log.d("AWS_UPLOAD", "Uploading video: $videoKey")
                    val videoRequest = PutObjectRequest.builder()
                        .bucket(BUCKET_NAME)
                        .key(videoKey)
                        .build()

                    // Use RequestBody.fromFile() for synchronous upload
                    s3Client.putObject(videoRequest, RequestBody.fromFile(videoFile))
                    videoUrl = "https://$BUCKET_NAME.s3.amazonaws.com/$videoKey"
                    Log.d("AWS_UPLOAD", "Video uploaded successfully: $videoUrl")
                } else {
                    Log.d("AWS_UPLOAD", "No video selected, skipping video upload.")
                }

                withContext(Dispatchers.Main) {
                    progressDialog.dismiss()
                    Log.d("AWS_UPLOAD", "Upload complete, dismissing dialog")
                    Toast.makeText(context, context.getString(R.string.upload_successful), Toast.LENGTH_SHORT).show()
                    onSuccess(imageUrl, videoUrl)
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    progressDialog.dismiss()
                    Log.e("AWS_UPLOAD", "Upload failed: ${e.message}")
                    Toast.makeText(context, context.getString(R.string.upload_failed, e.message), Toast.LENGTH_SHORT).show()
                    onError(e.message ?: "Unknown error")
                }
            }
        }
    }

  /*  fun uploadMediaToS3(
        context: Context,
        imageFile: File,
        videoFile: File?, // Video file is optional
        onSuccess: (String, String?) -> Unit, // Callback with both URLs
        onError: (String) -> Unit
    ) {
        val progressDialog = ProgressDialog(context).apply {
            setMessage(context.getString(R.string.uploading_please_wait))
            setCancelable(false)
            show()
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                Log.d("AWS_UPLOAD", "Upload started...")

                // Upload Image
                val imageKey = "$FOLDER_NAME/images/${UUID.randomUUID()}_${imageFile.name}"
                Log.d("AWS_UPLOAD", "Uploading image: $imageKey")
                val imageRequest = PutObjectRequest.builder()
                    .bucket(BUCKET_NAME)
                    .key(imageKey)
                    .build()
                s3Client.putObject(imageRequest, RequestBody.fromFile(imageFile))
                val imageUrl = "https://$BUCKET_NAME.s3.amazonaws.com/$imageKey"
                Log.d("AWS_UPLOAD", "Image uploaded successfully: $imageUrl")

                // Upload Video (if available)
                var videoUrl: String? = null
                if (videoFile != null) {
                    val videoKey = "$FOLDER_NAME/videos/${UUID.randomUUID()}_${videoFile.name}"
                    Log.d("AWS_UPLOAD", "Uploading video: $videoKey")
                    val videoRequest = PutObjectRequest.builder()
                        .bucket(BUCKET_NAME)
                        .key(videoKey)
                        .build()
                    s3Client.putObject(videoRequest, RequestBody.fromFile(videoFile))
                    videoUrl = "https://$BUCKET_NAME.s3.amazonaws.com/$videoKey"
                    Log.d("AWS_UPLOAD", "Video uploaded successfully: $videoUrl")
                } else {
                    Log.d("AWS_UPLOAD", "No video selected, skipping video upload.")
                }

                withContext(Dispatchers.Main) {
                    progressDialog.dismiss()
                    Log.d("AWS_UPLOAD", "Upload complete, dismissing dialog")
                    Toast.makeText(context, context.getString(R.string.upload_successful), Toast.LENGTH_SHORT).show()
                    onSuccess(imageUrl, videoUrl)
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    progressDialog.dismiss()
                    Log.e("AWS_UPLOAD", "Upload failed: ${e.message}")
                    Toast.makeText(context, context.getString(R.string.upload_failed, e.message), Toast.LENGTH_SHORT).show()
                    onError(e.message ?: "Unknown error")
                }
            }
        }
    }*/


    fun uriToFile(context: Context, uri: Uri): File {
        val inputStream = context.contentResolver.openInputStream(uri)
            ?: throw IllegalArgumentException("Cannot open URI")
        val fileExtension = getFileExtension(context, uri)
        val tempFile = File.createTempFile("selected_media", ".$fileExtension", context.cacheDir)

        tempFile.outputStream().use { output ->
            inputStream.copyTo(output)
        }
        return tempFile
    }



    fun getFileExtension(context: Context, uri: Uri): String {
        val mimeType = getMimeType(context, uri)
        return when {
            mimeType?.startsWith("image") == true -> "jpg"
            mimeType?.startsWith("video") == true -> "mp4"
            else -> "dat"
        }
    }

    private fun getMimeType(context: Context, file: File): String? {
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(file.extension)
            ?: "application/octet-stream"
    }

    private fun getMimeType(context: Context, uri: Uri): String? {
        return context.contentResolver.getType(uri)
    }

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

    fun ensureUserTableExists(tableName: String ,callback: (String) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                Log.e("AWS_TAG", "Checking if table exists: $tableName")

                val existingTables = dynamoDBClient.listTables().tableNames()
                Log.e("AWS_TAG", "Existing Tables: $existingTables")

                if (!existingTables.contains(tableName)) {
                    callback.invoke("creating")

                    val request = CreateTableRequest.builder()
                        .tableName(tableName)
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
                    waitForTableToBeActive(tableName)
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

    fun getUserByEmail(tableName: String, email: String): UserModel? {
        return try {
            val response = dynamoDBClient.getItem(
                GetItemRequest.builder()
                    .tableName(tableName)
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

    fun saveUser(tableName: String,user: UserModel): Boolean {
        return try {
            val putItemRequest = PutItemRequest.builder()
                .tableName(tableName)
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
