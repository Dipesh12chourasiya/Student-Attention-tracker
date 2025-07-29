package com.example.irlstudentattentiontracker

import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.irlstudentattentiontracker.databinding.ActivitySessionsOnDateBinding
import com.example.irlstudentattentiontracker.viewmodel.UserViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

import com.example.detectfaceandexpression.adapters.SessionAdapter
import com.example.detectfaceandexpression.models.SessionData
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.PercentFormatter
import java.util.*

class SessionsOnDateActivity : AppCompatActivity() {
    private val viewModel: UserViewModel by viewModels()
    private lateinit var adapter: SessionAdapter
    private lateinit var binding: ActivitySessionsOnDateBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySessionsOnDateBinding.inflate(layoutInflater)
        this.window.statusBarColor = Color.BLUE

        setContentView(binding.root)

        val selectedDate = intent.getStringExtra("selectedDate")?.trim() ?: ""
        binding.dateOfNow.text = selectedDate

        adapter = SessionAdapter(
            onItemClick = { session ->
                val intent = Intent(this, SessionDetailActivity::class.java)
                intent.putExtra("session_data", session) // session must be Serializable or Parcelable
                startActivity(intent)
            },
            onItemLongClick = { session ->
                AlertDialog.Builder(this)
                    .setTitle("Delete Session")
                    .setMessage("Are you sure you want to delete \"${session.title}\"?")
                    .setPositiveButton("Yes") { _, _ ->
                        viewModel.deleteSessionFromFb(session)
                    }
                    .setNegativeButton("No", null)
                    .show()
            }
        )

        binding.rvSessions.layoutManager = LinearLayoutManager(this)
        binding.rvSessions.adapter = adapter

        // Get all sessions and filter them by date
        lifecycleScope.launch {
            viewModel.getAllSessionsForUser().collect { allSessions ->
                val formatterFull = SimpleDateFormat("dd MMMM yyyy, hh:mm a", Locale.getDefault())
                val formatterDateOnly = SimpleDateFormat("dd MMMM yyyy", Locale.getDefault())

                val filtered = allSessions.filter { session ->
                    try {
                        val sessionDate = formatterFull.parse(session.dateTime)
                        val sessionDateStr = formatterDateOnly.format(sessionDate!!)
                        sessionDateStr == selectedDate
                    } catch (e: Exception) {
                        false
                    }
                }

                displayHeaderStats(filtered)
                setupLineChart(filtered)
                adapter.differ.submitList(filtered.reversed())
            }
        }
    }



    private fun displayHeaderStats(sessions: List<SessionData>) {

        var totalSeconds = 0
        var totalAttention = 0f

        for (session in sessions) {
            val durationStr = session.duration ?: continue

            // Parse optional hours, minutes, and seconds using regex
            val regex = Regex("(?:(\\d+)h)?\\s*(?:(\\d+)m)?\\s*(?:(\\d+)s)?")
            val match = regex.find(durationStr)

            if (match != null) {
                val hours = match.groupValues[1].toIntOrNull() ?: 0
                val minutes = match.groupValues[2].toIntOrNull() ?: 0
                val seconds = match.groupValues[3].toIntOrNull() ?: 0

                totalSeconds += hours * 3600 + minutes * 60 + seconds
            }

            totalAttention += session.attentionPercent?.toFloat() ?: 0f
        }

        // Convert totalSeconds to hh:mm:ss
        val totalHours = totalSeconds / 3600
        val remainingSeconds = totalSeconds % 3600
        val totalMinutes = remainingSeconds / 60
        val totalRemSeconds = remainingSeconds % 60

        val durationText = when {
            totalHours > 0 -> "${totalHours}h ${totalMinutes}m ${totalRemSeconds}s"
            totalMinutes > 0 -> "${totalMinutes}m ${totalRemSeconds}s"
            else -> "${totalRemSeconds}s"
        }

        val avgAttention = if (sessions.isNotEmpty()) totalAttention / sessions.size else 0f

        binding.tvTotalDuration.text = "Total Duration: $durationText"
        binding.tvAttentionPercent.text = "Overall Attention: ${"%.1f".format(avgAttention)}%"
    }


    private fun setupLineChart(sessions: List<SessionData>) {
        val entries = sessions.mapIndexed { index, session ->
            Log.d("LineChartDebug", "title=${session.title}, attentionPercent=${session.attentionPercent}")
            Entry(index.toFloat(), session.attentionPercent?.toFloat() ?: 0f)
        }

        val dataSet = LineDataSet(entries, "Attention %").apply {
            setDrawFilled(true)
            fillAlpha = 150
            fillDrawable = ContextCompat.getDrawable(this@SessionsOnDateActivity, R.drawable.chart_gradient)
            color = Color.BLUE
            valueTextColor = Color.BLACK
            lineWidth = 2f
            setCircleColor(Color.BLUE)
            circleRadius = 4f
            valueFormatter = PercentFormatter()
        }

        val lineData = LineData(dataSet)

        binding.lineChart.apply {
            data = lineData
            description.isEnabled = false
            axisLeft.apply {
                axisMinimum = 0f
                axisMaximum = 100f
                granularity = 10f
            }
            axisRight.isEnabled = false
            xAxis.granularity = 1f
            xAxis.setDrawLabels(false) // optional: hide index numbers
            invalidate()
        }

        val isDarkMode = resources.configuration.uiMode and
                Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES

        val textColor = if (isDarkMode) Color.WHITE else Color.BLACK

        // Set value text color for dataset
        dataSet.valueTextColor = textColor

// Axis label color
        val xAxis = binding.lineChart.xAxis
        xAxis.textColor = textColor

        val leftAxis = binding.lineChart.axisLeft
        leftAxis.textColor = textColor

        val rightAxis = binding.lineChart.axisRight
        rightAxis.textColor = textColor

// Legend color
        binding.lineChart.legend.textColor = textColor
    }

}
