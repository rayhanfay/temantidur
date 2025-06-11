package com.hackathon.temantidur.presentation.splash

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.hackathon.temantidur.R
import com.hackathon.temantidur.data.auth.SessionManager
import com.hackathon.temantidur.databinding.ActivitySplashScreenBinding
import com.hackathon.temantidur.presentation.auth.AuthActivity
import com.hackathon.temantidur.presentation.mainmenu.MainActivity
import com.hackathon.temantidur.presentation.onboarding.OnboardingActivity
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@SuppressLint("CustomSplashScreen")
class SplashScreen : AppCompatActivity() {

    private lateinit var binding: ActivitySplashScreenBinding
    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(R.style.Theme_TemanTidur_Splash)
        binding = ActivitySplashScreenBinding.inflate(layoutInflater)
        setContentView(binding.root)
        sessionManager = SessionManager(this)

        lifecycleScope.launch {
            delay(1500)
            val intent = when {
                !sessionManager.hasSeenOnboarding() ->
                    Intent(this@SplashScreen, OnboardingActivity::class.java)

                sessionManager.isLoggedIn() ->
                    Intent(this@SplashScreen, MainActivity::class.java)

                else ->
                    Intent(this@SplashScreen, AuthActivity::class.java)
            }
            startActivity(intent)
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
            finish()
        }

        binding.llTextContainer.post {
            val textWidth = binding.llTextContainer.width
            val newImageSize = textWidth / 2
            binding.ivLogo.layoutParams = binding.ivLogo.layoutParams.apply {
                width = newImageSize
                height = newImageSize
            }
        }
    }
}