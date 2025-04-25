package com.example.rfidstockpro

object  Helper {
    /*init {
        System.loadLibrary("native-lib") // Ensure "native-lib" is your library name
    }
*/
    /*external fun ACCESS(): String
    external fun SECRET(): String*/

    fun getCurrentFormattedDateTime(): String {
        val formatter = java.text.SimpleDateFormat("dd/MM/yy hh:mm a", java.util.Locale.getDefault())
        return formatter.format(java.util.Date())
    }

}