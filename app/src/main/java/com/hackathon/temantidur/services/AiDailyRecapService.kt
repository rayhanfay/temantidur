package com.hackathon.temantidur.services

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.JobIntentService
import com.hackathon.temantidur.data.chat.ChatStorageManager
import com.hackathon.temantidur.data.chat.TemanTidurRepository
import com.hackathon.temantidur.data.chat.model.DailyRecap
import com.hackathon.temantidur.data.chat.model.Message
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.text.SimpleDateFormat
import java.util.*

class AiDailyRecapService : JobIntentService() {

    companion object {
        private const val JOB_ID = 1234
        private const val TAG = "AiDailyRecapService"

        fun enqueueWork(context: Context, work: Intent) {
            enqueueWork(context, AiDailyRecapService::class.java, JOB_ID, work)
        }
    }

    override fun onHandleWork(intent: Intent) {
        Log.d(TAG, "Starting automatic daily recap generation...")
        runBlocking {
            generateRecapForYesterday()
        }
    }

    private suspend fun generateRecapForYesterday() {
        val repository = TemanTidurRepository(this)
        val storageManager = ChatStorageManager(this)

        // Initialize auth token before making API call
        val authSuccess = repository.initializeAuth()
        if (!authSuccess) {
            Log.e(TAG, "Failed to initialize auth for recap service.")
            return
        }

        val calendar = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
        val yesterdayDateStr = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(calendar.time)

        val allMessages = storageManager.loadChatMessages()
        val yesterdayMessages = allMessages.filter {
            val msgCal = Calendar.getInstance().apply { timeInMillis = it.timestamp }
            msgCal.get(Calendar.YEAR) == calendar.get(Calendar.YEAR) &&
                    msgCal.get(Calendar.DAY_OF_YEAR) == calendar.get(Calendar.DAY_OF_YEAR)
        }

        if (yesterdayMessages.isEmpty()) {
            Log.d(TAG, "No messages found for yesterday ($yesterdayDateStr). Skipping recap.")
            return
        }

        val apiMessages = yesterdayMessages.map { Message(if (it.isIncoming) "assistant" else "user", it.message) }

        when (val result = repository.getRecapFromApi(yesterdayDateStr, apiMessages)) {
            is com.hackathon.temantidur.common.ApiResult.Success -> {
                val recapData = result.data
                val dayLabel = SimpleDateFormat("EEEE", this.resources.configuration.locales[0]).format(calendar.time)
                val dailyRecap = DailyRecap(
                    date = recapData.date,
                    dayLabel = dayLabel,
                    summary = recapData.recap
                )
                storageManager.saveDailyRecap(dailyRecap)
                Log.d(TAG, "Successfully generated and saved recap for $yesterdayDateStr")
            }
            is com.hackathon.temantidur.common.ApiResult.Error -> {
                Log.e(TAG, "Failed to generate recap from API: ${result.message}")
            }
            else -> {}
        }
    }
}