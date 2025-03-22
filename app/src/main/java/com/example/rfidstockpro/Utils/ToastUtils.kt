package com.example.rfidstockpro.Utils

import android.content.Context
import android.widget.Toast

object ToastUtils {
    private var toast: Toast? = null

    public fun showToast(context: Context, message: String) {
        toast?.cancel()  // Cancel the previous toast if it exists
        toast = Toast.makeText(context, message, Toast.LENGTH_SHORT)
        toast?.show()
    }
}
