package com.example.rfidstockpro.api

import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.PUT
import retrofit2.http.Url

interface S3UploadService {
    @PUT
    fun uploadFile(
        @Url url: String,
        @Body body: RequestBody,
        @Header("Content-Type") contentType: String
    ): Call<ResponseBody>
}
