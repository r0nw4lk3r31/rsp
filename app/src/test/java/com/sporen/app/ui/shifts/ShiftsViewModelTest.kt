package com.sporen.app.ui.shifts

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import com.sporen.app.data.preferences.UserPreferences
import com.sporen.app.data.repository.ShiftRepository
import com.sporen.app.domain.model.Shift
import com.sporen.app.domain.model.ShiftType
import com.sporen.app.domain.usecase.ImportResult
import com.sporen.app.domain.usecase.ImportShiftsUseCase
import com.sporen.app.util.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.Runs
import app.cash.turbine.test
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

@OptIn(ExperimentalCoroutinesApi::class)
class ShiftsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = mockk<ShiftRepository>()
    private val importUseCase = mockk<ImportShiftsUseCase>()
    private val prefs = mockk<UserPreferences>()

    private fun buildViewModel(yearMonth: String = "2026-02"): ShiftsViewModel =
        ShiftsViewModel(
            savedStateHandle = SavedStateHandle(mapOf("yearMonth" to yearMonth)),
            repository = repository,
            importUseCase = importUseCase,
            prefs = prefs,
        )

    @Before
    fun setUp() {
        every { repository.getShiftsForMonth(any()) } returns flowOf(emptyList())
    }

    // ─── initial state ────────────────────────────────────────────────────────

    @Test
    fun `initial month matches nav arg`() {
        val vm = buildViewModel("2026-02")
        assertEquals(YearMonth.of(2026, 2), vm.currentMonth.value)
    }

    @Test
    fun `initial month defaults to current month when arg is blank`() {
        val vm = ShiftsViewModel(
            savedStateHandle = SavedStateHandle(),
            repository = repository,
            importUseCase = importUseCase,
            prefs = prefs,
        )
        assertEquals(YearMonth.now(), vm.currentMonth.value)
    }

    @Test
    fun `initial uiState has no loading and no message`() {
        val vm = buildViewModel()
        assertFalse(vm.uiState.value.isLoading)
        assertNull(vm.uiState.value.importMessage)
    }

    // ─── navigateMonth ────────────────────────────────────────────────────────

    @Test
    fun `navigateMonth +1 advances to next month`() {
        val vm = buildViewModel("2026-02")
        vm.navigateMonth(1)
        assertEquals(YearMonth.of(2026, 3), vm.currentMonth.value)
    }

    @Test
    fun `navigateMonth -1 goes back one month`() {
        val vm = buildViewModel("2026-02")
        vm.navigateMonth(-1)
        assertEquals(YearMonth.of(2026, 1), vm.currentMonth.value)
    }

    @Test
    fun `navigateMonth +12 advances a full year`() {
        val vm = buildViewModel("2026-02")
        vm.navigateMonth(12)
        assertEquals(YearMonth.of(2027, 2), vm.currentMonth.value)
    }

    @Test
    fun `navigateMonth over year boundary works`() {
        val vm = buildViewModel("2026-12")
        vm.navigateMonth(1)
        assertEquals(YearMonth.of(2027, 1), vm.currentMonth.value)
    }

    // ─── shifts StateFlow ─────────────────────────────────────────────────────

    @Test
    fun `shifts reflects repository data for current month`() = runTest {
        val sampleShifts = listOf(sampleShift())
        every { repository.getShiftsForMonth(YearMonth.of(2026, 2)) } returns flowOf(sampleShifts)

        val vm = buildViewModel("2026-02")

        // Subscribing via turbine triggers WhileSubscribed, which starts the upstream.
        vm.shifts.test {
            // StateFlow always delivers the current value first. The upstream
            // (flowOf) emits synchronously on UnconfinedTestDispatcher, so we
            // skip the initial emptyList() if it lands before the real data.
            var item = awaitItem()
            if (item.isEmpty()) item = awaitItem()
            assertEquals(sampleShifts, item)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `shifts updates when month changes`() = runTest {
        val febShifts = listOf(sampleShift("2026-02-05"))
        val marShifts = listOf(sampleShift("2026-03-01"), sampleShift("2026-03-10"))
        every { repository.getShiftsForMonth(YearMonth.of(2026, 2)) } returns flowOf(febShifts)
        every { repository.getShiftsForMonth(YearMonth.of(2026, 3)) } returns flowOf(marShifts)

        val vm = buildViewModel("2026-02")

        vm.shifts.test {
            // Drain to february data
            var feb = awaitItem()
            if (feb.isEmpty()) feb = awaitItem()
            assertEquals(1, feb.size)

            // Advance month — flatMapLatest switches to the march flow
            vm.navigateMonth(1)

            // Drain to march data
            var mar = awaitItem()
            if (mar.size < 2) mar = awaitItem()
            assertEquals(2, mar.size)

            cancelAndIgnoreRemainingEvents()
        }
    }

    // ─── importFile ───────────────────────────────────────────────────────────

    @Test
    fun `importFile sets isLoading during import then clears it`() = runTest {
        val uri = mockk<Uri>()
        every { prefs.alias } returns flowOf("ron")
        coEvery { importUseCase(uri, "ron") } returns ImportResult.Success(1, 0)

        val vm = buildViewModel()
        vm.importFile(uri)
        advanceUntilIdle()

        assertFalse(vm.uiState.value.isLoading)
    }

    @Test
    fun `importFile sets success message with imported count`() = runTest {
        val uri = mockk<Uri>()
        every { prefs.alias } returns flowOf("ron")
        coEvery { importUseCase(uri, "ron") } returns ImportResult.Success(imported = 5, skipped = 0)

        val vm = buildViewModel()
        vm.importFile(uri)
        advanceUntilIdle()

        assertEquals("5 nieuw geïmporteerd", vm.uiState.value.importMessage)
    }

    @Test
    fun `importFile appends skipped count to message when skipped is non-zero`() = runTest {
        val uri = mockk<Uri>()
        every { prefs.alias } returns flowOf("ron")
        coEvery { importUseCase(uri, "ron") } returns ImportResult.Success(imported = 3, skipped = 2)

        val vm = buildViewModel()
        vm.importFile(uri)
        advanceUntilIdle()

        val msg = vm.uiState.value.importMessage ?: ""
        assertTrue(msg.contains("3 nieuw"))
        assertTrue(msg.contains("2 overgeslagen"))
    }

    @Test
    fun `importFile sets error message on ImportResult Error`() = runTest {
        val uri = mockk<Uri>()
        every { prefs.alias } returns flowOf("ron")
        coEvery { importUseCase(uri, "ron") } returns ImportResult.Error("Could not open file")

        val vm = buildViewModel()
        vm.importFile(uri)
        advanceUntilIdle()

        assertTrue(vm.uiState.value.importMessage?.contains("Fout") == true)
    }

    // ─── clearImportMessage ───────────────────────────────────────────────────

    @Test
    fun `clearImportMessage nulls the importMessage`() = runTest {
        val uri = mockk<Uri>()
        every { prefs.alias } returns flowOf("ron")
        coEvery { importUseCase(uri, "ron") } returns ImportResult.Success(1, 0)

        val vm = buildViewModel()
        vm.importFile(uri)
        advanceUntilIdle()

        vm.clearImportMessage()
        assertNull(vm.uiState.value.importMessage)
    }

    // ─── deleteShift ──────────────────────────────────────────────────────────

    @Test
    fun `deleteShift calls repository deleteShift`() = runTest {
        val shift = sampleShift()
        coEvery { repository.deleteShift(shift) } just Runs

        val vm = buildViewModel()
        vm.deleteShift(shift)
        advanceUntilIdle()

        coVerify(exactly = 1) { repository.deleteShift(shift) }
    }

    // ─── factory ──────────────────────────────────────────────────────────────

    private fun sampleShift(date: String = "2026-02-05") = Shift(
        id = 1L,
        date = LocalDate.parse(date),
        startTime = "08:00",
        endTime = "16:00",
        crossesMidnight = false,
        shiftType = ShiftType.DAG1,
        ward = "TEST",
    )
}

