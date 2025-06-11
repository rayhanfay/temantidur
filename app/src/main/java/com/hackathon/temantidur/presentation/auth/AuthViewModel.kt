package com.hackathon.temantidur.presentation.auth

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.hackathon.temantidur.domain.AuthRepository
import com.hackathon.temantidur.domain.AuthResult
import com.hackathon.temantidur.data.auth.AuthRepositoryImpl

class AuthViewModel(
    private val repo: AuthRepository
) : ViewModel() {

    fun register(username: String, email: String, password: String): LiveData<AuthResult> {
        return repo.register(username, email, password)
    }

    fun login(email: String, password: String): LiveData<AuthResult> {
        return repo.login(email, password)
    }

    fun changeUsername(newUsername: String): LiveData<AuthResult> {
        return if (repo is AuthRepositoryImpl) {
            repo.changeUsername(newUsername)
        } else {
            throw IllegalStateException("Repository tidak mendukung perubahan email")
        }
    }

    fun changePassword(currentPassword: String, newPassword: String): LiveData<AuthResult> {
        return if (repo is AuthRepositoryImpl) {
            repo.changePassword(currentPassword, newPassword)
        } else {
            throw IllegalStateException("Repository tidak mendukung perubahan password")
        }
    }
    fun getCurrentEmail(): LiveData<AuthResult> {
        return repo.getCurrentEmail()
    }
}