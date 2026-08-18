package com.gaminghub.musify.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gaminghub.musify.util.AnalyticsManager
import com.gaminghub.musify.util.AuthManager
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    data class Authenticated(val user: FirebaseUser) : AuthState()
    object Guest : AuthState()

    data class Error(val message: String) : AuthState()
}


class AuthViewModel(
    private val authManager: AuthManager,
    private val analyticsManager: AnalyticsManager
) : ViewModel() {
    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState

    init {
        checkCurrentUser()
    }

    private fun checkCurrentUser() {
        // Priority 1: Check if user is signed in with Firebase (Google account)
        val user = authManager.currentUser
        if (user != null) {
            _authState.value = AuthState.Authenticated(user)
            return
        }
        // Priority 2: Check if user previously chose "Continue as Guest"
        if (authManager.isGuestSession) {
            _authState.value = AuthState.Guest
            return
        }
        // Otherwise stay Idle → will show login screen
    }

    val signInIntent: android.content.Intent
        get() = authManager.signInIntent

    fun handleGoogleSignInResult(account: GoogleSignInAccount?) {
        analyticsManager.logFeatureClick("google_sign_in")
        if (account == null) {
            _authState.value = AuthState.Error("Google Sign-In failed")
            return
        }

        _authState.value = AuthState.Loading
        viewModelScope.launch {
            val result = authManager.firebaseAuthWithGoogle(account)
            if (result.isSuccess) {
                _authState.value = AuthState.Authenticated(result.getOrThrow())
            } else {
                _authState.value = AuthState.Error(result.exceptionOrNull()?.message ?: "Authentication failed")
            }
        }
    }

    fun signInAsGuest() {
        analyticsManager.logFeatureClick("guest_sign_in")
        authManager.setGuestSession(true)
        _authState.value = AuthState.Guest
    }

    fun signOut(onComplete: () -> Unit = {}) {
        authManager.signOut {
            _authState.value = AuthState.Idle
            onComplete()
        }
    }
}
