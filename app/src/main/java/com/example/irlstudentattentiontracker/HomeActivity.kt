package com.example.irlstudentattentiontracker

import SessionDayDecorator
import android.content.Context
import android.content.Intent

import android.os.Bundle
import android.util.Log
import android.widget.PopupMenu
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.detectfaceandexpression.adapters.SessionAdapter
import com.example.irlstudentattentiontracker.databinding.ActivityHomeBinding
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
    private var sessionsList: List<SessionEntity> = emptyList()

    /// ON create method
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)


        getAllSessions(this)

        binding.topAppBar.setNavigationOnClickListener {
            // Show a menu, drawer, or dialog here
            showPopupMenu()
        }


        binding.fabStartSession.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }


        // to setup the logic of calender to highlight old days
        viewModel.getAllSessions().onEach { sessionList ->
            val sessionCalendarDays = extractSessionDatesToCalendarDays(sessionList)
            val decorator = SessionDayDecorator(sessionCalendarDays, this) // 'this' is context
            binding.calendarView.addDecorator(decorator)
        }.launchIn(lifecycleScope)


        binding.calendarView.setOnDateChangedListener { widget, calendarDay, selected ->
            val date = calendarDay.date // This returns a java.util.Calendar
            val sdf = SimpleDateFormat("dd MMMM yyyy", Locale.getDefault())
            val formattedDate = sdf.format(date.time) // Now you have the correct format

            val intent = Intent(this, SessionsOnDateActivity::class.java)
            intent.putExtra("selectedDate", formattedDate)
            startActivity(intent)
        }

    }

    fun extractSessionDatesToCalendarDays(sessions: List<SessionEntity>): HashSet<CalendarDay> {
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
                intent.putExtra("session_data", session)
                startActivity(intent)
            },
            onItemLongClick = { session ->
                AlertDialog.Builder(this)
                    .setTitle("Delete Session")
                    .setMessage("Are you sure you want to delete \"${session.title}\"?")
                    .setPositiveButton("Yes") { _, _ ->
                        viewModel.deleteSession(session)
                    }
                    .setNegativeButton("No", null)
                    .show()
            }
        )
        binding.rvSessions.adapter = adapter

        lifecycleScope.launch {
            viewModel.getAllSessions().collect { sessionList ->

                adapter.differ.submitList(sessionList.reversed())

                binding.rvSessions.adapter = adapter
                binding.rvSessions.layoutManager = LinearLayoutManager(context)
            }
        }
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
                    viewModel.deleteAllSessions()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }


    private fun showPopupMenu() {
        val popupMenu = PopupMenu(this, binding.topAppBar)
        popupMenu.menuInflater.inflate(R.menu.top_app_bar_menu, popupMenu.menu)

        popupMenu.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.menu_about -> {
                    showAboutDialog()
                    true
                }

                R.id.menu_feedback -> {
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_EMAIL, arrayOf("your-email@example.com"))
                        putExtra(Intent.EXTRA_SUBJECT, "Feedback for Student Attention Tracker")
                    }
                    startActivity(Intent.createChooser(intent, "Send feedback via"))
                    true
                }

                R.id.menu_clear_data -> {
                    showClearConfirmationDialog()
                    true
                }

                else -> false
            }
        }

        popupMenu.show()
    }
}