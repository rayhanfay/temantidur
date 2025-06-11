package com.hackathon.temantidur.domain

sealed class AuthResult {
    object Loading : AuthResult()
    data class Success(val message: String, val user: User? = null) : AuthResult()
    data class Error(val error: String) : AuthResult()
    data class OtpRequired(val message: String, val tempUserId: String) : AuthResult()
}