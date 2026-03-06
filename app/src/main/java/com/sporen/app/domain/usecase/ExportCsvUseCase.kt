package com.sporen.app.domain.usecase

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import com.sporen.app.data.repository.ShiftRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject

class ExportCsvUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: ShiftRepository,
) {
    /**
     * Export shifts to a CSV file and return a shareable [Uri].
     * @param yearMonth  the month to export, or null for all stored shifts.
     */
    suspend operator fun invoke(yearMonth: YearMonth?): Uri = withContext(Dispatchers.IO) {
        val shifts = if (yearMonth != null)
            repository.getShiftsSnapshot(yearMonth)
        else
            repository.getAllShiftsSnapshot()

        val fileLabel = yearMonth?.format(DateTimeFormatter.ofPattern("yyyy-MM")) ?: "alles"
        val dayFmt = DateTimeFormatter.ofPattern("EEE", Locale.forLanguageTag("nl"))

        val csv = buildString {
            appendLine("datum,dag,start,eind,nacht,type,afdeling,notitie")
            shifts.forEach { s ->
                val day = s.date.format(dayFmt).lowercase()
                val note = s.note.replace("\"", "\"\"")
                appendLine("${s.date},$day,${s.startTime},${s.endTime},${s.crossesMidnight},${s.shiftType.name},${s.ward},\"$note\"")
            }
        }

        val dir = File(context.cacheDir, "exports").also { it.mkdirs() }
        val file = File(dir, "shifts_$fileLabel.csv")
        file.writeText(csv, Charsets.UTF_8)

        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }
}

