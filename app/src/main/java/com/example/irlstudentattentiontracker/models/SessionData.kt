package com.example.detectfaceandexpression.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class SessionData(
    val sessionId: String? = null,
    val userId: String? = null,

    val title: String = "",
    val dateTime: String = "",
    val duration: String = "",
    val startTime: String = "",
    val endTime: String = "",
    val attentionPercent: Int = 0,

    val totalFaces: Int = 0,
    val totalFrames: Int = 0,
    val inattentiveCount: Int = 0,
    val attentiveCount: Int = 0,
    val maxInattentiveStreak: Int = 0,

    val notes: String? = null
) : Parcelable
