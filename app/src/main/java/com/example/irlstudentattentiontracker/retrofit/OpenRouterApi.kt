package com.example.irlstudentattentiontracker.retrofit

import com.example.irlstudentattentiontracker.Env
import com.example.irlstudentattentiontracker.models.ChatRequest
import com.example.irlstudentattentiontracker.models.ChatResponse
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

interface OpenRouterApi {
    @Headers(
        "Content-Type: application/json",
        "Authorization: Bearer ${Env.apiKey}",
        "X-Title: StudentAttentionTrackerApp"
    )
    @POST("chat/completions")
    fun getChatCompletion(@Body body: ChatRequest): Call<ChatResponse>
}

//ref
//https://www.youtube.com/watch?v=ECxtBChPbk0