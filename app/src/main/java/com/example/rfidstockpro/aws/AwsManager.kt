package com.example.rfidstockpro.aws

import android.app.ProgressDialog
import android.content.Context
import android.net.Uri
import android.util.Log
import android.webkit.MimeTypeMap
import android.widget.Toast
import com.example.rfidstockpro.RFIDApplication
import com.example.rfidstockpro.RFIDApplication.Companion.IN_OUT_COLLECTIONS_TABLE
import com.example.rfidstockpro.RFIDApplication.Companion.PRODUCT_TABLE
import com.example.rfidstockpro.RFIDApplication.Companion.USER_TABLE
import com.example.rfidstockpro.aws.models.ProductModel
import com.example.rfidstockpro.aws.models.UserModel
import com.example.rfidstockpro.aws.models.toMap
import com.example.rfidstockpro.aws.models.toProductModel
import com.example.rfidstockpro.inouttracker.model.CollectionModel
import com.example.rfidstockpro.inouttracker.model.toCollectionModel
import com.example.rfidstockpro.inouttracker.model.toMap
import com.example.rfidstockpro.ui.activities.AddProductActivity.Companion.previewImageUrls
import com.example.rfidstockpro.ui.activities.AddProductActivity.Companion.previewVideoUrl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.core.interceptor.ExecutionAttributes
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.dynamodb.DynamoDbClient
import software.amazon.awssdk.services.dynamodb.model.AttributeDefinition
import software.amazon.awssdk.services.dynamodb.model.AttributeValue
import software.amazon.awssdk.services.dynamodb.model.CreateTableRequest
import software.amazon.awssdk.services.dynamodb.model.DeleteItemRequest
import software.amazon.awssdk.services.dynamodb.model.DescribeTableRequest
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest
import software.amazon.awssdk.services.dynamodb.model.GlobalSecondaryIndex
import software.amazon.awssdk.services.dynamodb.model.KeySchemaElement
import software.amazon.awssdk.services.dynamodb.model.KeyType
import software.amazon.awssdk.services.dynamodb.model.Projection
import software.amazon.awssdk.services.dynamodb.model.ProjectionType
import software.amazon.awssdk.services.dynamodb.model.ProvisionedThroughput
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest
import software.amazon.awssdk.services.dynamodb.model.QueryRequest
import software.amazon.awssdk.services.dynamodb.model.ScalarAttributeType
import software.amazon.awssdk.services.dynamodb.model.ScanRequest
import software.amazon.awssdk.services.dynamodb.model.ScanResponse
import software.amazon.awssdk.services.dynamodb.model.UpdateItemRequest
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.CompleteMultipartUploadRequest
import software.amazon.awssdk.services.s3.model.CompletedMultipartUpload
import software.amazon.awssdk.services.s3.model.CompletedPart
import software.amazon.awssdk.services.s3.model.CreateMultipartUploadRequest
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest
import software.amazon.awssdk.services.s3.model.UploadPartRequest
import software.amazon.awssdk.services.ses.SesClient
import java.io.File
import java.io.FileInputStream
import java.text.SimpleDateFormat
import java.time.Duration
import java.util.Date
import java.util.Locale
import java.util.UUID

object AwsManager {

    lateinit var dynamoDBClient: DynamoDbClient
    private val AWS_ACCESS_KEY = RFIDApplication.getAwsAccessKeyFromNdk()
    private val AWS_SECRET_KEY = RFIDApplication.getAwsSecretKeyFromNdk()
    const val BUCKET_NAME = "rfid-stock-pro"
    private val AWS_REGION = Region.US_EAST_1


    fun init(context: Context) {
        try {
            val credentialsProvider = StaticCredentialsProvider.create(
                AwsBasicCredentials.create(AWS_ACCESS_KEY, AWS_SECRET_KEY)
            )

            dynamoDBClient = DynamoDbClient.builder()
                .region(AWS_REGION)
                .overrideConfiguration {
                    it.apiCallAttemptTimeout(Duration.ofSeconds(30))
                    it.addExecutionInterceptor(NoCompressionInterceptor()) // custom
                }
                .credentialsProvider(credentialsProvider)
                .httpClient(UrlConnectionHttpClient.create())
                .build()

            Log.e("AWS_TAG", "DynamoDB Initialized Successfully")
        } catch (e: Exception) {
            Log.e("AWS_TAG", "Error Initializing AWS: ${e.message}", e)
        }
    }

    // ✅ AWS SDK v2 S3 Client
    val s3Client: S3Client = S3Client.builder()
        .region(AWS_REGION)
        .credentialsProvider(
            StaticCredentialsProvider.create(
                AwsBasicCredentials.create(AWS_ACCESS_KEY, AWS_SECRET_KEY)
            )
        )
        .httpClient(UrlConnectionHttpClient.create()) // Required for Android
        .build()

    // ✅ AWS SDK v2 SES Client
    private val sesClient: SesClient = SesClient.builder()
        .region(AWS_REGION)
        .credentialsProvider(
            StaticCredentialsProvider.create(
                AwsBasicCredentials.create(AWS_ACCESS_KEY, AWS_SECRET_KEY)
            )
        )
        .httpClient(UrlConnectionHttpClient.create()) // Required for Android
        .build()

    private var instance: AwsManager? = null

    fun getInstance(): AwsManager {
        if (instance == null) {
            instance = AwsManager
        }
        return instance!!
    }

    fun uploadMediaToS3(
        scope: CoroutineScope,
        context: Context,
        imageFiles: List<File>,
        videoFile: File?,
        onSuccess: (List<String>, String?) -> Unit,
        onError: (String) -> Unit
    ) {
        scope.launch(Dispatchers.IO) {
            var progressDialog: ProgressDialog? = null

            try {
                withContext(Dispatchers.Main) {
                    progressDialog = ProgressDialog(context).apply {
                        setMessage("Uploading... 0%")
                        setProgressStyle(ProgressDialog.STYLE_HORIZONTAL)
                        isIndeterminate = false
                        max = 100
                        setCancelable(false)
                        show()
                    }
                }

                val uploadedImageUrls = mutableListOf<String>()

                // Upload multiple images one by one
                for ((index, imageFile) in imageFiles.withIndex()) {
                    val imageKey = "images/${UUID.randomUUID()}_${imageFile.name}"

                    val imageUrl = multipartUpload(
                        s3Client,
                        imageFile,
                        imageKey,
                        5 * 1024 * 1024
                    ) { progress ->
                        // Update progress on main thread
                        scope.launch(Dispatchers.Main) {
                            progressDialog?.progress = progress
                            progressDialog?.setMessage("Uploading Image ${index + 1}/${imageFiles.size}... $progress%")
                        }
                    }

                    uploadedImageUrls.add(imageUrl)
                }

                var videoUrl: String? = null
                if (videoFile != null) {
                    val videoExtension = videoFile.extension.ifEmpty { "mp4" }
                    val videoKey =
                        "videos/${UUID.randomUUID()}_${System.currentTimeMillis()}.$videoExtension"

                    videoUrl = multipartUpload(
                        s3Client,
                        videoFile,
                        videoKey,
                        5 * 1024 * 1024
                    ) { progress ->
                        // Update progress on main thread
                        scope.launch(Dispatchers.Main) {
                            progressDialog?.progress = progress
                            progressDialog?.setMessage("Uploading Video... $progress%")
                        }
                    }
                }

                withContext(Dispatchers.Main) {
                    progressDialog?.dismiss()
                    onSuccess(uploadedImageUrls, videoUrl)
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    progressDialog?.dismiss()
                    Toast.makeText(context, "Upload Failed: ${e.message}", Toast.LENGTH_SHORT)
                        .show()
                    Log.e("AWS_TAG", "Upload Failed: ${e.message}")
                    onError(e.message ?: "Unknown error")
                }
            }
        }
    }

    // ✅ Multipart Upload Function with 2MB Chunk Size & Progress Tracking
    private suspend fun multipartUpload(
        s3Client: S3Client,
        file: File,
        key: String,
        partSize: Int,
        onProgress: (Int) -> Unit // Remove suspend keyword from lambda
    ): String {

        val contentType = when (file.extension.lowercase()) {
            "png" -> "image/png"
            "jpg", "jpeg" -> "image/jpeg"
            "mp4" -> "video/mp4"
            "pdf" -> "application/pdf"
            "txt" -> "text/plain"
            "json" -> "application/json"
            else -> "application/octet-stream" // Default type if unknown
        }


        val initiateRequest = CreateMultipartUploadRequest.builder()
            .bucket(BUCKET_NAME)
            .key(key)
            .contentType(contentType)
            .build()
        val uploadId = s3Client.createMultipartUpload(initiateRequest).uploadId()

        val fileSize = file.length()
        val parts = mutableListOf<CompletedPart>()
        var totalUploadedBytes = 0L

        FileInputStream(file).use { input ->
            var partNumber = 1
            var position = 0L

            while (position < fileSize) {
                val size = minOf(partSize.toLong(), fileSize - position)
                val buffer = ByteArray(size.toInt())

                val bytesRead = input.read(buffer)
                if (bytesRead == -1) break  // Stop reading if EOF is reached

                val partRequest = UploadPartRequest.builder()
                    .bucket(BUCKET_NAME)
                    .key(key)
                    .uploadId(uploadId)
                    .partNumber(partNumber)
                    .build()

                val partETag =
                    s3Client.uploadPart(partRequest, RequestBody.fromBytes(buffer)).eTag()
                parts.add(CompletedPart.builder().partNumber(partNumber).eTag(partETag).build())

                totalUploadedBytes += bytesRead
                val progress = ((totalUploadedBytes.toDouble() / fileSize) * 100).toInt()

                // ✅ Update progress correctly in the main thread
                withContext(Dispatchers.Main) {
                    onProgress(progress)
                }

                position += size
                partNumber++
            }
        }

        val completeRequest = CompleteMultipartUploadRequest.builder()
            .bucket(BUCKET_NAME)
            .key(key)
            .uploadId(uploadId)
            .multipartUpload(CompletedMultipartUpload.builder().parts(parts).build())
            .build()

        s3Client.completeMultipartUpload(completeRequest)
        return "https://$BUCKET_NAME.s3.amazonaws.com/$key"
    }

    /*fun deleteMediaFromS3(imageUrls: List<String>, videoUrl: String?) {
        try {
            val s3Client = s3Client // your initialized S3 client

            // Delete images
            imageUrls.forEach { url ->
                val key = extractKeyFromUrl(url)
                s3Client.deleteObject { it.bucket(BUCKET_NAME).key(key) }
            }

            // Delete video if available
            videoUrl?.let {
                val key = extractKeyFromUrl(it)
                s3Client.deleteObject { it.bucket(BUCKET_NAME).key(key) }
            }

            Log.d("AWS_CLEANUP", "🧹 Deleted uploaded media after failure")
        } catch (e: Exception) {
            Log.e("AWS_CLEANUP", "❌ Failed to clean up uploaded media: ${e.message}", e)
        }
    }*/

    /*fun deleteMediaFromS3(
        imageUrls: List<String>,
        videoUrl: String?
    ): List<String> {
        val deletedMedia = mutableListOf<String>()
        val failedDeletions = mutableListOf<String>()

        try {
            val s3Client = s3Client // your initialized S3 client

            // Handle image deletions
            for (url in imageUrls) {
                try {
                    val key = extractKeyFromUrl(url)
                    s3Client.deleteObject { it.bucket(BUCKET_NAME).key(key) }
                    Log.d("AWS_CLEANUP", "✅ Deleted image from S3: $url (key: $key)")
                    deletedMedia.add(url)
                } catch (ex: Exception) {
                    Log.e("AWS_CLEANUP", "❌ Failed to delete image: $url — ${ex.message}")
                    failedDeletions.add(url)
                }
            }

            // Handle video deletion
            videoUrl?.let { video ->
                try {
                    val key = extractKeyFromUrl(video)
                    s3Client.deleteObject { it.bucket(BUCKET_NAME).key(key) }
                    Log.d("AWS_CLEANUP", "✅ Deleted video from S3: $video (key: $key)")
                    deletedMedia.add(video)
                } catch (ex: Exception) {
                    Log.e("AWS_CLEANUP", "❌ Failed to delete video: $video — ${ex.message}")
                    failedDeletions.add(video)
                }
            }

            if (failedDeletions.isEmpty()) {
                Log.d("AWS_CLEANUP", "🧹 All media deleted successfully.")
            } else {
                Log.w("AWS_CLEANUP", "⚠️ Some media failed to delete: $failedDeletions")
            }

        } catch (e: Exception) {
            Log.e("AWS_CLEANUP", "🔥 Unexpected error during media cleanup: ${e.message}", e)
        }
        return deletedMedia
    }*/

    fun deleteMediaFromS3(imageUrls: List<String>, videoUrl: String?) {
        try {
            val s3Client = s3Client // Initialized S3 client

            Log.d("AWS_DELETE_DEBUG", "🧹 Attempting to delete media from S3...")

            if (imageUrls.isEmpty() && videoUrl.isNullOrEmpty()) {
                Log.w("AWS_DELETE_DEBUG", "⚠️ No media URLs provided for deletion")
                return
            }

            imageUrls.forEach { url ->
                val key = extractKeyFromUrl(url)
                Log.d("AWS_DELETE_DEBUG", "🗑️ Deleting image with key: $key")
                try {
                    s3Client.deleteObject { it.bucket(BUCKET_NAME).key(key) }
                    Log.d("AWS_DELETE_DEBUG", "✅ Successfully deleted: $key")
                } catch (e: Exception) {
                    Log.e("AWS_DELETE_DEBUG", "❌ Failed to delete image: $key - ${e.message}")
                }
            }

            videoUrl?.let {
                val key = extractKeyFromUrl(it)
                Log.d("AWS_DELETE_DEBUG", "🗑️ Deleting video with key: $key")
                try {
                    s3Client.deleteObject { it.bucket(BUCKET_NAME).key(key) }
                    Log.d("AWS_DELETE_DEBUG", "✅ Successfully deleted video: $key")
                } catch (e: Exception) {
                    Log.e("AWS_DELETE_DEBUG", "❌ Failed to delete video: $key - ${e.message}")
                }
            }

            Log.d("AWS_DELETE_DEBUG", "🧹 All media delete operations attempted.")
        } catch (e: Exception) {
            Log.e("AWS_DELETE_DEBUG", "❌ deleteMediaFromS3() failed: ${e.message}", e)
        }
    }

    fun OnlyUpdateProductToAWS(
        context: Context,
        product: ProductModel,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val scope = CoroutineScope(Dispatchers.IO)

        scope.launch {
            Log.d("AWS_UPDATE", "🆕 Starting product add for ID: ${product.id}")
            Log.d("AWS_UPDATE", "product: ${product.toString()}")

            val imageFiles = product.selectedImages
                .filter { it.startsWith("/") || it.startsWith("content://") }
                .map { File(it) }
                .filter { it.exists() && it.length() > 0 }

            val videoFile = product.selectedVideo?.takeIf {
                it.startsWith("/") && File(it).exists() && File(it).length() > 0
            }?.let { File(it) }

            /*   if (imageFiles.isEmpty() && videoFile == null) {
                   withContext(Dispatchers.Main) {
                       onError("No valid media selected to upload.")
                   }
                   return@launch
               }*/

            Log.d("AWS_UPDATE", "⬆️ Uploading media to S3...")

            uploadMediaToS3(
                scope = scope,
                context = context,
                imageFiles = imageFiles,
                videoFile = videoFile,
                onSuccess = { uploadedImageUrls, uploadedVideoUrl ->
                    Log.d("AWS_UPDATE", "✅ Media uploaded.")
                    Log.d("AWS_UPDATE", "🖼️ Image URLs: $uploadedImageUrls")
                    Log.d("AWS_UPDATE", "🎥 Video URL: $uploadedVideoUrl")

                    val finalProduct = product.copy(
                        selectedImages = if (uploadedImageUrls.isNotEmpty()) uploadedImageUrls else product.selectedImages,
                        selectedVideo = uploadedVideoUrl ?: product.selectedVideo,
                        isMediaUpdated = true
                    )

                    scope.launch {
                        val (isSuccess, saveMessage) = updateProduct(PRODUCT_TABLE, finalProduct)
                        withContext(Dispatchers.Main) {
                            if (isSuccess) {
                                Log.i("AWS_UPDATE", "✅ Product saved to DynamoDB.")
                                onSuccess()

                                Log.i("AWS_UPDATE", "$previewImageUrls")
                                Log.i("AWS_UPDATE", "$previewVideoUrl")

                                Log.i(
                                    "AWS_UPDATE",
                                    "-> " + if (uploadedImageUrls.isNotEmpty()) previewImageUrls else emptyList()
                                )
                                Log.i(
                                    "AWS_UPDATE",
                                    "~> " + if (uploadedVideoUrl != null) previewVideoUrl else null
                                )
                                scope.launch {
                                    deleteMediaFromS3(
                                        imageUrls = if (uploadedImageUrls.isNotEmpty()) previewImageUrls else emptyList(),
                                        videoUrl = if (uploadedVideoUrl != null) previewVideoUrl else null
                                    )
                                }

                            } else {
                                Log.e("AWS_UPDATE", "❌ Failed to save product: $saveMessage")
                                onError("Failed to save product: $saveMessage")

                                // 🔁 Rollback uploaded media
                                scope.launch {
                                    deleteMediaFromS3(
                                        imageUrls = uploadedImageUrls,
                                        videoUrl = uploadedVideoUrl
                                    )
                                }
                            }
                        }
                    }
                },
                onError = { errorMessage ->
                    Log.e("AWS_UPDATE", "❌ Upload error: $errorMessage")
                    onError(errorMessage)
                }
            )
        }
    }

    private fun extractKeyFromUrl(url: String): String {
        val uri = Uri.parse(url)
        return uri.path?.removePrefix("/") ?: ""
    }


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

    /*    fun getAllImageUrls(callback: (List<String>?) -> Unit) {
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
        }*/

    fun close() {
        sesClient.close()
    }

    fun ensureUserTableExists(tableName: String, primaryKey: String, callback: (String) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                Log.e("AWS_TAG", "Checking if table exists: $tableName")

                val existingTables = dynamoDBClient.listTables().tableNames()
                Log.e("AWS_TAG", "Existing Tables: $existingTables")

                if (!existingTables.contains(tableName)) {
                    callback.invoke("creating")

                    val attributeDefinitions = mutableListOf(
                        AttributeDefinition.builder().attributeName(primaryKey)
                            .attributeType(ScalarAttributeType.S).build()
                    )

                    val createTableBuilder = CreateTableRequest.builder()
                        .tableName(tableName)
                        .keySchema(
                            KeySchemaElement.builder().attributeName(primaryKey)
                                .keyType(KeyType.HASH).build()
                        )
                        .provisionedThroughput(
                            ProvisionedThroughput.builder().readCapacityUnits(5L)
                                .writeCapacityUnits(5L).build()
                        )

                    // ✅ If it's the product table, add GSI on tagId
                    if (tableName == PRODUCT_TABLE) {
                        attributeDefinitions.add(
                            AttributeDefinition.builder().attributeName("tagId")
                                .attributeType(ScalarAttributeType.S).build()
                        )

                        createTableBuilder.globalSecondaryIndexes(
                            GlobalSecondaryIndex.builder()
                                .indexName("tagId-index")
                                .keySchema(
                                    KeySchemaElement.builder().attributeName("tagId")
                                        .keyType(KeyType.HASH).build()
                                )
                                .projection(
                                    Projection.builder().projectionType(ProjectionType.ALL).build()
                                )
                                .provisionedThroughput(
                                    ProvisionedThroughput.builder()
                                        .readCapacityUnits(5L)
                                        .writeCapacityUnits(5L)
                                        .build()
                                )
                                .build()
                        )
                    }

                    createTableBuilder.attributeDefinitions(attributeDefinitions)

                    val request = createTableBuilder.build()
                    dynamoDBClient.createTable(request)

                    Log.e("AWS_TAG", "Table creation started: $tableName")
                    waitForTableToBeActive(tableName)
                    callback.invoke("created")
                } else {
                    callback.invoke("exists")
                }
            } catch (e: Exception) {
                Log.e("AWS_TAG", "❌ Error Checking/Creating Table: ${e.message}", e)
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

    fun saveUser(tableName: String, user: UserModel): Boolean {
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

    suspend fun saveProduct(tableName: String, product: ProductModel): Pair<Boolean, String> {

        return withContext(Dispatchers.IO) {
            try {
                Log.e("saveProductTAG", "saveProduct: >> " + product.tagId  )
                if (!product.tagId.isNullOrBlank()) {
                    Log.e("saveProductTAG", "saveProduct: --->> " + product.tagId  )

                    // 🔍 Step 1: Scan for existing tagId
                    val scanRequest = ScanRequest.builder()
                        .tableName(tableName)
                        .filterExpression("tagId = :tagId")
                        .expressionAttributeValues(
                            mapOf(":tagId" to AttributeValue.builder().s(product.tagId).build())
                        )
                        .build()

                    val scanResponse = dynamoDBClient.scan(scanRequest)

                    if (scanResponse.count() > 0) {
                        val msg = "Product with tagId '${product.tagId}' already exists."
                        Log.e("AWS_SAVE", msg)
                        return@withContext Pair(false, msg)
                    }
                }

                // ✅ Step 2: Convert to map & clean up tagId if empty
                val item = product.toMap().toMutableMap()
                if (item["tagId"]?.s().isNullOrBlank()) {
                    item.remove("tagId")
                }

                val putItemRequest = PutItemRequest.builder()
                    .tableName(tableName)
//                    .item(product.toMap()) // Use the extension function
                    .item(item) // Use the extension function
                    .build()

                dynamoDBClient.putItem(putItemRequest)
                val successMsg = "Product added successfully: ${product.productName}"
                Log.d("AWS_SAVE", successMsg)
                Pair(true, successMsg)
            } catch (e: Exception) {
                val errorMsg = "Error saving product: ${e.message}"
                Log.e("AWS_SAVE", errorMsg, e)
                Pair(false, errorMsg)
            }
        }

    }

    suspend fun updateProduct(tableName: String, product: ProductModel): Pair<Boolean, String> {
        return withContext(Dispatchers.IO) {
            try {

                Log.i("AWS_UPDATE", "==> " + product.toString())

                val expressionAttributeNames = mutableMapOf(
                    "#status" to "status"
                )

                val expressionAttributeValues = mutableMapOf(
                    ":productName" to AttributeValue.builder().s(product.productName).build(),
                    ":productCategory" to AttributeValue.builder().s(product.productCategory)
                        .build(),
                    ":sku" to AttributeValue.builder().s(product.sku).build(),
                    ":price" to AttributeValue.builder().s(product.price).build(),
                    ":description" to AttributeValue.builder().s(product.description).build(),
                    ":isImageSelected" to AttributeValue.builder().bool(product.isImageSelected)
                        .build(),
                    ":tagId" to AttributeValue.builder().s(product.tagId).build(),
                    ":status" to AttributeValue.builder().s(product.status).build(),
                    ":createdAt" to AttributeValue.builder().s(product.createdAt).build(),
                    ":updatedAt" to AttributeValue.builder().s(product.updatedAt).build()
                )

                val updateExpressionParts = mutableListOf(
                    "productName = :productName",
                    "productCategory = :productCategory",
                    "sku = :sku",
                    "price = :price",
                    "description = :description",
                    "isImageSelected = :isImageSelected",
                    "tagId = :tagId",
                    "#status = :status",
                    "createdAt = :createdAt",
                    "updatedAt = :updatedAt"
                )

                if (product.isMediaUpdated) {
                    expressionAttributeValues[":selectedImages"] = AttributeValue.builder().l(
                        product.selectedImages.map { AttributeValue.builder().s(it).build() }
                    ).build()
                    expressionAttributeValues[":selectedVideo"] =
                        AttributeValue.builder().s(product.selectedVideo ?: "").build()

                    updateExpressionParts += listOf(
                        "selectedImages = :selectedImages",
                        "selectedVideo = :selectedVideo"
                    )
                }

                val updateRequest = UpdateItemRequest.builder()
                    .tableName(tableName)
                    .key(mapOf("id" to AttributeValue.builder().s(product.id ?: "").build()))
                    .updateExpression("SET ${updateExpressionParts.joinToString(", ")}")
                    .expressionAttributeNames(expressionAttributeNames)
                    .expressionAttributeValues(expressionAttributeValues)
                    .build()

                dynamoDBClient.updateItem(updateRequest)

                Log.i("AWS_UPDATE", "✅ Product '${product.productName}' updated successfully.")
                Pair(true, "Product updated successfully")
            } catch (e: Exception) {
                val errorMsg = "❌ Error updating product: ${e.message}"
                Log.e("AWS_UPDATE", errorMsg, e)
                Pair(false, errorMsg)
            }
        }
    }

    suspend fun checkIfTagIdExists(tableName: String, tagId: String): Pair<Boolean, String> {
        return withContext(Dispatchers.IO) {
            try {
                val queryRequest = QueryRequest.builder()
                    .tableName(tableName)
                    .indexName("tagId-index") // 🔥 Must match your GSI
                    .keyConditionExpression("tagId = :tagVal")
                    .expressionAttributeValues(
                        mapOf(
                            ":tagVal" to AttributeValue.builder().s(tagId).build()
                        )
                    )
                    .build()

                val result = dynamoDBClient.query(queryRequest)

                if (result.count() > 0) {
                    true to "Product with tagId '$tagId' already exists!"
                } else {
                    false to "✅ tagId is unique, continuing."
                }

            } catch (e: Exception) {
                Log.e("AWS_TAG_CHECK", "❌ Error checking tagId: ${e.message}", e)
                true to "❌ Failed to validate tagId: ${e.message}"
            }
        }
    }

    suspend fun getTotalProductCount(): Int {
        val request = ScanRequest.builder()
            .tableName(PRODUCT_TABLE)
            .select(software.amazon.awssdk.services.dynamodb.model.Select.COUNT)
            .build()

        val result = dynamoDBClient.scan(request)
        return result.count()
    }

    /* suspend fun getPaginatedProducts(
         lastKey: Map<String, AttributeValue>? = null
     ): Triple<List<ProductModel>, Map<String, AttributeValue>?,Int> {
         val requestBuilder = ScanRequest.builder()
             .tableName(PRODUCT_TABLE)
             .limit(5)

         lastKey?.let {
             requestBuilder.exclusiveStartKey(it)
         }

         val result = dynamoDBClient.scan(requestBuilder.build())
         val items = result.items().map { it.toProductModel() }
         val newLastKey = result.lastEvaluatedKey()

         return Pair(items, if (newLastKey.isEmpty()) null else newLastKey)
     }*/

    fun getPaginatedProducts(
        lastEvaluatedKey: Map<String, AttributeValue>? = null
    ): Pair<List<ProductModel>, Map<String, AttributeValue>?> {

        val dynamoDbClient: DynamoDbClient = dynamoDBClient // Replace with your instance
        val tableName = PRODUCT_TABLE // 🔁 Replace with your actual table name

        return try {
            val scanRequestBuilder = ScanRequest.builder()
                .tableName(tableName)
                .limit(5) // 👈 Number of items per page

            // Set last evaluated key if paginating
            lastEvaluatedKey?.let { scanRequestBuilder.exclusiveStartKey(it) }

            val scanRequest = scanRequestBuilder.build()
            val scanResponse: ScanResponse = dynamoDbClient.scan(scanRequest)

            val products = scanResponse.items().map { it.toProductModel() }
            val newLastKey = scanResponse.lastEvaluatedKey().takeIf { it.isNotEmpty() }

            Pair(products, newLastKey)

        } catch (e: Exception) {
            Log.e("DynamoDB", "getPaginatedProducts() error: ", e)
            Pair(emptyList(), null)
        }
    }

    fun getAllProducts(
        lastEvaluatedKey: Map<String, AttributeValue>? = null
    ): Pair<List<ProductModel>, Map<String, AttributeValue>?> {

        val dynamoDbClient: DynamoDbClient = dynamoDBClient // Replace with your instance
        val tableName = PRODUCT_TABLE // 🔁 Replace with your actual table name

        return try {
            val scanRequestBuilder = ScanRequest.builder()
                .tableName(tableName)
            // Set last evaluated key if paginating
            lastEvaluatedKey?.let { scanRequestBuilder.exclusiveStartKey(it) }

            val scanRequest = scanRequestBuilder.build()
            val scanResponse: ScanResponse = dynamoDbClient.scan(scanRequest)

            val products = scanResponse.items().map { it.toProductModel() }
            val newLastKey = scanResponse.lastEvaluatedKey().takeIf { it.isNotEmpty() }

            Pair(products, newLastKey)

        } catch (e: Exception) {
            Log.e("DynamoDB", "getPaginatedProducts() error: ", e)
            Pair(emptyList(), null)
        }
    }

    class NoCompressionInterceptor : ExecutionInterceptor {
        override fun modifyHttpRequest(
            context: software.amazon.awssdk.core.interceptor.Context.ModifyHttpRequest,
            executionAttributes: ExecutionAttributes
        ): software.amazon.awssdk.http.SdkHttpRequest {
            return context.httpRequest().toBuilder()
                .putHeader("Accept-Encoding", "identity")
                .build()
        }
    }

    suspend fun getProductById(
        tableName: String,
        productId: String
    ): ProductModel? {
        return withContext(Dispatchers.IO) {
            try {
                Log.e("productIds_TAG", "getProductById(): " + productId )
                val request = GetItemRequest.builder()
                    .tableName(tableName)
                    .key(mapOf("id" to AttributeValue.builder().s(productId).build()))
                    .build()

                val response = dynamoDBClient.getItem(request)

                if (response.hasItem()) {
                    response.item().toProductModel()
                } else {
                    null
                }
            } catch (e: Exception) {
                Log.e("DynamoDB", "getProductById error: ${e.message}")
                null
            }
        }
    }


    fun getProductById(
        tableName: String,
        productId: String,
        onSuccess: (ProductModel) -> Unit,
        onError: (Exception) -> Unit
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val request = GetItemRequest.builder()
                    .tableName(tableName)
                    .key(mapOf("id" to AttributeValue.builder().s(productId).build()))
                    .build()

                val response = dynamoDBClient.getItem(request)

                if (response.hasItem()) {
                    val product = response.item().toProductModel()
                    withContext(Dispatchers.Main) {
                        onSuccess(product)
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        onError(Exception("No product found with ID: $productId"))
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onError(e)
                }
            }
        }
    }

    suspend fun deleteProduct(product: ProductModel): Boolean {
        return try {

            // Delete all images from S3
            product.selectedImages.forEach { url ->
                val key = extractS3KeyFromUrl(url)
                deleteFileFromS3(key)
            }

            // Delete video if present
            product.selectedVideo?.let { videoUrl ->
                val key = extractS3KeyFromUrl(videoUrl)
                deleteFileFromS3(key)
            }

            val deleteRequest = DeleteItemRequest.builder()
                .tableName(PRODUCT_TABLE) // Use your actual DynamoDB table name here
                .key(
                    mapOf(
                        "id" to AttributeValue.builder().s(product.id).build()
                    )
                ) // Assuming "id" is your partition key
                .build()

            dynamoDBClient.deleteItem(deleteRequest)
            true // Return true if deletion is successful
        } catch (e: Exception) {
            Log.e("AwsManager", "Failed to delete product", e)
            false // Return false if there was an error
        }
    }

    fun extractS3KeyFromUrl(url: String): String {
        return url.substringAfter(".amazonaws.com/") // This gives you just the S3 object key
    }

    fun deleteFileFromS3(key: String) {
        val deleteRequest = DeleteObjectRequest.builder()
            .bucket(BUCKET_NAME) // Replace with your actual bucket
            .key(key)
            .build()

        s3Client.deleteObject(deleteRequest)
    }

    fun addCollectionToDynamoDB(collection: CollectionModel) {

        CoroutineScope(Dispatchers.IO).launch {
            ensureUserTableExists(IN_OUT_COLLECTIONS_TABLE, "collectionId") { status ->
                when (status) {
                    "creating" -> Log.e(
                        "AWS_TAG",
                        "$IN_OUT_COLLECTIONS_TABLE Creating DynamoDB Table..."
                    )

                    "created", "exists" -> {
                        Log.e("AWS_TAG", "$IN_OUT_COLLECTIONS_TABLE DynamoDB Table Ready!")

                        val itemValues = mutableMapOf<String, AttributeValue>(
                            "collectionId" to AttributeValue.builder().s(collection.collectionId).build(),
                            "collectionName" to AttributeValue.builder().s(collection.collectionName).build(),
                            "description" to AttributeValue.builder().s(collection.description).build(),
                            "createdDateTime" to AttributeValue.builder().s(collection.createdDateTime).build(),
                            "updatedDateTime" to AttributeValue.builder().s(collection.updatedDateTime).build(),
                            "userId" to AttributeValue.builder().s(collection.userId).build(),
                            /*"productIds" to AttributeValue.builder()
                                .s(if (collection.productIds.isEmpty()) "[]" else collection.productIds.joinToString(","))
                                .build()*/
                            "productIds" to AttributeValue.builder()
                                .l(collection.productIds.map { AttributeValue.builder().s(it).build() })
                                .build()
                        )

                        val request = PutItemRequest.builder()
                            .tableName(IN_OUT_COLLECTIONS_TABLE)
                            .item(itemValues)
                            .build()

                        dynamoDBClient.putItem(request)
                    }

                    else -> Log.e("AWS_TAG", "$IN_OUT_COLLECTIONS_TABLE Error: $status")
                }
            }
        }

    }

    suspend fun getCollectionById(collectionId: String): CollectionModel? {
        return try {
            val request = GetItemRequest.builder()
                .tableName(IN_OUT_COLLECTIONS_TABLE) // Replace with your actual table name
                .key(mapOf("collectionId" to AttributeValue.builder().s(collectionId).build()))
                .build()

            val response = dynamoDBClient.getItem(request)

            if (response.hasItem()) {
                response.item().toCollectionModel()
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun updateCollection(updatedCollection: CollectionModel): Boolean {
        return try {
            val request = PutItemRequest.builder()
                .tableName(IN_OUT_COLLECTIONS_TABLE) // Replace with your actual table name
                .item(updatedCollection.toMap())
                .build()

            dynamoDBClient.putItem(request)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }


    fun checkIfCollectionNameExists(
        tableName: String,
        collectionName: String,
        onResult: (Boolean) -> Unit
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {

                val scanRequest = ScanRequest.builder()
                    .tableName(tableName)
                    .build()

                val result = dynamoDBClient.scan(scanRequest)
                val trimmedInput = collectionName.trim().lowercase()
                val exists = result.items().any {
                    it["collectionName"]?.s()?.trim()?.lowercase() == trimmedInput
                }

                if (exists) {
                    Log.d("AWSManager", "✅ Collection name '$collectionName' exists in table '$tableName'")
                    result.items().forEach { item ->
                        Log.d("AWSManager", "Matched item: ${item["collectionName"]?.s()}")
                    }
                } else {
                    Log.d("AWSManager", "❌ Collection name '$collectionName' does NOT exist in table '$tableName'")
                }

                withContext(Dispatchers.Main) {
                    onResult(exists)
                }
            } catch (e: Exception) {
                Log.e("AWSManager", "Error checking collection name: ${e.message}")
                withContext(Dispatchers.Main) {
                    onResult(false)
                }
            }
        }
    }


    fun updateCollectionProductIds(
        tableName: String,
        collectionId: String,
        newProductIds: List<String>,
        onResult: (Boolean) -> Unit
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Step 1: Fetch the current item
                val getRequest = GetItemRequest.builder()
                    .tableName(tableName)
                    .key(mapOf("collectionId" to AttributeValue.builder().s(collectionId).build()))
                    .build()

                val result = dynamoDBClient.getItem(getRequest)
                val currentItem = result.item()

                if (currentItem == null) {
                    withContext(Dispatchers.Main) {
                        onResult(false)
                    }
                    return@launch
                }

                val existingProductIds = currentItem["productIds"]?.l()?.map { it.s()!! } ?: emptyList()

                // Step 2: Merge and remove duplicates
                val updatedProductIds = (existingProductIds + newProductIds).toSet().toList()

                val updateRequest = UpdateItemRequest.builder()
                    .tableName(tableName)
                    .key(mapOf("collectionId" to AttributeValue.builder().s(collectionId).build()))
                    .updateExpression("SET productIds = :p, updatedDateTime = :updatedTime")
                    .expressionAttributeValues(mapOf(
                        ":p" to AttributeValue.builder().l(
                            updatedProductIds.map { AttributeValue.builder().s(it).build() }
                        ).build(),
                        ":updatedTime" to AttributeValue.builder().s(currentTime()).build()
                    ))
                    .build()

                dynamoDBClient.updateItem(updateRequest)

                withContext(Dispatchers.Main) {
                    onResult(true)
                }
            } catch (e: Exception) {
                Log.e("AWSManager", "Error updating product IDs: ${e.message}")
                withContext(Dispatchers.Main) {
                    onResult(false)
                }
            }
        }
    }

    fun currentTime(): String {
        val format = SimpleDateFormat("dd-MM-yyyy hh:mm:ss a", Locale.getDefault())
        return format.format(Date())
    }

    suspend fun deleteCollectionById(collectionId: String) {
        try {
            // Build the request to delete the item by its collectionId
            val request = DeleteItemRequest.builder()
                .tableName(IN_OUT_COLLECTIONS_TABLE) // Replace with actual table name
                .key(
                    mapOf(
                        "collectionId" to AttributeValue.builder().s(collectionId).build()
                    )
                )
                .build()

            // Perform the delete operation
            val response = dynamoDBClient.deleteItem(request)

            // Log the success
            Log.d("AwsManager", "Successfully deleted collection with ID: $collectionId")
        } catch (e: Exception) {
            // Log the error
            Log.e("AwsManager", "Error deleting collection with ID: $collectionId. ${e.localizedMessage}", e)
            throw e // Re-throw the exception to be handled by the calling code
        }
    }


    suspend fun getProductBySku(
        tableName: String,
        sku: String
    ): ProductModel? = withContext(Dispatchers.IO) {
        // scan for an item where sku = :skuVal
        val scanReq = ScanRequest.builder()
            .tableName(tableName)
            .filterExpression("sku = :skuVal")
            .expressionAttributeValues(
                mapOf(":skuVal" to AttributeValue.builder().s(sku).build())
            )
            .build()

        val resp = dynamoDBClient.scan(scanReq)
        if (resp.count() > 0) {
            val item = resp.items()[0]
            // build a ProductModel from the raw attribute map:
            ProductModel(
                id               = item["id"]?.s(),
                selectedImages   = item["selectedImages"]?.l()?.map { it.s() } ?: emptyList(),
                selectedVideo    = item["selectedVideo"]?.s(),
                productName      = item["productName"]?.s().orEmpty(),
                productCategory  = item["productCategory"]?.s().orEmpty(),
                styleNo          = item["styleNo"]?.s().orEmpty(),
                sku              = item["sku"]?.s().orEmpty(),
                price            = item["price"]?.s().orEmpty(),
                description      = item["description"]?.s().orEmpty(),
                isImageSelected  = item["isImageSelected"]?.bool() ?: false,
                isMediaUpdated   = item["isMediaUpdated"]?.bool() ?: false,
                tagId            = item["tagId"]?.s().orEmpty(),
                status           = item["status"]?.s().orEmpty(),
                createdAt        = item["createdAt"]?.s().orEmpty(),
                updatedAt        = item["updatedAt"]?.s().orEmpty(),
                previewImageUrls = null,
                previewVideoUrl  = null
            )
        } else {
            null
        }
    }
}
