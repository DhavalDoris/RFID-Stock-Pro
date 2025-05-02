package com.example.rfidstockpro.Utils

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.location.LocationManager
import android.provider.Settings
import com.example.rfidstockpro.R

object PermissionUtils {

    const val REQUEST_ACTION_LOCATION_SETTINGS = 99

    fun showLocationDialogIfDisabled(
        activity: Activity,
        onLocationEnabled: () -> Unit
    ) {
        if (!isLocationEnabled(activity)) {
            AlertDialog.Builder(activity)
                .setTitle(activity.getString(R.string.permission))
                .setMessage(activity.getString(R.string.open_location_msg))
                .setPositiveButton(activity.getString(R.string.open_location)) { _, _ ->
                    val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                    activity.startActivityForResult(intent, REQUEST_ACTION_LOCATION_SETTINGS)
                }
                .setNegativeButton(activity.getString(R.string.cancel)) { _, _ ->  }
                .setCancelable(false)
                .show()
        } else {
            onLocationEnabled()
        }
    }

    fun isLocationEnabled(context: Context): Boolean {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }
}

