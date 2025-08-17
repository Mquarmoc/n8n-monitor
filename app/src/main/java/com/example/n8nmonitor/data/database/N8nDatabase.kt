package com.example.n8nmonitor.data.database

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import android.content.Context
import net.sqlcipher.database.SupportFactory
import javax.inject.Inject
import javax.inject.Singleton

@Database(
    entities = [WorkflowEntity::class, ExecutionEntity::class],
    version = 2,
    exportSchema = false
)
abstract class N8nDatabase : RoomDatabase() {
    abstract fun workflowDao(): WorkflowDao
    abstract fun executionDao(): ExecutionDao

    companion object {
        const val DATABASE_NAME = "n8n_monitor.db"
        
        // Migration from version 1 to 2 (adding new indexes)
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Create new indexes for WorkflowEntity
                db.execSQL("CREATE INDEX IF NOT EXISTS index_workflows_isBookmarked ON workflows(isBookmarked)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_workflows_lastExecutionStatus ON workflows(lastExecutionStatus)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_workflows_lastSyncTime ON workflows(lastSyncTime)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_workflows_active_isBookmarked_updatedAt ON workflows(active, isBookmarked, updatedAt)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_workflows_active_lastExecutionStatus ON workflows(active, lastExecutionStatus)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_workflows_lastSyncTime_active ON workflows(lastSyncTime, active)")
                
                // Create new indexes for ExecutionEntity
                db.execSQL("CREATE INDEX IF NOT EXISTS index_executions_lastSyncTime ON executions(lastSyncTime)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_executions_workflowId_startTime ON executions(workflowId, startTime)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_executions_workflowId_status ON executions(workflowId, status)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_executions_status_startTime ON executions(status, startTime)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_executions_workflowId_status_startTime ON executions(workflowId, status, startTime)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_executions_lastSyncTime_workflowId ON executions(lastSyncTime, workflowId)")
            }
        }
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
        .addMigrations(N8nDatabase.MIGRATION_1_2)
        .fallbackToDestructiveMigration()
        .build()
    }
}