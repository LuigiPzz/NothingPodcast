package com.example.nothingpodcast.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.nothingpodcast.data.repository.SyncRepository
import com.example.nothingpodcast.util.AppLogger
import com.google.android.gms.auth.api.signin.GoogleSignIn
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class AutoBackupWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val syncRepository: SyncRepository
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        AppLogger.log(applicationContext, "INFO", "AutoBackupWorker: Starting background auto backup")
        
        return try {
            val account = GoogleSignIn.getLastSignedInAccount(applicationContext)
            if (account == null) {
                AppLogger.log(applicationContext, "INFO", "AutoBackupWorker: No signed-in Google account found, skipping backup")
                return Result.success()
            }

            AppLogger.log(applicationContext, "INFO", "AutoBackupWorker: Executing Google Drive upload for account: ${account.email}")
            val result = syncRepository.upload(account)
            
            if (result.isSuccess) {
                AppLogger.log(applicationContext, "INFO", "AutoBackupWorker: Auto backup completed successfully")
                Result.success()
            } else {
                val exception = result.exceptionOrNull()
                AppLogger.log(applicationContext, "ERROR", "AutoBackupWorker: Backup failed: ${exception?.message}")
                Result.retry()
            }
        } catch (e: Exception) {
            AppLogger.log(applicationContext, "ERROR", "AutoBackupWorker: Critical failure: ${e.message}")
            Result.retry()
        }
    }
}
