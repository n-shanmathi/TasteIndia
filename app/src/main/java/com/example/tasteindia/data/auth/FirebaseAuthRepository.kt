package com.example.tasteindia.data.auth

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseException
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.example.tasteindia.R
import kotlinx.coroutines.tasks.await

interface AuthRepository {
    val currentUser: FirebaseUser?
    fun observeAuthState(onChanged: (FirebaseUser?) -> Unit): () -> Unit
    suspend fun signIn(email: String, password: String)
    suspend fun signUp(name: String, email: String, password: String)
    suspend fun signInWithGoogle(idToken: String)
    suspend fun sendPasswordReset(email: String)
    suspend fun resendVerificationEmail()
    suspend fun reloadUser()
    fun signOut()
    fun googleSignInOptions(context: Context): GoogleSignInOptions?
}

class DefaultAuthRepository(
    private val context: Context
) : AuthRepository {
    private val firebaseAuth: FirebaseAuth?

    init {
        firebaseAuth = FirebaseApp.initializeApp(context)?.let { FirebaseAuth.getInstance(it) }
            ?: FirebaseApp.getApps(context).firstOrNull()?.let { FirebaseAuth.getInstance(it) }
    }

    override val currentUser: FirebaseUser?
        get() = firebaseAuth?.currentUser

    override fun observeAuthState(onChanged: (FirebaseUser?) -> Unit): () -> Unit {
        val auth = firebaseAuth ?: run {
            onChanged(null)
            return {}
        }
        val listener = FirebaseAuth.AuthStateListener { onChanged(it.currentUser) }
        auth.addAuthStateListener(listener)
        return { auth.removeAuthStateListener(listener) }
    }

    override suspend fun signIn(email: String, password: String) {
        requireAuth().signInWithEmailAndPassword(email, password).await()
    }

    override suspend fun signUp(name: String, email: String, password: String) {
        val user = requireAuth().createUserWithEmailAndPassword(email, password).await().user
            ?: error("Firebase did not return a user after registration.")
        if (name.isNotBlank()) {
            user.updateProfile(
                com.google.firebase.auth.UserProfileChangeRequest.Builder()
                    .setDisplayName(name.trim())
                    .build()
            ).await()
        }
        user.sendEmailVerification().await()
    }

    override suspend fun signInWithGoogle(idToken: String) {
        val credential: AuthCredential = GoogleAuthProvider.getCredential(idToken, null)
        requireAuth().signInWithCredential(credential).await()
    }

    override suspend fun sendPasswordReset(email: String) {
        requireAuth().sendPasswordResetEmail(email).await()
    }

    override suspend fun resendVerificationEmail() {
        val user = requireAuth().currentUser ?: error("No signed-in user.")
        user.sendEmailVerification().await()
    }

    override suspend fun reloadUser() {
        requireAuth().currentUser?.reload()?.await()
    }

    override fun signOut() {
        firebaseAuth?.signOut()
    }

    override fun googleSignInOptions(context: Context): GoogleSignInOptions? {
        val resourceId = context.resources.getIdentifier(
            "default_web_client_id",
            "string",
            context.packageName
        )
        if (resourceId == 0) return null
        return GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(context.getString(resourceId))
            .requestEmail()
            .build()
    }

    private fun requireAuth(): FirebaseAuth {
        return firebaseAuth ?: throw FirebaseConfigurationException(
            "Firebase is not configured. Add app/google-services.json and rebuild the app."
        )
    }
}

class FirebaseConfigurationException(message: String) : FirebaseException(message)

fun Throwable.toAuthErrorMessage(): String {
    return when (this) {
        is FirebaseConfigurationException ->
            "Firebase is not configured for this app. Add google-services.json and rebuild."
        is FirebaseAuthException -> when (errorCode) {
            "ERROR_INVALID_EMAIL" -> "Enter a valid email address."
            "ERROR_EMAIL_ALREADY_IN_USE" -> "An account already exists with this email."
            "ERROR_WRONG_PASSWORD", "ERROR_INVALID_CREDENTIAL" ->
                "The email or password is incorrect."
            "ERROR_USER_NOT_FOUND" -> "No account exists with this email."
            "ERROR_WEAK_PASSWORD" -> "Choose a stronger password with at least 6 characters."
            "ERROR_USER_DISABLED" -> "This account has been disabled."
            "ERROR_ACCOUNT_EXISTS_WITH_DIFFERENT_CREDENTIAL" ->
                "An account already exists with a different sign-in method."
            "ERROR_NETWORK_REQUEST_FAILED" -> "Network unavailable. Check your connection."
            "ERROR_TOO_MANY_REQUESTS" -> "Too many attempts. Please try again later."
            else -> "Authentication failed. Please try again."
        }
        else -> message ?: "Authentication failed. Please try again."
    }
}
