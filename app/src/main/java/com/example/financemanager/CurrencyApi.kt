package com.example.financemanager

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
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