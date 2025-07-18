package com.example.irlstudentattentiontracker

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.irlstudentattentiontracker.auth.LoginActivity
import com.example.irlstudentattentiontracker.databinding.ActivitySplashBinding
import com.example.irlstudentattentiontracker.viewmodel.UserViewModel
import com.google.firebase.auth.FirebaseUser


class SplashActivity : AppCompatActivity() {

    private val viewModel: UserViewModel by viewModels()


    private val permissions = arrayOf(
        Manifest.permission.CAMERA
    )

    private val REQUEST_PERMISSIONS = 100
    lateinit var binding: ActivitySplashBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)


        if (!hasAllPermissions()) {
            ActivityCompat.requestPermissions(this, permissions, REQUEST_PERMISSIONS)
        } else {
            proceedToLogin()
        }


    }

    private fun hasAllPermissions(): Boolean {
        return permissions.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == REQUEST_PERMISSIONS && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
            proceedToLogin()
        } else {
            // Show error or close app
            finish()
        }
    }

    private fun proceedToLogin() {
        Handler(Looper.getMainLooper()).postDelayed({
            val currentUser: FirebaseUser? = viewModel.getCurrentUser()

            if (currentUser != null) {
                // User already logged in
                startActivity(Intent(this, HomeActivity::class.java))
            } else {
                // User not logged in
                startActivity(Intent(this, LoginActivity::class.java))
            }
            finish()
        }, 1000)
    }
}