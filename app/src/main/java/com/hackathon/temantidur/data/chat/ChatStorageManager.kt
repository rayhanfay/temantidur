package com.hackathon.temantidur.data.chat


import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.hackathon.temantidur.data.chat.model.ChatMessage
import com.hackathon.temantidur.data.chat.model.DailyRecap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ChatStorageManager(context: Context) {
    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()

    companion object {
        private const val PREF_NAME = "chat_storage"
        private const val KEY_CHAT_MESSAGES = "chat_messages"
        private const val KEY_LAST_CHAT_DATE = "last_chat_date"
        private const val KEY_DAILY_RECAPS = "daily_recaps"
    }

    suspend fun saveChatMessages(messages: List<ChatMessage>) {
        withContext(Dispatchers.IO) {
            val json = gson.toJson(messages)
            sharedPreferences.edit()
                .putString(KEY_CHAT_MESSAGES, json)
                .putLong(KEY_LAST_CHAT_DATE, System.currentTimeMillis())
                .apply()
        }
    }

    suspend fun saveDailyRecap(recap: DailyRecap) {
        val recaps = loadDailyRecaps().toMutableList()
        recaps.removeAll { it.date == recap.date }
        recaps.add(recap)

        val json = gson.toJson(recaps)
        sharedPreferences.edit()
            .putString(KEY_DAILY_RECAPS, json)
            .apply()
    }

    suspend fun loadDailyRecaps(): List<DailyRecap> {
        return withContext(Dispatchers.IO) {
            try {
                val json = sharedPreferences.getString(KEY_DAILY_RECAPS, null)
                if (json != null) {
                    val type = object : TypeToken<List<DailyRecap>>() {}.type
                    gson.fromJson<List<DailyRecap>>(json, type) ?: emptyList()
                } else {
                    emptyList()
                }
            } catch (e: Exception) {
                emptyList()
            }
        }
    }

    suspend fun loadChatMessages(): List<ChatMessage> {
        return withContext(Dispatchers.IO) {
            try {
                val json = sharedPreferences.getString(KEY_CHAT_MESSAGES, null)
                if (json != null) {
                    val type = object : TypeToken<List<ChatMessage>>() {}.type
                    gson.fromJson<List<ChatMessage>>(json, type) ?: emptyList()
                } else {
                    emptyList()
                }
            } catch (e: Exception) {
                emptyList()
            }
        }
    }

    suspend fun addMessage(message: ChatMessage) {
        val messages = loadChatMessages().toMutableList()
        messages.add(message)
        saveChatMessages(messages)
    }

    suspend fun clearChatHistory() {
        withContext(Dispatchers.IO) {
            sharedPreferences.edit()
                .remove(KEY_CHAT_MESSAGES)
                .remove(KEY_LAST_CHAT_DATE)
                .apply()
        }
    }

    suspend fun clearAllChatData() {
        withContext(Dispatchers.IO) {
            sharedPreferences.edit()
                .remove(KEY_CHAT_MESSAGES)
                .remove(KEY_LAST_CHAT_DATE)
                .remove(KEY_DAILY_RECAPS)
                .apply()
        }
    }

    fun getLastChatDate(): Long {
        return sharedPreferences.getLong(KEY_LAST_CHAT_DATE, 0)
    }

    fun hasChatHistory(): Boolean {
        return sharedPreferences.contains(KEY_CHAT_MESSAGES)
    }
}