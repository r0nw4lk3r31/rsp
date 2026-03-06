package com.sporen.app.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Persisted shift row. Dates and times are stored as plain strings to keep
 * serialisation simple and human-readable in the DB inspector.
 *
 * @param date          ISO date: "2026-02-03"
 * @param startTime     "21:00"
 * @param endTime       "09:30"  (next calendar day if crossesMidnight = true)
 * @param crossesMidnight  true for NACHT shifts
 * @param shiftType     see [ShiftType]
 * @param ward          source sheet name, e.g. "PIJL"
 * @param note          user-added note
 * @param syncedToCalendar  true once pushed to Google Calendar
 */
@Entity(
    tableName = "shifts",
    indices = [Index(value = ["date", "startTime"], unique = true)]
)
data class ShiftEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String,
    val startTime: String,
    val endTime: String,
    val crossesMidnight: Boolean,
    val shiftType: String,
    val ward: String,
    val note: String = "",
    val syncedToCalendar: Boolean = false,
)

