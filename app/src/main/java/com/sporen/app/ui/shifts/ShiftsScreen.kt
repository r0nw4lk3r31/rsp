package com.sporen.app.ui.shifts

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import androidx.hilt.navigation.compose.hiltViewModel
import com.sporen.app.domain.model.Shift
import com.sporen.app.domain.model.ShiftType
import com.sporen.app.ui.components.ClockHeader
import com.sporen.app.ui.theme.ShiftDag
import com.sporen.app.ui.theme.ShiftDp
import com.sporen.app.ui.theme.ShiftManual
import com.sporen.app.ui.theme.ShiftNacht
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShiftsScreen(
    onAddShift: () -> Unit,
    onEditShift: (Long) -> Unit,
    onSettings: (java.time.YearMonth) -> Unit,
    viewModel: ShiftsViewModel = hiltViewModel(),
) {
    val shifts by viewModel.shifts.collectAsState()
    val currentMonth by viewModel.currentMonth.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var shiftToDelete by remember { mutableStateOf<Shift?>(null) }

    // File picker for xlsx import
    val filePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri -> uri?.let { viewModel.importFile(it) } }

    // Show import result as snackbar
    LaunchedEffect(uiState.importMessage) {
        uiState.importMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearImportMessage()
        }
    }

    // Delete confirmation dialog
    shiftToDelete?.let { shift ->
        AlertDialog(
            onDismissRequest = { shiftToDelete = null },
            title = { Text("Shift verwijderen") },
            text = {
                val dayLabel = shift.date.format(
                    DateTimeFormatter.ofPattern("EEE dd MMM", Locale.forLanguageTag("nl"))
                ).uppercase()
                Text("$dayLabel  ${shift.startTime} → ${shift.endTime} verwijderen?")
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteShift(shift)
                    shiftToDelete = null
                }) { Text("Verwijderen", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { shiftToDelete = null }) { Text("Annuleer") }
            }
        )
    }

    Scaffold(
        modifier = Modifier.systemBarsPadding(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            ClockHeader(modifier = Modifier.padding(top = 8.dp))

            // Month navigation bar
            MonthBar(
                currentMonth = currentMonth,
                onPrev = { viewModel.navigateMonth(-1) },
                onNext = { viewModel.navigateMonth(1) },
                onImport = { filePicker.launch("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet") },
                onAddShift = onAddShift,
                onSettings = { onSettings(currentMonth) },
                isLoading = uiState.isLoading,
            )

            if (shifts.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "Geen shifts voor deze maand.\nImporteer een rooster of voeg een shift toe.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(24.dp)
                    )
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(shifts, key = { it.id }) { shift ->
                        val dismissState = rememberSwipeToDismissBoxState(
                            confirmValueChange = { value ->
                                when (value) {
                                    SwipeToDismissBoxValue.StartToEnd -> {
                                        // swipe right = delete (show confirm, snap back)
                                        shiftToDelete = shift
                                        false
                                    }
                                    SwipeToDismissBoxValue.EndToStart -> {
                                        // swipe left = edit (navigate, snap back)
                                        onEditShift(shift.id)
                                        false
                                    }
                                    else -> false
                                }
                            }
                        )
                        SwipeToDismissBox(
                            state = dismissState,
                            enableDismissFromStartToEnd = true,
                            enableDismissFromEndToStart = true,
                            backgroundContent = {
                                val isDeleteSwipe = dismissState.dismissDirection == SwipeToDismissBoxValue.StartToEnd
                                val bgColor = if (isDeleteSwipe)
                                    MaterialTheme.colorScheme.error
                                else
                                    MaterialTheme.colorScheme.primaryContainer
                                val icon = if (isDeleteSwipe) Icons.Default.Delete else Icons.Default.Edit
                                val iconTint = if (isDeleteSwipe)
                                    MaterialTheme.colorScheme.onError
                                else
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                val alignment = if (isDeleteSwipe) Alignment.CenterStart else Alignment.CenterEnd
                                Box(
                                    Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 16.dp, vertical = 6.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(bgColor),
                                    contentAlignment = alignment
                                ) {
                                    Icon(
                                        icon,
                                        contentDescription = null,
                                        tint = iconTint,
                                        modifier = Modifier.padding(16.dp)
                                    )
                                }
                            }
                        ) {
                            ShiftCard(shift = shift)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MonthBar(
    currentMonth: YearMonth,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onImport: () -> Unit,
    onAddShift: () -> Unit,
    onSettings: () -> Unit,
    isLoading: Boolean,
) {
    val label = currentMonth.format(
        DateTimeFormatter.ofPattern("MMMM yyyy", Locale.forLanguageTag("nl"))
    ).uppercase()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        // Left: month navigation as a balanced unit
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onPrev) {
                Icon(Icons.Default.ChevronLeft, contentDescription = "Vorige maand")
            }
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            IconButton(onClick = onNext) {
                Icon(Icons.Default.ChevronRight, contentDescription = "Volgende maand")
            }
        }

        // Right: actions
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                Spacer(Modifier.width(4.dp))
            }
            IconButton(onClick = onImport) {
                Icon(Icons.Default.FileOpen, contentDescription = "Importeer rooster")
            }
            IconButton(onClick = onAddShift) {
                Icon(Icons.Default.Add, contentDescription = "Shift toevoegen")
            }
            IconButton(onClick = onSettings) {
                Icon(Icons.Default.Settings, contentDescription = "Instellingen")
            }
        }
    }
}

@Composable
private fun ShiftCard(shift: Shift) {
    val accentColor = when (shift.shiftType) {
        ShiftType.NACHT -> ShiftNacht
        ShiftType.DAG1, ShiftType.DAG2 -> ShiftDag
        ShiftType.DP -> ShiftDp
        ShiftType.MANUAL -> ShiftManual
    }

    val dayLabel = shift.date.format(DateTimeFormatter.ofPattern("EEE dd MMM", Locale.forLanguageTag("nl"))).uppercase()
    val timeLabel = "${shift.startTime} → ${shift.endTime}${if (shift.crossesMidnight) "  (+1)" else ""}"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Accent stripe
        Box(
            modifier = Modifier
                .size(width = 4.dp, height = 48.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(accentColor)
        )

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(dayLabel, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
            Text(timeLabel, style = MaterialTheme.typography.bodyMedium, color = accentColor)
            if (shift.note.isNotBlank()) {
                Text(shift.note, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = shift.shiftType.label.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = accentColor,
            )
            if (shift.ward.isNotBlank()) {
                Text(
                    text = shift.ward,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

