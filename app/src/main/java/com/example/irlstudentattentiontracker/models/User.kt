package com.example.detectfaceandexpression.models

data class User(
    val uid: String = "",
    val username: String = "",
    val email: String = "",
    val profileImageUrl: String? = null,

    val totalSessions: Int = 0,
    val averageAttention: Int = 0,
    val mostRecentSessionTime: String? = null,

    val totalStudyTimeMinutes: Int = 0,
    val totalInattentiveMinutes: Int = 0,
    val maxInattentiveStreak: Int = 0,
    val achievements: List<String>? = null
)