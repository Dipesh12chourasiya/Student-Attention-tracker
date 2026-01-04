package com.example.irlstudentattentiontracker.retrofit.quotes

import com.example.irlstudentattentiontracker.models.NinjaQuote
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

interface NinjaQuoteApi {

    @GET("randomquotes")
    suspend fun getRandomQuotes(
        @Header("X-Api-Key") apiKey: String,
        @Query("categories") categories: String = "success,wisdom"
    ): Response<List<NinjaQuote>>
}
