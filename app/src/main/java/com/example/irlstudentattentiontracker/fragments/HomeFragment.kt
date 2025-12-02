package com.example.irlstudentattentiontracker.fragments

import SessionDayDecorator
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.GravityCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.detectfaceandexpression.adapters.SessionAdapter
import com.example.detectfaceandexpression.models.SessionData
import com.example.irlstudentattentiontracker.ProfileActivity
import com.example.irlstudentattentiontracker.R
import com.example.irlstudentattentiontracker.SessionDetailActivity
import com.example.irlstudentattentiontracker.SessionsOnDateActivity
import com.example.irlstudentattentiontracker.TimeTableActivity
import com.example.irlstudentattentiontracker.databinding.FragmentHomeBinding
import com.example.irlstudentattentiontracker.viewmodel.UserViewModel
import com.prolificinteractive.materialcalendarview.CalendarDay
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.HashSet
import java.util.Locale

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: SessionAdapter
    private val viewModel: UserViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupDrawer()
        setupCalendarHighlighting()
        setupSessionsRecycler()
        setupCalendarClick()
    }

    // -----------------------
    // Drawer setup
    // -----------------------
    private fun setupDrawer() {
        val activity = requireActivity()

        // Open drawer when navigation icon clicked
        binding.topAppBar.setNavigationOnClickListener {
            val drawerLayout = activity.findViewById<androidx.drawerlayout.widget.DrawerLayout>(R.id.drawer_layout)
            drawerLayout.openDrawer(GravityCompat.START)
        }

        // Handle drawer menu clicks
        val navView = activity.findViewById<com.google.android.material.navigation.NavigationView>(R.id.nav_view)
        navView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_profile -> startActivity(Intent(activity, ProfileActivity::class.java))
                R.id.nav_about -> {
                    androidx.appcompat.app.AlertDialog.Builder(activity)
                        .setTitle("About")
                        .setMessage("Student Attention Tracker helps monitor student attentiveness.\n\nVersion 1.0")
                        .setPositiveButton("OK", null)
                        .show()
                }
                R.id.nav_feedback -> {
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_EMAIL, arrayOf("your-email@example.com"))
                        putExtra(Intent.EXTRA_SUBJECT, "Feedback for Student Attention Tracker")
                    }
                    startActivity(Intent.createChooser(intent, "Send feedback via"))
                }
                R.id.nav_clear -> {
                    androidx.appcompat.app.AlertDialog.Builder(activity)
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
            }
            val drawerLayout = activity.findViewById<androidx.drawerlayout.widget.DrawerLayout>(R.id.drawer_layout)
            drawerLayout.closeDrawers()
            true
        }
    }

    // -----------------------------------------
    // 1) Highlight old session days in calendar
    // -----------------------------------------
    private fun setupCalendarHighlighting() {
        viewModel.getAllSessionsForUser().onEach { sessionList ->
            val sessionCalendarDays = extractSessionDatesToCalendarDays(sessionList)
            binding.calendarView.addDecorator(SessionDayDecorator(sessionCalendarDays, requireContext()))
        }.launchIn(viewLifecycleOwner.lifecycleScope)
    }

    private fun extractSessionDatesToCalendarDays(sessions: List<SessionData>): HashSet<CalendarDay> {
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

    // -----------------------------------------
    // 2) Setup RecyclerView for recent sessions
    // -----------------------------------------
    private fun setupSessionsRecycler() {
        adapter = SessionAdapter(
            onItemClick = { session ->
                val intent = Intent(requireContext(), SessionDetailActivity::class.java)
                intent.putExtra("session_data", session)
                startActivity(intent)
            },
            onItemLongClick = { session -> /* optional */ }
        )

        binding.rvSessions.layoutManager = LinearLayoutManager(requireContext())
        binding.rvSessions.adapter = adapter

        viewModel.getAllSessionsForUser().onEach { sessionList ->
            adapter.differ.submitList(sessionList.reversed())
            binding.tvEmptyState.visibility =
                if (sessionList.isEmpty()) View.VISIBLE else View.GONE
            Log.d("sessions", "List size: ${sessionList.size}")
        }.launchIn(viewLifecycleOwner.lifecycleScope)
    }

    // -----------------------------------------
    // 3) When selecting a date → open day details
    // -----------------------------------------
    private fun setupCalendarClick() {
        binding.calendarView.setOnDateChangedListener { _, calendarDay, _ ->
            val date = calendarDay.date
            val sdf = SimpleDateFormat("dd MMMM yyyy", Locale.getDefault())
            val formattedDate = sdf.format(date.time)

            val intent = Intent(requireContext(), SessionsOnDateActivity::class.java)
            intent.putExtra("selectedDate", formattedDate)
            startActivity(intent)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
