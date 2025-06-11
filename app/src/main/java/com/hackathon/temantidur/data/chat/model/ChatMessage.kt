package com.hackathon.temantidur.data.chat.model

import java.text.SimpleDateFormat
import java.util.*

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val message: String,
    val isIncoming: Boolean,
    val timestamp: Long = System.currentTimeMillis(),
    val isTypingIndicator: Boolean = false,
    val isVoiceMessage: Boolean = false,
    val voiceFilePath: String? = null,
    val voiceDuration: Long = 0L
) {
    fun getFormattedTime(): String {
        val dateFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        return dateFormat.format(Date(timestamp))
    }

    fun getFormattedDate(): String {
        val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale("id", "ID"))
        return dateFormat.format(Date(timestamp))
    }

    fun isToday(): Boolean {
        val today = Calendar.getInstance()
        val messageDate = Calendar.getInstance().apply { timeInMillis = timestamp }
        return today.get(Calendar.YEAR) == messageDate.get(Calendar.YEAR) &&
                today.get(Calendar.DAY_OF_YEAR) == messageDate.get(Calendar.DAY_OF_YEAR)
    }

    fun isYesterday(): Boolean {
        val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
        val messageDate = Calendar.getInstance().apply { timeInMillis = timestamp }
        return yesterday.get(Calendar.YEAR) == messageDate.get(Calendar.YEAR) &&
                yesterday.get(Calendar.DAY_OF_YEAR) == messageDate.get(Calendar.DAY_OF_YEAR)
    }
}