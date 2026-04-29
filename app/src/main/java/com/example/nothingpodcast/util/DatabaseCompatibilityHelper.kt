package com.example.nothingpodcast.util

import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import com.example.nothingpodcast.data.local.database.AppDatabase
import java.io.File

object DatabaseCompatibilityHelper {

    fun isDatabaseCompatible(context: Context): Boolean {
        val dbFile = context.getDatabasePath(AppDatabase.DATABASE_NAME)
        if (!dbFile.exists()) return true 

        return try {
            // Open database in read-only mode to check version without triggering anything
            val db = android.database.sqlite.SQLiteDatabase.openDatabase(
                dbFile.path,
                null,
                android.database.sqlite.SQLiteDatabase.OPEN_READONLY
            )
            val currentVersion = db.version
            db.close()
            
            // Compatible if version is 4 or newer. Room handles upgrades > 4.
            currentVersion >= 4
        } catch (e: Exception) {
            // If we can't even open it, it's definitely not compatible or corrupted
            false
        }
    }

    fun resetDatabase(context: Context): Boolean {
        val dbFile = context.getDatabasePath(AppDatabase.DATABASE_NAME)
        val dbWal = File(dbFile.path + "-wal")
        val dbShm = File(dbFile.path + "-shm")
        
        return try {
            dbFile.delete()
            if (dbWal.exists()) dbWal.delete()
            if (dbShm.exists()) dbShm.delete()
            true
        } catch (e: Exception) {
            false
        }
    }
}
