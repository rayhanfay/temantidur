package com.hackathon.temantidur.presentation.voicechat

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat
import java.io.File
import java.io.IOException

class VoiceRecordingManager(private val context: Context) {

    private var mediaRecorder: MediaRecorder? = null
    private var audioFile: File? = null
    private var isRecording = false

    companion object {
        private const val TAG = "VoiceRecordingManager"
        private const val SAMPLE_RATE = 44100
        private const val BIT_RATE = 128000
    }

    interface RecordingListener {
        fun onRecordingStarted()
        fun onRecordingStopped(audioFile: File)
        fun onRecordingError(error: String)
        fun onPermissionRequired()
    }

    private var listener: RecordingListener? = null

    fun setRecordingListener(listener: RecordingListener) {
        this.listener = listener
    }

    fun hasAudioPermission(): Boolean {
        return ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun startRecording() {
        if (!hasAudioPermission()) {
            listener?.onPermissionRequired()
            return
        }

        if (isRecording) {
            Log.w(TAG, "Already recording")
            return
        }

        try {
            audioFile = createAudioFile()
            mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }

            mediaRecorder?.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)

                setAudioSamplingRate(SAMPLE_RATE)
                setAudioEncodingBitRate(BIT_RATE)
                setOutputFile(audioFile?.absolutePath)

                prepare()
                start()

                isRecording = true
                listener?.onRecordingStarted()
                Log.d(TAG, "Recording started: ${audioFile?.absolutePath}")
            }

        } catch (e: IOException) {
            Log.e(TAG, "Failed to start recording", e)
            listener?.onRecordingError("Gagal memulai perekaman: ${e.message}")
            cleanup()
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error during recording start", e)
            listener?.onRecordingError("Terjadi kesalahan: ${e.message}")
            cleanup()
        }
    }

    fun stopRecording() {
        if (!isRecording) {
            Log.w(TAG, "Not currently recording")
            return
        }

        try {
            mediaRecorder?.apply {
                stop()
                release()
            }

            mediaRecorder = null
            isRecording = false

            audioFile?.let { file ->
                if (file.exists() && file.length() > 0) {
                    Log.d(TAG, "Recording stopped successfully: ${file.absolutePath}, size: ${file.length()}")
                    listener?.onRecordingStopped(file)
                } else {
                    Log.e(TAG, "Audio file is empty or doesn't exist")
                    listener?.onRecordingError("File audio kosong atau tidak ditemukan")
                }
            } ?: run {
                listener?.onRecordingError("File audio tidak ditemukan")
            }

        } catch (e: RuntimeException) {
            Log.e(TAG, "Failed to stop recording", e)
            listener?.onRecordingError("Gagal menghentikan perekaman: ${e.message}")
            cleanup()
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error during recording stop", e)
            listener?.onRecordingError("Terjadi kesalahan: ${e.message}")
            cleanup()
        }
    }

    fun cancelRecording() {
        if (!isRecording) {
            return
        }

        try {
            mediaRecorder?.apply {
                stop()
                release()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error during recording cancellation", e)
        }

        cleanup()
        audioFile?.let { file ->
            if (file.exists()) {
                file.delete()
                Log.d(TAG, "Audio file deleted after cancellation")
            }
        }
    }

    fun isCurrentlyRecording(): Boolean = isRecording

    private fun createAudioFile(): File {
        val audioDir = File(context.filesDir, "audio")
        if (!audioDir.exists()) {
            audioDir.mkdirs()
        }

        val timestamp = System.currentTimeMillis()
        return File(audioDir, "voice_message_$timestamp.m4a")
    }

    private fun cleanup() {
        try {
            mediaRecorder?.release()
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing MediaRecorder", e)
        }

        mediaRecorder = null
        isRecording = false
    }

    fun destroy() {
        if (isRecording) {
            cancelRecording()
        }
        cleanup()
    }
}