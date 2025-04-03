package com.example.rfidstockpro.encryption

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec
import java.util.Base64

object AESUtils {
    private const val ALGORITHM = "AES"
    
    @RequiresApi(Build.VERSION_CODES.O)
    fun generateKey(): String {
        val keyGen = KeyGenerator.getInstance(ALGORITHM)
        keyGen.init(256)  // Use 256-bit AES encryption
        val secretKey = keyGen.generateKey()
        return Base64.getEncoder().encodeToString(secretKey.encoded)
    }

    @SuppressLint("GetInstance")
    @RequiresApi(Build.VERSION_CODES.O)
    fun encrypt(data: String, key: String): String {
        val secretKey = SecretKeySpec(Base64.getDecoder().decode(key), ALGORITHM)
        val cipher = Cipher.getInstance(ALGORITHM)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)
        return Base64.getEncoder().encodeToString(cipher.doFinal(data.toByteArray()))
    }

    @SuppressLint("GetInstance")
    @RequiresApi(Build.VERSION_CODES.O)
    fun decrypt(encryptedData: String, key: String): String {
        val secretKey = SecretKeySpec(Base64.getDecoder().decode(key), ALGORITHM)
        val cipher = Cipher.getInstance(ALGORITHM)
        cipher.init(Cipher.DECRYPT_MODE, secretKey)
        return String(cipher.doFinal(Base64.getDecoder().decode(encryptedData)))
    }


   /* @RequiresApi(Build.VERSION_CODES.O)
    fun getOrCreateEncryptionKey(context: Context): String {
        val sharedPreferences = context.getSharedPreferences("EncryptionPrefs", Context.MODE_PRIVATE)
        val existingKey = sharedPreferences.getString("AES_KEY", null)

        return if (existingKey == null) {
            val newKey = AESUtils.generateKey()
            sharedPreferences.edit().putString("AES_KEY", newKey).apply()
            newKey
        } else {
            existingKey
        }
    }*/
}
