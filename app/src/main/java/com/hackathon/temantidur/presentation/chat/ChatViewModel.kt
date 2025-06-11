package com.hackathon.temantidur.presentation.chat

import android.app.Application
import android.content.Context
import android.media.MediaPlayer
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hackathon.temantidur.common.ApiResult
import com.hackathon.temantidur.data.chat.ChatStorageManager
import com.hackathon.temantidur.data.chat.TemanTidurRepository
import com.hackathon.temantidur.data.chat.model.ChatMessage
import com.hackathon.temantidur.data.chat.model.DailyRecap
import com.hackathon.temantidur.data.chat.model.VoiceApiResponse
import com.hackathon.temantidur.utils.RecapGenerator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.delay
import java.util.UUID

class ChatViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = TemanTidurRepository(application)
    private val storageManager = ChatStorageManager(application)
    private val _chatState = MutableStateFlow<ApiResult<String>>(ApiResult.Loading())
    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages
    private val _isTyping = MutableStateFlow(false)
    val isTyping: StateFlow<Boolean> = _isTyping
    private val _voiceResponse = MutableStateFlow<VoiceApiResponse?>(null)
    val voiceResponse: StateFlow<VoiceApiResponse?> = _voiceResponse
    fun voiceResponseHandled() {
        _voiceResponse.value = null
    }

    private var authInitJob: Job? = null
    private var tokenCheckJob: Job? = null
    private val chatStorageManager = ChatStorageManager(application)
    private val recapGenerator = RecapGenerator(application)

    private val _voiceMessageAddedEvent = MutableSharedFlow<File>()
    val voiceMessageAddedEvent: SharedFlow<File> = _voiceMessageAddedEvent

    init {
        initializeAuthentication()
        startPeriodicTokenCheck()
        checkApiConnection()
        loadChatHistory()
    }

    private fun loadChatHistory() {
        viewModelScope.launch {
            try {
                val savedMessages = storageManager.loadChatMessages()
                _chatMessages.value = savedMessages
                Log.d("ChatViewModel", "Loaded chat history. Size: ${savedMessages.size}")

                if (savedMessages.isEmpty()) {
                    addWelcomeMessages()
                }
            } catch (e: Exception) {
                Log.e("ChatViewModel", "Error loading chat history", e)
                addWelcomeMessages()
            }
        }
    }

    private suspend fun addWelcomeMessages() {
        val welcomeMessages = listOf(
            ChatMessage(
                message = "Halo! Selamat datang di Teman Tidur 😊",
                isIncoming = true
            ),
            ChatMessage(
                message = "Aku di sini untuk mendengarkan ceritamu. Ada yang ingin kamu ceritakan hari ini?",
                isIncoming = true
            )
        )

        _chatMessages.value = welcomeMessages
        storageManager.saveChatMessages(welcomeMessages)
        Log.d("ChatViewModel", "Added welcome messages.")
    }

    private fun getAudioDuration(file: File): Long {
        return try {
            val mediaPlayer = MediaPlayer()
            mediaPlayer.setDataSource(file.absolutePath)
            mediaPlayer.prepare()
            val duration = mediaPlayer.duration.toLong()
            mediaPlayer.release()
            duration
        } catch (e: Exception) {
            Log.e("ChatViewModel", "Error getting audio duration for ${file.name}: ${e.message}", e)
            0L
        }
    }

    private suspend fun addMessageToChat(message: ChatMessage) {
        val currentMessages = _chatMessages.value.toMutableList()
        currentMessages.add(message)
        _chatMessages.value = currentMessages.toList()
        storageManager.saveChatMessages(currentMessages)
        Log.d(
            "ChatViewModel",
            "Message added to chat: '${message.message.take(30)}...'. New size: ${_chatMessages.value.size}"
        )
    }

    private fun getErrorMessage(error: ApiResult.Error<*>): String {
        return when {
            error.message.contains("Authentication") -> {
                "Session berakhir. Silakan login ulang untuk melanjutkan chat."
            }

            error.message.contains("Network") -> {
                "Koneksi bermasalah. Cek internet kamu ya! 🌙"
            }

            error.message.contains("cancelled", ignoreCase = true) -> {
                "Pesan dibatalkan"
            }

            error.code == 429 -> {
                "Kamu terlalu cepat mengirim pesan. Tunggu sebentar ya! 🌙"
            }

            else -> {
                "Maaf, aku sedang bermasalah. Coba kirim pesan lagi ya! 🌙"
            }
        }
    }

    fun handleInitialMessage(initialMessage: String?) {
        viewModelScope.launch {
            if (!initialMessage.isNullOrEmpty()) {
                val messages = mutableListOf<ChatMessage>()
                messages.add(
                    ChatMessage(
                        message = "Halo! Terima kasih sudah berbagi ceritamu 😊",
                        isIncoming = true
                    )
                )
                messages.add(
                    ChatMessage(
                        message = initialMessage,
                        isIncoming = false
                    )
                )

                _chatMessages.value = messages
                storageManager.saveChatMessages(messages)

                sendMessage(initialMessage)
            }
        }
    }

    fun clearChatHistory() {
        viewModelScope.launch {
            storageManager.clearChatHistory()
            _chatMessages.value = emptyList()
            addWelcomeMessages()
            Log.d("ChatViewModel", "Chat history cleared.")
        }
    }

    private fun initializeAuthentication() {
        authInitJob = viewModelScope.launch {
            try {
                val authInitialized = repository.initializeAuth()
                if (!authInitialized) {
                    _chatState.value = ApiResult.Error("Authentication failed. Please login again.")
                    Log.e("ChatViewModel", "Authentication failed during initialization.")
                } else {
                    Log.d("ChatViewModel", "Authentication initialized successfully.")
                }
            } catch (e: Exception) {
                _chatState.value = ApiResult.Error("Authentication failed: ${e.message}")
                Log.e(
                    "ChatViewModel",
                    "Exception during authentication initialization: ${e.message}",
                    e
                )
            }
        }
    }

    private fun startPeriodicTokenCheck() {
        tokenCheckJob = viewModelScope.launch {
            try {
                while (true) {
                    delay(10 * 60 * 1000L)
                    repository.checkAndRefreshToken()
                    Log.d("ChatViewModel", "Token refreshed (if needed).")
                }
            } catch (e: Exception) {
                Log.e("ChatViewModel", "Token check failed: ${e.message}", e)
            }
        }
    }

    private fun checkApiConnection() {
        viewModelScope.launch {
            try {
                repository.checkApiStatus()
                Log.d("ChatViewModel", "API connection checked. Status OK.")
            } catch (e: Exception) {
                Log.e("ChatViewModel", "API connection check failed: ${e.message}", e)
            }
        }
    }

    fun generateWeeklyRecaps() {
        viewModelScope.launch {
            try {
                val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
                val dayFormat = SimpleDateFormat("EEEE", Locale("id", "ID"))
                val allMessages = chatStorageManager.loadChatMessages()

                for (i in 0..6) {
                    val calendar = Calendar.getInstance()
                    calendar.add(Calendar.DAY_OF_YEAR, -i)

                    val targetDate = dateFormat.format(calendar.time)
                    val targetDayLabel = dayFormat.format(calendar.time)

                    val dayMessages = allMessages.filter { message ->
                        val messageCalendar = Calendar.getInstance()
                        messageCalendar.timeInMillis = message.timestamp

                        messageCalendar.get(Calendar.YEAR) == calendar.get(Calendar.YEAR) &&
                                messageCalendar.get(Calendar.DAY_OF_YEAR) == calendar.get(Calendar.DAY_OF_YEAR)
                    }

                    if (dayMessages.isNotEmpty()) {
                        val summary = recapGenerator.generateDailyRecap(dayMessages)

                        val recap = DailyRecap(
                            date = targetDate,
                            dayLabel = targetDayLabel,
                            summary = summary.toString()
                        )

                        chatStorageManager.saveDailyRecap(recap)
                        Log.d("ChatViewModel", "Recap generated for $targetDate")
                    }
                }

            } catch (e: Exception) {
                Log.e("ChatViewModel", "Error generating weekly recaps", e)
            }
        }
    }

    fun sendMessage(message: String) {
        viewModelScope.launch {
            val userMessage = ChatMessage(
                id = UUID.randomUUID().toString(),
                message = message,
                isIncoming = false
            )
            addMessageToChat(userMessage)

            _isTyping.value = true
            _chatState.value = ApiResult.Loading()
            Log.d("ChatViewModel", "Sending text message: '${message.take(30)}...'")

            val result = repository.sendChatMessage(message)
            _isTyping.value = false
            _chatState.value = result

            when (result) {
                is ApiResult.Success -> {
                    val aiMessage = ChatMessage(
                        message = result.data,
                        isIncoming = true
                    )
                    addMessageToChat(aiMessage)
                    Log.d(
                        "ChatViewModel",
                        "Received AI text response: '${result.data.take(30)}...'"
                    )
                    generateTodayRecap()
                }

                is ApiResult.Error -> {
                    val errorMessage = getErrorMessage(result)
                    val aiMessage = ChatMessage(
                        message = errorMessage,
                        isIncoming = true
                    )
                    addMessageToChat(aiMessage)
                    Log.e("ChatViewModel", "Error sending text message: ${result.message}")
                }

                else -> {
                    // Do nothing for Loading state here
                }
            }
        }
    }

    fun sendVoiceMessage(audioFile: File) {
        if (_isTyping.value) {
            Log.d("ChatViewModel", "Already processing a message, ignoring voice message")
            return
        }

        viewModelScope.launch {
            try {
                _isTyping.value = true
                _chatState.value = ApiResult.Loading()

                val duration = getAudioDuration(audioFile)
                val voiceMessage = ChatMessage(
                    message = "🎤 Pesan Suara",
                    isIncoming = false,
                    timestamp = System.currentTimeMillis(),
                    isVoiceMessage = true,
                    voiceFilePath = audioFile.absolutePath,
                    voiceDuration = duration
                )
                Log.d(
                    "ChatViewModel",
                    "Before adding user voice message, current chatMessages size: ${_chatMessages.value.size}"
                )
                addMessageToChat(voiceMessage)
                Log.d(
                    "ChatViewModel",
                    "After adding user voice message, new chatMessages size: ${_chatMessages.value.size}"
                )

                if (_voiceMessageAddedEvent.subscriptionCount.value > 0) {
                    _voiceMessageAddedEvent.emit(audioFile)
                    Log.d("ChatViewModel", "Emitted voiceMessageAddedEvent to MainActivity.")
                }


                val result = repository.sendVoiceMessage(audioFile)

                when (result) {
                    is ApiResult.Success -> {
                        val responseData = result.data
                        responseData.aiText?.let {
                            val aiResponseMessage = ChatMessage(
                                id = UUID.randomUUID().toString(),
                                message = it,
                                isIncoming = true,
                                timestamp = System.currentTimeMillis()
                            )
                            Log.d(
                                "ChatViewModel",
                                "Before adding AI response message, current chatMessages size: ${_chatMessages.value.size}"
                            )
                            addMessageToChat(aiResponseMessage)
                            Log.d(
                                "ChatViewModel",
                                "After adding AI response message, new chatMessages size: ${_chatMessages.value.size}"
                            )
                            Log.d(
                                "ChatViewModel",
                                "Received AI voice text response: '${it.take(30)}...'"
                            )
                        }
                        _voiceResponse.value = responseData
                        _chatState.value = ApiResult.Success("Success")
                        Log.d(
                            "ChatViewModel",
                            "Received AI voice audio data. Size: ${responseData.audioData?.size ?: 0}"
                        )

                        generateTodayRecap()
                    }

                    is ApiResult.Error -> {
                        val errorMessageText = getErrorMessage(result)
                        val errorMessage = ChatMessage(
                            id = UUID.randomUUID().toString(),
                            message = errorMessageText,
                            isIncoming = true,
                            timestamp = System.currentTimeMillis()
                        )
                        addMessageToChat(errorMessage)
                        _chatState.value = ApiResult.Error(result.message, result.code)
                        Log.e("ChatViewModel", "Error sending voice message: ${result.message}")
                    }

                    else -> {
                        // Do nothing for Loading state here }
                    }
                }

            } catch (e: Exception) {
                Log.e("ChatViewModel", "Unexpected error sending voice message: ${e.message}", e)
                val errorMessage = ChatMessage(
                    message = "Terjadi kesalahan saat mengirim pesan suara",
                    isIncoming = true,
                    timestamp = System.currentTimeMillis()
                )
                addMessageToChat(errorMessage)
                _chatState.value = ApiResult.Error("Terjadi kesalahan: ${e.message}")
            } finally {
                _isTyping.value = false
            }
        }
    }

    fun generateTodayRecap() {
        viewModelScope.launch {
            val messages = chatStorageManager.loadChatMessages()
            val recapGenerator = RecapGenerator(context = getApplication<Application>())
            val recap = recapGenerator.generateDailyRecap(messages)
            chatStorageManager.saveDailyRecap(recap)
        }
    }

    fun cleanup() {
        authInitJob?.cancel()
        tokenCheckJob?.cancel()
        repository.cleanup()
        repository.clearAuthCache()
    }

    override fun onCleared() {
        super.onCleared()
        cleanup()
    }
}