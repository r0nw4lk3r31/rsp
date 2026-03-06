package com.sporen.app.ui.lock

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import com.sporen.app.ui.theme.SporenOnSurfaceMuted
import com.sporen.app.ui.theme.SporenTeal
import kotlin.math.roundToInt

@Composable
fun LockScreen(
    onUnlocked: () -> Unit,
    viewModel: LockViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val unlocked by viewModel.unlocked.collectAsState()
    val context = LocalContext.current

    // Navigate away when unlocked
    LaunchedEffect(unlocked) {
        if (unlocked) onUnlocked()
    }

    // Trigger biometric automatically on first show
    LaunchedEffect(Unit) {
        val canAuth = BiometricManager.from(context)
            .canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK)
        if (canAuth == BiometricManager.BIOMETRIC_SUCCESS) {
            showBiometricPrompt(context as FragmentActivity, viewModel)
        }
    }

    // Shake animation on wrong PIN
    val shakeOffset = remember { Animatable(0f) }
    LaunchedEffect(state.error) {
        if (state.error) {
            repeat(3) {
                shakeOffset.animateTo(10f, tween(50))
                shakeOffset.animateTo(-10f, tween(50))
            }
            shakeOffset.animateTo(0f, tween(50))
            viewModel.clearError()
        }
    }

    val hasBiometrics = BiometricManager.from(context)
        .canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK) ==
            BiometricManager.BIOMETRIC_SUCCESS

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(28.dp),
        ) {
            // App title
            Text(
                text = "Sporen Rooster",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = SporenTeal,
            )

            Text(
                text = "Voer je pincode in",
                style = MaterialTheme.typography.bodyMedium,
                color = SporenOnSurfaceMuted,
            )

            // 4 PIN dots
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.offset { IntOffset(shakeOffset.value.roundToInt(), 0) },
            ) {
                repeat(4) { i ->
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .clip(CircleShape)
                            .background(
                                if (i < state.pin.length) SporenTeal
                                else MaterialTheme.colorScheme.surfaceVariant,
                            ),
                    )
                }
            }

            // Biometric button (only if enrolled)
            if (hasBiometrics) {
                IconButton(
                    onClick = { showBiometricPrompt(context as FragmentActivity, viewModel) },
                    modifier = Modifier.size(56.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.Fingerprint,
                        contentDescription = "Vingerafdruk gebruiken",
                        modifier = Modifier.size(44.dp),
                        tint = SporenTeal,
                    )
                }
            } else {
                Spacer(Modifier.height(4.dp))
            }

            // Numeric keypad
            PinPad(onDigit = viewModel::digit, onBackspace = viewModel::backspace)
        }
    }
}

@Composable
private fun PinPad(onDigit: (String) -> Unit, onBackspace: () -> Unit) {
    val rows = listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9"),
        listOf("",  "0", "⌫"),
    )
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        rows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                row.forEach { key ->
                    PinKey(key, onDigit, onBackspace)
                }
            }
        }
    }
}

@Composable
private fun PinKey(label: String, onDigit: (String) -> Unit, onBackspace: () -> Unit) {
    Box(
        modifier = Modifier
            .size(72.dp)
            .clip(CircleShape)
            .background(
                if (label.isEmpty()) Color.Transparent
                else MaterialTheme.colorScheme.surfaceVariant,
            )
            .then(
                if (label.isNotEmpty()) Modifier.clickable {
                    if (label == "⌫") onBackspace() else onDigit(label)
                } else Modifier,
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (label.isNotEmpty()) {
            Text(
                text = label,
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Medium),
                color = if (label == "⌫") MaterialTheme.colorScheme.onSurfaceVariant
                else MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

private fun showBiometricPrompt(activity: FragmentActivity, viewModel: LockViewModel) {
    val executor = ContextCompat.getMainExecutor(activity)
    val prompt = BiometricPrompt(
        activity,
        executor,
        object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                viewModel.onBiometricSuccess()
            }
        },
    )
    val info = BiometricPrompt.PromptInfo.Builder()
        .setTitle("Sporen Rooster")
        .setSubtitle("Authenticeer om door te gaan")
        .setNegativeButtonText("Gebruik pincode")
        .build()
    prompt.authenticate(info)
}
