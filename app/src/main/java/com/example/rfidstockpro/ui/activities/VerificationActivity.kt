package com.example.rfidstockpro.ui.activities

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.Spannable
import android.text.SpannableString
import android.text.Spanned
import android.text.TextPaint
import android.text.TextWatcher
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.text.style.ForegroundColorSpan
import android.util.Log
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import com.example.rfidstockpro.R
import com.example.rfidstockpro.Utils.StatusBarUtils
import com.example.rfidstockpro.databinding.ActivityVerificationBinding
import com.example.rfidstockpro.sharedpref.SessionManager
import com.example.rfidstockpro.viewmodel.VerificationViewModel
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class VerificationActivity : AppCompatActivity() {

    private lateinit var binding: ActivityVerificationBinding
    private val verificationViewModel: VerificationViewModel by viewModels()
    var comesFrom: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityVerificationBinding.inflate(layoutInflater)
        setContentView(binding.root)
//        StatusBarUtils.setStatusBarColor(this)
        setupOtpInputs()
        setupListeners()
        InitView()
    }

    private fun InitView() {
        comesFrom = intent.getStringExtra("comeFrom").toString()

        val sessionManager = SessionManager.getInstance(this) // Get Singleton Instance
        val email = sessionManager.getEmail()
        updateEmailText(email!!)

        verificationViewModel.resendOtpText.observe(this) { text ->
            CoroutineScope(Dispatchers.Main).launch {
                Log.e("OTP_TAG", "observe: " + text )
                binding.tvResendOtp.text = text
            }
        }

        verificationViewModel.resetResendText(this) // Set initial text

        // Observe enable/disable state
        verificationViewModel.isResendEnabled.observe(this, Observer { isEnabled ->
            binding.tvResendOtp.isClickable = isEnabled
            binding.tvResendOtp.setTextColor(
                if (isEnabled) ContextCompat.getColor(this, R.color.appMainColor) else Color.BLACK
            )
        })

        // Initialize Click Listener
        binding.tvResendOtp.setOnClickListener {
            Log.e("OTP_TAG", "Click: "  )
            if (verificationViewModel.isResendEnabled.value == true) {
                verificationViewModel.startResendTimer(this)
            }
        }
    }

    private fun setupOtpInputs() {
        val otpFields = listOf(binding.etOtp1, binding.etOtp2, binding.etOtp3, binding.etOtp4)

        for (i in otpFields.indices) {
            otpFields[i].addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int
                ) {
                }

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    if (s?.length == 1 && i < otpFields.size - 1) {
                        otpFields[i + 1].requestFocus()
                    }
                }

                override fun afterTextChanged(s: Editable?) {}
            })
        }
    }

    private fun setupListeners() {

        verificationViewModel.otpError.observe(this) { message ->
            if (message == getString(R.string.otp_verified_successfully)) {
                // Proceed to next screen on successful OTP verification
                if (comesFrom == "Login") {
                    startActivity(Intent(this, DashboardActivity::class.java))
                    finish()
                } else {
                    Toast.makeText(
                        this,
                        getString(R.string.account_created_login_now), Toast.LENGTH_SHORT
                    ).show()
                    startActivity(Intent(this, AuthActivity::class.java))
                    finish()
                }

            } else {
                // Show error message
                Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT).show()
            }
        }

        binding.btnConfirm.setOnClickListener {
            val otp =
                "${binding.etOtp1.text}${binding.etOtp2.text}${binding.etOtp3.text}${binding.etOtp4.text}"
            verificationViewModel.verifyOtp(otp)
        }

    }


    private fun updateEmailText(email: String) {
        val fullText = getString(R.string.we_sent_a_code_to, email)
        val spannable = SpannableString(fullText)

        val startIndex = fullText.indexOf(email)
        val endIndex = startIndex + email.length

        // Set email in BLACK color
        spannable.setSpan(
            ForegroundColorSpan(Color.BLACK), // Black color
            startIndex,
            endIndex,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        // Set default text color to gray (optional)
        spannable.setSpan(
            ForegroundColorSpan(Color.GRAY), // Gray color
            0,
            startIndex,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        binding.tvEmail.text = spannable
    }
}
