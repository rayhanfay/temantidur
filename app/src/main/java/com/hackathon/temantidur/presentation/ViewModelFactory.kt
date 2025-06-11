package com.hackathon.temantidur.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.google.firebase.database.FirebaseDatabase
import com.hackathon.temantidur.data.auth.AuthRepositoryImpl
import com.hackathon.temantidur.domain.AuthRepository
import com.hackathon.temantidur.presentation.auth.AuthViewModel

class ViewModelFactory : ViewModelProvider.Factory {

    private val authRepo: AuthRepository by lazy {
        AuthRepositoryImpl(FirebaseDatabase.getInstance())
    }

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AuthViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AuthViewModel(authRepo) as T
        }
        throw IllegalArgumentException("Unknown ViewModel: ${modelClass.name}")
    }
}