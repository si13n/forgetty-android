package com.si13.forgetty

import android.app.Activity
import android.content.Context
import androidx.credentials.Credential
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.gms.tasks.Task
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

class GoogleAuthClient {
    suspend fun signIn(activity: Activity): GoogleAuthResult {
        val connectivity = AndroidConnectivityObserver(activity.applicationContext)
        if (!connectivity.isOnline()) {
            return GoogleAuthResult.Failure(activity.getString(R.string.google_sign_in_offline))
        }

        val credential = requestCredential(activity)
            ?: return GoogleAuthResult.Failure(activity.getString(R.string.firebase_config_missing))
        if (credential is CredentialRequestResult.Cancelled) return GoogleAuthResult.Cancelled
        if (credential is CredentialRequestResult.Failure) {
            return GoogleAuthResult.Failure(activity.getString(R.string.google_sign_in_failed))
        }

        val firebaseCredential = (credential as CredentialRequestResult.Success).firebaseCredential
        return try {
            val result = FirebaseAuth.getInstance().signInWithCredential(firebaseCredential).await()
            val user = result.user
                ?: return GoogleAuthResult.Failure(activity.getString(R.string.google_sign_in_failed))
            GoogleAuthResult.Success(user)
        } catch (exception: Exception) {
            GoogleAuthResult.Failure(
                activity.getString(
                    if (connectivity.isOnline()) R.string.google_sign_in_failed
                    else R.string.google_sign_in_offline
                )
            )
        }
    }

    suspend fun reauthenticate(activity: Activity, user: FirebaseUser): GoogleReauthenticationResult {
        val connectivity = AndroidConnectivityObserver(activity.applicationContext)
        if (!connectivity.isOnline()) {
            return GoogleReauthenticationResult.Failure(activity.getString(R.string.google_sign_in_offline))
        }

        return when (val credential = requestCredential(activity)) {
            null -> GoogleReauthenticationResult.Failure(activity.getString(R.string.firebase_config_missing))
            CredentialRequestResult.Cancelled -> GoogleReauthenticationResult.Cancelled
            CredentialRequestResult.Failure ->
                GoogleReauthenticationResult.Failure(activity.getString(R.string.google_sign_in_failed))
            is CredentialRequestResult.Success -> try {
                user.reauthenticate(credential.firebaseCredential).await()
                GoogleReauthenticationResult.Success
            } catch (exception: Exception) {
                GoogleReauthenticationResult.Failure(
                    activity.getString(R.string.account_reauthentication_failed)
                )
            }
        }
    }

    private suspend fun requestCredential(activity: Activity): CredentialRequestResult? {
        val webClientId = getWebClientId(activity) ?: return null
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(GetSignInWithGoogleOption.Builder(webClientId).build())
            .build()
        val credential = try {
            CredentialManager.create(activity).getCredential(
                context = activity,
                request = request
            ).credential
        } catch (exception: GetCredentialCancellationException) {
            return CredentialRequestResult.Cancelled
        } catch (exception: GetCredentialException) {
            return CredentialRequestResult.Failure
        }

        return try {
            CredentialRequestResult.Success(
                GoogleAuthProvider.getCredential(getGoogleIdToken(credential), null)
            )
        } catch (exception: GoogleIdTokenParsingException) {
            CredentialRequestResult.Failure
        }
    }

    private fun getGoogleIdToken(credential: Credential): String {
        if (
            credential is CustomCredential &&
            credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) {
            return GoogleIdTokenCredential.createFrom(credential.data).idToken
        }
        throw GoogleIdTokenParsingException()
    }

    private fun getWebClientId(context: Context): String? {
        val resourceId = context.resources.getIdentifier(
            "default_web_client_id",
            "string",
            context.packageName
        )
        if (resourceId == 0) return null
        return context.getString(resourceId).takeIf { it.isNotBlank() }
    }

    private sealed interface CredentialRequestResult {
        data class Success(
            val firebaseCredential: com.google.firebase.auth.AuthCredential
        ) : CredentialRequestResult
        data object Cancelled : CredentialRequestResult
        data object Failure : CredentialRequestResult
    }
}

sealed class GoogleAuthResult {
    data class Success(val user: FirebaseUser) : GoogleAuthResult()
    data object Cancelled : GoogleAuthResult()
    data class Failure(val message: String) : GoogleAuthResult()
}

sealed interface GoogleReauthenticationResult {
    data object Success : GoogleReauthenticationResult
    data object Cancelled : GoogleReauthenticationResult
    data class Failure(val message: String) : GoogleReauthenticationResult
}

private suspend fun <T> Task<T>.await(): T {
    return suspendCancellableCoroutine { continuation ->
        addOnSuccessListener { result ->
            if (continuation.isActive) continuation.resume(result)
        }
        addOnFailureListener { exception ->
            if (continuation.isActive) continuation.resumeWithException(exception)
        }
        addOnCanceledListener { continuation.cancel() }
    }
}
