package com.example.rfidstockpro.data

data class UHFTagModel(
    val reserved: String = "",
    val epc: String = "",
    val tid: String = "",
    val user: String = "",
    val rssi: String = "",
    val phase: Double = 0.0,
    val count: Int = 1,
    val extraData: Map<String, String> = mapOf()
) {
    companion object {
        const val KEY_TAG = "KEY_TAG"
    }

    fun generateTagString(): String {
        var data = ""
        if (reserved.isNotEmpty()) {
            data += "RESERVED:$reserved\n"
            data += "EPC:$epc"
        } else {
            data += if (tid.isEmpty()) epc else "EPC:$epc"
        }

        if (tid.isNotEmpty() && tid != "0000000000000000" && tid != "000000000000000000000000") {
            data += "\nTID:$tid"
        }

        if (user.isNotEmpty()) {
            data += "\nUSER:$user"
        }

        return data
    }
}