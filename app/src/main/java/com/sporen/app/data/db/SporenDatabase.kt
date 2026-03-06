package com.sporen.app.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [ShiftEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class SporenDatabase : RoomDatabase() {
    abstract fun shiftDao(): ShiftDao
}

