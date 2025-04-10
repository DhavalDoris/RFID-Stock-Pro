package com.example.rfidstockpro.viewmodel

import android.content.Context
import android.os.Build
import android.util.Log
import android.util.Patterns
import androidx.annotation.RequiresApi
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.rfidstockpro.RFIDApplication.Companion.ENCRYPTION_KEY
import com.example.rfidstockpro.RFIDApplication.Companion.USER_TABLE
import com.example.rfidstockpro.aws.AwsManager
import com.example.rfidstockpro.aws.models.UserModel
import com.example.rfidstockpro.aws.repository.UserRepository
import com.example.rfidstockpro.encryption.AESUtils
import com.example.rfidstockpro.sharedpref.SessionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.regex.Pattern

class AuthViewModel : ViewModel() {

    private val repository = UserRepository()

    private val _operationResult = MutableLiveData<String>()
    val operationResult: LiveData<String> get() = _operationResult

    private val _userData = MutableLiveData<UserModel?>()
    val userData: LiveData<UserModel?> get() = _userData


    private val _emailError = MutableLiveData<String?>()
    val emailError: LiveData<String?> = _emailError

    private val _passwordError = MutableLiveData<String?>()
    val passwordError: LiveData<String?> = _passwordError

    private val _confirmPasswordError = MutableLiveData<String?>()
    val confirmPasswordError: LiveData<String?> = _confirmPasswordError

    private val _signupError = MutableLiveData<String>()
    val signupError: LiveData<String> get() = _signupError

    private val _loginResult = MutableLiveData<String>()
    val loginResult: LiveData<String> get() = _loginResult

    private val _userEmail = MutableLiveData<String>()
    val userEmail: LiveData<String> get() = _userEmail

    fun setUserEmail(email: String) {
        _userEmail.value = email
    }

    /** ✅ Validate Login Inputs */
    fun validateLogin(email: String, password: String): Boolean {
        if (email.isEmpty()) {
            _emailError.postValue("Email is required")
            return false
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            _emailError.postValue("Enter a valid email address")
            return false
        }
        if (password.isEmpty()) {
            _passwordError.postValue("Password is required")
            return false
        }
        return true
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun loginUser(email: String, password: String, context: Context) {
        val sessionManager = SessionManager.getInstance(context) // Get Singleton Instance

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val user = AwsManager.getUserByEmail(USER_TABLE, email)
                withContext(Dispatchers.Main) {
                    if (user == null) {
                        _loginResult.value = "User not found. Please sign up."
                    } else {
                        try {

                            val decryptedPassword = AESUtils.decrypt(user.password, ENCRYPTION_KEY)
                            Log.e("AWS_PASS", "loginUser Password: " + user.password )

                            if (decryptedPassword == password) {
                                if (user.status == "active") {
                                    // Store user data in SharedPreferences
//                                    saveUserDataToPreferences(context, user.userName, user.role)
                                    sessionManager.saveUserData(user.userName, user.email, user.role) // Save session

                                    _loginResult.value = "Login successful"
                                } else {
                                    _loginResult.value = "Your account is not active"
                                }
                            } else {
                                _loginResult.value = "Invalid password. Try again."
                            }
                        } catch (e: Exception) {
                            _loginResult.value = "Error decrypting password"
                            Log.e("AWS_PASS", "Error loginUser Password 1: " + user.password )
                            Log.e("AWS_PASS", "Error loginUser Password 2: " + e.message )
                        }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _loginResult.value = "Login error: ${e.message}"
                }
            }
        }
    }




    fun validateSignup(
        username: String, companyName: String, email: String, contactNumber: String,
        password: String, confirmPassword: String
    ): Boolean {
        return when {
            username.isEmpty() -> {
                _signupError.postValue("Username is required")
                false
            }

            companyName.isEmpty() -> {
                _signupError.postValue("Company Name is required")
                false
            }

            email.isEmpty() -> {
                _signupError.postValue("Email Address is required")
                false
            }

            !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                _signupError.postValue("Enter a valid Email Address")
                false
            }

            contactNumber.isEmpty() -> {
                _signupError.postValue("Contact Number is required")
                false
            }

            contactNumber.length != 10 -> {
                _signupError.postValue("Enter a valid 10-digit Contact Number")
                false
            }

            password.isEmpty() -> {
                _signupError.postValue("Password is required")
                false
            }

            password.length < 6 -> {
                _signupError.postValue("Password must be at least 6 characters")
                false
            }

            confirmPassword.isEmpty() -> {
                _signupError.postValue("Confirm Password is required")
                false
            }

            confirmPassword != password -> {
                _signupError.postValue("Passwords do not match")
                false
            }

            else -> true
        }
    }


    private fun validateEmail(email: String): Boolean {
        return if (email.isEmpty()) {
            _emailError.value = "Email is required"
            false
        } else if (!Pattern.compile("^[A-Za-z0-9+_.-]+@(.+)$").matcher(email).matches()) {
            _emailError.value = "Invalid email format"
            false
        } else {
            _emailError.value = null
            true
        }
    }

    private fun validatePassword(password: String): Boolean {
        return if (password.isEmpty()) {
            _passwordError.value = "Password is required"
            false
        } else if (password.length < 6) {
            _passwordError.value = "Password must be at least 6 characters"
            false
        } else {
            _passwordError.value = null
            true
        }
    }

    private fun validateConfirmPassword(password: String, confirmPassword: String): Boolean {
        return if (confirmPassword.isEmpty()) {
            _confirmPasswordError.value = "Confirm Password is required"
            false
        } else if (confirmPassword != password) {
            _confirmPasswordError.value = "Passwords do not match"
            false
        } else {
            _confirmPasswordError.value = null
            true
        }
    }

    // Create User
    fun createUser(user: UserModel, tableName: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val existingUser = AwsManager.getUserByEmail(tableName,user.email)
                if (existingUser == null) {
                    val success = AwsManager.saveUser(tableName,user)
                    if (success) {
                        _operationResult.postValue("User created successfully")
                    } else {
                        _operationResult.postValue("Error creating user")
                    }
                } else {
                    _operationResult.postValue("Email already exists")
                }
            } catch (e: Exception) {
                _operationResult.postValue("Error: ${e.message}")
            }
        }
    }




    fun deleteUserByEmail(email: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val success = AwsManager.deleteUser(email)
                if (success) {
                    _operationResult.postValue("User deleted successfully")
                } else {
                    _operationResult.postValue("User not found")
                }
            } catch (e: Exception) {
                _operationResult.postValue("Error: ${e.message}")
            }
        }
    }


    fun readUserData(email: String) {
        CoroutineScope(Dispatchers.IO).launch {
            val user = AwsManager.getUserByEmail(USER_TABLE, email)
            withContext(Dispatchers.Main) {
                if (user != null) {
                    _userData.postValue(user)
                } else {
                    _operationResult.postValue("User not found")
                }
            }
        }
    }

    fun updateUserData(tableName: String,user: UserModel) {
        CoroutineScope(Dispatchers.IO).launch {
            /*val resultMessage = AwsManager.saveUser(user) // Get success/error message
            withContext(Dispatchers.Main) {
                _operationResult.postValue(resultMessage) // Post exact result message
            }*/

            val success = AwsManager.saveUser(tableName,user) // Save updated user
            withContext(Dispatchers.Main) {
                if (success) {
                    _operationResult.postValue("User updated successfully")
                } else {
                    _operationResult.postValue("Error updating user")
                }
            }
        }
    }


    fun deleteUserData(email: String) {
        CoroutineScope(Dispatchers.IO).launch {
            val success = AwsManager.deleteUser(email)
            withContext(Dispatchers.Main) {
                if (success) {
                    _operationResult.postValue("User deleted successfully")
                } else {
                    _operationResult.postValue("User not found")
                }
            }
        }
    }

    // Read User
    fun getUser(userId: String) {
        viewModelScope.launch {
//            val user = repository.getUser(userId)
//            _userData.postValue(user)
        }
    }

    // Update User
    fun updateUser(tableName:String,user: UserModel) {
        viewModelScope.launch {
            val result = repository.updateUser(tableName,user)
            _operationResult.postValue(result)
        }
    }



}
