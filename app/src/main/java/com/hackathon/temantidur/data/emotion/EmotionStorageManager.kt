package com.hackathon.temantidur.data.emotion

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.hackathon.temantidur.data.emotion.model.EmotionResult

class EmotionStorageManager(private val context: Context) {

    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences("emotion_storage", Context.MODE_PRIVATE)
    private val gson = Gson()

    companion object {
        private const val KEY_LAST_EMOTION_RESULT = "last_emotion_result"
        private const val KEY_LAST_ANALYSIS_TIME = "last_analysis_time"
        private const val TAG = "EmotionStorageManager"
    }

    fun saveLastEmotionResult(emotionResult: EmotionResult) {
        try {
            val json = gson.toJson(emotionResult)
            val currentTime = System.currentTimeMillis()

            sharedPreferences.edit().apply {
                putString(KEY_LAST_EMOTION_RESULT, json)
                putLong(KEY_LAST_ANALYSIS_TIME, currentTime)
                apply()
            }

            Log.d(TAG, "Last emotion result saved successfully")
            Log.d(TAG, "Emotion: ${emotionResult.emotion}")
            Log.d(TAG, "Recommendations count: ${emotionResult.recommendations.size}")
        } catch (e: Exception) {
            Log.e(TAG, "Error saving emotion result", e)
        }
    }

    fun getLastEmotionResult(): EmotionResult? {
        return try {
            val json = sharedPreferences.getString(KEY_LAST_EMOTION_RESULT, null)
            if (json != null) {
                val emotionResult = gson.fromJson(json, EmotionResult::class.java)
                Log.d(TAG, "Retrieved last emotion result: ${emotionResult.emotion}")
                Log.d(TAG, "Recommendations count: ${emotionResult.recommendations.size}")
                emotionResult
            } else {
                Log.d(TAG, "No last emotion result found")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error retrieving emotion result", e)
            null
        }
    }

    fun clearAllEmotionData() {
        sharedPreferences.edit()
            .remove(KEY_LAST_EMOTION_RESULT)
            .remove(KEY_LAST_ANALYSIS_TIME)
            .apply()
    }
}