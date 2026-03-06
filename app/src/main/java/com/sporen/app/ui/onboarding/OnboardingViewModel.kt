package com.sporen.app.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sporen.app.data.preferences.UserPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val prefs: UserPreferences
) : ViewModel() {

    val onboardingComplete = prefs.onboardingComplete
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun saveProfile(fullName: String, alias: String, onDone: () -> Unit) {
        viewModelScope.launch {
            prefs.saveProfile(fullName, alias)
            onDone()
        }
    }
}

