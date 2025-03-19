package com.example.rfidstockpro.ui.activitys

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.rfidstockpro.databinding.ActivitySplashBinding

class SplashActivity : AppCompatActivity() {


    private lateinit var binding: ActivitySplashBinding


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)


        initActions()

    }

    private fun initActions() {
        binding.continueButton.setOnClickListener {
            startActivity(Intent(this@SplashActivity, AuthActivity::class.java))
        }
    }
}