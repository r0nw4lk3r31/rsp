package com.sporen.app.domain.model

import java.time.LocalDate

data class Shift(
    val id: Long = 0,
    val date: LocalDate,
    val startTime: String,   // "HH:mm"
    val endTime: String,     // "HH:mm"  (next calendar day if crossesMidnight)
    val crossesMidnight: Boolean,
    val shiftType: ShiftType,
    val ward: String,
    val note: String = "",
    val syncedToCalendar: Boolean = false,
)

