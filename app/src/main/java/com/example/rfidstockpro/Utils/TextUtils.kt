package com.example.rfidstockpro.Utils

import android.content.Context
import android.graphics.Color
import android.text.SpannableString
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.text.style.ForegroundColorSpan
import android.text.style.UnderlineSpan
import android.view.View
import android.widget.TextView
import com.example.rfidstockpro.R

object TextUtils {

    fun applyUnderlineAndColor(
        context: Context,
        textView: TextView,
        onTermsClick: () -> Unit,
        onPrivacyClick: () -> Unit
    ) {
        val text = context.getString(R.string.by_signing_in_with_an_account_you_agree_to_so_s_terms_of_service_and_privacy_policy)
        val spannableString = SpannableString(text)

        val termsText = context.getString(R.string.terms_of_service)
        val privacyText = context.getString(R.string.privacy_policy)

        val termsStart = text.indexOf(termsText)
        val termsEnd = termsStart + termsText.length

        val privacyStart = text.indexOf(privacyText)
        val privacyEnd = privacyStart + privacyText.length

        // Set entire text color to Gray first
        spannableString.setSpan(
            ForegroundColorSpan(Color.GRAY),
            0,
            text.length,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        // ClickableSpan for "Terms of Service"
        val termsClickableSpan = object : ClickableSpan() {
            override fun onClick(widget: View) {
                onTermsClick() // Callback when clicked
            }

            override fun updateDrawState(ds: android.text.TextPaint) {
                super.updateDrawState(ds)
                ds.isUnderlineText = true // Keep underlined
                ds.color = Color.BLACK // Force black color
            }
        }

        // ClickableSpan for "Privacy Policy"
        val privacyClickableSpan = object : ClickableSpan() {
            override fun onClick(widget: View) {
                onPrivacyClick() // Callback when clicked
            }

            override fun updateDrawState(ds: android.text.TextPaint) {
                super.updateDrawState(ds)
                ds.isUnderlineText = true // Keep underlined
                ds.color = Color.BLACK // Force black color
            }
        }

        // Apply ClickableSpan to text
        spannableString.setSpan(termsClickableSpan, termsStart, termsEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        spannableString.setSpan(privacyClickableSpan, privacyStart, privacyEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

        textView.text = spannableString
        textView.movementMethod = LinkMovementMethod.getInstance() // Enables clickability
    }
}
