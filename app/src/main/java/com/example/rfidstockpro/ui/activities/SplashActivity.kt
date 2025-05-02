package com.example.rfidstockpro.ui.activities

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.rfidstockpro.databinding.ActivitySplashBinding
import com.example.rfidstockpro.sharedpref.SessionManager

class SplashActivity : AppCompatActivity() {





    private lateinit var binding: ActivitySplashBinding


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initActions()
    }

    private external fun getAwsAccessKeyFromNdk(): String

    private fun initActions() {
        binding.continueButton.setOnClickListener {
            val sessionManager = SessionManager.getInstance(this) // Get Singleton Instance
            val intent = if (sessionManager.isLoggedIn()) {
                Intent(this, DashboardActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
            } else {
                Intent(this, AuthActivity::class.java) // Otherwise, go to Login screen
            }
            startActivity(intent)
            finish()
        }
    }
}