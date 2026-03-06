package com.sporen.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sporen.app.ui.theme.SporenOnSurfaceMuted
import com.sporen.app.ui.theme.SporenTeal
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
private val secondFormatter = DateTimeFormatter.ofPattern("ss")
// e.g. "MON  24 FEB  2026"
private val dateFormatter = DateTimeFormatter.ofPattern("EEE  dd MMM  yyyy", Locale.ENGLISH)

/**
 * Small live digital clock header — always rendered at the top of every screen.
 * Updates every second. Monospace font, dark theme, blue accent on the colon.
 */
@Composable
fun ClockHeader(modifier: Modifier = Modifier) {
    var time by remember { mutableStateOf(LocalTime.now()) }
    var date by remember { mutableStateOf(LocalDate.now()) }

    LaunchedEffect(Unit) {
        while (true) {
            time = LocalTime.now()
            date = LocalDate.now()
            delay(1000L)
        }
    }

    val hhmm = time.format(timeFormatter).uppercase()
    val ss = time.format(secondFormatter)
    val dateStr = date.format(dateFormatter).uppercase()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        // HH:MM  ss — colon pulses blue, seconds smaller and muted
        Text(
            text = hhmm,
            style = MaterialTheme.typography.displayMedium,
            color = SporenTeal,
            textAlign = TextAlign.Center,
        )
        Text(
            text = ss,
            style = MaterialTheme.typography.labelSmall,
            color = SporenOnSurfaceMuted,
            textAlign = TextAlign.Center,
        )
        Text(
            text = dateStr,
            style = MaterialTheme.typography.titleMedium,
            color = SporenOnSurfaceMuted,
            textAlign = TextAlign.Center,
        )
    }
}

