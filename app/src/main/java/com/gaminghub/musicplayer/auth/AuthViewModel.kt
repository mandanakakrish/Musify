package com.gaminghub.musicplayer.auth

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AuthViewModel(application: Application) : AndroidViewModel(application) {
    private val authManager = AuthManager.getInstance(application)

    val currentUser: StateFlow<FirebaseUser?> = authManager.currentUser
    val googleEmail: StateFlow<String?> = authManager.googleEmail
    val googleDisplayName: StateFlow<String?> = authManager.googleDisplayName
    val googlePhotoUrl: StateFlow<String?> = authManager.googlePhotoUrl
    val isLoggedIn: StateFlow<Boolean> = authManager.isLoggedIn
    val isAdmin: StateFlow<Boolean> = authManager.isAdmin

    fun verifyAdminPasscode(passcode: String): Boolean {
        val success = authManager.verifyAdminPasscode(passcode)
        if (success) {
            _successMessage.value = "Admin access granted!"
        } else {
            _errorMessage.value = "Invalid Admin passcode"
        }
        return success
    }

    fun revokeAdminAccess() {
        authManager.revokeAdminAccess()
        _successMessage.value = "Admin access revoked"
    }

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage.asStateFlow()

    fun clearMessages() {
        _errorMessage.value = null
        _successMessage.value = null
    }

    fun handleGoogleSignInResult(account: GoogleSignInAccount, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            _isLoading.value = true
            clearMessages()
            when (val result = authManager.signInWithGoogleAccount(account)) {
                is AuthResult.Success -> {
                    _isLoading.value = false
                    _successMessage.value = "Welcome, ${result.displayName ?: result.email}!"
                    viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                        com.gaminghub.musicplayer.data.firebase.FirestoreSyncManager.syncAll(
                            getApplication(),
                            authManager.getSyncUserId()
                        )
                    }
                    onSuccess()
                }
                is AuthResult.Error -> {
                    _isLoading.value = false
                    _errorMessage.value = result.message
                }
            }
        }
    }

    fun signInWithGoogleEmail(email: String, displayName: String? = null, onSuccess: () -> Unit = {}) {
        val trimmed = email.trim()
        if (trimmed.isBlank() || !trimmed.contains("@")) {
            _errorMessage.value = "Please enter a valid Google email address"
            return
        }
        viewModelScope.launch {
            _isLoading.value = true
            clearMessages()
            when (val result = authManager.signInWithCustomGoogleEmail(trimmed, displayName)) {
                is AuthResult.Success -> {
                    _isLoading.value = false
                    _successMessage.value = "Signed in as ${result.displayName ?: result.email}"
                    viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                        com.gaminghub.musicplayer.data.firebase.FirestoreSyncManager.syncAll(
                            getApplication(),
                            authManager.getSyncUserId()
                        )
                    }
                    onSuccess()
                }
                is AuthResult.Error -> {
                    _isLoading.value = false
                    _errorMessage.value = result.message
                }
            }
        }
    }

    fun continueAsGuest(onSuccess: () -> Unit = {}) {
        authManager.continueAsGuest()
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            com.gaminghub.musicplayer.data.firebase.FirestoreSyncManager.syncAll(
                getApplication(),
                authManager.getSyncUserId()
            )
        }
        onSuccess()
    }

    fun signOut(onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            _isLoading.value = true
            authManager.signOut()
            _isLoading.value = false
            _successMessage.value = "Signed out"
            onComplete()
        }
    }
}
