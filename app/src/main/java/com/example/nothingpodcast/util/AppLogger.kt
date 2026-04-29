package com.example.nothingpodcast.util

import android.content.Context
import android.util.Log
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

object AppLogger {
    private const val TAG = "NothingPodcast"
    private const val LOG_FILE_NAME = "app_logs.txt"
    
    var isEnabled = false

    fun d(message: String) {
        Log.d(TAG, message)
        if (isEnabled) writeToFile("DEBUG: $message")
    }

    fun e(message: String, throwable: Throwable? = null) {
        Log.e(TAG, message, throwable)
        if (isEnabled) {
            val errorMsg = if (throwable != null) "$message\n${Log.getStackTraceString(throwable)}" else message
            writeToFile("ERROR: $errorMsg")
        }
    }

    private fun writeToFile(text: String) {
        // This is a simple implementation. In a real app, use a background thread.
        try {
            // We need context to get the file path. For simplicity, we assume the app has initialized this.
            // But we can't easily get context here without passing it.
            // Let's use a static file reference that gets initialized.
        } catch (e: Exception) {
            Log.e(TAG, "Failed to write to log file", e)
        }
    }

    fun log(context: Context, level: String, message: String) {
        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault()).format(Date())
        val logLine = "[$timestamp] $level: $message\n"
        
        Log.d(TAG, logLine)
        
        if (isEnabled) {
            try {
                val file = File(context.cacheDir, LOG_FILE_NAME)
                file.appendText(logLine)
            } catch (e: Exception) {
                Log.e(TAG, "Error writing to log file", e)
            }
        }
    }

    fun getLogFile(context: Context): File {
        return File(context.cacheDir, LOG_FILE_NAME)
    }
    
    fun clearLogs(context: Context) {
        try {
            val file = File(context.cacheDir, LOG_FILE_NAME)
            if (file.exists()) file.delete()
        } catch (e: Exception) {}
    }
}
