package com.example.rfidstockpro.Utils

import android.app.Activity
import android.os.Build
import android.view.View
import androidx.core.content.ContextCompat
import com.example.rfidstockpro.R

object StatusBarUtils {
    fun setStatusBarColor(activity: Activity) {
        val defaultColor = ContextCompat.getColor(activity, R.color.appMainColor)
        activity.window?.statusBarColor = defaultColor
    }

    // Set Transparent StatusBar with white text/icons
    fun setTransparentStatusBar(activity: Activity) {
        activity.window?.apply {
            decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            statusBarColor = android.graphics.Color.TRANSPARENT
        }
        setLightStatusBar(activity, false) // Set white text/icons
    }

    // Helper function to adjust status bar text/icon color
    private fun setLightStatusBar(activity: Activity, isLight: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val decorView = activity.window?.decorView
            decorView?.systemUiVisibility = if (isLight) {
                View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR // Dark text/icons
            } else {
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE // White text/icons
            }
        }
    }
}
