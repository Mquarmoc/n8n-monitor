package com.example.n8nmonitor.data.database

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import android.content.Context
import net.sqlcipher.database.SupportFactory
import javax.inject.Inject
import javax.inject.Singleton

@Database(
    entities = [WorkflowEntity::class, ExecutionEntity::class],
    version = 1,
    exportSchema = false
)
abstract class N8nDatabase : RoomDatabase() {
    abstract fun workflowDao(): WorkflowDao
    abstract fun executionDao(): ExecutionDao

    companion object {
        const val DATABASE_NAME = "n8n_monitor.db"
    }
}

@Singleton
class DatabaseModule @Inject constructor() {
    
    fun provideDatabase(context: Context, passphrase: String): N8nDatabase {
        val factory = SupportFactory(passphrase.toByteArray())
        
        return Room.databaseBuilder(
            context.applicationContext,
            N8nDatabase::class.java,
            N8nDatabase.DATABASE_NAME
        )
        .openHelperFactory(factory)
        .fallbackToDestructiveMigration()
        .build()
    }
} 