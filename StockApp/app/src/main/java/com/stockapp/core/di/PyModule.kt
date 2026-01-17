package com.stockapp.core.di

import android.content.Context
import com.stockapp.core.py.PyClient
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object PyModule {

    @Provides
    @Singleton
    fun providePyClient(@ApplicationContext context: Context): PyClient {
        return PyClient(context)
    }
}
