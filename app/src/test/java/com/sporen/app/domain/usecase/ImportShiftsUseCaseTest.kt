package com.sporen.app.domain.usecase

import android.net.Uri
import com.sporen.app.data.repository.ShiftRepository
import com.sporen.app.domain.model.Shift
import com.sporen.app.domain.model.ShiftType
import com.sporen.app.parser.ExcelParser
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.Runs
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.InputStream
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ImportShiftsUseCaseTest {

    private val parser = mockk<ExcelParser>()
    private val repository = mockk<ShiftRepository>()
    private val context = mockk<android.content.Context>()
    private val contentResolver = mockk<android.content.ContentResolver>()
    private val uri = mockk<Uri>()

    private lateinit var useCase: ImportShiftsUseCase

    private val sampleShifts = listOf(
        Shift(
            id = 0L,
            date = LocalDate.of(2026, 2, 5),
            startTime = "08:00",
            endTime = "16:00",
            crossesMidnight = false,
            shiftType = ShiftType.DAG1,
            ward = "PIJL",
        )
    )

    @Before
    fun setUp() {
        every { context.contentResolver } returns contentResolver
        // Default: query returns null so filename falls back to lastPathSegment
        every { contentResolver.query(uri, any(), null, null, null) } returns null
        every { uri.lastPathSegment } returns "202602.xlsx"

        useCase = ImportShiftsUseCase(context, parser, repository)
    }

    // ─── success path ─────────────────────────────────────────────────────────

    @Test
    fun `success result contains correct inserted and skipped counts`() = runTest {
        val stream = "irrelevant".byteInputStream()
        every { contentResolver.openInputStream(uri) } returns stream
        coEvery { parser.parse(any(), "202602.xlsx", "ron") } returns sampleShifts
        coEvery { repository.importShifts(sampleShifts) } returns Pair(1, 0)
        coEvery { repository.pruneOldMonths() } just Runs

        val result = useCase(uri, "ron")

        assertTrue(result is ImportResult.Success)
        assertEquals(1, (result as ImportResult.Success).imported)
        assertEquals(0, result.skipped)
    }

    @Test
    fun `pruneOldMonths is always called after a successful import`() = runTest {
        every { contentResolver.openInputStream(uri) } returns "x".byteInputStream()
        coEvery { parser.parse(any(), any(), any()) } returns sampleShifts
        coEvery { repository.importShifts(any()) } returns Pair(1, 0)
        coEvery { repository.pruneOldMonths() } just Runs

        useCase(uri, "ron")

        coVerify(exactly = 1) { repository.pruneOldMonths() }
    }

    @Test
    fun `skipped count is included in success result`() = runTest {
        every { contentResolver.openInputStream(uri) } returns "x".byteInputStream()
        coEvery { parser.parse(any(), any(), any()) } returns sampleShifts
        coEvery { repository.importShifts(any()) } returns Pair(2, 3)
        coEvery { repository.pruneOldMonths() } just Runs

        val result = useCase(uri, "ron") as ImportResult.Success
        assertEquals(2, result.imported)
        assertEquals(3, result.skipped)
    }

    // ─── filename resolution ──────────────────────────────────────────────────

    @Test
    fun `filename from lastPathSegment is passed to parser when cursor is null`() = runTest {
        every { uri.lastPathSegment } returns "202603.xlsx"
        every { contentResolver.openInputStream(uri) } returns "x".byteInputStream()
        coEvery { parser.parse(any(), "202603.xlsx", "ron") } returns emptyList()
        coEvery { repository.importShifts(any()) } returns Pair(0, 0)
        coEvery { repository.pruneOldMonths() } just Runs

        useCase(uri, "ron")

        coVerify { parser.parse(any(), "202603.xlsx", "ron") }
    }

    // ─── error paths ──────────────────────────────────────────────────────────

    @Test
    fun `blank alias returns Error without touching the file`() = runTest {
        val result = useCase(uri, "  ")

        assertTrue(result is ImportResult.Error)
        assertTrue((result as ImportResult.Error).message.contains("alias"))
        // ContentResolver should never be called
        io.mockk.verify(exactly = 0) { contentResolver.openInputStream(any()) }
    }

    @Test
    fun `null InputStream returns Error with could-not-open message`() = runTest {
        every { contentResolver.openInputStream(uri) } returns null

        val result = useCase(uri, "ron")

        assertEquals(ImportResult.Error("Could not open file"), result)
    }

    @Test
    fun `parser exception returns Error with exception class name`() = runTest {
        every { contentResolver.openInputStream(uri) } returns "x".byteInputStream()
        coEvery { parser.parse(any(), any(), any()) } throws IllegalArgumentException("Bad file")

        val result = useCase(uri, "ron")

        assertTrue(result is ImportResult.Error)
        assertTrue((result as ImportResult.Error).message.contains("IllegalArgumentException"))
    }

    @Test
    fun `repository exception returns Error`() = runTest {
        every { contentResolver.openInputStream(uri) } returns "x".byteInputStream()
        coEvery { parser.parse(any(), any(), any()) } returns sampleShifts
        coEvery { repository.importShifts(any()) } throws RuntimeException("DB error")

        val result = useCase(uri, "ron")

        assertTrue(result is ImportResult.Error)
        assertTrue((result as ImportResult.Error).message.contains("RuntimeException"))
    }

    @Test
    fun `alias is forwarded to parser unchanged`() = runTest {
        every { contentResolver.openInputStream(uri) } returns "x".byteInputStream()
        coEvery { parser.parse(any(), any(), "bob") } returns emptyList()
        coEvery { repository.importShifts(any()) } returns Pair(0, 0)
        coEvery { repository.pruneOldMonths() } just Runs

        useCase(uri, "bob")

        coVerify { parser.parse(any(), any(), "bob") }
    }
}

