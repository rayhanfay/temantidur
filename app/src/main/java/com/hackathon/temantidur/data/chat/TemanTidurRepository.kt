package com.hackathon.temantidur.data.chat

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.hackathon.temantidur.common.ApiResult
import com.hackathon.temantidur.data.chat.api.TemanTidurApiService
import com.hackathon.temantidur.data.chat.model.ChatRequest
import com.hackathon.temantidur.data.chat.model.Message
import com.hackathon.temantidur.data.auth.AuthManager
import com.hackathon.temantidur.data.auth.SessionManager
import com.hackathon.temantidur.data.chat.model.VoiceApiResponse
import com.hackathon.temantidur.utils.AudioConverter
import com.hackathon.temantidur.data.chat.model.RecapRequest
import com.hackathon.temantidur.data.chat.model.RecapResponse
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File

class TemanTidurRepository(private val context: Context) {
    private val api: TemanTidurApiService
    private val authManager = AuthManager()
    private val audioConverter = AudioConverter(context)
    private val conversationHistory = mutableListOf<Message>()
    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val sessionManager = SessionManager(context)

    companion object {
        private const val BASE_URL = com.hackathon.temantidur.BuildConfig.BASE_URL
        private const val CONNECT_TIMEOUT = 30L
        private const val READ_TIMEOUT = 60L
        private const val WRITE_TIMEOUT = 30L
    }

    init {
        val loggingInterceptor = HttpLoggingInterceptor { message ->
            Log.d("HTTP_LOG", message)
        }.apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(CONNECT_TIMEOUT, TimeUnit.SECONDS)
            .readTimeout(READ_TIMEOUT, TimeUnit.SECONDS)
            .writeTimeout(WRITE_TIMEOUT, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val request = chain.request()
                val response = chain.proceed(request)
                Log.d("API_RESPONSE", "${request.method} ${request.url} -> ${response.code}")
                if (!response.isSuccessful) {
                    val errorBody = response.peekBody(2048).string()
                    Log.e("API_ERROR_BODY", "Error response: $errorBody")
                }
                response
            }
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        api = retrofit.create(TemanTidurApiService::class.java)
    }

    suspend fun sendChatMessage(message: String): ApiResult<String> {
        Log.d("DEBUG_REPO", "Sending message, history size = ${conversationHistory.size}")

        return try {
            // Try to send message with current token
            val result = sendChatMessageInternal(message)

            // If we get 401, try to refresh token and retry once
            if (result is ApiResult.Error && result.code == 401) {
                Log.d("TemanTidurRepository", "Got 401, attempting token refresh and retry...")
                val refreshedToken = authManager.forceRefreshToken()

                if (refreshedToken != null) {
                    // Retry with refreshed token
                    sendChatMessageInternal(message)
                } else {
                    ApiResult.Error("Authentication failed. Please login again.")
                }
            } else {
                result
            }
        } catch (e: CancellationException) {
            Log.d("TemanTidurRepository", "Chat message request was cancelled")
            ApiResult.Error("Request was cancelled")
        } catch (e: Exception) {
            Log.e("TemanTidurRepository", "Network exception: ${e.message}", e)
            ApiResult.Error("Network error: ${e.message}")
        }
    }

    private suspend fun sendChatMessageInternal(message: String): ApiResult<String> {
        val token = authManager.getValidToken()
        if (token == null) {
            Log.e("TemanTidurRepository", "Failed to get valid token")
            return ApiResult.Error("Authentication failed. Please login again.")
        }

        // Add user message to history
        val userMessage = Message(role = "user", content = message)
        conversationHistory.add(userMessage)

        val chatRequest = ChatRequest(
            messages = conversationHistory.toList()
        )

        // Log the request for debugging
        Log.d("TemanTidurRepository", "Request payload: ${Gson().toJson(chatRequest)}")
        Log.d("TemanTidurRepository", "Authorization: Bearer ${token.take(20)}...")

        // Show token expiry info for debugging
        val remainingTime = authManager.getRemainingTokenTime()
        Log.d("TemanTidurRepository", "Token remaining time: ${remainingTime / 1000 / 60} minutes")

        // Make the API call
        val response = api.sendChat("Bearer $token", chatRequest)

        return if (response.isSuccessful) {
            val chatResponse = response.body()
            val reply = chatResponse?.reply

            Log.d("TemanTidurRepository", "Successful response: $reply")

            if (!reply.isNullOrEmpty()) {
                // Add assistant response to conversation history
                conversationHistory.add(Message(role = "assistant", content = reply))
                ApiResult.Success(reply)
            } else {
                Log.e("TemanTidurRepository", "Empty or null reply from API")
                ApiResult.Error("Empty response from AI")
            }
        } else {
            val errorBody = response.errorBody()?.string()
            Log.e("TemanTidurRepository", "API Error - Code: ${response.code()}")
            Log.e("TemanTidurRepository", "API Error - Body: $errorBody")

            // If we got 401, remove the user message we just added
            if (response.code() == 401 && conversationHistory.isNotEmpty()) {
                val lastMessage = conversationHistory.last()
                if (lastMessage.role == "user" && lastMessage.content == message) {
                    conversationHistory.removeAt(conversationHistory.size - 1)
                }
            }

            ApiResult.Error(
                message = getErrorMessage(response.code(), errorBody),
                code = response.code()
            )
        }
    }

    suspend fun sendVoiceMessage(audioFile: File): ApiResult<VoiceApiResponse> {
        Log.d("DEBUG_REPO", "Sending voice message, file size = ${audioFile.length()}")
        return try {
            val result = sendVoiceMessageInternal(audioFile)
            if (result is ApiResult.Error && result.code == 401) {
                Log.d("TemanTidurRepository", "Got 401 for voice message, attempting token refresh and retry...")
                val refreshedToken = authManager.forceRefreshToken()
                if (refreshedToken != null) {
                    sendVoiceMessageInternal(audioFile)
                } else {
                    ApiResult.Error("Authentication failed. Please login again.")
                }
            } else {
                result
            }
        } catch (e: CancellationException) {
            Log.d("TemanTidurRepository", "Voice message request was cancelled")
            ApiResult.Error("Request was cancelled")
        } catch (e: Exception) {
            Log.e("TemanTidurRepository", "Network exception for voice message: ${e.message}", e)
            ApiResult.Error("Network error: ${e.message}")
        }
    }

    private suspend fun sendVoiceMessageInternal(audioFile: File): ApiResult<VoiceApiResponse> {
        val token = authManager.getValidToken()
        if (token == null) {
            Log.e("TemanTidurRepository", "Failed to get valid token for voice message")
            return ApiResult.Error("Authentication failed. Please login again.")
        }
        if (!audioFile.exists() || audioFile.length() == 0L) {
            Log.e("TemanTidurRepository", "Audio file is empty or doesn't exist")
            return ApiResult.Error("File audio tidak valid")
        }
        Log.d("TemanTidurRepository", "Converting audio to WAV format...")
        val wavFile = withContext(Dispatchers.IO) { audioConverter.convertToWav(audioFile) }
        if (wavFile == null || !wavFile.exists()) {
            Log.e("TemanTidurRepository", "Failed to convert audio to WAV")
            return ApiResult.Error("Gagal mengkonversi audio ke format WAV")
        }
        Log.d("TemanTidurRepository", "Audio conversion successful: ${wavFile.absolutePath}, size: ${wavFile.length()}")
        try {
            val mimeType = "audio/wav"
            val requestFile = wavFile.asRequestBody(mimeType.toMediaTypeOrNull())
            val audioPart = MultipartBody.Part.createFormData("audio", wavFile.name, requestFile)
            Log.d("TemanTidurRepository", "Sending voice message - WAV File: ${wavFile.name}, Size: ${wavFile.length()}, MIME: $mimeType")
            Log.d("TemanTidurRepository", "Authorization: Bearer ${token.take(20)}...")
            val remainingTime = authManager.getRemainingTokenTime()
            Log.d("TemanTidurRepository", "Token remaining time: ${remainingTime / 1000 / 60} minutes")

            val response = api.sendVoiceChat("Bearer $token", audioPart)

            return if (response.isSuccessful) {
                val aiText = response.headers()["x-ai-text"]
                val userText = response.headers()["x-user-text"]

                val audioData = response.body()?.bytes()

                Log.d("TemanTidurRepository", "Voice message successful - AI text: $aiText")
                Log.d("TemanTidurRepository", "Voice message successful - User text: $userText")
                Log.d("TemanTidurRepository", "Voice message successful - Audio data size: ${audioData?.size ?: 0} bytes")

                if (!aiText.isNullOrEmpty()) {
                    val voiceMessageContent = if (!userText.isNullOrEmpty() && userText != "Error processing audio") userText else "[Voice Message]"
                    conversationHistory.add(Message(role = "user", content = voiceMessageContent))
                    conversationHistory.add(Message(role = "assistant", content = aiText))

                    // *** ✨ KEMBALIKAN OBJECT VoiceApiResponse ✨ ***
                    val voiceResponse = VoiceApiResponse(userText, aiText, audioData)
                    ApiResult.Success(voiceResponse)
                } else {
                    Log.e("TemanTidurRepository", "Empty AI text in response headers")
                    ApiResult.Error("Empty response from AI")
                }
            } else {
                val errorBody = response.errorBody()?.string()
                Log.e("TemanTidurRepository", "Voice API Error - Code: ${response.code()}")
                Log.e("TemanTidurRepository", "Voice API Error - Body: $errorBody")
                ApiResult.Error(
                    message = getErrorMessage(response.code(), errorBody),
                    code = response.code()
                )
            }
        } finally {
            try {
                if (wavFile.exists()) {
                    wavFile.delete()
                    Log.d("TemanTidurRepository", "Cleaned up converted WAV file")
                }
            } catch (e: Exception) {
                Log.w("TemanTidurRepository", "Failed to clean up WAV file: ${e.message}")
            }
        }
    }

    suspend fun checkApiStatus(): ApiResult<String> {
        return try {
            Log.d("TemanTidurRepository", "Checking API status...")
            val response = api.getRoot()

            if (response.isSuccessful) {
                val message = response.body()?.message ?: "API is working"
                Log.d("TemanTidurRepository", "API Status OK: $message")
                ApiResult.Success(message)
            } else {
                Log.e("TemanTidurRepository", "API status check failed: ${response.code()}")
                ApiResult.Error("API not responding properly")
            }
        } catch (e: CancellationException) {
            Log.d("TemanTidurRepository", "API status check was cancelled")
            ApiResult.Error("Request was cancelled")
        } catch (e: Exception) {
            Log.e("TemanTidurRepository", "API status check exception: ${e.message}", e)
            ApiResult.Error("Failed to connect to API: ${e.message}")
        }
    }

    suspend fun initializeAuth(): Boolean {
        return try {
            val token = authManager.initializeToken()
            token != null
        } catch (e: CancellationException) {
            Log.d("TemanTidurRepository", "Auth initialization was cancelled")
            false
        } catch (e: Exception) {
            Log.e("TemanTidurRepository", "Failed to initialize auth: ${e.message}")
            false
        }
    }

    suspend fun checkAndRefreshToken(): Boolean {
        return try {
            if (authManager.isTokenExpiringSoon()) {
                Log.d("TemanTidurRepository", "Token expiring soon, refreshing...")
                authManager.forceRefreshToken() != null
            } else {
                true
            }
        } catch (e: CancellationException) {
            Log.d("TemanTidurRepository", "Token refresh check was cancelled")
            false
        } catch (e: Exception) {
            Log.e("TemanTidurRepository", "Token refresh check failed: ${e.message}")
            false
        }
    }

    suspend fun getRecapFromApi(date: String, messages: List<Message>): ApiResult<RecapResponse> {
        // Ambil bahasa dari SessionManager
        val language = sessionManager.getLanguage() ?: "id"

        return try {
            // Ubah pemanggilan ke getRecapInternal
            val result = getRecapInternal(date, messages, language)
            if (result is ApiResult.Error && result.code == 401) {
                Log.d("TemanTidurRepository", "Recap failed with 401, refreshing token...")
                val refreshedToken = authManager.forceRefreshToken()
                if (refreshedToken != null) {
                    getRecapInternal(date, messages, language)
                } else {
                    ApiResult.Error("Authentication failed. Please login again.", 401)
                }
            } else {
                result
            }
        } catch (e: Exception) {
            Log.e("TemanTidurRepository", "Exception in getRecapFromApi: ${e.message}", e)
            ApiResult.Error("Network error: ${e.message}")
        }
    }

    private suspend fun getRecapInternal(date: String, messages: List<Message>, language: String): ApiResult<RecapResponse> {
        val token = authManager.getValidToken() ?: return ApiResult.Error("Authentication token is missing.", 401)

        val request = RecapRequest(date = date, messages = messages, language = language)
        Log.d("TemanTidurRepository", "Sending recap request for language: $language")
        val response = api.getRecap("Bearer $token", request)

        return if (response.isSuccessful) {
            response.body()?.let { ApiResult.Success(it) }
                ?: ApiResult.Error("Empty recap response body.")
        } else {
            ApiResult.Error(
                message = getErrorMessage(response.code(), response.errorBody()?.string()),
                code = response.code()
            )
        }
    }

    fun clearConversationHistory() {
        Log.d("TemanTidurRepository", "Clearing conversation history of ${conversationHistory.size} messages")
        conversationHistory.clear()
    }

    fun getConversationHistory(): List<Message> {
        return conversationHistory.toList()
    }

    fun cleanup() {
        authManager.cleanup()
        repositoryScope.cancel()
    }

    fun clearAuthCache() {
        authManager.clearTokenCache()
    }

    private fun getErrorMessage(code: Int, errorBody: String? = null): String {
        return when (code) {
            400 -> context.getString(com.hackathon.temantidur.R.string.error_bad_request)
            401 -> context.getString(com.hackathon.temantidur.R.string.error_session_expired)
            403 -> context.getString(com.hackathon.temantidur.R.string.error_access_denied)
            429 -> context.getString(com.hackathon.temantidur.R.string.error_too_many_requests)
            500 -> context.getString(com.hackathon.temantidur.R.string.error_server_error)
            502, 503, 504 -> context.getString(com.hackathon.temantidur.R.string.error_server_maintenance)
            else -> {
                if (!errorBody.isNullOrEmpty()) {
                    try {
                        val gson = Gson()
                        val errorResponse = gson.fromJson(errorBody, Map::class.java)
                        val errorMessage = errorResponse["message"] as? String
                        if (!errorMessage.isNullOrEmpty()) {
                            return errorMessage
                        }
                    } catch (e: Exception) {
                        Log.w("TemanTidurRepository", "Could not parse error body: $errorBody")
                    }

                    when {
                        errorBody.contains("sorry, I am having issues", ignoreCase = true) ->
                            context.getString(com.hackathon.temantidur.R.string.error_ai_issues)
                        errorBody.contains("timeout", ignoreCase = true) ->
                            context.getString(com.hackathon.temantidur.R.string.error_timeout)
                        else -> context.getString(com.hackathon.temantidur.R.string.error_generic, code)
                    }
                } else {
                    context.getString(com.hackathon.temantidur.R.string.error_generic, code)
                }
            }
        }
    }
}