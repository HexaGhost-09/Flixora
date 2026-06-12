package com.hexaghost.flixora.di

import com.hexaghost.flixora.data.repository.MediaRepositoryImpl
import com.hexaghost.flixora.data.repository.WatchlistRepositoryImpl
import com.hexaghost.flixora.domain.repository.MediaRepository
import com.hexaghost.flixora.domain.repository.WatchlistRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindMediaRepository(
        impl: MediaRepositoryImpl
    ): MediaRepository

    @Binds
    @Singleton
    abstract fun bindWatchlistRepository(
        impl: WatchlistRepositoryImpl
    ): WatchlistRepository
}
