package com.hackathon.temantidur.data.auth

import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.tasks.await
import android.util.Log
import kotlinx.coroutines.*

class AuthManager {
    private var cachedToken: String? = null
    private var tokenExpiry: Long = 0
    private var refreshJob: Job? = null

    private val authScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    companion object {
        private const val TOKEN_EXPIRY_BUFFER = 5 * 60 * 1000L // 5 minutes buffer before actual expiry
        private const val TOKEN_REFRESH_INTERVAL = 50 * 60 * 1000L // 50 minutes (Firebase tokens last ~1 hour)
    }

    suspend fun getValidToken(): String? {
        // Check if token is still valid (with buffer time)
        if (cachedToken != null && System.currentTimeMillis() < (tokenExpiry - TOKEN_EXPIRY_BUFFER)) {
            return cachedToken
        }

        // Token is expired or about to expire, refresh it
        return refreshToken()
    }

    private suspend fun refreshToken(): String? {
        return try {
            val user = FirebaseAuth.getInstance().currentUser
            if (user != null) {
                Log.d("AuthManager", "Refreshing token...")
                val task = user.getIdToken(true).await()
                cachedToken = task.token
                tokenExpiry = System.currentTimeMillis() + TOKEN_REFRESH_INTERVAL

                // Schedule next refresh
                scheduleTokenRefresh()

                Log.d("AuthManager", "Token refreshed successfully")
                cachedToken
            } else {
                Log.e("AuthManager", "No authenticated user found")
                clearTokenCache()
                null
            }
        } catch (e: CancellationException) {
            Log.d("AuthManager", "Token refresh was cancelled")
            null
        } catch (e: Exception) {
            Log.e("AuthManager", "Token refresh failed: ${e.message}")
            clearTokenCache()
            null
        }
    }

    private fun scheduleTokenRefresh() {
        // Cancel any existing refresh job
        refreshJob?.cancel()

        // Calculate delay until next refresh (5 minutes before expiry)
        val refreshDelay = (tokenExpiry - TOKEN_EXPIRY_BUFFER) - System.currentTimeMillis()

        if (refreshDelay > 0) {
            refreshJob = authScope.launch {
                try {
                    delay(refreshDelay)
                    if (isActive) {
                        refreshToken()
                    }
                } catch (e: CancellationException) {
                    Log.d("AuthManager", "Scheduled token refresh was cancelled")
                } catch (e: Exception) {
                    Log.e("AuthManager", "Scheduled token refresh failed: ${e.message}")
                }
            }

            Log.d("AuthManager", "Scheduled token refresh in ${refreshDelay / 1000 / 60} minutes")
        }
    }

    suspend fun initializeToken(): String? {
        return refreshToken()
    }

    fun isTokenExpiringSoon(): Boolean {
        return System.currentTimeMillis() >= (tokenExpiry - TOKEN_EXPIRY_BUFFER)
    }

    fun getRemainingTokenTime(): Long {
        return maxOf(0, tokenExpiry - System.currentTimeMillis())
    }

    fun clearTokenCache() {
        cachedToken = null
        tokenExpiry = 0
        refreshJob?.cancel()
        refreshJob = null
        Log.d("AuthManager", "Token cache cleared")
    }

    suspend fun forceRefreshToken(): String? {
        Log.d("AuthManager", "Force refreshing token...")
        clearTokenCache()
        return refreshToken()
    }

    // Clean up resources when done
    fun cleanup() {
        clearTokenCache()
        authScope.cancel()
    }
}