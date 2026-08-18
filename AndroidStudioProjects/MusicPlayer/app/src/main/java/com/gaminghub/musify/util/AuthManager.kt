package com.gaminghub.musify.util

import android.content.Context
import android.content.Intent
import com.gaminghub.musicplayer.R
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.tasks.await

class AuthManager(private val context: Context) {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val googleSignInClient: GoogleSignInClient
    private val prefs = context.getSharedPreferences("musify_auth", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_GUEST_SESSION = "is_guest_session"
    }

    init {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(context.getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(context, gso)
    }

    val currentUser: FirebaseUser?
        get() = auth.currentUser

    /** Returns true if the user previously chose "Continue as Guest". */
    val isGuestSession: Boolean
        get() = prefs.getBoolean(KEY_GUEST_SESSION, false)

    /** Persist guest session so it survives app restarts. */
    fun setGuestSession(isGuest: Boolean) {
        prefs.edit().putBoolean(KEY_GUEST_SESSION, isGuest).apply()
    }

    val signInIntent: Intent
        get() = googleSignInClient.signInIntent

    fun signOut(onComplete: () -> Unit) {
        auth.signOut()
        setGuestSession(false)
        googleSignInClient.signOut().addOnCompleteListener {
            onComplete()
        }
    }

    suspend fun firebaseAuthWithGoogle(account: GoogleSignInAccount): Result<FirebaseUser> {
        return try {
            val credential = GoogleAuthProvider.getCredential(account.idToken, null)
            val result = auth.signInWithCredential(credential).await()
            val user = result.user
            if (user != null) {
                // Clear guest flag since we have a real account now
                setGuestSession(false)
                Result.success(user)
            } else {
                Result.failure(Exception("Firebase user is null"))
            }
        } catch (e: Exception) {
            Result.failure<FirebaseUser>(e)
        }
    }
}
