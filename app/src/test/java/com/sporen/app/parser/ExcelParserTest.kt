package com.sporen.app.parser

import com.sporen.app.domain.model.ShiftType
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.time.LocalDate

class ExcelParserTest {

    private lateinit var parser: ExcelParser

    @Before
    fun setUp() {
        parser = ExcelParser()
    }

    // ─── helpers ──────────────────────────────────────────────────────────────

    /**
     * Build an in-memory XLSX with a single sheet and return its InputStream.
     * [applyRows] receives the sheet so callers can add rows before the stream is sealed.
     */
    private fun workbook(
        sheetName: String = "PIJL",
        applyRows: (org.apache.poi.ss.usermodel.Sheet) -> Unit = {},
    ): ByteArrayInputStream {
        val wb = XSSFWorkbook()
        applyRows(wb.createSheet(sheetName))
        return wb.toStream()
    }

    /** Build a workbook with multiple sheets. */
    private fun multiSheetWorkbook(
        vararg sheets: Pair<String, (org.apache.poi.ss.usermodel.Sheet) -> Unit>,
    ): ByteArrayInputStream {
        val wb = XSSFWorkbook()
        sheets.forEach { (name, fill) -> fill(wb.createSheet(name)) }
        return wb.toStream()
    }

    private fun XSSFWorkbook.toStream(): ByteArrayInputStream {
        val out = ByteArrayOutputStream()
        write(out)
        close()
        return ByteArrayInputStream(out.toByteArray())
    }

    /**
     * Create a data row at [rowIndex] (data rows start at index 3).
     * Only the cells explicitly provided are created; everything else is absent.
     */
    private fun org.apache.poi.ss.usermodel.Sheet.dataRow(
        rowIndex: Int,
        dayNum: Int? = null,
        dpName: String? = null,
        dag1Name: String? = null, dag1Start: String? = null, dag1End: String? = null,
        dag2Name: String? = null, dag2Start: String? = null, dag2End: String? = null,
        nacht1Name: String? = null,
        nacht2Name: String? = null,
        nachtStart: String? = null, nachtEnd: String? = null,
    ) {
        val row = createRow(rowIndex)
        dayNum?.let    { row.createCell(1).setCellValue(it.toDouble()) }
        dpName?.let    { row.createCell(4).setCellValue(it) }
        dag1Name?.let  { row.createCell(9).setCellValue(it) }
        dag1Start?.let { row.createCell(10).setCellValue(it) }
        dag1End?.let   { row.createCell(11).setCellValue(it) }
        dag2Name?.let  { row.createCell(13).setCellValue(it) }
        dag2Start?.let { row.createCell(14).setCellValue(it) }
        dag2End?.let   { row.createCell(15).setCellValue(it) }
        nacht1Name?.let { row.createCell(18).setCellValue(it) }
        nacht2Name?.let { row.createCell(19).setCellValue(it) }
        nachtStart?.let { row.createCell(20).setCellValue(it) }
        nachtEnd?.let   { row.createCell(21).setCellValue(it) }
    }

    // ─── filename parsing ─────────────────────────────────────────────────────

    @Test(expected = IllegalArgumentException::class)
    fun `bad filename throws IllegalArgumentException`() {
        parser.parse(workbook(), "schedule.xlsx", "ron")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `filename too short throws`() {
        parser.parse(workbook(), "2602.xlsx", "ron")
    }

    @Test
    fun `filename with path separators is handled`() {
        val stream = workbook { it.dataRow(3, dayNum = 1, dag1Name = "ron", dag1Start = "8:00", dag1End = "16:00") }
        val shifts = parser.parse(stream, "/downloads/202602.xlsx", "ron")
        assertEquals(1, shifts.size)
        assertEquals(LocalDate.of(2026, 2, 1), shifts[0].date)
    }

    // ─── DAG 1 ────────────────────────────────────────────────────────────────

    @Test
    fun `DAG1 shift is parsed correctly`() {
        val stream = workbook { it.dataRow(3, dayNum = 3, dag1Name = "ron", dag1Start = "8:00", dag1End = "16:00") }
        val shifts = parser.parse(stream, "202602.xlsx", "ron")

        assertEquals(1, shifts.size)
        with(shifts[0]) {
            assertEquals(LocalDate.of(2026, 2, 3), date)
            assertEquals("08:00", startTime)
            assertEquals("16:00", endTime)
            assertEquals(ShiftType.DAG1, shiftType)
            assertEquals("PIJL", ward)
            assertFalse(crossesMidnight)
        }
    }

    @Test
    fun `alias match is case-insensitive`() {
        val stream = workbook { it.dataRow(3, dayNum = 5, dag1Name = "RON", dag1Start = "9:00", dag1End = "17:00") }
        val shifts = parser.parse(stream, "202602.xlsx", "ron")
        assertEquals(1, shifts.size)
    }

    @Test
    fun `alias with extra whitespace in cell is ignored`() {
        val wb = XSSFWorkbook()
        val sheet = wb.createSheet("PIJL")
        val row = sheet.createRow(3)
        row.createCell(1).setCellValue(7.0)
        row.createCell(9).setCellValue("  ron  ")   // extra spaces
        row.createCell(10).setCellValue("8:00")
        row.createCell(11).setCellValue("16:00")
        val shifts = parser.parse(wb.toStream(), "202602.xlsx", "ron")
        assertEquals(1, shifts.size)
    }

    @Test
    fun `non-matching alias returns empty list`() {
        val stream = workbook { it.dataRow(3, dayNum = 7, dag1Name = "jan", dag1Start = "8:00", dag1End = "16:00") }
        val shifts = parser.parse(stream, "202602.xlsx", "ron")
        assertTrue(shifts.isEmpty())
    }

    // ─── DAG 2 ────────────────────────────────────────────────────────────────

    @Test
    fun `DAG2 shift is parsed correctly`() {
        val stream = workbook { it.dataRow(3, dayNum = 20, dag2Name = "ron", dag2Start = "13:00", dag2End = "21:00") }
        val shifts = parser.parse(stream, "202602.xlsx", "ron")

        assertEquals(1, shifts.size)
        with(shifts[0]) {
            assertEquals(ShiftType.DAG2, shiftType)
            assertEquals("13:00", startTime)
            assertEquals("21:00", endTime)
            assertFalse(crossesMidnight)
        }
    }

    // ─── NACHT ────────────────────────────────────────────────────────────────

    @Test
    fun `NACHT person-1 shift has crossesMidnight true`() {
        val stream = workbook { it.dataRow(3, dayNum = 10, nacht1Name = "ron", nachtStart = "21:00", nachtEnd = "9:30") }
        val shifts = parser.parse(stream, "202602.xlsx", "ron")

        assertEquals(1, shifts.size)
        with(shifts[0]) {
            assertTrue(crossesMidnight)
            assertEquals(ShiftType.NACHT, shiftType)
            assertEquals("21:00", startTime)
            assertEquals("09:30", endTime)
        }
    }

    @Test
    fun `NACHT person-2 column is also parsed`() {
        val stream = workbook { it.dataRow(3, dayNum = 15, nacht2Name = "ron", nachtStart = "21:00", nachtEnd = "9:30") }
        val shifts = parser.parse(stream, "202602.xlsx", "ron")
        assertEquals(1, shifts.size)
        assertEquals(ShiftType.NACHT, shifts[0].shiftType)
    }

    // ─── DP ───────────────────────────────────────────────────────────────────

    @Test
    fun `DP shift gets fixed times 09-00 to 14-30`() {
        val stream = workbook { it.dataRow(3, dayNum = 1, dpName = "ron") }
        val shifts = parser.parse(stream, "202602.xlsx", "ron")

        assertEquals(1, shifts.size)
        with(shifts[0]) {
            assertEquals(ShiftType.DP, shiftType)
            assertEquals("09:00", startTime)
            assertEquals("14:30", endTime)
            assertFalse(crossesMidnight)
        }
    }

    @Test
    fun `DP slash-separated name like Ron-noemie matches alias ron`() {
        val stream = workbook { it.dataRow(3, dayNum = 23, dpName = "Ron/noemie") }
        val shifts = parser.parse(stream, "202602.xlsx", "ron")

        assertEquals(1, shifts.size)
        with(shifts[0]) {
            assertEquals(ShiftType.DP, shiftType)
            assertEquals("09:00", startTime)
            assertEquals("14:30", endTime)
            assertEquals(LocalDate.of(2026, 2, 23), date)
        }
    }

    @Test
    fun `comma-separated name in any slot matches alias`() {
        val stream = workbook { it.dataRow(3, dayNum = 5, dag1Name = "jan,ron", dag1Start = "8:00", dag1End = "16:00") }
        val shifts = parser.parse(stream, "202602.xlsx", "ron")
        assertEquals(1, shifts.size)
        assertEquals(ShiftType.DAG1, shifts[0].shiftType)
    }

    // ─── multiple rows / deduplication ───────────────────────────────────────

    @Test
    fun `multiple shifts on different days are all returned`() {
        val stream = workbook { sheet ->
            sheet.dataRow(3, dayNum = 3, dag1Name = "ron", dag1Start = "8:00", dag1End = "16:00")
            sheet.dataRow(4, dayNum = 5, dag1Name = "ron", dag1Start = "8:00", dag1End = "16:00")
            sheet.dataRow(5, dayNum = 10, dag2Name = "ron", dag2Start = "13:00", dag2End = "21:00")
        }
        val shifts = parser.parse(stream, "202602.xlsx", "ron")
        assertEquals(3, shifts.size)
    }

    @Test
    fun `duplicate date and startTime within file is deduplicated`() {
        val stream = workbook { sheet ->
            sheet.dataRow(3, dayNum = 5, dag1Name = "ron", dag1Start = "8:00", dag1End = "16:00")
            // same day + same start — should be skipped
            sheet.dataRow(4, dayNum = 5, dag1Name = "ron", dag1Start = "8:00", dag1End = "16:30")
        }
        val shifts = parser.parse(stream, "202602.xlsx", "ron")
        assertEquals(1, shifts.size)
    }

    @Test
    fun `same day different startTime is NOT deduplicated`() {
        val stream = workbook { sheet ->
            sheet.dataRow(3, dayNum = 5, dag1Name = "ron", dag1Start = "8:00", dag1End = "16:00")
            sheet.dataRow(4, dayNum = 5, dag2Name = "ron", dag2Start = "13:00", dag2End = "21:00")
        }
        val shifts = parser.parse(stream, "202602.xlsx", "ron")
        assertEquals(2, shifts.size)
    }

    // ─── multiple sheets ──────────────────────────────────────────────────────

    @Test
    fun `shifts are read from every sheet`() {
        val stream = multiSheetWorkbook(
            "PIJL" to { it.dataRow(3, dayNum = 3, dag1Name = "ron", dag1Start = "8:00", dag1End = "16:00") },
            "HART" to { it.dataRow(3, dayNum = 4, dag1Name = "ron", dag1Start = "9:00", dag1End = "17:00") },
        )
        val shifts = parser.parse(stream, "202602.xlsx", "ron")
        assertEquals(2, shifts.size)
        assertEquals("PIJL", shifts[0].ward)
        assertEquals("HART", shifts[1].ward)
    }

    // ─── row skipping ─────────────────────────────────────────────────────────

    @Test
    fun `row without day-number cell is skipped`() {
        val wb = XSSFWorkbook()
        val sheet = wb.createSheet("PIJL")
        val row = sheet.createRow(3)
        // col 1 intentionally absent
        row.createCell(9).setCellValue("ron")
        row.createCell(10).setCellValue("8:00")
        row.createCell(11).setCellValue("16:00")
        val shifts = parser.parse(wb.toStream(), "202602.xlsx", "ron")
        assertTrue(shifts.isEmpty())
    }

    @Test
    fun `rows before index 3 are header rows and ignored`() {
        val stream = workbook { sheet ->
            // row 0–2 are headers
            sheet.dataRow(0, dayNum = 1, dag1Name = "ron", dag1Start = "8:00", dag1End = "16:00")
            sheet.dataRow(1, dayNum = 2, dag1Name = "ron", dag1Start = "8:00", dag1End = "16:00")
            sheet.dataRow(2, dayNum = 3, dag1Name = "ron", dag1Start = "8:00", dag1End = "16:00")
            // row 3 is the first real data row
            sheet.dataRow(3, dayNum = 4, dag1Name = "ron", dag1Start = "8:00", dag1End = "16:00")
        }
        val shifts = parser.parse(stream, "202602.xlsx", "ron")
        assertEquals(1, shifts.size)
        assertEquals(LocalDate.of(2026, 2, 4), shifts[0].date)
    }

    // ─── time format flexibility ──────────────────────────────────────────────

    @Test
    fun `single-digit hour string is parsed and zero-padded in output`() {
        val stream = workbook { it.dataRow(3, dayNum = 3, dag1Name = "ron", dag1Start = "8:00", dag1End = "9:30") }
        val shifts = parser.parse(stream, "202602.xlsx", "ron")
        assertEquals("08:00", shifts[0].startTime)
        assertEquals("09:30", shifts[0].endTime)
    }
}

