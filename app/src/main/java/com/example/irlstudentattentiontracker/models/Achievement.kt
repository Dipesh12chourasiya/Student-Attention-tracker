package com.example.irlstudentattentiontracker.models

import com.example.detectfaceandexpression.models.SessionData
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale

data class Achievement(
    val title: String,
    val description: String,
    val unlocked: Boolean
)

fun getUserAchievements(sessions: List<SessionData>, targetPerDay: Int = 3): List<Achievement> {
    val achievements = mutableListOf<Achievement>()

    // Goal Setter
    val today = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(Date())
    val sessionsToday = sessions.count {
        SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(it.timestamp()) == today
    }
    achievements.add(
        Achievement(
            "Goal Setter",
            "Complete $targetPerDay sessions today",
            sessionsToday >= targetPerDay
        )
    )

    // Consistency Champ
    val sortedDates = sessions.map { it.timestamp() }.map {
        SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(Date(it))
    }.distinct().sorted()
    var streak = 0
    var prevDate: LocalDate? = null
    for(dateStr in sortedDates) {
        val date = LocalDate.parse(dateStr, DateTimeFormatter.ofPattern("dd-MM-yyyy"))
        if(prevDate != null && prevDate.plusDays(1) == date) streak++ else streak = 1
        prevDate = date
    }
    achievements.add(
        Achievement(
            "Consistency Champ",
            "Maintain a streak of daily sessions",
            streak >= 3 // example threshold
        )
    )

    // Attention Hero
    val maxAttention = sessions.maxOfOrNull { it.attentionPercent ?: 0 } ?: 0
    achievements.add(
        Achievement(
            "Attention Hero",
            "Achieve highest attention in a session",
            maxAttention >= 90 // example threshold
        )
    )

    // Improver
    if (sessions.size >= 6) {
        val last3Avg = sessions.takeLast(3).map { it.attentionPercent ?: 0 }.average()
        val prev3Avg = sessions.take(sessions.size - 3).takeLast(3).map { it.attentionPercent ?: 0 }.average()
        achievements.add(
            Achievement(
                "Improver",
                "Improve your attention by 10% over last sessions",
                last3Avg - prev3Avg >= 10
            )
        )
    }

    return achievements
}

// Extension function to get timestamp from duration and startTime
fun SessionData.timestamp(): Long {
    // Combine session date and startTime into one string
    val dateTimeStr = "$dateTime, $startTime" // adjust depending on your stored fields
    val sdf = SimpleDateFormat("dd MMMM yyyy, hh:mm a", Locale.getDefault())
    return sdf.parse(dateTimeStr)?.time ?: 0L
}
