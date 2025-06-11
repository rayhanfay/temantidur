package com.hackathon.temantidur.presentation.auth

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.hackathon.temantidur.R
import com.hackathon.temantidur.databinding.ActivityAuthBinding

class AuthActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAuthBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAuthBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (savedInstanceState == null) {
            val target = intent.getStringExtra("target_fragment")
            val fragment = when (target) {
                "register" -> RegisterFragment()
                else -> LoginFragment()
            }

            supportFragmentManager.beginTransaction()
                .replace(R.id.auth_container, fragment)
                .commit()
        }
    }
}