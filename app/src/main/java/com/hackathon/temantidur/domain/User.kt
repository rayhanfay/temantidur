package com.hackathon.temantidur.domain

data class User(
    val uid: String = "",
    val username: String = "",
    val email: String = "",
    val createdAt: Long = 0L,
    val isEmailVerified: Boolean = false,
    val profileImageUrl: String = "",
    val lastLoginAt: Long = 0L,
    val role: String = "user",
    val isActive: Boolean = true,
    val updatedAt: Long = 0L
)