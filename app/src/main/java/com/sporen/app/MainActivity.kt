package com.sporen.app

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.fragment.app.FragmentActivity
import com.sporen.app.data.preferences.UserPreferences
import com.sporen.app.navigation.SporenNavGraph
import com.sporen.app.ui.lock.LockScreen
import com.sporen.app.ui.theme.SporenRoosterTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import javax.inject.Inject

private sealed interface AppState {
    object Loading : AppState
    object Locked : AppState
    object Unlocked : AppState
}

@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    @Inject
    lateinit var prefs: UserPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SporenRoosterTheme {
                var appState by remember { mutableStateOf<AppState>(AppState.Loading) }

                LaunchedEffect(Unit) {
                    val pinHash = prefs.pinHash.first()
                    appState = if (pinHash.isEmpty()) AppState.Unlocked else AppState.Locked
                }

                when (appState) {
                    AppState.Loading -> Unit  // blank — matches window background, no flash
                    AppState.Locked -> LockScreen(onUnlocked = { appState = AppState.Unlocked })
                    AppState.Unlocked -> SporenNavGraph()
                }
            }
        }
    }
}

