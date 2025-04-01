package com.example.rfidstockpro.Utils

import android.content.Context
import android.graphics.Color
import android.text.SpannableString
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.text.style.ForegroundColorSpan
import android.text.style.UnderlineSpan
import android.widget.TextView
import com.example.rfidstockpro.R

object TextUtils {

    fun applyUnderlineAndColor(context: Context, textView: TextView) {
        val text =
            context.getString(R.string.by_signing_in_with_an_account_you_agree_to_so_s_terms_of_service_and_privacy_policy)
        val spannableString = SpannableString(text)

        val termsStart = text.indexOf(context.getString(R.string.terms_of_service))
        val termsEnd = termsStart + "Terms of Service".length

        val privacyStart = text.indexOf(context.getString(R.string.privacy_policy))
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
