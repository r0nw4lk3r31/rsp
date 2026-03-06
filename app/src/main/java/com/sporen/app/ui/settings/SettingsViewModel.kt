package com.sporen.app.ui.settings

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sporen.app.data.preferences.UserPreferences
import com.sporen.app.data.repository.ShiftRepository
import com.sporen.app.domain.usecase.ExportCsvUseCase
import com.sporen.app.ui.lock.LockViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.YearMonth
import javax.inject.Inject

data class SettingsUiState(
    val fullName: String = "",
    val alias: String = "",
    val isSaving: Boolean = false,
    val isExporting: Boolean = false,
    val exportUri: Uri? = null,
    val savedMessage: String? = null,
    val pinEnabled: Boolean = false,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val prefs: UserPreferences,
    private val repository: ShiftRepository,
    private val exportCsv: ExportCsvUseCase,
) : ViewModel() {

    private val selectedMonth: YearMonth =
        savedStateHandle.get<String>("yearMonth")
            ?.let { runCatching { YearMonth.parse(it) }.getOrNull() }
            ?: YearMonth.now()

    private val _state = MutableStateFlow(SettingsUiState())
    val state: StateFlow<SettingsUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            _state.value = _state.value.copy(
                fullName = prefs.fullName.first(),
                alias = prefs.alias.first(),
                pinEnabled = prefs.pinHash.first().isNotEmpty(),
            )
        }
    }

    fun updateFullName(v: String) { _state.value = _state.value.copy(fullName = v) }
    fun updateAlias(v: String) { _state.value = _state.value.copy(alias = v) }

    fun saveProfile() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isSaving = true)
            prefs.saveProfile(_state.value.fullName, _state.value.alias)
            _state.value = _state.value.copy(isSaving = false, savedMessage = "Opgeslagen")
        }
    }

    fun clearSavedMessage() {
        _state.value = _state.value.copy(savedMessage = null)
    }

    val selectedMonthLabel: String = selectedMonth.format(
        java.time.format.DateTimeFormatter.ofPattern("MMMM yyyy", java.util.Locale.forLanguageTag("nl"))
    ).replaceFirstChar { it.uppercase() }

    fun exportSelectedMonth() = launchExport(selectedMonth)
    fun exportAllShifts() = launchExport(null)

    private fun launchExport(yearMonth: YearMonth?) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isExporting = true)
            try {
                val uri = exportCsv(yearMonth)
                _state.value = _state.value.copy(isExporting = false, exportUri = uri)
            } catch (e: Throwable) {
                _state.value = _state.value.copy(
                    isExporting = false,
                    savedMessage = "Export mislukt: ${e.message}"
                )
            }
        }
    }

    fun clearExportUri() {
        _state.value = _state.value.copy(exportUri = null)
    }

    fun clearDatabase(onDone: () -> Unit) {
        viewModelScope.launch {
            repository.clearAll()
            _state.value = _state.value.copy(savedMessage = "Alle shifts verwijderd")
            onDone()
        }
    }

    fun enablePin(pin: String) {
        viewModelScope.launch {
            prefs.savePinHash(LockViewModel.sha256(pin))
            _state.value = _state.value.copy(pinEnabled = true, savedMessage = "Pincode ingesteld")
        }
    }

    fun disablePin() {
        viewModelScope.launch {
            prefs.clearPin()
            _state.value = _state.value.copy(pinEnabled = false, savedMessage = "Pincode verwijderd")
        }
    }

    fun logout(onDone: () -> Unit) {
        viewModelScope.launch {
            repository.clearAll()
            prefs.logout()
            onDone()
        }
    }
}

