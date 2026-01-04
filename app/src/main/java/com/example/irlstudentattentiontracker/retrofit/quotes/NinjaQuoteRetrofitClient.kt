package com.example.irlstudentattentiontracker.retrofit.quotes

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object NinjaQuoteRetrofitClient {

    private const val BASE_URL = "https://api.api-ninjas.com/v2/"

    val api: NinjaQuoteApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(NinjaQuoteApi::class.java)
    }
}
