package com.example.nothingpodcast.data.auth

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

// ── Auth state ────────────────────────────────────────────────────────────────

sealed class AuthState {
    data object Loading   : AuthState()
    data object LoggedOut : AuthState()
    data class  LoggedIn(val email: String) : AuthState()
}

// ── DataStore instance ────────────────────────────────────────────────────────

private val Context.authStore: DataStore<Preferences>
    by preferencesDataStore("nothing_auth")

// ── Repository ────────────────────────────────────────────────────────────────

@Singleton
class AuthRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object Keys {
        val CURRENT_EMAIL = stringPreferencesKey("current_email")
        /**
         * Accounts serialised as "email1:hash1|email2:hash2"
         * Passwords are stored as SHA-256 hashes (one-way).
         */
        val ACCOUNTS = stringPreferencesKey("accounts")
    }

    // ── Auth state flow ───────────────────────────────────────────────────────

    val authState: Flow<AuthState> = context.authStore.data.map { prefs ->
        val email = prefs[Keys.CURRENT_EMAIL]
        if (!email.isNullOrBlank()) AuthState.LoggedIn(email)
        else AuthState.LoggedOut
    }

    // ── Operations ────────────────────────────────────────────────────────────

    suspend fun signInWithEmail(email: String, password: String): Result<String> = runCatching {
        val accounts = loadAccounts()
        val key      = email.trim().lowercase()
        val stored   = accounts[key] ?: error("No account found for this email")
        if (stored != hash(password)) error("Incorrect password")
        saveCurrentEmail(key)
        key
    }

    suspend fun createAccount(email: String, password: String): Result<String> = runCatching {
        val accounts = loadAccounts()
        val key      = email.trim().lowercase()
        if (accounts.containsKey(key)) error("This email is already registered")
        saveAccounts(accounts + (key to hash(password)))
        saveCurrentEmail(key)
        key
    }

    /** Local password reset — replaces the stored hash */
    suspend fun resetPassword(email: String, newPassword: String): Result<Unit> = runCatching {
        val accounts = loadAccounts()
        val key      = email.trim().lowercase()
        if (!accounts.containsKey(key)) error("No account found for this email")
        saveAccounts(accounts + (key to hash(newPassword)))
    }

    suspend fun signOut() {
        context.authStore.edit { it[Keys.CURRENT_EMAIL] = "" }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private suspend fun loadAccounts(): Map<String, String> {
        val raw = context.authStore.data.first()[Keys.ACCOUNTS] ?: ""
        if (raw.isBlank()) return emptyMap()
        return raw.split("|").mapNotNull { entry ->
            val idx = entry.indexOf(':')
            if (idx < 0) null else entry.substring(0, idx) to entry.substring(idx + 1)
        }.toMap()
    }

    private suspend fun saveAccounts(accounts: Map<String, String>) {
        val serialised = accounts.entries.joinToString("|") { "${it.key}:${it.value}" }
        context.authStore.edit { it[Keys.ACCOUNTS] = serialised }
    }

    private suspend fun saveCurrentEmail(email: String) {
        context.authStore.edit { it[Keys.CURRENT_EMAIL] = email }
    }

    private fun hash(password: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest(password.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
}
