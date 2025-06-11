package com.hackathon.temantidur.data.auth

import com.hackathon.temantidur.R
import android.content.Context
import android.content.SharedPreferences
import com.google.firebase.auth.FirebaseAuth
import androidx.core.content.edit

class SessionManager(context: Context) {

    private val PREF_NAME = "UserSession"
    private val KEY_IS_LOGGED_IN = "isLoggedIn"
    private val KEY_USER_ID = "userId"
    private val KEY_USERNAME = "username"
    private val KEY_EMAIL = "email"
    private val KEY_LANGUAGE = "language_code"
    private val KEY_ONBOARDING_SEEN = "onboarding_seen"
    private val KEY_DAILY_REMINDER_ENABLED = "daily_reminder_enabled"
    private val PROFILE_PICTURE_RES_ID = "profile_picture_res_id"
    private val KEY_LAST_USER_ID = "last_user_id"

    private val pref: SharedPreferences =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    private val editor: SharedPreferences.Editor = pref.edit()
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    fun saveProfilePictureResId(resId: Int) {
        editor.putInt(PROFILE_PICTURE_RES_ID, resId).apply()
    }

    fun getProfilePictureResId(): Int {
        return pref.getInt(PROFILE_PICTURE_RES_ID, R.drawable.ic_profile)
    }

    fun getLastUserId(): String? {
        return pref.getString(KEY_LAST_USER_ID, null)
    }

    fun clearUserSessionData() {
        editor.remove(KEY_USER_ID)
        editor.remove(KEY_USERNAME)
        editor.remove(KEY_EMAIL)
        editor.remove(PROFILE_PICTURE_RES_ID)
        editor.apply()
    }

    fun createLoginSession(userId: String, username: String, email: String) {
        val lastStoredUserId = getLastUserId()

        if (lastStoredUserId != null && lastStoredUserId != userId) {
            clearUserSessionData()
            editor.putBoolean("should_clear_all_user_data", true)
        } else {
            editor.putBoolean("should_clear_all_user_data", false)
        }

        editor.putBoolean(KEY_IS_LOGGED_IN, true)
        editor.putString(KEY_USER_ID, userId)
        editor.putString(KEY_USERNAME, username)
        editor.putString(KEY_EMAIL, email)
        editor.putString(KEY_LAST_USER_ID, userId)
        editor.apply()
    }

    fun shouldClearAllUserData(): Boolean {
        val shouldClear = pref.getBoolean("should_clear_all_user_data", false)
        editor.remove("should_clear_all_user_data").apply()
        return shouldClear
    }


    fun isLoggedIn(): Boolean {
        val firebaseUser = auth.currentUser
        val sharedPrefLoggedIn = pref.getBoolean(KEY_IS_LOGGED_IN, false)

        return firebaseUser != null && sharedPrefLoggedIn
    }

    fun getUserName(): String? {
        return pref.getString(KEY_USERNAME, null)
    }

    fun saveUserEmail(email: String) {
        pref.edit {
            putString(KEY_EMAIL, email)
        }
    }

    fun getUserEmail(): String? {
        return pref.getString(KEY_EMAIL, null)
    }

    fun logoutUser() {
        editor.putBoolean(KEY_IS_LOGGED_IN, false)
        editor.apply()
        auth.signOut()
    }

    fun setLanguage(languageCode: String) {
        editor.putString(KEY_LANGUAGE, languageCode)
        editor.apply()
    }

    fun getLanguage(): String? {
        return pref.getString(KEY_LANGUAGE, "id")
    }

    fun setOnboardingSeen(seen: Boolean) {
        editor.putBoolean(KEY_ONBOARDING_SEEN, seen)
        editor.apply()
    }

    fun hasSeenOnboarding(): Boolean {
        return pref.getBoolean(KEY_ONBOARDING_SEEN, false)
    }

    fun saveUserName(username: String) {
        editor.putString(KEY_USERNAME, username)
        editor.apply()
    }

    fun setDailyReminderEnabled(enabled: Boolean) {
        editor.putBoolean(KEY_DAILY_REMINDER_ENABLED, enabled)
        editor.apply()
    }

    fun isDailyReminderEnabled(): Boolean {
        return pref.getBoolean(KEY_DAILY_REMINDER_ENABLED, false)
    }
}