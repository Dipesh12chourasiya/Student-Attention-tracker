package com.example.irlstudentattentiontracker.repository

import android.util.Log
import com.example.irlstudentattentiontracker.models.NinjaQuote
import com.example.irlstudentattentiontracker.retrofit.quotes.NinjaQuoteRetrofitClient.api

class NinjaQuoteRepository {

    private val apiKey = "wVz3GLiekxi1xCq0vK01ag==oSqHZyZz07Rnt6Ig"

    suspend fun fetchRandomQuote(): NinjaQuote? {
        return try {
            val response = api.getRandomQuotes(apiKey)
            Log.d("NinjaQuoteRepo", "HTTP code: ${response.code()}")
            Log.d("NinjaQuoteRepo", "HTTP message: ${response.message()}")

            if (response.isSuccessful) {
                val body = response.body()
                Log.d("NinjaQuoteRepo", "Body: $body")
                body?.firstOrNull() // return first quote from the list
            } else {
                Log.e("NinjaQuoteRepo", "Error body: ${response.errorBody()?.string()}")
                null
            }
        } catch (e: Exception) {
            Log.e("NinjaQuoteRepo", "Exception: ${e.localizedMessage}")
            null
        }
    }
}
