package com.example.irlstudentattentiontracker.notifications


import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.irlstudentattentiontracker.R

class NotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val message = intent.getStringExtra("message")

        // Check if we have permission
        if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            // Log or handle the case where permission is not granted
            Log.w("NotificationReceiver", "POST_NOTIFICATIONS permission not granted")
            return
        }

        val builder = NotificationCompat.Builder(context, "daily_channel")
            .setSmallIcon(R.drawable.baseline_people_24)
            .setContentTitle("📚 Daily Study Reminder")
            .setContentText(message ?: "Time to study!")
            .setPriority(NotificationCompat.PRIORITY_HIGH)

        NotificationManagerCompat.from(context).notify(message.hashCode(), builder.build())
    }
}
