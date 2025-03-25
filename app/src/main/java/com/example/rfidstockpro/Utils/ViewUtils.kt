package com.example.rfidstockpro.Utils

import android.view.View
import android.widget.TextView

object ViewUtils {

    @JvmStatic
    fun setViewAlpha(view: View, alpha: Float) {
        view.alpha = alpha
    }

    @JvmStatic
    fun setTextViewDisabled(textView: TextView, isDisabled: Boolean) {
        if (isDisabled) {
            textView.alpha = 0.5f // Adjust transparency for a disabled effect
            textView.isEnabled = false
        } else {
            textView.alpha = 1.0f // Fully visible when enabled
            textView.isEnabled = true
        }
    }
}
