package com.hexaghost.flixora.di

import android.content.Context
import androidx.room.Room
import com.hexaghost.flixora.data.local.FlixoraDatabase
import com.hexaghost.flixora.data.local.dao.WatchlistDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): FlixoraDatabase =
        Room.databaseBuilder(
            context,
            FlixoraDatabase::class.java,
            FlixoraDatabase.DATABASE_NAME
        ).build()

    @Provides
    @Singleton
    fun provideWatchlistDao(database: FlixoraDatabase): WatchlistDao =
        database.watchlistDao()
}
