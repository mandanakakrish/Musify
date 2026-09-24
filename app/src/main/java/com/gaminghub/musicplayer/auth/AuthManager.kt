package com.gaminghub.musicplayer.auth

import android.content.Context
import android.util.Log
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

sealed class AuthResult {
    data class Success(val email: String, val displayName: String?) : AuthResult()
    data class Error(val message: String) : AuthResult()
}

class AuthManager private constructor(private val context: Context) {
    private val auth: FirebaseAuth? by lazy {
        try {
            if (FirebaseApp.getApps(context).isEmpty()) {
                val options = FirebaseOptions.Builder()
                    .setApplicationId("1:123456789012:android:musifyplayerapp")
                    .setApiKey("AIzaSyMusifyLocalPlayerKeyFallback12345")
                    .setProjectId("musify-player-gaminghub")
                    .build()
                FirebaseApp.initializeApp(context, options)
            }
            FirebaseAuth.getInstance()
        } catch (e: Exception) {
            Log.w("AuthManager", "FirebaseAuth fallback: ${e.message}")
            null
        }
    }

    private val prefs = context.getSharedPreferences("musify_auth_prefs", Context.MODE_PRIVATE)

    private val _currentUser = MutableStateFlow<FirebaseUser?>(null)
    val currentUser: StateFlow<FirebaseUser?> = _currentUser.asStateFlow()

    private val _googleEmail = MutableStateFlow<String?>(prefs.getString("google_email", null))
    val googleEmail: StateFlow<String?> = _googleEmail.asStateFlow()

    private val _googleDisplayName = MutableStateFlow<String?>(prefs.getString("google_display_name", null))
    val googleDisplayName: StateFlow<String?> = _googleDisplayName.asStateFlow()

    private val _googlePhotoUrl = MutableStateFlow<String?>(prefs.getString("google_photo_url", null))
    val googlePhotoUrl: StateFlow<String?> = _googlePhotoUrl.asStateFlow()

    private val _isLoggedIn = MutableStateFlow(isUserLoggedIn())
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    private val _isAdmin = MutableStateFlow(false)
    val isAdmin: StateFlow<Boolean> = _isAdmin.asStateFlow()

    private var adminListenerEmail: com.google.firebase.firestore.ListenerRegistration? = null
    private var adminListenerUid: com.google.firebase.firestore.ListenerRegistration? = null

    init {
        // Clear any old insecure local admin bypass
        if (prefs.contains("is_admin_session")) {
            prefs.edit().remove("is_admin_session").apply()
        }
        try {
            _currentUser.value = auth?.currentUser
            auth?.addAuthStateListener { firebaseAuth ->
                _currentUser.value = firebaseAuth.currentUser
                updateLoginState()
                checkAdminStatus()
            }
            // Check Google Last Signed In Account
            val lastAccount = GoogleSignIn.getLastSignedInAccount(context)
            if (lastAccount != null && !lastAccount.email.isNullOrBlank()) {
                saveGoogleAccount(lastAccount)
            }
            updateLoginState()
            checkAdminStatus()
        } catch (e: Exception) {
            Log.e("AuthManager", "Error in init: ${e.message}")
        }
    }

    private fun isUserLoggedIn(): Boolean {
        val hasFirebaseUser = auth?.currentUser != null && !auth?.currentUser!!.isAnonymous
        val savedEmail = prefs.getString("google_email", null)
        val hasSavedGoogle = !savedEmail.isNullOrBlank() && savedEmail != "guest_listener@gmail.com"
        val lastGoogleAccount = GoogleSignIn.getLastSignedInAccount(context) != null
        val isGuest = prefs.getBoolean("is_guest", false)
        return hasFirebaseUser || hasSavedGoogle || lastGoogleAccount || isGuest
    }

    private fun updateLoginState() {
        _isLoggedIn.value = isUserLoggedIn()
    }

    fun checkAdminStatus(onResult: ((Boolean) -> Unit)? = null) {
        adminListenerEmail?.remove()
        adminListenerEmail = null
        adminListenerUid?.remove()
        adminListenerUid = null

        val email = (_googleEmail.value ?: _currentUser.value?.email ?: "").lowercase().trim()
        val uid = (_currentUser.value?.uid ?: "").trim()

        if (email.isBlank() && uid.isBlank()) {
            _isAdmin.value = false
            onResult?.invoke(false)
            return
        }

        try {
            val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
            var checkedEmail = false
            var checkedUid = false

            if (email.isNotBlank()) {
                adminListenerEmail = db.collection("admins").document(email)
                    .addSnapshotListener { snapshot, error ->
                        if (error != null) {
                            Log.w("AuthManager", "Admin email listener error: ${error.message}")
                            if (!checkedEmail) {
                                checkedEmail = true
                                if (checkedUid || uid.isBlank()) onResult?.invoke(_isAdmin.value)
                            }
                            return@addSnapshotListener
                        }
                        val isActive = snapshot != null && snapshot.exists() && snapshot.getBoolean("active") != false
                        if (isActive) {
                            _isAdmin.value = true
                        } else if (uid.isBlank() || !(_isAdmin.value)) {
                            _isAdmin.value = false
                        }
                        if (!checkedEmail) {
                            checkedEmail = true
                            if (checkedUid || uid.isBlank()) onResult?.invoke(_isAdmin.value)
                        }
                    }
            }

            if (uid.isNotBlank()) {
                adminListenerUid = db.collection("admins").document(uid)
                    .addSnapshotListener { snapshot, error ->
                        if (error != null) {
                            Log.w("AuthManager", "Admin uid listener error: ${error.message}")
                            if (!checkedUid) {
                                checkedUid = true
                                if (checkedEmail || email.isBlank()) onResult?.invoke(_isAdmin.value)
                            }
                            return@addSnapshotListener
                        }
                        val isActive = snapshot != null && snapshot.exists() && snapshot.getBoolean("active") != false
                        if (isActive) {
                            _isAdmin.value = true
                        } else if (email.isBlank() || !(_isAdmin.value)) {
                            _isAdmin.value = false
                        }
                        if (!checkedUid) {
                            checkedUid = true
                            if (checkedEmail || email.isBlank()) onResult?.invoke(_isAdmin.value)
                        }
                    }
            }
        } catch (e: Exception) {
            Log.e("AuthManager", "Error setting up admin listener: ${e.message}")
            _isAdmin.value = false
            onResult?.invoke(false)
        }
    }

    fun verifyAdminPasscode(passcode: String, onResult: (Boolean, String) -> Unit) {
        val clean = passcode.trim()
        if (clean.isBlank()) {
            onResult(false, "Passcode cannot be empty")
            return
        }

        try {
            val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
            // Checks dynamic passcode configured by owner in Firebase Firestore: "admins/config"
            db.collection("admins").document("config").get()
                .addOnSuccessListener { doc ->
                    val firebaseKey = doc.getString("passcode") ?: doc.getString("admin_key")
                    val isEnabled = doc.getBoolean("enabled") ?: true
                    if (isEnabled && !firebaseKey.isNullOrBlank() && firebaseKey == clean) {
                        _isAdmin.value = true
                        val email = (_googleEmail.value ?: _currentUser.value?.email ?: "").lowercase().trim()
                        if (email.isNotBlank()) {
                            db.collection("admins").document(email).set(
                                mapOf("active" to true, "role" to "admin", "grantedAt" to com.google.firebase.Timestamp.now()),
                                com.google.firebase.firestore.SetOptions.merge()
                            )
                        }
                        onResult(true, "Admin permission verified via Firebase!")
                    } else {
                        // Fallback check "admin_config/access"
                        db.collection("admin_config").document("access").get()
                            .addOnSuccessListener { fallbackDoc ->
                                val fallbackKey = fallbackDoc.getString("passcode") ?: fallbackDoc.getString("admin_key")
                                val fallbackEnabled = fallbackDoc.getBoolean("enabled") ?: true
                                if (fallbackEnabled && !fallbackKey.isNullOrBlank() && fallbackKey == clean) {
                                    _isAdmin.value = true
                                    val email = (_googleEmail.value ?: _currentUser.value?.email ?: "").lowercase().trim()
                                    if (email.isNotBlank()) {
                                        db.collection("admins").document(email).set(
                                            mapOf("active" to true, "role" to "admin", "grantedAt" to com.google.firebase.Timestamp.now()),
                                            com.google.firebase.firestore.SetOptions.merge()
                                        )
                                    }
                                    onResult(true, "Admin permission verified via Firebase!")
                                } else {
                                    onResult(false, "Invalid passcode. Permission denied by Firebase.")
                                }
                            }
                            .addOnFailureListener {
                                onResult(false, "Firebase verification failed. Check internet connection.")
                            }
                    }
                }
                .addOnFailureListener { e ->
                    onResult(false, "Firebase error: ${e.message}")
                }
        } catch (e: Exception) {
            onResult(false, "Error: ${e.message}")
        }
    }

    fun revokeAdminAccess() {
        _isAdmin.value = false
        val email = (_googleEmail.value ?: _currentUser.value?.email ?: "").lowercase().trim()
        val uid = (_currentUser.value?.uid ?: "").trim()
        try {
            val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
            if (email.isNotBlank()) {
                db.collection("admins").document(email).update("active", false)
            }
            if (uid.isNotBlank()) {
                db.collection("admins").document(uid).update("active", false)
            }
        } catch (e: Exception) {
            Log.w("AuthManager", "Error updating Firestore on revoke: ${e.message}")
        }
    }

    fun saveGoogleAccount(account: GoogleSignInAccount) {
        val email = account.email ?: ""
        val name = account.displayName ?: email.substringBefore("@")
        val photo = account.photoUrl?.toString()

        prefs.edit()
            .putBoolean("is_guest", false)
            .putString("google_email", email)
            .putString("google_display_name", name)
            .putString("google_photo_url", photo)
            .apply()

        _googleEmail.value = email
        _googleDisplayName.value = name
        _googlePhotoUrl.value = photo
        updateLoginState()
    }

    fun continueAsGuest() {
        prefs.edit()
            .putBoolean("is_guest", true)
            .remove("google_email")
            .remove("google_display_name")
            .remove("google_photo_url")
            .apply()

        _googleEmail.value = null
        _googleDisplayName.value = null
        _googlePhotoUrl.value = null
        updateLoginState()
    }

    fun signInWithCustomGoogleEmail(email: String, displayName: String? = null): AuthResult {
        val cleanEmail = email.trim()
        val name = if (!displayName.isNullOrBlank()) displayName.trim() else cleanEmail.substringBefore("@").replace(".", " ").replaceFirstChar { it.uppercase() }

        prefs.edit()
            .putString("google_email", cleanEmail)
            .putString("google_display_name", name)
            .apply()

        _googleEmail.value = cleanEmail
        _googleDisplayName.value = name
        updateLoginState()
        return AuthResult.Success(cleanEmail, name)
    }

    suspend fun signInWithGoogleAccount(account: GoogleSignInAccount): AuthResult = withContext(Dispatchers.IO) {
        try {
            saveGoogleAccount(account)

            // Try Firebase credential login if ID token is available
            val idToken = account.idToken
            if (!idToken.isNullOrBlank() && auth != null) {
                try {
                    val credential = GoogleAuthProvider.getCredential(idToken, null)
                    val result = auth?.signInWithCredential(credential)?.await()
                    _currentUser.value = result?.user
                } catch (e: Exception) {
                    Log.e("AuthManager", "Firebase credential error: ${e.message}")
                }
            }

            updateLoginState()
            AuthResult.Success(account.email ?: "", account.displayName)
        } catch (e: Exception) {
            AuthResult.Error(e.localizedMessage ?: "Google sign-in failed. Please try again.")
        }
    }

    /**
     * Returns a deterministic, distinct user identifier for Firestore data isolation:
     * 1. Firebase Auth UID if authenticated.
     * 2. Sanitized Google email if signed in with Google.
     * 3. Persistent installation UUID for guest / offline users.
     */
    fun getSyncUserId(): String {
        // 1. Authenticated Firebase Auth user email
        val fbEmail = _currentUser.value?.email ?: auth?.currentUser?.email
        if (!fbEmail.isNullOrBlank() && fbEmail != "guest_listener@gmail.com") {
            return fbEmail.trim().lowercase().replace(".", "_").replace("@", "_")
        }

        // 2. Verified Google Sign-In account email
        val googleAccEmail = try { GoogleSignIn.getLastSignedInAccount(context)?.email } catch (_: Exception) { null }
        if (!googleAccEmail.isNullOrBlank() && googleAccEmail != "guest_listener@gmail.com") {
            return googleAccEmail.trim().lowercase().replace(".", "_").replace("@", "_")
        }

        // 3. Saved Google email in preferences
        val email = _googleEmail.value ?: prefs.getString("google_email", null)
        if (!email.isNullOrBlank() && email != "guest_listener@gmail.com") {
            return email.trim().lowercase().replace(".", "_").replace("@", "_")
        }

        // 4. Authenticated Firebase UID
        val fbUid = _currentUser.value?.uid
        if (!fbUid.isNullOrBlank()) return fbUid

        // 5. Distinct persistent UUID device ID for guest listeners
        var deviceId = prefs.getString("musify_persistent_user_id", null)
        if (deviceId.isNullOrBlank()) {
            deviceId = "guest_" + java.util.UUID.randomUUID().toString().replace("-", "").take(16)
            prefs.edit().putString("musify_persistent_user_id", deviceId).apply()
        }
        return deviceId
    }

    suspend fun signOut() = withContext(Dispatchers.IO) {
        try {
            auth?.signOut()
            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).build()
            val client = GoogleSignIn.getClient(context, gso)
            client.signOut().await()
        } catch (e: Exception) {
            Log.e("AuthManager", "SignOut error: ${e.message}")
        } finally {
            prefs.edit().clear().apply()
            _currentUser.value = null
            _googleEmail.value = null
            _googleDisplayName.value = null
            _googlePhotoUrl.value = null
            _isLoggedIn.value = false
            _isAdmin.value = false
        }
    }

    companion object {
        @Volatile
        private var INSTANCE: AuthManager? = null

        fun getInstance(context: Context): AuthManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AuthManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
