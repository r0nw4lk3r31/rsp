package com.sporen.app.ui.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sporen.app.ui.components.ClockHeader

@Composable
fun OnboardingScreen(
    onComplete: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel(),
) {
    var fullName by remember { mutableStateOf("") }
    var alias by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .imePadding()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top,
    ) {
        ClockHeader(modifier = Modifier.padding(top = 16.dp))

        Spacer(Modifier.height(32.dp))

        Text(
            text = "Welkom bij Sporen Rooster",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary,
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = "Vul je naam in zoals die in het rooster staat.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(32.dp))

        OutlinedTextField(
            value = fullName,
            onValueChange = { fullName = it },
            label = { Text("Volledige naam") },
            placeholder = { Text("Ronnie Spoelstra") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Words,
                imeAction = ImeAction.Next
            )
        )

        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = alias,
            onValueChange = { alias = it.lowercase() },
            label = { Text("Alias in rooster") },
            placeholder = { Text("ron") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            supportingText = { Text("De naam/afkorting die in het rooster staat, bijv. \"ron\"") },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done)
        )

        Spacer(Modifier.height(24.dp))

        Button(
            onClick = {
                viewModel.saveProfile(fullName.trim(), alias.trim(), onDone = onComplete)
            },
            enabled = fullName.isNotBlank() && alias.isNotBlank(),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Aan de slag")
        }
    }
}

