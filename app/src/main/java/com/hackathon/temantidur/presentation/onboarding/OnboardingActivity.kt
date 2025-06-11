package com.hackathon.temantidur.presentation.onboarding

import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.hackathon.temantidur.R
import com.hackathon.temantidur.data.auth.SessionManager
import com.hackathon.temantidur.databinding.ActivityOnboardingBinding
import com.hackathon.temantidur.presentation.auth.AuthActivity
import com.hackathon.temantidur.presentation.mainmenu.MainActivity
import com.google.android.material.tabs.TabLayoutMediator

class OnboardingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOnboardingBinding
    private lateinit var sessionManager: SessionManager

    companion object {
        private const val PERMISSION_REQUEST_CODE = 100
        private const val REQUEST_CODE_SCHEDULE_EXACT_ALARM_SETTINGS = 101
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOnboardingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sessionManager = SessionManager(this)

        if (sessionManager.hasSeenOnboarding()) {
            if (sessionManager.isLoggedIn()) {
                startActivity(Intent(this, MainActivity::class.java))
            } else {
                login()
            }
            finish()
            return
        }

        val adapter = OnboardingPagerAdapter(this)
        binding.viewPager.adapter = adapter

        TabLayoutMediator(binding.indicator, binding.viewPager) { tab, position ->
            val customView = View(this)
            customView.layoutParams = ViewGroup.LayoutParams(
                if (position == 0) 48.dpToPx() else 16.dpToPx(),
                16.dpToPx()
            )
            customView.setBackgroundResource(R.drawable.tab_indicator_selector)
            tab.customView = customView
        }.attach()

        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                updateTabIndicators(position)
                if (position == adapter.itemCount - 1) {
                    binding.btnLogin.visibility = View.VISIBLE
                    binding.btnRegister.visibility = View.VISIBLE
                    binding.btnSkip.visibility = View.INVISIBLE
                } else {
                    binding.btnSkip.visibility = View.VISIBLE
                    binding.btnSkip.text = getString(R.string.skip)
                    binding.btnLogin.visibility = View.INVISIBLE
                    binding.btnRegister.visibility = View.INVISIBLE
                }
            }
        })

        binding.btnSkip.setOnClickListener {
            if (binding.viewPager.currentItem < adapter.itemCount - 1) {
                binding.viewPager.currentItem = adapter.itemCount - 1
            } else {
                sessionManager.setOnboardingSeen(true)
                login()
                finish()
            }
        }

        binding.btnLogin.setOnClickListener {
            login()
        }

        binding.btnRegister.setOnClickListener {
            register()
        }
    }

    private var isLoginAction: Boolean = false

    private fun login() {
        isLoginAction = true
        checkAndRequestPermissions()
    }

    private fun register() {
        isLoginAction = false
        checkAndRequestPermissions()
    }

    private fun proceedToAuthActivity() {
        sessionManager.setOnboardingSeen(true)
        val intent = Intent(this, AuthActivity::class.java)
        if (isLoginAction) {
            intent.putExtra("target_fragment", "login")
        } else {
            intent.putExtra("target_fragment", "register")
        }
        startActivity(intent)
        finish()
    }

    private fun updateTabIndicators(selectedPosition: Int) {
        for (i in 0 until binding.indicator.tabCount) {
            val tab = binding.indicator.getTabAt(i)
            tab?.customView?.let { customView ->
                val layoutParams = customView.layoutParams
                if (i == selectedPosition) {
                    layoutParams.width = 48.dpToPx()
                    customView.setBackgroundResource(R.drawable.selected_tab_background)
                } else {
                    layoutParams.width = 16.dpToPx()
                    customView.setBackgroundResource(R.drawable.unselected_tab_background)
                }
                customView.layoutParams = layoutParams
            }
        }
    }

    private fun Int.dpToPx(): Int {
        return (this * resources.displayMetrics.density).toInt()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            var allPermissionsGranted = true
            for (result in grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allPermissionsGranted = false
                    break
                }
            }

            if (allPermissionsGranted) {
                proceedToAuthActivity()
            } else {
                Toast.makeText(this, getString(R.string.onboarding_permission_rejected), Toast.LENGTH_LONG).show()
                proceedToAuthActivity()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_SCHEDULE_EXACT_ALARM_SETTINGS) {
            checkAndRequestPermissions()
        }
    }

    private fun checkAndRequestPermissions() {
        val permissionsToRequest = mutableListOf<String>()
        var navigateToExactAlarmSettings = false

        if (checkSelfPermission(android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(android.Manifest.permission.CAMERA)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(android.Manifest.permission.POST_NOTIFICATIONS)
            }
            if (checkSelfPermission(android.Manifest.permission.READ_MEDIA_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(android.Manifest.permission.READ_MEDIA_AUDIO)
            }
        } else {
            if (checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
            if (checkSelfPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(android.Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }


        if (checkSelfPermission(android.Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(android.Manifest.permission.RECORD_AUDIO)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
            if (!alarmManager.canScheduleExactAlarms()) {
                navigateToExactAlarmSettings = true
            }
        }

        if (navigateToExactAlarmSettings) {
            val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
            startActivityForResult(intent, REQUEST_CODE_SCHEDULE_EXACT_ALARM_SETTINGS)
        } else if (permissionsToRequest.isNotEmpty()) {
            requestPermissions(permissionsToRequest.toTypedArray(), PERMISSION_REQUEST_CODE)
        } else {
            proceedToAuthActivity()
        }
    }
}