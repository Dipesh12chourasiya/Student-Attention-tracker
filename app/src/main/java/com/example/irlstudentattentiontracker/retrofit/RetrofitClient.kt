package com.example.irlstudentattentiontracker.retrofit

object RetrofitClient {
    private const val BASE_URL = "https://openrouter.ai/api/v1/"

    val api: OpenRouterApi by lazy {
        retrofit2.Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(retrofit2.converter.gson.GsonConverterFactory.create())
            .build()
            .create(OpenRouterApi::class.java)
    }
}
