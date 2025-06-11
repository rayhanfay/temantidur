package com.hackathon.temantidur.presentation.mainmenu

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.view.animation.AnimationSet
import android.view.animation.AnimationUtils
import android.view.animation.TranslateAnimation
import android.view.inputmethod.InputMethodManager
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentManager.OnBackStackChangedListener
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.hackathon.temantidur.R
import com.hackathon.temantidur.data.auth.SessionManager
import com.hackathon.temantidur.data.emotion.EmotionStorageManager
import com.hackathon.temantidur.databinding.ActivityMainBinding
import com.hackathon.temantidur.databinding.DrawerMenuBinding
import com.hackathon.temantidur.presentation.auth.AuthActivity
import com.hackathon.temantidur.presentation.chat.ChatFragment
import com.hackathon.temantidur.presentation.chat.ChatViewModel
import com.hackathon.temantidur.presentation.emotion.CameraActivity
import com.hackathon.temantidur.presentation.emotion.EmotionResultFragment
import com.hackathon.temantidur.presentation.recommendation.RecommendationFragment
import com.hackathon.temantidur.presentation.sidemenu.AboutUsFragment
import com.hackathon.temantidur.presentation.sidemenu.LanguageFragment
import com.hackathon.temantidur.presentation.sidemenu.ProfileFragment
import com.hackathon.temantidur.presentation.voicechat.VoiceRecordingManager
import com.hackathon.temantidur.receivers.DailyReminderReceiver
import com.hackathon.temantidur.utils.ToolbarVisibilityListener
import com.hackathon.temantidur.utils.dialogs.ConfirmationDialogFragment
import java.io.File
import java.util.Calendar
import java.util.Locale
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import com.hackathon.temantidur.BuildConfig
import com.hackathon.temantidur.receivers.AutoRecapReceiver
import com.hackathon.temantidur.services.AiDailyRecapService
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity(), ToolbarVisibilityListener,
    LanguageFragment.OnLanguageChangeListener {

    private val emotionIcons = listOf(
        R.drawable.emotion_angry,
        R.drawable.emotion_disgust,
        R.drawable.emotion_happy,
        R.drawable.emotion_neutral,
        R.drawable.emotion_sad,
        R.drawable.emotion_sickened,
        R.drawable.emotion_surprised
    )

    private lateinit var binding: ActivityMainBinding
    private lateinit var drawerMenuBinding: DrawerMenuBinding
    private lateinit var sessionManager: SessionManager
    private lateinit var auth: FirebaseAuth
    private lateinit var iconNotification: ImageView
    private lateinit var gestureDetector: GestureDetector
    private lateinit var emotionIconHandler: Handler
    private lateinit var voiceRecordingManager: VoiceRecordingManager
    private val chatViewModel: ChatViewModel by viewModels()
    private var audioFile: File? = null
    private var isRecording = false

    private val dotsHandler = Handler(Looper.getMainLooper())
    private var dotsRunnable: Runnable? = null
    private var dotCount = 0

    private var activeNavItem: Int = R.id.nav_chat

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            setupDailyReminderAlarm(true)
        } else {
            Toast.makeText(
                this,
                getString(R.string.notification_permission_required),
                Toast.LENGTH_LONG
            ).show()
            drawerMenuBinding.switchNotification.isChecked = false
            sessionManager.setDailyReminderEnabled(false)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val drawerView = binding.drawerLayout.findViewById<NavigationView>(R.id.nav_view)
        drawerMenuBinding = DrawerMenuBinding.bind(drawerView)
        gestureDetector = GestureDetector(this, SwipeGestureListener())
        sessionManager = SessionManager(this)
        auth = FirebaseAuth.getInstance()
        applyLanguage(sessionManager.getLanguage())
        setupUI()
        emotionIconHandler = Handler(Looper.getMainLooper())
        updateNavigationState()
        setupDailyRecapAlarm()
        setupDailyReminderSwitch()
        supportFragmentManager.addOnBackStackChangedListener(onBackStackChangedListener)

        lifecycleScope.launch {
            chatViewModel.voiceMessageAddedEvent.collectLatest { file ->
                navigateToChatFragment(voiceMode = true, audioFile = file)
            }
        }

        if (savedInstanceState == null) {
            handleIntentDestination(intent)
        }

        voiceRecordingManager = VoiceRecordingManager(this).apply {
            setRecordingListener(object : VoiceRecordingManager.RecordingListener {
                override fun onRecordingStarted() {
                    runOnUiThread {
                        animateVoiceRecordOverlay(true)
                        startDotsAnimation()
                        isRecording = true
                    }
                }

                override fun onRecordingStopped(audioFile: File) {
                    runOnUiThread {
                        animateVoiceRecordOverlay(false)
                        stopDotsAnimation()
                        isRecording = false
                        this@MainActivity.audioFile = audioFile
                        Log.d("MainActivity", "Recording stopped. Navigating to ChatFragment with audio file: ${audioFile.absolutePath}")
                        navigateToChatFragment(voiceMode = true, audioFile = audioFile)
                    }
                }

                override fun onRecordingError(error: String) {
                    runOnUiThread {
                        animateVoiceRecordOverlay(false)
                        stopDotsAnimation()
                        isRecording = false
                        Toast.makeText(this@MainActivity, error, Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onPermissionRequired() {
                    requestAudioPermissionForRecording()
                }
            })
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntentDestination(intent)
    }

    private fun handleIntentDestination(intent: Intent?) {
        intent?.let {
            val startDestination = it.getStringExtra("start_destination")
            when (startDestination) {
                "chat" -> navigateToChatFragment()
                "emotion_check" -> navigateToEmotionCheck()
                "recommendation" -> navigateToRecommendationFromWidget()
                "emotion_result_from_widget" -> {
                    val emotionDataBundle = it.getBundleExtra("emotion_data_bundle")
                    if (emotionDataBundle != null) {
                        navigateToEmotionResult(
                            emotionDataBundle.getString("emotion", ""),
                            emotionDataBundle.getFloat("confidence", 0f),
                            emotionDataBundle.getString("description", ""),
                            emotionDataBundle.getStringArrayList("recommendations") ?: arrayListOf()
                        )
                    } else {
                        Toast.makeText(this, getString(R.string.no_emotion_data), Toast.LENGTH_SHORT).show()
                        navigateToEmotionCheck()
                    }
                }
                else -> {
                    val currentFragment = supportFragmentManager.findFragmentById(R.id.main_content_fragment_container)
                    if (currentFragment == null || currentFragment !is HomeFragment) {
                        replaceFragment(HomeFragment(), showToolbar = true, tag = "HomeFragment")
                    }
                }
            }
        }
    }

    private fun navigateToRecommendationFromWidget() {
        val emotionStorageManager = EmotionStorageManager(this@MainActivity)
        val lastEmotionResult = emotionStorageManager.getLastEmotionResult()

        val bundle = Bundle().apply {
            if (lastEmotionResult != null) {
                putString("emotion", lastEmotionResult.emotion)
                putStringArrayList("recommendations", ArrayList(lastEmotionResult.recommendations))
            } else {
                putString("emotion", "neutral")
                putStringArrayList("recommendations", arrayListOf(
                    getString(R.string.default_recommendation_1),
                    getString(R.string.default_recommendation_2),
                    getString(R.string.default_recommendation_3)
                ))
            }
        }

        val recommendationFragment = RecommendationFragment().apply {
            arguments = bundle
        }

        replaceFragment(
            recommendationFragment,
            showToolbar = true,
            tag = "RecommendationFragment"
        )
    }

    private fun requestAudioPermissionForRecording() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.RECORD_AUDIO),
                AUDIO_PERMISSION_REQUEST_CODE
            )
        }
    }

    private val emotionIconRunnable = object : Runnable {
        override fun run() {
            changeEmotionIconRandomly()
            emotionIconHandler.postDelayed(this, 5000)
        }
    }

    private fun changeEmotionIconRandomly() {
        val randomIndex = (0 until emotionIcons.size).random()
        val randomEmotionIcon = emotionIcons[randomIndex]

        runOnUiThread {
            binding.navEmotion.findViewById<ImageView>(R.id.emotion_icon).apply {
                setImageResource(randomEmotionIcon)
            }
        }
    }

    override fun setToolbarVisibility(visible: Boolean, title: String?) {
        if (visible) {
            binding.toolbar.visibility = View.VISIBLE
            binding.toolbarBorder.visibility = View.VISIBLE
            title?.let { binding.titleText.text = it }
        } else {
            binding.toolbar.visibility = View.GONE
            binding.toolbarBorder.visibility = View.GONE
        }
    }

    private fun setupUI() {
        val rotateAnimation = AnimationUtils.loadAnimation(this, R.anim.rotate_gradient)
        binding.gradientIconView.startAnimation(rotateAnimation)

        setupBottomNavigation()
        setupSettingsButton()
        setupSidebarNavigation()
        updateLanguageIcon()
    }

    private fun setupDailyRecapAlarm() {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, AiDailyRecapService::class.java)
        val pendingIntent = PendingIntent.getService(
            this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 0)

            if (before(Calendar.getInstance())) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        alarmManager.setRepeating(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            AlarmManager.INTERVAL_DAY,
            pendingIntent
        )
    }

    private fun setupDailyReminderAlarm(enable: Boolean) {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, DailyReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            this,
            DailyReminderReceiver.REMINDER_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        if (enable) {
            val calendar = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, DailyReminderReceiver.REMINDER_HOUR)
                set(Calendar.MINUTE, DailyReminderReceiver.REMINDER_MINUTE)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)

                if (before(Calendar.getInstance())) {
                    add(Calendar.DAY_OF_YEAR, 1)
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (!alarmManager.canScheduleExactAlarms()) {
                    Toast.makeText(
                        this,
                        getString(R.string.exact_alarm_permission_required),
                        Toast.LENGTH_LONG
                    ).show()
                    val settingsIntent =
                        Intent(android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                    startActivity(settingsIntent)
                    return
                }
            }

            alarmManager.setExact(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                pendingIntent
            )
            val timeFormat = String.format(
                Locale.getDefault(),
                "%02d:%02d",
                DailyReminderReceiver.REMINDER_HOUR,
                DailyReminderReceiver.REMINDER_MINUTE
            )
            Toast.makeText(
                this,
                getString(R.string.daily_reminder_enabled, timeFormat),
                Toast.LENGTH_SHORT
            ).show()
        } else {
            alarmManager.cancel(pendingIntent)
            Toast.makeText(this, getString(R.string.daily_reminder_disabled), Toast.LENGTH_SHORT).show()
        }
        sessionManager.setDailyReminderEnabled(enable)
    }


    @SuppressLint("ClickableViewAccessibility")
    private fun setupBottomNavigation() {
        binding.navChat.setOnClickListener {
            setActiveNavigation(R.id.nav_chat)
            navigateToChatFragment()
        }

        binding.navVoice.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    if (!isRecording) {
                        startVoiceRecording()
                    }
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    if (isRecording) {
                        stopVoiceRecording()
                    }
                    true
                }
                else -> false
            }
        }

        binding.navEmotion.setOnClickListener {
            setActiveNavigation(R.id.nav_emotion)
            navigateToEmotionCheck()
        }
    }

    private fun startVoiceRecording() {
        if (voiceRecordingManager.hasAudioPermission()) {
            voiceRecordingManager.startRecording()
        } else {
            requestAudioPermissionForRecording()
        }
    }

    private fun stopVoiceRecording() {
        if (voiceRecordingManager.isCurrentlyRecording()) {
            voiceRecordingManager.stopRecording()
        }
    }


    internal fun setActiveNavigation(navId: Int) {
        activeNavItem = navId
        updateNavigationState()
    }

    private fun updateNavigationState() {
        binding.navChat.isSelected = false
        binding.navVoice.isSelected = false
        binding.navEmotion.isSelected = false

        binding.navChat.alpha = if (activeNavItem == R.id.nav_chat) 0.7f else 0.7f
        binding.navVoice.alpha = if (activeNavItem == R.id.nav_voice) 1.0f else 1.0f
        binding.navEmotion.alpha = if (activeNavItem == R.id.nav_emotion) 0.7f else 0.7f

        when (activeNavItem) {
            R.id.nav_chat -> binding.navChat.isSelected = true
            R.id.nav_voice -> binding.navVoice.isSelected = true
            R.id.nav_emotion -> binding.navEmotion.isSelected = true
        }
    }

    private fun setupSettingsButton() {
        binding.settingsButton.setOnClickListener {
            val drawer = binding.drawerLayout
            if (drawer.isDrawerOpen(GravityCompat.END)) {
                drawer.closeDrawer(GravityCompat.END)
            } else {
                hideKeyboard()
                drawer.openDrawer(GravityCompat.END)
            }
        }
    }

    private fun hideKeyboard() {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        currentFocus?.windowToken?.let {
            imm.hideSoftInputFromWindow(it, 0)
        }
    }

    private fun setupSidebarNavigation() {
        drawerMenuBinding.tvAppVersion.text = getString(R.string.app_version, BuildConfig.VERSION_NAME)
        drawerMenuBinding.navProfile.setOnClickListener {
            binding.drawerLayout.closeDrawer(GravityCompat.END)
            replaceFragment(ProfileFragment(), showToolbar = false, tag = "ProfileFragment")
        }
        drawerMenuBinding.navRecommendation.setOnClickListener {
            binding.drawerLayout.closeDrawer(GravityCompat.END)

            val emotionStorageManager = EmotionStorageManager(this@MainActivity)
            val lastEmotionResult = emotionStorageManager.getLastEmotionResult()

            val bundle = Bundle().apply {
                if (lastEmotionResult != null) {
                    putString("emotion", lastEmotionResult.emotion)
                    putStringArrayList("recommendations", ArrayList(lastEmotionResult.recommendations))
                } else {
                    putString("emotion", "neutral")
                    putStringArrayList("recommendations", arrayListOf(
                        getString(R.string.default_recommendation_1),
                        getString(R.string.default_recommendation_2),
                        getString(R.string.default_recommendation_3)
                    ))
                }
            }

            val recommendationFragment = RecommendationFragment().apply {
                arguments = bundle
            }

            replaceFragment(
                recommendationFragment,
                showToolbar = true,
                tag = "RecommendationFragment"
            )
        }
        drawerMenuBinding.navLanguage.setOnClickListener {
            binding.drawerLayout.closeDrawer(GravityCompat.END)
            LanguageFragment().show(supportFragmentManager, LanguageFragment.TAG)
        }
        drawerMenuBinding.navAbout.setOnClickListener {
            binding.drawerLayout.closeDrawer(GravityCompat.END)
            replaceFragment(AboutUsFragment(), showToolbar = false, tag = "AboutUsFragment")
        }
        drawerMenuBinding.navLogout.setOnClickListener {
            binding.drawerLayout.closeDrawer(GravityCompat.END)
            showLogoutConfirmationDialog()
        }
        drawerMenuBinding.switchNotification.isChecked = sessionManager.isDailyReminderEnabled()

        drawerMenuBinding.switchNotification.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    if (ContextCompat.checkSelfPermission(
                            this,
                            android.Manifest.permission.POST_NOTIFICATIONS
                        ) == PackageManager.PERMISSION_GRANTED
                    ) {
                        setupDailyReminderAlarm(true)
                    } else {
                        requestPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                    }
                } else {
                    setupDailyReminderAlarm(true)
                }
            } else {
                setupDailyReminderAlarm(false)
            }
        }
    }

    private val cameraLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.extras?.let { bundle ->
                val emotion = bundle.getString("emotion", "")
                val confidence = bundle.getFloat("confidence", 0f)
                val description = bundle.getString("description", "")
                val recommendations = bundle.getStringArrayList("recommendations") ?: arrayListOf()

                Log.d("YourFragment", "Received from camera:")
                Log.d("YourFragment", "  - emotion: $emotion")
                Log.d("YourFragment", "  - confidence: $confidence")
                Log.d("YourFragment", "  - description: $description")
                Log.d("YourFragment", "  - recommendations count: ${recommendations.size}")
                recommendations.forEachIndexed { index, rec ->
                    Log.d("YourFragment", "  - recommendation $index: $rec")
                }

                navigateToEmotionResult(emotion, confidence, description, recommendations)
            }
        }
    }

    private fun navigateToEmotionResult(
        emotion: String,
        confidence: Float,
        description: String,
        recommendations: ArrayList<String>
    ) {
        val bundle = Bundle().apply {
            putString("emotion", emotion)
            putFloat("confidence", confidence)
            putString("description", description)
            putStringArrayList("recommendations", recommendations)
        }

        val emotionResultFragment = EmotionResultFragment()
        emotionResultFragment.arguments = bundle

        replaceFragment(
            emotionResultFragment,
            showToolbar = false,
            tag = "EmotionResultFragment"
        )
    }

    internal fun navigateToEmotionCheck() {
        val intent = Intent(this, CameraActivity::class.java)
        cameraLauncher.launch(intent)
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
    }

    internal fun replaceFragment(fragment: Fragment, showToolbar: Boolean, tag: String? = null) {
        if (fragment is RecapFragment) {
            setToolbarVisibility(false)
            binding.root.findViewById<View>(R.id.main_layout_root).background =
                ContextCompat.getDrawable(this, R.drawable.bg_gradient_secondary)
        }
        if (fragment is EmotionResultFragment) {
            setToolbarVisibility(false)
            binding.root.findViewById<View>(R.id.main_layout_root).background =
                ContextCompat.getDrawable(this, R.drawable.bg_gradient_main)
        } else if (fragment is ProfileFragment || fragment is AboutUsFragment || fragment is RecommendationFragment) {
            setToolbarVisibility(false)
            binding.root.findViewById<View>(R.id.main_layout_root).background =
                ContextCompat.getDrawable(this, R.drawable.bg_gradient_secondary)
        } else {
            setToolbarVisibility(showToolbar)
            binding.root.findViewById<View>(R.id.main_layout_root).background =
                ContextCompat.getDrawable(this, R.drawable.bg_gradient_main)
        }

        val fragmentManager = supportFragmentManager
        val currentFragment = fragmentManager.findFragmentById(R.id.main_content_fragment_container)

        if (currentFragment != null && currentFragment::class.java == fragment::class.java &&
            currentFragment.tag == tag) {
            fragmentManager.popBackStack(tag, FragmentManager.POP_BACK_STACK_INCLUSIVE)
            fragmentManager.beginTransaction()
                .setCustomAnimations(
                    R.anim.slide_in_bottom, R.anim.slide_out_bottom,
                    R.anim.slide_in_top, R.anim.slide_out_top
                )
                .replace(R.id.main_content_fragment_container, fragment, tag)
                .addToBackStack(tag)
                .commit()
            return
        }

        val transaction = fragmentManager.beginTransaction()
            .setCustomAnimations(
                R.anim.slide_in_bottom,
                R.anim.slide_out_bottom,
                R.anim.slide_in_top,
                R.anim.slide_out_top
            )
            .replace(R.id.main_content_fragment_container, fragment, tag)

        if (tag != null) {
            transaction.addToBackStack(tag)
        }
        transaction.commit()
    }


    fun showLogoutConfirmationDialog() {
        ConfirmationDialogFragment.newInstance(
            title = getString(R.string.logout_title),
            message = getString(R.string.logout_message),
            onYes = {
                performLogout()
            },
            onNo = {},
            onDismiss = {}
        ).show(supportFragmentManager, ConfirmationDialogFragment.TAG)
    }

    private fun performLogout() {
        try {
            setupDailyReminderAlarm(false)
            sessionManager.logoutUser()
            val intent = Intent(this, AuthActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
            finish()
        } catch (e: Exception) {
            Toast.makeText(this, getString(R.string.logout_failed), Toast.LENGTH_SHORT).show()
        }
    }

    private val onBackStackChangedListener = OnBackStackChangedListener {
        val currentFragment =
            supportFragmentManager.findFragmentById(R.id.main_content_fragment_container)

        if (currentFragment != null && currentFragment.isAdded) {
            when (currentFragment) {
                is ProfileFragment -> {
                    setToolbarVisibility(false)
                    binding.bottomNavigation.visibility = View.GONE
                    binding.root.findViewById<View>(R.id.main_layout_root).background =
                        ContextCompat.getDrawable(this, R.drawable.bg_gradient_secondary)
                }

                is RecommendationFragment, is AboutUsFragment -> {
                    setToolbarVisibility(false)
                    binding.bottomNavigation.visibility = View.VISIBLE
                    binding.root.findViewById<View>(R.id.main_layout_root).background =
                        ContextCompat.getDrawable(this, R.drawable.bg_gradient_secondary)
                }

                is RecapFragment -> {
                    setToolbarVisibility(true)
                    binding.bottomNavigation.visibility = View.VISIBLE
                }

                is ChatFragment -> {
                    setToolbarVisibility(false)
                    setActiveNavigation(R.id.nav_chat)
                    binding.bottomNavigation.visibility = View.GONE
                    binding.root.findViewById<View>(R.id.main_layout_root).background =
                        ContextCompat.getDrawable(this, R.drawable.bg_gradient_main)
                }

                is EmotionResultFragment -> {
                    setToolbarVisibility(false)
                    setActiveNavigation(R.id.nav_emotion)
                    binding.bottomNavigation.visibility = View.VISIBLE
                    binding.root.findViewById<View>(R.id.main_layout_root).background =
                        ContextCompat.getDrawable(this, R.drawable.bg_gradient_main)
                }

                is HomeFragment -> {
                    setToolbarVisibility(true)
                    binding.bottomNavigation.visibility = View.VISIBLE
                    binding.root.findViewById<View>(R.id.main_layout_root).background =
                        ContextCompat.getDrawable(this, R.drawable.bg_gradient_main)
                }

                else -> {
                    setToolbarVisibility(true)
                    binding.bottomNavigation.visibility = View.VISIBLE
                    binding.root.findViewById<View>(R.id.main_layout_root).background =
                        ContextCompat.getDrawable(this, R.drawable.bg_gradient_main)
                }
            }
        }
    }

    internal fun navigateToChatFragment(voiceMode: Boolean = false, audioFile: File? = null) {
        Log.d("MainActivity", "navigateToChatFragment called - voiceMode: $voiceMode, audioFile: ${audioFile?.absolutePath}")

        setToolbarVisibility(false)
        binding.bottomNavigation.visibility = View.GONE

        val chatFragment = ChatFragment().apply {
            arguments = Bundle().apply {
                audioFile?.let {
                    Log.d("MainActivity", "Adding voice_file_path to bundle: ${it.absolutePath}")
                    putString("voice_file_path", it.absolutePath)
                }
                putBoolean("voice_mode", voiceMode)
                putBoolean("show_keyboard", !voiceMode)
            }
        }

        val fragmentManager = supportFragmentManager
        fragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
        replaceFragment(chatFragment, showToolbar = false, tag = "ChatFragment")

        Log.d("MainActivity", "Fragment navigation completed")
    }

    override fun onBackPressed() {
        val fragmentManager = supportFragmentManager
        val currentFragment = fragmentManager.findFragmentById(R.id.main_content_fragment_container)

        if (binding.drawerLayout.isDrawerOpen(GravityCompat.END)) {
            binding.drawerLayout.closeDrawer(GravityCompat.END)
            hideKeyboard()
            return
        }

        if (currentFragment != null && currentFragment !is HomeFragment) {
            val homeFragmentFound = fragmentManager.backStackEntryCount > 0 &&
                    (0 until fragmentManager.backStackEntryCount).any {
                        fragmentManager.getBackStackEntryAt(it).name == "HomeFragment"
                    }

            if (homeFragmentFound) {
                fragmentManager.popBackStack("HomeFragment", 0)
                return
            } else {
                if (fragmentManager.backStackEntryCount > 0) {
                    super.onBackPressed()
                    return
                }
            }

            if (fragmentManager.backStackEntryCount <= 1 || currentFragment is HomeFragment) {
                hideKeyboard()
                showExitConfirmationDialog()
            } else {
                hideKeyboard()
                super.onBackPressed()
            }
        } else {
            hideKeyboard()
            showExitConfirmationDialog()
        }

        if (fragmentManager.backStackEntryCount <= 1 || currentFragment is HomeFragment) {
            showExitConfirmationDialog()
        } else {
            super.onBackPressed()
        }
    }

    private fun animateVoiceRecordOverlay(show: Boolean) {
        val overlay = binding.voiceRecordOverlay
        if (show) {
            overlay.visibility = View.VISIBLE
            val animationSet = AnimationSet(true)

            // Slide up animation
            val slideUp = TranslateAnimation(
                Animation.RELATIVE_TO_SELF, 0f,
                Animation.RELATIVE_TO_SELF, 0f,
                Animation.RELATIVE_TO_SELF, 1f,
                Animation.RELATIVE_TO_SELF, 0f
            )
            slideUp.duration = 300
            animationSet.addAnimation(slideUp)

            // Fade in animation
            val fadeIn = AlphaAnimation(0f, 1f)
            fadeIn.duration = 300
            animationSet.addAnimation(fadeIn)

            overlay.startAnimation(animationSet)
        } else {
            val animationSet = AnimationSet(true)

            // Slide down animation
            val slideDown = TranslateAnimation(
                Animation.RELATIVE_TO_SELF, 0f,
                Animation.RELATIVE_TO_SELF, 0f,
                Animation.RELATIVE_TO_SELF, 0f,
                Animation.RELATIVE_TO_SELF, 1f
            )
            slideDown.duration = 300
            animationSet.addAnimation(slideDown)

            // Fade out animation
            val fadeOut = AlphaAnimation(1f, 0f)
            fadeOut.duration = 300
            animationSet.addAnimation(fadeOut)

            animationSet.setAnimationListener(object : Animation.AnimationListener {
                override fun onAnimationStart(animation: Animation?) {}
                override fun onAnimationEnd(animation: Animation?) {
                    overlay.visibility = View.GONE
                }
                override fun onAnimationRepeat(animation: Animation?) {}
            })
            overlay.startAnimation(animationSet)
        }
    }

    // Animation for the "..." text
    private fun startDotsAnimation() {
        dotCount = 0
        dotsRunnable = object : Runnable {
            override fun run() {
                dotCount = (dotCount + 1) % 4 // Cycle through 0, 1, 2, 3 dots
                val dots = when (dotCount) {
                    0 -> ""
                    1 -> "."
                    2 -> ".."
                    else -> "..."
                }
                binding.textRecordingDots.text = dots
                dotsHandler.postDelayed(this, 500) // Update every 500ms
            }
        }
        dotsHandler.post(dotsRunnable!!)
    }

    private fun stopDotsAnimation() {
        dotsRunnable?.let { dotsHandler.removeCallbacks(it) }
        binding.textRecordingDots.text = "" // Clear dots when not recording
    }

    private fun showExitConfirmationDialog() {
        ConfirmationDialogFragment.newInstance(
            title = getString(R.string.exit_app_title),
            message = getString(R.string.exit_app_message),
            onYes = {
                finishAffinity()
            },
            onNo = {},
            onDismiss = {}
        ).show(supportFragmentManager, ConfirmationDialogFragment.TAG)
    }

    companion object {
        private const val AUDIO_PERMISSION_REQUEST_CODE = 1002
    }

    override fun onResume() {
        super.onResume()
        updateNavigationState()
        emotionIconHandler.post(emotionIconRunnable)
        restartGradientAnimation()
    }

    private fun restartGradientAnimation() {
        binding.gradientIconView.clearAnimation()
        val rotateAnimation = AnimationUtils.loadAnimation(this, R.anim.rotate_gradient)
        binding.gradientIconView.startAnimation(rotateAnimation)
    }

    override fun onPause() {
        super.onPause()
        if (binding.drawerLayout.isDrawerOpen(GravityCompat.END)) {
            binding.drawerLayout.closeDrawer(GravityCompat.END, false)
        }
        if (isRecording) {
            voiceRecordingManager.cancelRecording()
            binding.voiceRecordOverlay.visibility = View.GONE
        }
        emotionIconHandler.removeCallbacks(emotionIconRunnable)
    }

    override fun onDestroy() {
        super.onDestroy()
        binding.gradientIconView.clearAnimation()
        supportFragmentManager.removeOnBackStackChangedListener(onBackStackChangedListener)
        voiceRecordingManager.destroy()
    }

    private fun updateLanguageIcon() {
        val currentLanguage = sessionManager.getLanguage()
        if (currentLanguage == "id") {
            drawerMenuBinding.navLanguage.findViewById<ImageView>(R.id.imageView)
                ?.setImageResource(R.drawable.ic_flag_indonesian)
        } else {
            drawerMenuBinding.navLanguage.findViewById<ImageView>(R.id.imageView)
                ?.setImageResource(R.drawable.ic_flag_united_states)
        }
    }

    private fun applyLanguage(languageCode: String?) {
        val locale = Locale(languageCode ?: "id")
        Locale.setDefault(locale)
        val config = Configuration()
        config.setLocale(locale)
        resources.updateConfiguration(config, resources.displayMetrics)
    }

    override fun onLanguageChanged() {
        applyLanguage(sessionManager.getLanguage())
        updateLanguageIcon()
        recreate()
    }

    private fun updateNotificationIcon(isChecked: Boolean) {
        iconNotification = drawerMenuBinding.root.findViewById(R.id.iv_notification)
        val iconRes = if (isChecked) {
            R.drawable.ic_notification_active
        } else {
            R.drawable.ic_notification_slient
        }
        iconNotification.setImageResource(iconRes)
    }

    private fun setupDailyReminderSwitch() {
        drawerMenuBinding.switchNotification.isChecked = sessionManager.isDailyReminderEnabled()
        updateNotificationIcon(drawerMenuBinding.switchNotification.isChecked)

        drawerMenuBinding.switchNotification.setOnCheckedChangeListener { _, isChecked ->
            updateNotificationIcon(isChecked)
            if (isChecked) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    if (ContextCompat.checkSelfPermission(
                            this,
                            android.Manifest.permission.POST_NOTIFICATIONS
                        ) == PackageManager.PERMISSION_GRANTED
                    ) {
                        setupDailyReminderAlarm(true)
                    } else {
                        requestPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                    }
                } else {
                    setupDailyReminderAlarm(true)
                }
            } else {
                setupDailyReminderAlarm(false)
            }
        }
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        gestureDetector.onTouchEvent(ev)
        return super.dispatchTouchEvent(ev)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == AUDIO_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startVoiceRecording()
            } else {
                Toast.makeText(this, "Izin rekaman suara diperlukan", Toast.LENGTH_SHORT).show()
            }
        }
    }

    inner class SwipeGestureListener : GestureDetector.SimpleOnGestureListener() {
        private val SWIPE_THRESHOLD = 100
        private val SWIPE_VELOCITY_THRESHOLD = 100

        override fun onFling(
            e1: MotionEvent?, e2: MotionEvent, velocityX: Float, velocityY: Float
        ): Boolean {
            if (e1 == null) return false

            val diffX = e2.x - e1.x
            val diffY = e2.y - e1.y

            if (Math.abs(diffX) > Math.abs(diffY)
                && Math.abs(diffX) > SWIPE_THRESHOLD
                && Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD
            ) {
                if (diffX > 0) { // Swipe Right
                    Log.d("SwipeGesture", "Swipe Right detected, calling onBackPressed")
                    onBackPressed()
                    return true
                } else { // Swipe Left
                    Log.d("SwipeGesture", "Swipe Left detected, opening drawer")
                    binding.drawerLayout.openDrawer(GravityCompat.END)
                    return true
                }
            }
            return false
        }
    }
}