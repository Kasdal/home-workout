package com.example.workoutapp.di

import com.example.workoutapp.data.storage.AndroidSourceOpener
import com.example.workoutapp.data.storage.SourceOpener
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class StorageModule {
    @Binds
    @Singleton
    abstract fun bindSourceOpener(impl: AndroidSourceOpener): SourceOpener
}
