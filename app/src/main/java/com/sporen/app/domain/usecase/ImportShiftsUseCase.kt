package com.sporen.app.domain.usecase

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import com.sporen.app.data.repository.ShiftRepository
import com.sporen.app.parser.ExcelParser
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

sealed class ImportResult {
    data class Success(val imported: Int, val skipped: Int) : ImportResult()
    data class Error(val message: String) : ImportResult()
}

class ImportShiftsUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
    private val parser: ExcelParser,
    private val repository: ShiftRepository,
) {
    suspend operator fun invoke(uri: Uri, alias: String): ImportResult {
        if (alias.isBlank()) return ImportResult.Error("Geen alias ingesteld — stel je naam in via Instellingen")
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
                ?: return ImportResult.Error("Could not open file")

            val filename = context.contentResolver
                .query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
                ?.use { cursor ->
                    if (cursor.moveToFirst()) cursor.getString(0) else null
                }
                ?: uri.lastPathSegment
                ?: ""
            val shifts = withContext(Dispatchers.IO) {
                inputStream.use { parser.parse(it, filename, alias) }
            }

            val (inserted, skipped) = repository.importShifts(shifts)
            repository.pruneOldMonths()

            ImportResult.Success(imported = inserted, skipped = skipped)
        } catch (e: Throwable) {
            Log.e("SporenImport", "Import failed", e)
            ImportResult.Error("${e.javaClass.simpleName}: ${e.message}")
        }
    }
}

