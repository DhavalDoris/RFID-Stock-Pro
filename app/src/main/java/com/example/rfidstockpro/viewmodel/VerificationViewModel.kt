package com.example.rfidstockpro.viewmodel

import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class VerificationViewModel : ViewModel() {


    private val _resendOtpText = MutableLiveData<String>("RESEND OTP")
    val resendOtpText: LiveData<String> get() = _resendOtpText

    private val _isResendEnabled = MutableLiveData<Boolean>(true)
    val isResendEnabled: LiveData<Boolean> get() = _isResendEnabled

    private var countDownTimer: CountDownTimer? = null
    private val RESEND_DELAY: Long = 5 * 60 * 1000 // 5 minutes in milliseconds

    private val _otpError = MutableLiveData<String>()
    val otpError: LiveData<String> get() = _otpError

    fun verifyOtp(otp: String) {
        if (otp.length < 4) {
            _otpError.postValue("Please enter a valid OTP")
        } else {
            _otpError.postValue("OTP Verified Successfully!")
        }
    }


    fun startResendTimer() {
        _isResendEnabled.value = false // Disable click
        countDownTimer?.cancel() // Cancel any running timer

        countDownTimer = object : CountDownTimer(RESEND_DELAY, 1000) {
            override fun onTick(millisUntilFinished: Long) {

                val minutes = (millisUntilFinished / 1000) / 60
                val seconds = (millisUntilFinished / 1000) % 60

                val formattedTime = if (minutes > 0) {
                    "Resend OTP in ${minutes}m ${seconds}s"
                } else {
                    "Resend OTP in ${seconds}s"
                }

                _resendOtpText.value = formattedTime
                Log.e("OTP_TAG", "onTick: $formattedTime")
            }

            override fun onFinish() {
                _resendOtpText.value = "RESEND OTP"
                _isResendEnabled.value = true

            }
        }.start()
    }
}
