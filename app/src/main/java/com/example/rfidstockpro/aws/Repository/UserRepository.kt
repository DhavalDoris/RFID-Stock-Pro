package com.example.rfidstockpro.aws.repository

import android.util.Log
import com.example.rfidstockpro.aws.AwsManager
import com.example.rfidstockpro.aws.models.UserModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import software.amazon.awssdk.services.dynamodb.model.*

class UserRepository {

    private val tableName = AwsManager.USER_TABLE

    // ✅ Create User (Insert into DynamoDB)
    suspend fun createUser(user: UserModel): String = withContext(Dispatchers.IO) {
        try {
            val itemValues = UserModel.toMap(user)

            val request = PutItemRequest.builder()
                .tableName(tableName)
                .item(itemValues)
                .build()

            AwsManager.dynamoDBClient.putItem(request)
            "User created successfully"
        } catch (e: Exception) {
            "Error: ${e.localizedMessage}"
        }
    }

    // ✅ Read User (Fetch from DynamoDB)
    suspend fun getUser(email: String): UserModel? = withContext(Dispatchers.IO) {
        try {
            val request = GetItemRequest.builder()
                .tableName(tableName)
                .key(mapOf("email" to AttributeValue.builder().s(email).build()))
                .build()

            val response = AwsManager.dynamoDBClient.getItem(request)

            response.item()?.let { item ->
                UserModel.fromMap(item) // Convert from Map to UserModel
            }
        } catch (e: Exception) {
            Log.e("AWS_TAG", "Error fetching user: ${e.localizedMessage}", e)
            null
        }
    }

    // ✅ Read User (Callback Version)
    fun getUser(email: String, callback: (UserModel?) -> Unit) {
        Thread {
            try {
                val request = GetItemRequest.builder()
                    .tableName(tableName)
                    .key(mapOf("email" to AttributeValue.builder().s(email).build()))
                    .build()

                val response = AwsManager.dynamoDBClient.getItem(request)

                val user = response.item()?.let { UserModel.fromMap(it) }
                callback(user)
            } catch (e: Exception) {
                Log.e("AWS_TAG", "Error fetching user: ${e.localizedMessage}", e)
                callback(null)
            }
        }.start()
    }

    // ✅ Update User (Using SDK v2)
    suspend fun updateUser(user: UserModel): String = withContext(Dispatchers.IO) {
        try {
            val attributeUpdates = UserModel.toMap(user).mapValues { (_, value) ->
                AttributeValueUpdate.builder()
                    .value(value)
                    .action(AttributeAction.PUT)
                    .build()
            }

            val request = UpdateItemRequest.builder()
                .tableName(tableName)
                .key(mapOf("email" to AttributeValue.builder().s(user.email).build()))
                .attributeUpdates(attributeUpdates)
                .build()

            AwsManager.dynamoDBClient.updateItem(request)
            "User updated successfully"
        } catch (e: Exception) {
            "Error: ${e.localizedMessage}"
        }
    }

    // ✅ Delete User
    suspend fun deleteUser(email: String): String = withContext(Dispatchers.IO) {
        try {
            val request = DeleteItemRequest.builder()
                .tableName(tableName)
                .key(mapOf("email" to AttributeValue.builder().s(email).build()))
                .build()

            AwsManager.dynamoDBClient.deleteItem(request)
            "User deleted successfully"
        } catch (e: Exception) {
            "Error: ${e.localizedMessage}"
        }
    }
}
