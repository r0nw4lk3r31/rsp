package com.sporen.app.ui.lock

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sporen.app.data.preferences.UserPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.security.MessageDigest
import javax.inject.Inject

data class LockUiState(
    val pin: String = "",       // digits entered so far (max 4)
    val error: Boolean = false, // triggers shake animation
)

@HiltViewModel
class LockViewModel @Inject constructor(
    private val prefs: UserPreferences,
) : ViewModel() {

    private val _state = MutableStateFlow(LockUiState())
    val state: StateFlow<LockUiState> = _state.asStateFlow()

    private val _unlocked = MutableStateFlow(false)
    val unlocked: StateFlow<Boolean> = _unlocked.asStateFlow()

    private var storedHash = ""

    init {
        viewModelScope.launch {
            storedHash = prefs.pinHash.first()
        }
    }

    fun digit(d: String) {
        val cur = _state.value.pin
        if (cur.length >= 4) return
        val next = cur + d
        _state.value = _state.value.copy(pin = next, error = false)
        if (next.length == 4) verify(next)
    }

    fun backspace() {
        val cur = _state.value.pin
        if (cur.isEmpty()) return
        _state.value = _state.value.copy(pin = cur.dropLast(1), error = false)
    }

    fun clearError() {
        _state.value = _state.value.copy(error = false)
    }

    fun onBiometricSuccess() {
        _unlocked.value = true
    }

    private fun verify(pin: String) {
        if (sha256(pin) == storedHash) {
            _unlocked.value = true
        } else {
            _state.value = _state.value.copy(pin = "", error = true)
        }
    }

    companion object {
        fun sha256(input: String): String {
            val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
            return bytes.joinToString("") { "%02x".format(it) }
        }
    }
}
