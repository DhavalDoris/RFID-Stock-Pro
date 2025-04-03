package com.example.rfidstockpro.viewmodel

import android.content.Context
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.rfidstockpro.R

class VerificationViewModel : ViewModel() {


    private val _resendOtpText = MutableLiveData<String>("Don't receive the OTP? RESEND OTP")
    val resendOtpText: LiveData<String> get() = _resendOtpText

    private val _isResendEnabled = MutableLiveData<Boolean>(true)
    val isResendEnabled: LiveData<Boolean> get() = _isResendEnabled

    private var countDownTimer: CountDownTimer? = null
    private val RESEND_DELAY: Long = 1 * 60 * 1000 // 5 minutes in milliseconds

    private val _otpError = MutableLiveData<String>()
    val otpError: LiveData<String> get() = _otpError

    fun resetResendText(context: Context) {
        _resendOtpText.value = getFormattedText("Don't receive the OTP? RESEND OTP", context).toString()
    }

    fun verifyOtp(otp: String) {
        if (otp.length < 4) {
            _otpError.postValue("Please enter a valid OTP")
        } else {
            _otpError.postValue("OTP Verified Successfully!")
        }
    }


    fun startResendTimer(context: Context) {
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
                _resendOtpText.postValue(getFormattedText(formattedTime, context).toString()) // Update UI
//                Log.e("OTP_TAG", "onTick: $formattedTime")
            }

            override fun onFinish() {

                _resendOtpText.postValue("Don't receive the OTP? RESEND OTP")
                _isResendEnabled.postValue(true)

            }
        }.start()
    }

    private fun getFormattedText(fullText: String, context: Context): SpannableString {
        val spannable = SpannableString(fullText)

        val blueColor = ContextCompat.getColor(context, R.color.appMainColor) // Ensure blue is defined
        val blackColor = ContextCompat.getColor(context, R.color.black) // Ensure black is defined

        val startIndex = fullText.indexOf("RESEND OTP")

        if (startIndex > 0) {
            Log.e("OTP_TAG", "IF: "  )
            // Set "Don't receive the OTP?" in black
            spannable.setSpan(ForegroundColorSpan(blackColor), 0, startIndex, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)

            // Set "RESEND OTP" in blue
            spannable.setSpan(ForegroundColorSpan(blueColor), startIndex, startIndex + "RESEND OTP".length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        } else {
            Log.e("OTP_TAG", "ELSE: "  )
            // If "RESEND OTP" is not found, just return the text as is
            spannable.setSpan(ForegroundColorSpan(blackColor), 0, fullText.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        }

        return spannable
    }
}
