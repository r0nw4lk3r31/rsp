package com.sporen.app.data.repository

import com.sporen.app.data.db.ShiftEntity
import com.sporen.app.domain.model.Shift
import com.sporen.app.domain.model.ShiftType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class ShiftMappersTest {

    private val sampleEntity = ShiftEntity(
        id = 42L,
        date = "2026-02-03",
        startTime = "08:00",
        endTime = "16:00",
        crossesMidnight = false,
        shiftType = "DAG1",
        ward = "PIJL",
        note = "test note",
        syncedToCalendar = false,
    )

    private val sampleDomain = Shift(
        id = 42L,
        date = LocalDate.of(2026, 2, 3),
        startTime = "08:00",
        endTime = "16:00",
        crossesMidnight = false,
        shiftType = ShiftType.DAG1,
        ward = "PIJL",
        note = "test note",
        syncedToCalendar = false,
    )

    @Test
    fun `entity toDomain maps every field correctly`() {
        val domain = sampleEntity.toDomain()
        assertEquals(42L, domain.id)
        assertEquals(LocalDate.of(2026, 2, 3), domain.date)
        assertEquals("08:00", domain.startTime)
        assertEquals("16:00", domain.endTime)
        assertFalse(domain.crossesMidnight)
        assertEquals(ShiftType.DAG1, domain.shiftType)
        assertEquals("PIJL", domain.ward)
        assertEquals("test note", domain.note)
        assertFalse(domain.syncedToCalendar)
    }

    @Test
    fun `domain toEntity maps every field correctly`() {
        val entity = sampleDomain.toEntity()
        assertEquals(42L, entity.id)
        assertEquals("2026-02-03", entity.date)
        assertEquals("08:00", entity.startTime)
        assertEquals("16:00", entity.endTime)
        assertFalse(entity.crossesMidnight)
        assertEquals("DAG1", entity.shiftType)
        assertEquals("PIJL", entity.ward)
        assertEquals("test note", entity.note)
        assertFalse(entity.syncedToCalendar)
    }

    @Test
    fun `round-trip entity → domain → entity preserves all fields`() {
        assertEquals(sampleEntity, sampleEntity.toDomain().toEntity())
    }

    @Test
    fun `round-trip domain → entity → domain preserves all fields`() {
        assertEquals(sampleDomain, sampleDomain.toEntity().toDomain())
    }

    @Test
    fun `all ShiftTypes survive the round-trip`() {
        ShiftType.entries.forEach { type ->
            val entity = sampleEntity.copy(shiftType = type.name)
            val domain = entity.toDomain()
            assertEquals(type, domain.shiftType)
            assertEquals(type.name, domain.toEntity().shiftType)
        }
    }

    @Test
    fun `crossesMidnight true round-trips correctly`() {
        val entity = sampleEntity.copy(crossesMidnight = true)
        assertTrue(entity.toDomain().crossesMidnight)
        assertTrue(entity.toDomain().toEntity().crossesMidnight)
    }

    @Test
    fun `syncedToCalendar true round-trips correctly`() {
        val entity = sampleEntity.copy(syncedToCalendar = true)
        assertTrue(entity.toDomain().syncedToCalendar)
        assertTrue(entity.toDomain().toEntity().syncedToCalendar)
    }

    @Test
    fun `new shift with id 0 survives toEntity`() {
        val domain = sampleDomain.copy(id = 0L)
        assertEquals(0L, domain.toEntity().id)
    }

    @Test
    fun `empty note survives round-trip`() {
        val entity = sampleEntity.copy(note = "")
        assertEquals("", entity.toDomain().note)
        assertEquals("", entity.toDomain().toEntity().note)
    }
}

