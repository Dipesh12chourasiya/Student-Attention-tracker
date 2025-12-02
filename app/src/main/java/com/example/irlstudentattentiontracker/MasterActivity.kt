package com.example.irlstudentattentiontracker

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.irlstudentattentiontracker.databinding.ActivityMasterBinding
import com.example.irlstudentattentiontracker.fragments.*

class MasterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMasterBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMasterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Load HomeFragment by default
        replaceFragment(HomeFragment())

        binding.bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {

                R.id.navigation_home ->
                    replaceFragment(HomeFragment())

                R.id.navigation_create_timetable ->
                    replaceFragment(CreateTimetableFragment())

                R.id.navigation_view_timetable ->
                    replaceFragment(ViewTimetableFragment())

                R.id.navigation_start_session ->
                    replaceFragment(StartSessionFragment())

                R.id.navigation_todays_report ->
                    replaceFragment(DailyReportFragment())
            }
            true
        }

        // Handle intent if activity was started with a flag
        intent?.let {
            val openHome = it.getBooleanExtra("openHomeFragment", false)
            if (openHome) {
                replaceFragment(HomeFragment())
                binding.bottomNavigationView.selectedItemId = R.id.navigation_home
            }
        }
    }

    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        val openHome = intent.getBooleanExtra("openHomeFragment", false)
        if (openHome) {
            replaceFragment(HomeFragment())
            binding.bottomNavigationView.selectedItemId = R.id.navigation_home
        }
    }


}
