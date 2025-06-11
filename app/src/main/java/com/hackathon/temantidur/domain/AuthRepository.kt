package com.hackathon.temantidur.domain

import androidx.lifecycle.LiveData

interface AuthRepository {
    fun register(username: String, email: String, password: String): LiveData<AuthResult>
    fun login(email: String, password: String): LiveData<AuthResult>
    fun verifyOtp(tempUserId: String, otp: String): LiveData<AuthResult>
    fun resendOtp(tempUserId: String): LiveData<AuthResult>
    fun changeEmail(newEmail: String): LiveData<AuthResult>
    fun changeEmailWithVerification(newEmail: String): LiveData<AuthResult>
    fun checkEmailUpdate(): LiveData<AuthResult>
    fun changePassword(currentPassword: String, newPassword: String): LiveData<AuthResult>
    fun changeUsername(newUsername: String): LiveData<AuthResult>
    fun getCurrentEmail(): LiveData<AuthResult>
}