package com.example.rfidstockpro.aws

import android.content.Context
import android.net.Uri
import android.util.Log
import android.webkit.MimeTypeMap
import com.example.rfidstockpro.RFIDApplication
import com.example.rfidstockpro.RFIDApplication.Companion.PRODUCT_TABLE
import com.example.rfidstockpro.RFIDApplication.Companion.USER_TABLE
import com.example.rfidstockpro.api.S3UploadService
import com.example.rfidstockpro.aws.models.ProductModel
import com.example.rfidstockpro.aws.models.UserModel
import com.example.rfidstockpro.aws.models.toMap
import com.example.rfidstockpro.aws.models.toProductModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.ResponseBody
import okio.BufferedSink
import okio.ForwardingSink
import okio.buffer
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
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
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.CompleteMultipartUploadRequest
import software.amazon.awssdk.services.s3.model.CompletedMultipartUpload
import software.amazon.awssdk.services.s3.model.CompletedPart
import software.amazon.awssdk.services.s3.model.CreateMultipartUploadRequest
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import software.amazon.awssdk.services.s3.model.UploadPartRequest
import software.amazon.awssdk.services.s3.presigner.S3Presigner
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest
import software.amazon.awssdk.services.ses.SesClient
import java.io.File
import java.io.FileInputStream
import java.nio.Buffer
import java.time.Duration
import java.util.UUID
import java.util.concurrent.TimeUnit

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
    /*val s3Client: S3Client = S3Client.builder()
        .region(AWS_REGION)
        .credentialsProvider(
            StaticCredentialsProvider.create(
                AwsBasicCredentials.create(AWS_ACCESS_KEY, AWS_SECRET_KEY)
            )
        )
        .httpClient(UrlConnectionHttpClient.create()) // Required for Android
        .build()*/


  /*  val amazonS3Client: AmazonS3 = AmazonS3Client(
        BasicAWSCredentials(AWS_ACCESS_KEY, AWS_SECRET_KEY)
    ).apply {
        setRegion(com.amazonaws.regions.Region.getRegion("us-east-1"))
    }
    val transferUtility: TransferUtility = TransferUtility.builder()
        .context(RFIDApplication.instance)
        .s3Client(amazonS3Client)  // Reuse your existing S3 client setup
        .build()*/


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

    /*fun uploadMediaToS3(
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
                        2 * 1024 * 1024
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
                    val videoKey = "videos/${UUID.randomUUID()}_${System.currentTimeMillis()}.$videoExtension"

                    videoUrl = multipartUpload(
                        s3Client,
                        videoFile,
                        videoKey,
                        2 * 1024 * 1024
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
                    Toast.makeText(context, "Upload Failed: ${e.message}", Toast.LENGTH_SHORT).show()
                    onError(e.message ?: "Unknown error")
                }
            }
        }
    }*/

    /*fun uploadMediaToS3(
        scope: CoroutineScope,
        context: Context,
        imageFiles: List<File>,
        videoFile: File?,
        onSuccess: (List<String>, String?) -> Unit,
        onError: (String) -> Unit
    ) {
        val transferUtility = TransferUtility.builder()
            .context(context)
            .s3Client(AwsManager.amazonS3Client) // Must be AmazonS3, not S3Client
            .defaultBucket(AwsManager.BUCKET_NAME)
            .build()

        scope.launch(Dispatchers.IO) {
            var progressDialog: ProgressDialog? = null
            try {
                withContext(Dispatchers.Main) {
                    Log.d("S3Upload", "Starting upload, showing progress dialog")
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

                for ((index, imageFile) in imageFiles.withIndex()) {
                    val imageKey = "images/${UUID.randomUUID()}_${imageFile.name}"
                    Log.d("S3Upload", "Uploading image ${index + 1}: $imageKey")
                    Log.d("S3Upload", "File size: ${imageFile.length() / 1024} KB")

                    val uploadObserver = transferUtility.upload(
                        AwsManager.BUCKET_NAME,
                        imageKey,
                        imageFile
                    )

                    val uploadComplete = CompletableDeferred<Unit>()

                    uploadObserver.setTransferListener(object : TransferListener {
                        override fun onStateChanged(id: Int, state: TransferState) {
                            Log.d("S3Upload", "Image upload state changed: $state")
                            if (state == TransferState.COMPLETED) {
                                val url = "https://${AwsManager.BUCKET_NAME}.s3.amazonaws.com/$imageKey"
                                uploadedImageUrls.add(url)
                                Log.d("S3Upload", "Image uploaded successfully: $url")
                                uploadComplete.complete(Unit)

                            } else if (state == TransferState.FAILED) {
                                Log.e("S3Upload", "Image upload failed for: $imageKey")
                                uploadComplete.completeExceptionally(Exception("Upload failed"))
                            }
                        }

                        override fun onProgressChanged(id: Int, bytesCurrent: Long, bytesTotal: Long) {
                            val percentage = if (bytesTotal > 0) (bytesCurrent * 100 / bytesTotal).toInt() else 0
                            Log.d("S3Upload", "Image progress: $bytesCurrent / $bytesTotal")
                            Log.d("S3Upload", "Image upload progress: $percentage%")
                            scope.launch(Dispatchers.Main) {
                                progressDialog?.progress = percentage
                                progressDialog?.setMessage("Uploading Image ${index + 1}/${imageFiles.size}... $percentage%")
                            }
                        }

                        override fun onError(id: Int, ex: Exception) {
                            Log.e("S3Upload", "Image upload error: ${ex.message}", ex)
                            uploadComplete.completeExceptionally(ex)
                        }
                    })

                    // Wait until this upload is done before continuing
                    uploadComplete.await()
                }

                var videoUrl: String? = null
                if (videoFile != null) {
                    val videoKey = "videos/${UUID.randomUUID()}_${videoFile.name}"
                    Log.d("S3Upload", "Uploading video: $videoKey")

                    val videoUploadObserver = transferUtility.upload(
                        AwsManager.BUCKET_NAME,
                        videoKey,
                        videoFile
                    )

                    val videoUploadComplete = CompletableDeferred<Unit>()

                    videoUploadObserver.setTransferListener(object : TransferListener {
                        override fun onStateChanged(id: Int, state: TransferState) {
                            Log.d("S3Upload", "Video upload state changed: $state")
                            if (state == TransferState.COMPLETED) {
                                videoUrl = "https://${AwsManager.BUCKET_NAME}.s3.amazonaws.com/$videoKey"
                                Log.d("S3Upload", "Video uploaded successfully: $videoUrl")
                                videoUploadComplete.complete(Unit)
                            } else if (state == TransferState.FAILED) {
                                Log.e("S3Upload", "Video upload failed for: $videoKey")
                                videoUploadComplete.completeExceptionally(Exception("Video upload failed"))
                            }
                        }

                        override fun onProgressChanged(id: Int, bytesCurrent: Long, bytesTotal: Long) {
                            val percentage = if (bytesTotal > 0) (bytesCurrent * 100 / bytesTotal).toInt() else 0
                            Log.d("S3Upload", "Video upload progress: $percentage%")
                            scope.launch(Dispatchers.Main) {
                                progressDialog?.progress = percentage
                                progressDialog?.setMessage("Uploading Video... $percentage%")
                            }
                        }

                        override fun onError(id: Int, ex: Exception) {
                            Log.e("S3Upload", "Video upload error: ${ex.message}", ex)
                            videoUploadComplete.completeExceptionally(ex)
                        }
                    })

                    videoUploadComplete.await()
                }

                withContext(Dispatchers.Main) {
                    Log.d("S3Upload", "All uploads completed successfully")
                    progressDialog?.dismiss()
                    onSuccess(uploadedImageUrls, videoUrl)
                }

            } catch (e: Exception) {
                Log.e("S3Upload", "Upload failed: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    progressDialog?.dismiss()
                    Toast.makeText(context, "Upload failed: ${e.message}", Toast.LENGTH_SHORT).show()
                    onError(e.message ?: "Unknown error")
                }
            }
        }
    }*/

    fun uploadFileToS3ViaPresignedUrl(
        file: File,
        contentType: String,
        region: String,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        val objectKey = "uploads/${UUID.randomUUID()}_${file.name}"
        Log.d("S3_UPLOAD", "Preparing to upload file: ${file.name}")
        Log.d("S3_UPLOAD", "File size: ${file.length() / (1024 * 1024)} MB")
        Log.d("S3_UPLOAD", "Generating pre-signed URL for: $objectKey")

        val presignedUrl = generatePresignedUrl(
            objectKey = objectKey,
            region = region,
            contentType = contentType
        )
        Log.d("S3_UPLOAD", "Pre-signed URL generated:\n$presignedUrl")

        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(2, TimeUnit.MINUTES) // connection timeout
            .writeTimeout(5, TimeUnit.MINUTES)   // time allowed for writing data
            .readTimeout(5, TimeUnit.MINUTES)    // time allowed for reading server response
            .build()

        val requestFile = file.asRequestBody(contentType.toMediaTypeOrNull())
        val retrofit = Retrofit.Builder()
            .baseUrl("https://s3.amazonaws.com/") // baseUrl is required but will be overridden
            .client(okHttpClient)
            .build()

        val service = retrofit.create(S3UploadService::class.java)
        val call = service.uploadFile(presignedUrl, requestFile, contentType)

        call.enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                if (response.isSuccessful) {
                    val finalUrl = "https://$BUCKET_NAME.s3.amazonaws.com/$objectKey"
                    Log.d("S3_UPLOAD", "✅ Upload succeeded in  seconds")
                    onSuccess(finalUrl)
                } else {
                    Log.e("S3_UPLOAD", "❌ Upload failed with code ${response.code()} in  seconds")
                    onError("Upload failed: HTTP ${response.code()}")
                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                Log.e("S3_UPLOAD", "❌ Upload error: ${t.localizedMessage}", t)
                onError("Upload error: ${t.localizedMessage}")
            }
        })
    }



    fun generatePresignedUrl(
        objectKey: String,
        region: String,
        contentType: String
    ): String {
        Log.d("S3PreSignLogger", "Generating pre-signed URL for objectKey: $objectKey, region: $region, contentType: $contentType")
        val credentials = AwsBasicCredentials.create(AWS_ACCESS_KEY, AWS_SECRET_KEY)

        val presigner = S3Presigner.builder()
            .region(Region.of(region))
            .credentialsProvider { credentials }
            .build()

        val putObjectRequest = PutObjectRequest.builder()
            .bucket(BUCKET_NAME)
            .key(objectKey)
            .contentType(contentType)
            .build()
        Log.d("S3PreSignLogger", "Created PutObjectRequest: $putObjectRequest")

        val presignRequest = PutObjectPresignRequest.builder()
            .putObjectRequest(putObjectRequest)
            .signatureDuration(Duration.ofMinutes(15))
            .build()

        val presignedRequest = presigner.presignPutObject(presignRequest)
        presigner.close()

        // Log the final URL
        val presignedUrl = presignedRequest.url().toString()
        Log.d("S3PreSignLogger", "Generated pre-signed URL: $presignedUrl")

        return presignedRequest.url().toString()
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
                        AttributeDefinition.builder().attributeName(primaryKey).attributeType(ScalarAttributeType.S).build()
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
                            AttributeDefinition.builder().attributeName("tagId").attributeType(ScalarAttributeType.S).build()
                        )

                        createTableBuilder.globalSecondaryIndexes(
                            GlobalSecondaryIndex.builder()
                                .indexName("tagId-index")
                                .keySchema(
                                    KeySchemaElement.builder().attributeName("tagId").keyType(KeyType.HASH).build()
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


                val putItemRequest = PutItemRequest.builder()
                    .tableName(tableName)
                    .item(product.toMap()) // Use the extension function
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

    suspend fun checkIfTagIdExists(tableName: String, tagId: String): Pair<Boolean, String> {
        return withContext(Dispatchers.IO) {
            try {
                val queryRequest = QueryRequest.builder()
                    .tableName(tableName)
                    .indexName("tagId-index") // 🔥 Must match your GSI
                    .keyConditionExpression("tagId = :tagVal")
                    .expressionAttributeValues(mapOf(
                        ":tagVal" to AttributeValue.builder().s(tagId).build()
                    ))
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

}
