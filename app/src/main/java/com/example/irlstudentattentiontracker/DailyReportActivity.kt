package com.example.irlstudentattentiontracker

import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.detectfaceandexpression.adapters.SessionAdapter
import com.example.detectfaceandexpression.models.SessionData
import com.example.irlstudentattentiontracker.databinding.ActivityDailyReportBinding
import com.example.irlstudentattentiontracker.viewmodel.UserViewModel
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.PercentFormatter
import com.github.mikephil.charting.formatter.ValueFormatter
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DailyReportActivity : AppCompatActivity() {
    private lateinit var binding: ActivityDailyReportBinding
    private val viewModel: UserViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDailyReportBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Back button
        binding.topAppBar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        val todayDateOnly = SimpleDateFormat("dd MMMM yyyy", Locale.getDefault()).format(Date())
//        val todayDateOnly = "16 July 2025"

        lifecycleScope.launch {
            viewModel.getAllSessionsForUser().collect { allSessions ->
                val todaySessions = allSessions.filter { session ->
                    session.dateTime?.substringBefore(",") == todayDateOnly
                }

                if (todaySessions.isEmpty()) {
                    binding.tvEmptyState.visibility = View.VISIBLE
                    binding.recyclerView.visibility = View.GONE
                    binding.lineChart.visibility = View.GONE
                } else {
                    binding.tvEmptyState.visibility = View.GONE
                    binding.recyclerView.visibility = View.VISIBLE
                    binding.lineChart.visibility = View.VISIBLE

                    displayHeaderStats(todaySessions)
                    setupLineChart(todaySessions)
                    setupRecyclerView(todaySessions)
                }
            }
        }
    }


    private fun displayHeaderStats(sessions: List<SessionData>) {
        // Show today's date
        binding.tvDate.text =
            "Date: " + SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date())

        var totalMinutes = 0
        var totalAttention = 0f

        for (session in sessions) {
            // Parse "duration" string like "31m 12s" into total minutes
            val durationStr = session.duration ?: continue
            val regex = Regex("(\\d+)m.*?(\\d+)s")
            val match = regex.find(durationStr)
            if (match != null) {
                val minutes = match.groupValues[1].toIntOrNull() ?: 0
                val seconds = match.groupValues[2].toIntOrNull() ?: 0
                totalMinutes += (minutes + seconds / 60)
            }

            // Use pre-stored attentionPercent
            totalAttention += session.attentionPercent?.toFloat() ?: 0f
        }

        // Convert totalMinutes to hours and minutes
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60
        val durationText = if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"

        // Average attention percent
        val avgAttention = if (sessions.isNotEmpty()) totalAttention / sessions.size else 0f

        binding.tvTotalDuration.text = "Total Duration: $durationText"
        binding.tvAttentionPercent.text = "Overall Attention: ${"%.1f".format(avgAttention)}%"
    }


    private fun setupLineChart(sessions: List<SessionData>) {
        val entries = sessions.mapIndexed { index, session ->
            Log.d(
                "LineChartDebug",
                "title=${session.title}, attentionPercent=${session.attentionPercent}"
            )
            Entry(index.toFloat(), session.attentionPercent?.toFloat() ?: 0f)
        }

        val isDarkMode = resources.configuration.uiMode and
                Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES

        val textColor = if (isDarkMode) Color.WHITE else Color.BLACK

        val dataSet = LineDataSet(entries, "Attention %").apply {
            setDrawFilled(true)
            fillAlpha = 150
            fillDrawable =
                ContextCompat.getDrawable(this@DailyReportActivity, R.drawable.chart_gradient)
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


    private fun setupRecyclerView(sessions: List<SessionData>) {
        val adapter = SessionAdapter(
            onItemClick = { session ->
                val intent = Intent(this, SessionDetailActivity::class.java)
                intent.putExtra(
                    "session_data",
                    session
                ) // session must be Serializable or Parcelable
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

        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter
        adapter.differ.submitList(sessions.reversed())
    }
}