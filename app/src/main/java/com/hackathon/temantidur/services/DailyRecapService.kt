package com.hackathon.temantidur.services

import android.content.Context
import android.content.Intent
import androidx.core.app.JobIntentService
import com.hackathon.temantidur.data.chat.ChatStorageManager
import com.hackathon.temantidur.data.chat.model.DailyRecap
import com.hackathon.temantidur.utils.RecapGenerator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class DailyRecapService : JobIntentService() {

    override fun onHandleWork(intent: Intent) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                generateDailyRecap()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private suspend fun generateDailyRecap() {
        val chatStorageManager = ChatStorageManager(this)
        val recapGenerator = RecapGenerator(this)

        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, -1)

        val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
        val dayFormat = SimpleDateFormat("EEEE", Locale("id", "ID"))

        val yesterdayDate = dateFormat.format(calendar.time)
        val yesterdayLabel = dayFormat.format(calendar.time)

        val allMessages = chatStorageManager.loadChatMessages()
        val yesterdayMessages = allMessages.filter { message ->
            val messageCalendar = Calendar.getInstance()
            messageCalendar.timeInMillis = message.timestamp

            messageCalendar.get(Calendar.YEAR) == calendar.get(Calendar.YEAR) &&
                    messageCalendar.get(Calendar.DAY_OF_YEAR) == calendar.get(Calendar.DAY_OF_YEAR)
        }

        if (yesterdayMessages.isNotEmpty()) {
            val summary = recapGenerator.generateDailyRecap(yesterdayMessages)
            val recap = DailyRecap(
                date = yesterdayDate,
                dayLabel = yesterdayLabel,
                summary = summary.toString()
            )

            chatStorageManager.saveDailyRecap(recap)
        }
    }
}