package com.sporen.app.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.sporen.app.data.db.ClockedDatabase
import com.sporen.app.domain.model.Shift
import com.sporen.app.domain.model.ShiftType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate
import java.time.YearMonth

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ShiftRepositoryTest {

    private lateinit var db: ClockedDatabase
    private lateinit var repository: ShiftRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, ClockedDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = ShiftRepository(db.shiftDao())
    }

    @After
    fun tearDown() {
        db.close()
    }

    // ─── importShifts ──────────────────────────────────────────────────────────

    @Test
    fun `importShifts returns correct inserted and skipped counts`() = runTest {
        val shifts = listOf(
            shift(date = "2026-02-05", startTime = "08:00"),
            shift(date = "2026-02-06", startTime = "08:00"),
        )
        val (inserted, skipped) = repository.importShifts(shifts)
        assertEquals(2, inserted)
        assertEquals(0, skipped)
    }

    @Test
    fun `importShifts skips exact duplicate date+startTime`() = runTest {
        val s = shift(date = "2026-02-05", startTime = "08:00")
        repository.importShifts(listOf(s))

        val (inserted, skipped) = repository.importShifts(listOf(s))
        assertEquals(0, inserted)
        assertEquals(1, skipped)
    }

    @Test
    fun `importShifts accepts same date with different startTime`() = runTest {
        val s1 = shift(date = "2026-02-05", startTime = "08:00")
        val s2 = shift(date = "2026-02-05", startTime = "13:00")
        val (inserted, _) = repository.importShifts(listOf(s1, s2))
        assertEquals(2, inserted)
    }

    // ─── saveShift ────────────────────────────────────────────────────────────

    @Test
    fun `saveShift inserts when id is 0`() = runTest {
        repository.saveShift(shift(date = "2026-02-05", startTime = "08:00"))
        val result = repository.getShiftsForMonth(YearMonth.of(2026, 2)).first()
        assertEquals(1, result.size)
    }

    @Test
    fun `saveShift updates when id is non-zero`() = runTest {
        repository.saveShift(shift(date = "2026-02-05", startTime = "08:00"))
        val saved = repository.getShiftsForMonth(YearMonth.of(2026, 2)).first().first()

        val updated = saved.copy(note = "overtime")
        repository.saveShift(updated)

        val after = repository.getShiftsForMonth(YearMonth.of(2026, 2)).first()
        assertEquals(1, after.size)
        assertEquals("overtime", after[0].note)
    }

    // ─── getById ──────────────────────────────────────────────────────────────

    @Test
    fun `getById returns shift after insert`() = runTest {
        repository.saveShift(shift(date = "2026-02-05", startTime = "08:00"))
        val saved = repository.getShiftsForMonth(YearMonth.of(2026, 2)).first().first()
        assertNotNull(repository.getById(saved.id))
    }

    @Test
    fun `getById returns null for unknown id`() = runTest {
        val result = repository.getById(9999L)
        assertTrue(result == null)
    }

    // ─── deleteShift ──────────────────────────────────────────────────────────

    @Test
    fun `deleteShift removes the shift`() = runTest {
        repository.saveShift(shift(date = "2026-02-05", startTime = "08:00"))
        val saved = repository.getShiftsForMonth(YearMonth.of(2026, 2)).first().first()
        repository.deleteShift(saved)

        val remaining = repository.getShiftsForMonth(YearMonth.of(2026, 2)).first()
        assertTrue(remaining.isEmpty())
    }

    // ─── getShiftsForMonth ────────────────────────────────────────────────────

    @Test
    fun `getShiftsForMonth only returns shifts in the requested month`() = runTest {
        repository.saveShift(shift("2026-02-05", "08:00"))
        repository.saveShift(shift("2026-03-01", "08:00"))

        val feb = repository.getShiftsForMonth(YearMonth.of(2026, 2)).first()
        assertEquals(1, feb.size)
        assertEquals(LocalDate.of(2026, 2, 5), feb[0].date)
    }

    @Test
    fun `getShiftsForMonth returns shifts ordered by date then startTime`() = runTest {
        repository.saveShift(shift("2026-02-10", "13:00"))
        repository.saveShift(shift("2026-02-05", "08:00"))
        repository.saveShift(shift("2026-02-10", "08:00"))

        val shifts = repository.getShiftsForMonth(YearMonth.of(2026, 2)).first()
        assertEquals(LocalDate.of(2026, 2, 5), shifts[0].date)
        assertEquals("08:00", shifts[1].startTime)
        assertEquals("13:00", shifts[2].startTime)
    }

    // ─── pruneOldMonths ───────────────────────────────────────────────────────

    @Test
    fun `pruneOldMonths removes shifts older than 1 month ago`() = runTest {
        val twoMonthsAgo = YearMonth.now().minusMonths(2).atDay(1)
        repository.saveShift(shift(twoMonthsAgo.toString(), "08:00"))
        repository.saveShift(shift(LocalDate.now().toString(), "09:00"))

        repository.pruneOldMonths()

        val oldShifts = repository.getShiftsForMonth(
            YearMonth.of(twoMonthsAgo.year, twoMonthsAgo.monthValue)
        ).first()
        assertTrue(oldShifts.isEmpty())
    }

    @Test
    fun `pruneOldMonths keeps shifts in current and future months`() = runTest {
        repository.saveShift(shift(LocalDate.now().toString(), "08:00"))
        repository.saveShift(shift(LocalDate.now().plusMonths(1).toString(), "08:00"))

        repository.pruneOldMonths()

        val current = repository.getShiftsForMonth(YearMonth.now()).first()
        assertEquals(1, current.size)
    }

    // ─── clearAll ─────────────────────────────────────────────────────────────

    @Test
    fun `clearAll removes every shift`() = runTest {
        repository.saveShift(shift("2026-02-05", "08:00"))
        repository.saveShift(shift("2026-03-10", "09:00"))
        repository.clearAll()

        val feb = repository.getShiftsForMonth(YearMonth.of(2026, 2)).first()
        val mar = repository.getShiftsForMonth(YearMonth.of(2026, 3)).first()
        assertTrue(feb.isEmpty())
        assertTrue(mar.isEmpty())
    }

    // ─── factory ──────────────────────────────────────────────────────────────

    private fun shift(date: String, startTime: String) = Shift(
        id = 0L,
        date = LocalDate.parse(date),
        startTime = startTime,
        endTime = "16:00",
        crossesMidnight = false,
        shiftType = ShiftType.DAG1,
        ward = "TEST",
    )
}

