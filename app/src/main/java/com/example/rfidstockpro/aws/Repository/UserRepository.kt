package com.example.rfidstockpro.aws.Repository


import android.util.Log
import com.example.rfidstockpro.aws.AwsManager
import com.example.rfidstockpro.aws.models.UserModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class UserRepository {

    // Create User (Insert into DynamoDB)
    suspend fun createUser(user: UserModel) = withContext(Dispatchers.IO) {
        try {
            AwsManager.dynamoDBMapper.save(user)
            "User created successfully"
        } catch (e: Exception) {
            "Error: ${e.localizedMessage}"
        }
    }

    // Read User (Fetch from DynamoDB)
    suspend fun getUser(userId: String): UserModel? = withContext(Dispatchers.IO) {
        try {
            AwsManager.dynamoDBMapper.load(UserModel::class.java, userId)
        } catch (e: Exception) {
            null
        }
    }

    fun getUser(email: String, callback: (UserModel?) -> Unit) {
        Thread {
            try {
                val user = AwsManager.dynamoDBMapper.load(UserModel::class.java, email)
                callback(user)
            } catch (e: Exception) {
                Log.e("AWS", "Error fetching user: ${e.message}")
                callback(null)
            }
        }.start()
    }

    // Update User
    suspend fun updateUser(user: UserModel) = withContext(Dispatchers.IO) {
        try {
            AwsManager.dynamoDBMapper.save(user)
            "User updated successfully"
        } catch (e: Exception) {
            "Error: ${e.localizedMessage}"
        }
    }

    // Delete User
    suspend fun deleteUser(user: UserModel) = withContext(Dispatchers.IO) {
        try {
            AwsManager.dynamoDBMapper.delete(user)
            "User deleted successfully"
        } catch (e: Exception) {
            "Error: ${e.localizedMessage}"
        }
    }
}
