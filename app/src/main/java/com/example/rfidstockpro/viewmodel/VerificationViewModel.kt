package com.example.rfidstockpro.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class VerificationViewModel : ViewModel() {
    private val _otpError = MutableLiveData<String>()
    val otpError: LiveData<String> get() = _otpError

    fun verifyOtp(otp: String) {
        if (otp.length < 4) {
            _otpError.postValue("Please enter a valid OTP")
        } else {
            _otpError.postValue("OTP Verified Successfully!")
        }
    }
}
