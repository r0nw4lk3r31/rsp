package com.sporen.app.ui.edit

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sporen.app.data.repository.ShiftRepository
import com.sporen.app.domain.model.Shift
import com.sporen.app.domain.model.ShiftType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject

data class EditShiftUiState(
    val date: LocalDate = LocalDate.now(),
    val startTime: String = "09:00",
    val endTime: String = "17:00",
    val shiftType: ShiftType = ShiftType.MANUAL,
    val note: String = "",
    val timeError: String? = null,
    val isSaving: Boolean = false,
    val isNewShift: Boolean = true,
)

@HiltViewModel
class EditShiftViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: ShiftRepository,
) : ViewModel() {

    private val shiftId = savedStateHandle.get<Long>("shiftId") ?: -1L

    private val _state = MutableStateFlow(EditShiftUiState())
    val state: StateFlow<EditShiftUiState> = _state.asStateFlow()

    init {
        if (shiftId > 0L) {
            viewModelScope.launch {
                val existing = repository.getById(shiftId)
                if (existing != null) {
                    _state.value = EditShiftUiState(
                        date = existing.date,
                        startTime = existing.startTime,
                        endTime = existing.endTime,
                        shiftType = existing.shiftType,
                        note = existing.note,
                        isNewShift = false,
                    )
                }
            }
        }
    }

    fun updateDate(date: LocalDate) { _state.value = _state.value.copy(date = date) }
    fun updateStartTime(t: String) { _state.value = _state.value.copy(startTime = t) }
    fun updateEndTime(t: String) { _state.value = _state.value.copy(endTime = t) }
    fun updateShiftType(t: ShiftType) { _state.value = _state.value.copy(shiftType = t) }
    fun updateNote(n: String) { _state.value = _state.value.copy(note = n) }

    fun save(onDone: () -> Unit) {
        val s = _state.value
        val fmt = DateTimeFormatter.ofPattern("HH:mm")
        val startT = runCatching { LocalTime.parse(s.startTime, fmt) }.getOrNull()
        val endT = runCatching { LocalTime.parse(s.endTime, fmt) }.getOrNull()
        if (startT == null || endT == null) {
            _state.value = s.copy(timeError = "Tijden moeten het formaat HH:mm hebben")
            return
        }
        _state.value = s.copy(timeError = null)
        viewModelScope.launch {
            _state.value = _state.value.copy(isSaving = true)
            val shift = Shift(
                id = if (shiftId > 0L) shiftId else 0L,
                date = s.date,
                startTime = s.startTime,
                endTime = s.endTime,
                crossesMidnight = endT <= startT,
                shiftType = s.shiftType,
                ward = "Manueel",
                note = s.note,
            )
            repository.saveShift(shift)
            onDone()
        }
    }
}

