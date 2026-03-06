package com.sporen.app.ui.edit

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sporen.app.domain.model.ShiftType
import com.sporen.app.ui.components.ClockHeader
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditShiftScreen(
    onDone: () -> Unit,
    viewModel: EditShiftViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    var showDatePicker by remember { mutableStateOf(false) }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = state.date
                .atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        viewModel.updateDate(
                            Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
                        )
                    }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Annuleer") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .imePadding()
    ) {
        TopAppBar(
            title = { Text(if (state.isNewShift) "Shift toevoegen" else "Shift bewerken") },
            navigationIcon = {
                IconButton(onClick = onDone) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Terug")
                }
            }
        )

        ClockHeader(modifier = Modifier.padding(top = 4.dp))

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Date field — tap to open DatePickerDialog
            val dateInteraction = remember { MutableInteractionSource() }
            val datePressed by dateInteraction.collectIsPressedAsState()
            LaunchedEffect(datePressed) { if (datePressed) showDatePicker = true }
            OutlinedTextField(
                value = state.date.format(
                    DateTimeFormatter.ofPattern("EEE d MMMM yyyy", Locale.forLanguageTag("nl"))
                ).replaceFirstChar { it.uppercase() },
                onValueChange = {},
                label = { Text("Datum") },
                readOnly = true,
                modifier = Modifier.fillMaxWidth(),
                trailingIcon = {
                    Icon(Icons.Default.CalendarMonth, contentDescription = "Kies datum")
                },
                interactionSource = dateInteraction,
            )

            // Start time
            OutlinedTextField(
                value = state.startTime,
                onValueChange = { viewModel.updateStartTime(it) },
                label = { Text("Begintijd  (HH:mm)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            // End time
            OutlinedTextField(
                value = state.endTime,
                onValueChange = { viewModel.updateEndTime(it) },
                label = { Text("Eindtijd  (HH:mm)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                isError = state.timeError != null,
            )

            // Time validation error
            state.timeError?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.labelSmall,
                )
            }

            // Shift type chips
            Text("Type", style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ShiftType.entries.forEach { type ->
                    FilterChip(
                        selected = state.shiftType == type,
                        onClick = { viewModel.updateShiftType(type) },
                        label = { Text(type.label) }
                    )
                }
            }

            // Note field
            OutlinedTextField(
                value = state.note,
                onValueChange = { viewModel.updateNote(it) },
                label = { Text("Notitie (optioneel)") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
            )

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = { viewModel.save(onDone) },
                enabled = !state.isSaving,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (state.isNewShift) "Opslaan" else "Bijwerken")
            }
        }
    }
}

