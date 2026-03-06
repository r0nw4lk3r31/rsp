package com.sporen.app.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_prefs")

@Singleton
class UserPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val store = context.dataStore

    private object Keys {
        val FULL_NAME = stringPreferencesKey("full_name")
        val ALIAS = stringPreferencesKey("alias")
        val ONBOARDING_COMPLETE = booleanPreferencesKey("onboarding_complete")
        val PIN_HASH = stringPreferencesKey("pin_hash")
    }

    val fullName: Flow<String> = store.data.map { it[Keys.FULL_NAME] ?: "" }
    val alias: Flow<String> = store.data.map { it[Keys.ALIAS] ?: "" }
    val onboardingComplete: Flow<Boolean> = store.data.map { it[Keys.ONBOARDING_COMPLETE] ?: false }

    /** SHA-256 hash of the PIN, empty string means no PIN set. */
    val pinHash: Flow<String> = store.data.map { it[Keys.PIN_HASH] ?: "" }

    suspend fun saveProfile(fullName: String, alias: String) {
        store.edit {
            it[Keys.FULL_NAME] = fullName.trim()
            it[Keys.ALIAS] = alias.trim().lowercase()
            it[Keys.ONBOARDING_COMPLETE] = true
        }
    }

    suspend fun savePinHash(hash: String) {
        store.edit { it[Keys.PIN_HASH] = hash }
    }

    suspend fun clearPin() {
        store.edit { it.remove(Keys.PIN_HASH) }
    }

    suspend fun logout() {
        store.edit { it.clear() }
    }
}

