package com.example.irlstudentattentiontracker

import SessionDayDecorator
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Color
import android.os.Build

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.PopupMenu
import androidx.activity.viewModels
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.detectfaceandexpression.adapters.SessionAdapter
import com.example.detectfaceandexpression.models.SessionData
import com.example.irlstudentattentiontracker.databinding.ActivityHomeBinding
import com.example.irlstudentattentiontracker.notifications.NotificationUtils
import com.example.irlstudentattentiontracker.roomDB.SessionEntity
import com.example.irlstudentattentiontracker.viewmodel.UserViewModel
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import com.prolificinteractive.materialcalendarview.CalendarDay
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import java.text.SimpleDateFormat
import java.util.Calendar


class HomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHomeBinding
    private val viewModel: UserViewModel by viewModels()
    private lateinit var adapter: SessionAdapter
    private var sessionsList: List<SessionData> = emptyList()

    /// ON create method
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        this.window.statusBarColor = Color.BLUE
        setContentView(binding.root)

        // Check and request notification permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                    1001
                )
            } else {

                setupDailyNotifications()
            }
        } else {

            setupDailyNotifications()
        }

        getAllSessions(this)

        val drawerToggle = ActionBarDrawerToggle(
            this,
            binding.drawerLayout,
            binding.topAppBar,
            R.string.navigation_drawer_open,
            R.string.navigation_drawer_close
        )

        // Attach drawer listener
        binding.drawerLayout.addDrawerListener(drawerToggle)
        drawerToggle.syncState()

        // setting Nav view click
        binding.navView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_profile -> {
                    startActivity(Intent(this, ProfileActivity::class.java))
                    true
                }
                R.id.time_table ->{
                    startActivity(Intent(this,TimeTableActivity::class.java))
                    true
                }
                R.id.nav_about -> {
                    showAboutDialog()
                    true
                }
                R.id.nav_feedback -> {
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_EMAIL, arrayOf("your-email@example.com"))
                        putExtra(Intent.EXTRA_SUBJECT, "Feedback for Student Attention Tracker")
                    }
                    startActivity(Intent.createChooser(intent, "Send feedback via"))
                    true
                }
                R.id.nav_clear -> {
                    showClearConfirmationDialog()
                    true
                }
                else -> false
            }.also {
                binding.drawerLayout.closeDrawers()
            }
        }


        binding.bottomNavigationView.setOnItemSelectedListener {
            when (it.itemId) {
                R.id.navigation_create_timetable -> {
                    startActivity(Intent(this, ChatBotActivity::class.java))
                    true
                }
                R.id.navigation_view_timetable -> {
                    startActivity(Intent(this, TimeTableActivity::class.java))
                    true
                }
                R.id.navigation_start_session -> {
                    startActivity(Intent(this, MainActivity::class.java)) // your face detection session
                    true
                }
                R.id.navigation_todays_report -> {
                    startActivity(Intent(this, DailyReportActivity::class.java))
                    true
                }
                else -> false
            }
        }


//        binding.btDashboard.setOnClickListener {
//            val intent = Intent(this, DailyReportActivity::class.java)
//            startActivity(intent)
//        }
//
//
//        binding.fabStartSession.setOnClickListener {
//            val intent = Intent(this, MainActivity::class.java)
//            startActivity(intent)
//        }
//
//        binding.btnChatBot.setOnClickListener {
//            val intent = Intent(this, ChatBotActivity::class.java)
//            startActivity(intent)
//        }


        // to setup the logic of calender to highlight old days
        viewModel.getAllSessionsForUser().onEach { sessionList :List<SessionData> ->
            val sessionCalendarDays = extractSessionDatesToCalendarDays(sessionList)
            val decorator = SessionDayDecorator(sessionCalendarDays, this) // 'this' is context
            binding.calendarView.addDecorator(decorator)
        }.launchIn(lifecycleScope)

        // to set on date changed
        binding.calendarView.setOnDateChangedListener { widget, calendarDay, selected ->
            val date = calendarDay.date // This returns a java.util.Calendar
            val sdf = SimpleDateFormat("dd MMMM yyyy", Locale.getDefault())
            val formattedDate = sdf.format(date.time) // Now you have the correct format

            val intent = Intent(this, SessionsOnDateActivity::class.java)
            intent.putExtra("selectedDate", formattedDate)
            startActivity(intent)
        }

    }

    fun extractSessionDatesToCalendarDays(sessions: List<SessionData>): HashSet<CalendarDay> {
        val formatter = SimpleDateFormat("dd MMMM yyyy, hh:mm a", Locale.getDefault())
        val calendarDates = HashSet<CalendarDay>()

        for (session in sessions) {
            try {
                val date = formatter.parse(session.dateTime)
                val cal = Calendar.getInstance()
                cal.time = date!!
                calendarDates.add(CalendarDay.from(cal))
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        return calendarDates
    }



    private fun getAllSessions(context: Context) {
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

        lifecycleScope.launch {
            viewModel.getAllSessionsForUser().collect { sessionList ->
                adapter.differ.submitList(sessionList.reversed())
                binding.tvEmptyState.visibility = if (sessionList.isEmpty()) View.VISIBLE else View.GONE
                Log.d("sessons","${sessionsList.size} Size is")
            }
        }

        binding.rvSessions.adapter = adapter
        binding.rvSessions.layoutManager = LinearLayoutManager(context)
    }


    private fun showAboutDialog() {
        AlertDialog.Builder(this)
            .setTitle("About")
            .setMessage("Student Attention Tracker helps monitor and analyze student attentiveness during classes using real-time face detection.\n\nVersion 1.0")
            .setPositiveButton("OK", null)
            .show()
    }


    private fun showClearConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle("Clear All Sessions?")
            .setMessage("This will delete all recorded sessions. Are you sure?")
            .setPositiveButton("Yes") { _, _ ->
                lifecycleScope.launch {
                    viewModel.deleteAllSessionsFromFirebase()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == 1001 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            setupDailyNotifications()
        }
    }

// function to set up notifications
    private fun setupDailyNotifications() {
        NotificationUtils.createNotificationChannel(this)

        NotificationUtils.scheduleDailyNotification(
            this, 7, 0, "🌅 Morning! Time to Start.", 101
        )
        NotificationUtils.scheduleDailyNotification(
            this, 14, 0, "📖 Focus on your midday session.", 102
        )
        NotificationUtils.scheduleDailyNotification(
            this, 18, 5, "🌅 Focus on your evening session.", 103
        )
        NotificationUtils.scheduleDailyNotification(
            this, 21, 0, "🌙 Night time! Review your topics.", 104
        )
    }
}