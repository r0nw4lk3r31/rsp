package com.sporen.app.ui.settings

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.Switch
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onLogout: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    var showClearConfirm by remember { mutableStateOf(false) }
    var showLogoutConfirm by remember { mutableStateOf(false) }
    var showPinSetup by remember { mutableStateOf(false) }
    var newPin by remember { mutableStateOf("") }

    // Fire share sheet when export is ready
    LaunchedEffect(state.exportUri) {
        state.exportUri?.let { uri ->
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Exporteer shifts"))
            viewModel.clearExportUri()
        }
    }

    LaunchedEffect(state.savedMessage) {
        state.savedMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearSavedMessage()
        }
    }


    // PIN setup dialog
    if (showPinSetup) {
        AlertDialog(
            onDismissRequest = { showPinSetup = false; newPin = "" },
            title = { Text("Stel pincode in") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Kies een 4-cijferige pincode.")
                    OutlinedTextField(
                        value = newPin,
                        onValueChange = { if (it.length <= 4 && it.all(Char::isDigit)) newPin = it },
                        label = { Text("Pincode") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newPin.length == 4) {
                            viewModel.enablePin(newPin)
                            showPinSetup = false
                            newPin = ""
                        }
                    },
                    enabled = newPin.length == 4,
                ) { Text("Opslaan") }
            },
            dismissButton = {
                TextButton(onClick = { showPinSetup = false; newPin = "" }) { Text("Annuleer") }
            },
        )
    }

    // Confirmation: clear database
    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = { Text("Shifts verwijderen") },
            text = { Text("Alle shifts worden permanent verwijderd. Weet je het zeker?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showClearConfirm = false
                        viewModel.clearDatabase {
                            // snackbar feedback
                        }
                    }
                ) { Text("Verwijderen", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirm = false }) { Text("Annuleer") }
            }
        )
    }

    // Confirmation: logout
    if (showLogoutConfirm) {
        AlertDialog(
            onDismissRequest = { showLogoutConfirm = false },
            title = { Text("Uitloggen") },
            text = { Text("Je profiel en alle shifts worden gewist. Weet je het zeker?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLogoutConfirm = false
                        viewModel.logout(onLogout)
                    }
                ) { Text("Uitloggen", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutConfirm = false }) { Text("Annuleer") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Instellingen") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Terug")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── Profile ──────────────────────────────────────────────
            Text(
                "Profiel",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
            )

            OutlinedTextField(
                value = state.fullName,
                onValueChange = { viewModel.updateFullName(it) },
                label = { Text("Volledige naam") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = state.alias,
                onValueChange = { viewModel.updateAlias(it) },
                label = { Text("Alias in rooster (bijv. ron)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            Button(
                onClick = { viewModel.saveProfile() },
                enabled = !state.isSaving,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Opslaan")
            }

            Spacer(Modifier.height(8.dp))
            HorizontalDivider()
            Spacer(Modifier.height(8.dp))

            // ── Beveiliging ───────────────────────────────────────────
            Text(
                "Beveiliging",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Vergrendel met pincode", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        if (state.pinEnabled) "App vergrendeld bij elke start"
                        else "Uit — iedereen kan de app openen",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = state.pinEnabled,
                    onCheckedChange = { enabled ->
                        if (enabled) showPinSetup = true else viewModel.disablePin()
                    },
                )
            }

            Spacer(Modifier.height(8.dp))
            HorizontalDivider()
            Spacer(Modifier.height(8.dp))

            // ── Data ─────────────────────────────────────────────────
            Text(
                "Data",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
            )

            OutlinedButton(
                onClick = { viewModel.exportSelectedMonth() },
                enabled = !state.isExporting,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (state.isExporting) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .align(Alignment.CenterVertically)
                            .height(18.dp),
                        strokeWidth = 2.dp,
                    )
                } else {
                    Text("Exporteer ${viewModel.selectedMonthLabel} (CSV)")
                }
            }

            OutlinedButton(
                onClick = { viewModel.exportAllShifts() },
                enabled = !state.isExporting,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Exporteer alle shifts (CSV)")
            }

            OutlinedButton(
                onClick = { showClearConfirm = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                ),
            ) {
                Text("Verwijder alle shifts")
            }

            Button(
                onClick = { showLogoutConfirm = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                ),
            ) {
                Text("Uitloggen")
            }
        }
    }
}

