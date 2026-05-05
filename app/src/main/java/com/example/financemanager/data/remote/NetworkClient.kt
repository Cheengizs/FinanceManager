package com.example.financemanager.data.remote

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path

data class CurrencyResponse(
    val base: String,
    val rates: Map<String, Double>
)

interface CurrencyApiService {
    @GET("v4/latest/{base}")
    suspend fun getRates(@Path("base") baseCurrency: String = "BYN"): CurrencyResponse
}

object RetrofitClient {
    private const val BASE_URL = "https://api.exchangerate-api.com/"

    val api: CurrencyApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(CurrencyApiService::class.java)
    }
}

data class ImageKitResponse(
    val url: String
)

interface ImageKitApiService {
    @FormUrlEncoded
    @POST("api/v1/files/upload")
    suspend fun uploadImage(
        @Header("Authorization") authHeader: String,
        @Field("file") fileBase64: String,
        @Field("fileName") fileName: String
    ): ImageKitResponse
}

object ImageKitClient {
    private const val BASE_URL = "https://upload.imagekit.io/"

    val api: ImageKitApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ImageKitApiService::class.java)
    }
}