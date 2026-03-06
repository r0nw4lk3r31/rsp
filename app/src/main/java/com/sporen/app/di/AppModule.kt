package com.sporen.app.di

import android.content.Context
import androidx.room.Room
import com.sporen.app.data.db.SporenDatabase
import com.sporen.app.data.db.ShiftDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): SporenDatabase =
        Room.databaseBuilder(context, SporenDatabase::class.java, "sporen_rooster.db")
            .build()

    @Provides
    fun provideShiftDao(db: SporenDatabase): ShiftDao = db.shiftDao()
}

