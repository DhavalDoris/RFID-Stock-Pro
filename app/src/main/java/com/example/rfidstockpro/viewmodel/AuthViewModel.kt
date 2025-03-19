package com.example.rfidstockpro.viewmodel

import android.util.Patterns
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import java.util.regex.Pattern

class AuthViewModel : ViewModel() {

    private val _emailError = MutableLiveData<String?>()
    val emailError: LiveData<String?> = _emailError

    private val _passwordError = MutableLiveData<String?>()
    val passwordError: LiveData<String?> = _passwordError

    private val _confirmPasswordError = MutableLiveData<String?>()
    val confirmPasswordError: LiveData<String?> = _confirmPasswordError


    private val _signupError = MutableLiveData<String>()
    val signupError: LiveData<String> get() = _signupError


    fun validateLogin(email: String, password: String): Boolean {
        /*  val isEmailValid = validateEmail(email)
          val isPasswordValid = validatePassword(password)
          return isEmailValid && isPasswordValid*/

        if (email.isEmpty()) {
            _emailError.postValue("Email is required")
            return false
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            _emailError.postValue("Enter a valid email address")
            return false
        }

        if (password.isEmpty()) {
            _passwordError.postValue("Password is required")
            return false
        }
        return true
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
}
