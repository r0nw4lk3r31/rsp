package com.sporen.app.parser

import com.sporen.app.domain.model.Shift
import com.sporen.app.domain.model.ShiftType
import org.apache.poi.ss.usermodel.Cell
import org.apache.poi.ss.usermodel.CellType
import org.apache.poi.ss.usermodel.Row
import org.apache.poi.ss.usermodel.WorkbookFactory
import java.io.InputStream
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Parses the company's specific .xlsx schedule format.
 *
 * Layout per sheet (0-indexed columns):
 *  Col 0 (A)  — day-of-week abbreviation: Ma/Di/Wo/Do/Vr/Za/Zo
 *  Col 1 (B)  — day number (integer)
 *  Col 4 (E)  — DP person name (fixed shift 09:00–14:30)
 *  Col 9 (J)  — DAG 1 person name
 *  Col 10 (K) — DAG 1 start time
 *  Col 11 (L) — DAG 1 end time
 *  Col 13 (N) — DAG 2 person name
 *  Col 14 (O) — DAG 2 start time
 *  Col 15 (P) — DAG 2 end time
 *  Col 18 (S) — NACHT person 1 name
 *  Col 19 (T) — NACHT person 2 name
 *  Col 20 (U) — NACHT start time
 *  Col 21 (V) — NACHT end time
 *
 * Filename format: YYYYMM.xlsx — year and month are extracted from filename.
 * Data rows start at row index 3 (row 4 in Excel).
 */
@Singleton
class ExcelParser @Inject constructor() {

    // Column indices (0-based) matching the company's .xlsx schedule layout
    private companion object {
        const val COL_DAY_NUMBER  = 1
        const val COL_DP_NAME     = 4
        const val COL_DAG1_NAME   = 9
        const val COL_DAG1_START  = 10
        const val COL_DAG1_END    = 11
        const val COL_DAG2_NAME   = 13
        const val COL_DAG2_START  = 14
        const val COL_DAG2_END    = 15
        const val COL_NACHT1_NAME = 18
        const val COL_NACHT2_NAME = 19
        const val COL_NACHT_START = 20
        const val COL_NACHT_END   = 21
    }

    private val timeOut = DateTimeFormatter.ofPattern("HH:mm")

    fun parse(inputStream: InputStream, filename: String, alias: String): List<Shift> {
        val (year, month) = extractYearMonth(filename)
        val normalizedAlias = alias.trim().lowercase()
        val shifts = mutableListOf<Shift>()
        val seen = mutableSetOf<Pair<LocalDate, String>>() // dedup within file

        val workbook = WorkbookFactory.create(inputStream)

        for (sheetIndex in 0 until workbook.numberOfSheets) {
            val sheet = workbook.getSheetAt(sheetIndex)
            val wardName = sheet.sheetName.trim()

            for (rowIndex in 3..sheet.lastRowNum) {
                val row = sheet.getRow(rowIndex) ?: continue
                val dayNum = row.intCell(COL_DAY_NUMBER) ?: continue

                val date = try {
                    LocalDate.of(year, month, dayNum)
                } catch (e: Exception) {
                    continue
                }

                // DP slot — fixed times 09:00–14:30
                if (row.nameMatches(COL_DP_NAME, normalizedAlias)) {
                    shifts.addIfNew(
                        seen, date, "09:00", "14:30",
                        crossesMidnight = false, ShiftType.DP, wardName
                    )
                }

                // DAG 1
                if (row.nameMatches(COL_DAG1_NAME, normalizedAlias)) {
                    val start = row.timeCell(COL_DAG1_START) ?: continue
                    val end = row.timeCell(COL_DAG1_END) ?: continue
                    shifts.addIfNew(
                        seen, date, start.format(timeOut), end.format(timeOut),
                        crossesMidnight = false, ShiftType.DAG1, wardName
                    )
                }

                // DAG 2
                if (row.nameMatches(COL_DAG2_NAME, normalizedAlias)) {
                    val start = row.timeCell(COL_DAG2_START) ?: continue
                    val end = row.timeCell(COL_DAG2_END) ?: continue
                    shifts.addIfNew(
                        seen, date, start.format(timeOut), end.format(timeOut),
                        crossesMidnight = false, ShiftType.DAG2, wardName
                    )
                }

                // NACHT person 1 (col S)
                if (row.nameMatches(COL_NACHT1_NAME, normalizedAlias)) {
                    val start = row.timeCell(COL_NACHT_START) ?: continue
                    val end = row.timeCell(COL_NACHT_END) ?: continue
                    shifts.addIfNew(
                        seen, date, start.format(timeOut), end.format(timeOut),
                        crossesMidnight = true, ShiftType.NACHT, wardName
                    )
                }

                // NACHT person 2 / WE PERM (col T)
                if (row.nameMatches(COL_NACHT2_NAME, normalizedAlias)) {
                    val start = row.timeCell(COL_NACHT_START) ?: continue
                    val end = row.timeCell(COL_NACHT_END) ?: continue
                    shifts.addIfNew(
                        seen, date, start.format(timeOut), end.format(timeOut),
                        crossesMidnight = true, ShiftType.NACHT, wardName
                    )
                }
            }
        }

        workbook.close()
        return shifts
    }

    // ---- Helpers ----

    private fun extractYearMonth(filename: String): Pair<Int, Int> {
        // Strip path and extension, expect YYYYMM
        val bare = filename.substringAfterLast('/').substringAfterLast('\\')
            .substringBefore('.')
        return if (bare.length >= 6) {
            val year = bare.take(4).toIntOrNull() ?: throw IllegalArgumentException("Bad filename year: $filename")
            val month = bare.substring(4, 6).toIntOrNull() ?: throw IllegalArgumentException("Bad filename month: $filename")
            Pair(year, month)
        } else {
            throw IllegalArgumentException("Cannot derive year/month from filename: $filename. Expected YYYYMM.xlsx")
        }
    }

    private fun Row.nameMatches(colIndex: Int, alias: String): Boolean {
        val cell = getCell(colIndex) ?: return false
        val raw = cell.stringValue()?.trim()?.lowercase() ?: return false
        // Handle slash-separated or comma-separated names like "Ron/noemie"
        return raw.split('/', ',').any { it.trim() == alias }
    }

    private fun Row.intCell(colIndex: Int): Int? {
        val cell = getCell(colIndex) ?: return null
        return when (cell.cellType) {
            CellType.NUMERIC -> cell.numericCellValue.toInt()
            else -> cell.stringValue()?.trim()?.toIntOrNull()
        }
    }

    private fun Row.timeCell(colIndex: Int): LocalTime? {
        val cell = getCell(colIndex) ?: return null
        return cell.localTimeValue()
    }

    private fun Cell.stringValue(): String? = when (cellType) {
        CellType.STRING -> stringCellValue?.takeIf { it.isNotBlank() }
        CellType.NUMERIC -> numericCellValue.toString()
        else -> null
    }

    private fun Cell.localTimeValue(): LocalTime? {
        return when (cellType) {
            CellType.NUMERIC -> {
                // Excel time is stored as a fraction of a 24-hour day; avoid deprecated java.util.Date
                val totalSeconds = Math.round(numericCellValue % 1.0 * 86_400.0)
                LocalTime.ofSecondOfDay(totalSeconds)
            }
            CellType.STRING -> {
                val s = stringCellValue?.trim() ?: return null
                runCatching { LocalTime.parse(s, DateTimeFormatter.ofPattern("H:mm")) }.getOrNull()
                    ?: runCatching { LocalTime.parse(s, DateTimeFormatter.ofPattern("HH:mm")) }.getOrNull()
            }
            else -> null
        }
    }

    private fun MutableList<Shift>.addIfNew(
        seen: MutableSet<Pair<LocalDate, String>>,
        date: LocalDate,
        startTime: String,
        endTime: String,
        crossesMidnight: Boolean,
        shiftType: ShiftType,
        ward: String,
    ) {
        val key = Pair(date, startTime)
        if (seen.add(key)) {
            add(Shift(date = date, startTime = startTime, endTime = endTime,
                crossesMidnight = crossesMidnight, shiftType = shiftType, ward = ward))
        }
    }
}

