package com.example.n8nmonitor.di

import android.content.Context
// BuildConfig will be generated at build time
// For now, we'll use a constant for DEBUG mode
import com.example.n8nmonitor.data.database.ExecutionDao
import com.example.n8nmonitor.data.database.N8nDatabase
import com.example.n8nmonitor.data.database.WorkflowDao
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
    fun provideDatabase(@ApplicationContext context: Context): N8nDatabase {
        // For now, using a simple passphrase - in production this should be more secure
        // TODO: In production, this should be generated securely and stored in EncryptedSharedPreferences
        // For now, using a placeholder that should be replaced with proper key management
        val passphrase = "debug_placeholder_key_should_be_replaced"
        return com.example.n8nmonitor.data.database.DatabaseModule().provideDatabase(context, passphrase)
    }

    @Provides
    fun provideWorkflowDao(database: N8nDatabase): WorkflowDao {
        return database.workflowDao()
    }

    @Provides
    fun provideExecutionDao(database: N8nDatabase): ExecutionDao {
        return database.executionDao()
    }
}