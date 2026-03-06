package com.sporen.app.data.repository

import com.sporen.app.data.db.ShiftDao
import com.sporen.app.data.db.ShiftEntity
import com.sporen.app.domain.model.Shift
import com.sporen.app.domain.model.ShiftType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ShiftRepository @Inject constructor(private val dao: ShiftDao) {

    fun getShiftsForMonth(yearMonth: YearMonth): Flow<List<Shift>> =
        dao.getShiftsForMonth(yearMonth.toString())
            .map { entities -> entities.map { it.toDomain() } }

    suspend fun getById(id: Long): Shift? = dao.getById(id)?.toDomain()

    /** One-shot snapshot for export — returns a plain list, not a Flow. */
    suspend fun getShiftsSnapshot(yearMonth: YearMonth): List<Shift> =
        dao.getShiftsForMonthOnce(yearMonth.toString()).map { it.toDomain() }

    /** One-shot snapshot of every stored shift — used for full export. */
    suspend fun getAllShiftsSnapshot(): List<Shift> =
        dao.getAllShifts().map { it.toDomain() }

    /**
     * Import a list of parsed shifts. Duplicate detection is handled by
     * the UNIQUE index on (date, startTime) — duplicates are silently ignored.
     * Returns Pair(inserted, skipped).
     */
    suspend fun importShifts(shifts: List<Shift>): Pair<Int, Int> {
        val entities = shifts.map { it.toEntity() }
        val result = dao.insertAll(entities)
        val inserted = result.count { it >= 0L }
        val skipped = result.count { it == -1L }
        return Pair(inserted, skipped)
    }

    suspend fun saveShift(shift: Shift) {
        if (shift.id == 0L) {
            dao.insert(shift.toEntity())
        } else {
            dao.update(shift.toEntity())
        }
    }

    suspend fun deleteShift(shift: Shift) = dao.delete(shift.toEntity())

    suspend fun clearAll() = dao.deleteAll()

    /** Call after every import to prune months outside the retention window. */
    suspend fun pruneOldMonths() {
        val cutoff = YearMonth.now().minusMonths(1).atDay(1)
        dao.deleteOlderThan(cutoff.toString())
    }
}

// ---- Mappers ----

fun ShiftEntity.toDomain() = Shift(
    id = id,
    date = LocalDate.parse(date),
    startTime = startTime,
    endTime = endTime,
    crossesMidnight = crossesMidnight,
    shiftType = runCatching { ShiftType.valueOf(shiftType) }.getOrDefault(ShiftType.MANUAL),
    ward = ward,
    note = note,
    syncedToCalendar = syncedToCalendar,
)

fun Shift.toEntity() = ShiftEntity(
    id = id,
    date = date.toString(),
    startTime = startTime,
    endTime = endTime,
    crossesMidnight = crossesMidnight,
    shiftType = shiftType.name,
    ward = ward,
    note = note,
    syncedToCalendar = syncedToCalendar,
)

