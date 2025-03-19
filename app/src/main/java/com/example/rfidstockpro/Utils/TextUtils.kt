package com.example.rfidstockpro.Utils

import android.content.Context
import android.graphics.Color
import android.text.SpannableString
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.text.style.ForegroundColorSpan
import android.text.style.UnderlineSpan
import android.widget.TextView

object TextUtils {

    fun applyUnderlineAndColor(context: Context, textView: TextView) {
        val text = "By signing in with an account, you agree to SO's \n Terms of Service and Privacy Policy."
        val spannableString = SpannableString(text)

        val termsStart = text.indexOf("Terms of Service")
        val termsEnd = termsStart + "Terms of Service".length

        val privacyStart = text.indexOf("Privacy Policy")
        val privacyEnd = privacyStart + "Privacy Policy".length

        // Set entire text color to Gray first
        spannableString.setSpan(ForegroundColorSpan(Color.GRAY), 0, text.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

        // Underline and set color to Black for "Terms of Service"
        spannableString.setSpan(UnderlineSpan(), termsStart, termsEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        spannableString.setSpan(ForegroundColorSpan(Color.BLACK), termsStart, termsEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

        // Underline and set color to Black for "Privacy Policy"
        spannableString.setSpan(UnderlineSpan(), privacyStart, privacyEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        spannableString.setSpan(ForegroundColorSpan(Color.BLACK), privacyStart, privacyEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

        textView.text = spannableString
        textView.movementMethod = LinkMovementMethod.getInstance() // Enables clickable links if needed
    }
}
