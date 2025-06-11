package com.hackathon.temantidur.presentation.voicechat

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Context
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.animation.AccelerateDecelerateInterpolator
import com.hackathon.temantidur.R
import com.hackathon.temantidur.databinding.FragmentChatBinding
import java.io.File

class VoiceChatHandlerFragment(
    private val binding: FragmentChatBinding,
    private val context: Context,
    private val listener: VoiceChatListener
) {

    private val voiceRecordingManager = VoiceRecordingManager(context)
    private var isRecording = false
    private var pulseAnimator: AnimatorSet? = null
    private var vibrator: Vibrator? = null

    interface VoiceChatListener {
        fun onVoiceMessageSent(audioFile: File)
        fun onVoiceError(error: String)
        fun onPermissionRequired()
    }

    init {
        vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        setupVoiceRecording()
    }

    private fun setupVoiceRecording() {
        voiceRecordingManager.setRecordingListener(object : VoiceRecordingManager.RecordingListener {
            override fun onRecordingStarted() {
                isRecording = true
                showRecordingUI()
                startRecordingAnimation()
                vibratePhone(50)
            }

            override fun onRecordingStopped(audioFile: File) {
                isRecording = false
                hideRecordingUI()
                stopRecordingAnimation()
                listener.onVoiceMessageSent(audioFile)
                vibratePhone(25)
            }

            override fun onRecordingError(error: String) {
                isRecording = false
                hideRecordingUI()
                stopRecordingAnimation()
                listener.onVoiceError(error)
            }

            override fun onPermissionRequired() {
                listener.onPermissionRequired()
            }
        })
    }

    fun startRecording() {
        if (!voiceRecordingManager.hasAudioPermission()) {
            listener.onPermissionRequired()
            return
        }

        voiceRecordingManager.startRecording()
    }

    fun stopRecording() {
        if (isRecording) {
            voiceRecordingManager.stopRecording()
        }
    }

    private fun showRecordingUI() {
        val micButton = binding.btnMic
        micButton.setBackgroundResource(R.drawable.rotating_gradient_circle)
        micButton.animate()
            .scaleX(1.2f)
            .scaleY(1.2f)
            .setDuration(200)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .start()
    }

    private fun hideRecordingUI() {
        val micButton = binding.btnMic
        micButton.setBackgroundResource(R.drawable.rotating_gradient_circle)
        micButton.animate()
            .scaleX(1.0f)
            .scaleY(1.0f)
            .setDuration(200)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .start()
    }

    private fun startRecordingAnimation() {
        stopRecordingAnimation()

        val micButton = binding.btnMic
        val scaleXAnimator = ObjectAnimator.ofFloat(micButton, "scaleX", 1.0f, 1.3f, 1.0f)
        val scaleYAnimator = ObjectAnimator.ofFloat(micButton, "scaleY", 1.0f, 1.3f, 1.0f)
        val alphaAnimator = ObjectAnimator.ofFloat(micButton, "alpha", 1.0f, 0.7f, 1.0f)

        pulseAnimator = AnimatorSet().apply {
            playTogether(scaleXAnimator, scaleYAnimator, alphaAnimator)
            duration = 1000
            interpolator = AccelerateDecelerateInterpolator()

            addListener(object : android.animation.Animator.AnimatorListener {
                override fun onAnimationStart(animation: android.animation.Animator) {}
                override fun onAnimationEnd(animation: android.animation.Animator) {
                    if (isRecording) {
                        start()
                    }
                }
                override fun onAnimationCancel(animation: android.animation.Animator) {}
                override fun onAnimationRepeat(animation: android.animation.Animator) {}
            })

            start()
        }
    }

    private fun stopRecordingAnimation() {
        pulseAnimator?.cancel()
        pulseAnimator = null

        val micButton = binding.btnMic

        micButton.alpha = 1.0f
        micButton.scaleX = 1.0f
        micButton.scaleY = 1.0f
    }

    private fun vibratePhone(duration: Long) {
        vibrator?.let { vib ->
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                vib.vibrate(VibrationEffect.createOneShot(duration, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vib.vibrate(duration)
            }
        }
    }

    fun isCurrentlyRecording(): Boolean = isRecording

    fun handlePermissionResult(grantResults: IntArray) {
        if (grantResults.isNotEmpty() && grantResults[0] == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            // Permission granted, can start recording if needed
        } else {
            listener.onVoiceError("Izin mikrofon diperlukan untuk merekam pesan suara")
        }
    }

    fun cleanup() {
        stopRecordingAnimation()
        voiceRecordingManager.destroy()
    }
}