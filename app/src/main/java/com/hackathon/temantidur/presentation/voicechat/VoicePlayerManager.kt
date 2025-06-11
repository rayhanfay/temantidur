package com.hackathon.temantidur.presentation.voicechat

import android.media.MediaPlayer
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.IOException

object VoicePlayerManager {

    private const val TAG = "VoicePlayerManager"
    private var mediaPlayer: MediaPlayer? = null
    private var progressJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main)

    private val _playbackState = MutableStateFlow<PlaybackState>(PlaybackState.Stopped)
    val playbackState = _playbackState.asStateFlow()

    private val _currentPlayingAudioUrl = MutableStateFlow<String?>(null)
    val currentPlayingAudioUrl = _currentPlayingAudioUrl.asStateFlow()

    private val _currentProgress = MutableStateFlow(0)
    val currentProgress = _currentProgress.asStateFlow()

    private val _currentDuration = MutableStateFlow(0)
    val currentDuration = _currentDuration.asStateFlow()

    sealed class PlaybackState {
        object Playing : PlaybackState()
        object Paused : PlaybackState()
        object Stopped : PlaybackState()
        data class Error(val message: String) : PlaybackState()
    }

    fun playOrPause(audioUrl: String) {
        Log.d(TAG, "playOrPause called for: ${audioUrl.takeLast(20)}")
        Log.d(TAG, "Current state: ${_playbackState.value}, Current URL: ${_currentPlayingAudioUrl.value?.takeLast(20)}")

        if (currentPlayingAudioUrl.value == audioUrl && _playbackState.value is PlaybackState.Playing) {
            Log.d(TAG, "Pausing current audio")
            pause()
        } else if (currentPlayingAudioUrl.value == audioUrl && _playbackState.value is PlaybackState.Paused) {
            Log.d(TAG, "Resuming paused audio")
            resume()
        } else {
            Log.d(TAG, "Starting new audio")
            start(audioUrl)
        }
    }

    private fun start(audioUrl: String) {
        Log.d(TAG, "Starting audio: ${audioUrl.takeLast(20)}")
        stop()
        _currentPlayingAudioUrl.value = audioUrl

        try {
            mediaPlayer = MediaPlayer().apply {
                reset()

                setOnPreparedListener { mp ->
                    Log.d(TAG, "MediaPlayer prepared, duration: ${mp.duration}")
                    _currentDuration.value = mp.duration
                    _playbackState.value = PlaybackState.Playing
                    mp.start()
                    startProgressUpdates()
                }

                setOnCompletionListener {
                    Log.d(TAG, "Playback completed")
                    stop()
                }

                setOnErrorListener { mp, what, extra ->
                    Log.e(TAG, "MediaPlayer Error: What $what, Extra $extra")
                    val errorMsg = when (what) {
                        MediaPlayer.MEDIA_ERROR_UNKNOWN -> "Unknown error"
                        MediaPlayer.MEDIA_ERROR_SERVER_DIED -> "Server died"
                        else -> "Error code: $what"
                    }
                    _playbackState.value = PlaybackState.Error("$errorMsg (Extra: $extra)")
                    stop()
                    true
                }

                val file = java.io.File(audioUrl)
                if (!file.exists() || file.length() == 0L) {
                    Log.e(TAG, "Audio file does not exist or is empty: $audioUrl")
                    _playbackState.value = PlaybackState.Error("Audio file not found or empty")
                    return
                }

                Log.d(TAG, "Setting data source: $audioUrl, file size: ${file.length()}")
                setDataSource(audioUrl)
                prepareAsync()
            }
        } catch (e: IOException) {
            Log.e(TAG, "IOException while setting data source", e)
            _playbackState.value = PlaybackState.Error("Failed to load audio: ${e.message}")
            stop()
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error while starting playback", e)
            _playbackState.value = PlaybackState.Error("Unexpected error: ${e.message}")
            stop()
        }
    }

    private fun resume() {
        mediaPlayer?.let { mp ->
            try {
                if (!mp.isPlaying) {
                    Log.d(TAG, "Resuming playback")
                    _playbackState.value = PlaybackState.Playing
                    mp.start()
                    startProgressUpdates()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error resuming playback", e)
                _playbackState.value = PlaybackState.Error("Failed to resume: ${e.message}")
            }
        }
    }

    private fun pause() {
        mediaPlayer?.let { mp ->
            try {
                if (mp.isPlaying) {
                    Log.d(TAG, "Pausing playback")
                    mp.pause()
                    _playbackState.value = PlaybackState.Paused
                    progressJob?.cancel()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error pausing playback", e)
                _playbackState.value = PlaybackState.Error("Failed to pause: ${e.message}")
            }
        }
    }

    fun stop() {
        Log.d(TAG, "Stopping playback")
        progressJob?.cancel()

        mediaPlayer?.let { mp ->
            try {
                if (mp.isPlaying) {
                    mp.stop()
                }
                mp.release()
            } catch (e: Exception) {
                Log.e(TAG, "Error stopping MediaPlayer", e)
            }
        }

        mediaPlayer = null
        _playbackState.value = PlaybackState.Stopped
        _currentPlayingAudioUrl.value = null
        _currentProgress.value = 0
        _currentDuration.value = 0
        Log.d(TAG, "Playback stopped and reset")
    }

    fun seekTo(position: Int) {
        mediaPlayer?.let { mp ->
            try {
                mp.seekTo(position)
                _currentProgress.value = position
                Log.d(TAG, "Seeked to position: $position")
            } catch (e: Exception) {
                Log.e(TAG, "Error seeking to position: $position", e)
            }
        }
    }

    private fun startProgressUpdates() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (isActive && mediaPlayer?.isPlaying == true) {
                try {
                    val currentPos = mediaPlayer?.currentPosition ?: 0
                    _currentProgress.value = currentPos
                    Log.v(TAG, "Progress update: $currentPos")
                    delay(100) // Update setiap 100ms
                } catch (e: Exception) {
                    Log.e(TAG, "Error updating progress", e)
                    break
                }
            }
        }
    }
}