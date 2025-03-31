package com.example.rfidstockpro.ui.activities

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.Spannable
import android.text.SpannableString
import android.text.TextPaint
import android.text.TextWatcher
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.text.style.ForegroundColorSpan
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.rfidstockpro.R
import com.example.rfidstockpro.Utils.StatusBarUtils
import com.example.rfidstockpro.databinding.ActivityVerificationBinding
import com.example.rfidstockpro.viewmodel.VerificationViewModel
import com.google.android.material.snackbar.Snackbar

class VerificationActivity : AppCompatActivity() {

    private lateinit var binding: ActivityVerificationBinding
    private val verificationViewModel: VerificationViewModel by viewModels()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityVerificationBinding.inflate(layoutInflater)
        setContentView(binding.root)


        StatusBarUtils.setStatusBarColor(this)
        setupOtpInputs()
        setupListeners()
        applySpannableText()
        Init()
    }

    private fun Init() {
        val email = intent.getStringExtra("email")
        updateEmailText(email!!)
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
            if (message == "OTP Verified Successfully!") {
                // Proceed to next screen on successful OTP verification
                Toast.makeText(this, "Account Created - Login Now", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, AuthActivity::class.java))
                finish()

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

    private fun applySpannableText() {
        val fullText = "Don't receive the OTP? RESEND OTP"
        val spannable = SpannableString(fullText)

        // Find the index of "RESEND OTP"
        val startIndex = fullText.indexOf("RESEND OTP")
        val endIndex = startIndex + "RESEND OTP".length

        val blueColor = ContextCompat.getColor(
            this,
            R.color.appMainColor
        ) // Ensure R.color.blue is defined in colors.xml

        // Apply blue color to "RESEND OTP"
        spannable.setSpan(
            ForegroundColorSpan(blueColor),
            startIndex,
            endIndex,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )


        // Make "RESEND OTP" clickable
        val clickableSpan = object : ClickableSpan() {
            override fun onClick(widget: View) {
                // Handle the resend OTP click event
                Toast.makeText(this@VerificationActivity, "Resend OTP clicked", Toast.LENGTH_SHORT)
                    .show()
                // Add your logic here to resend OTP
            }

            override fun updateDrawState(ds: TextPaint) {
                super.updateDrawState(ds)
                ds.isUnderlineText = false // Remove underline
                ds.color = blueColor // Ensure text color remains blue
            }
        }

        spannable.setSpan(
            clickableSpan,
            startIndex,
            endIndex,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        // Set the formatted text to the TextView
        binding.tvResendOtp.text = spannable
        // Enable clickable spans in TextView
        binding.tvResendOtp.movementMethod = LinkMovementMethod.getInstance()
        binding.tvResendOtp.highlightColor = Color.TRANSPARENT // Remove highlight when clicked
    }


    private fun updateEmailText(email: String) {
        val fullText = "We sent a code to $email"
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
