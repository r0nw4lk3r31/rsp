package com.sporen.app.ui.shifts

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sporen.app.data.preferences.UserPreferences
import com.sporen.app.data.repository.ShiftRepository
import com.sporen.app.domain.model.Shift
import com.sporen.app.domain.usecase.ImportResult
import com.sporen.app.domain.usecase.ImportShiftsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.YearMonth
import javax.inject.Inject

data class ShiftsUiState(
    val isLoading: Boolean = false,
    val importMessage: String? = null,
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ShiftsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: ShiftRepository,
    private val importUseCase: ImportShiftsUseCase,
    private val prefs: UserPreferences,
) : ViewModel() {

    // yearMonth from nav arg, defaults to current month
    private val yearMonthArg = savedStateHandle.get<String>("yearMonth")
    private val _currentMonth = MutableStateFlow(
        yearMonthArg?.let { runCatching { YearMonth.parse(it) }.getOrNull() } ?: YearMonth.now()
    )
    val currentMonth: StateFlow<YearMonth> = _currentMonth.asStateFlow()

    val shifts: StateFlow<List<Shift>> = _currentMonth
        .flatMapLatest { month -> repository.getShiftsForMonth(month) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _uiState = MutableStateFlow(ShiftsUiState())
    val uiState: StateFlow<ShiftsUiState> = _uiState.asStateFlow()

    fun navigateMonth(delta: Int) {
        _currentMonth.value = _currentMonth.value.plusMonths(delta.toLong())
    }

    fun importFile(uri: Uri) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val alias = prefs.alias.first()
            val result = importUseCase(uri, alias)
            val message = when (result) {
                is ImportResult.Success -> buildString {
                    append("${result.imported} nieuw geïmporteerd")
                    if (result.skipped > 0) append(", ${result.skipped} overgeslagen")
                }
                is ImportResult.Error -> "Fout: ${result.message}"
            }
            _uiState.value = _uiState.value.copy(isLoading = false, importMessage = message)
        }
    }

    fun clearImportMessage() {
        _uiState.value = _uiState.value.copy(importMessage = null)
    }

    fun deleteShift(shift: Shift) {
        viewModelScope.launch { repository.deleteShift(shift) }
    }
}

