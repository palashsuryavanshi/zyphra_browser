package com.zyphra.browser.update

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit

/**
 * WorkManager worker for periodic update checks.
 */
class UpdateWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    
    companion object {
        private const val TAG = "UpdateWorker"
        const val WORK_NAME = "update_check_worker"
        
        fun getPeriodicRequest(): PeriodicWorkRequest {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .setRequiresBatteryNotLow(true)
                .build()
            
            return PeriodicWorkRequest.Builder(
                UpdateWorker::class.java,
                1, // Repeat interval
                TimeUnit.DAYS
            )
            .setConstraints(constraints)
            .setInitialDelay(1, TimeUnit.HOURS) // Initial delay
            .build()
        }
    }
    
    override suspend fun doWork(): Result {
        return try {
            Log.d(TAG, "Starting periodic update check")
            
            val updateChecker = UpdateCheckerJava(applicationContext)
            
            if (updateChecker.isUpdateCheckNeeded()) {
                val updateInfo = updateChecker.checkForUpdatesSync()
                
                if (updateInfo != null) {
                    // Store update info for later use
                    UpdateManager.getInstance(applicationContext).saveUpdateInfo(updateInfo)
                    
                    // Show notification that update is available
                    UpdateManager.getInstance(applicationContext).showUpdateNotification(
                        applicationContext,
                        updateInfo
                    )
                }
                
                updateChecker.recordUpdateCheck()
            }
            
            Result.success()
            
        } catch (e: Exception) {
            Log.e(TAG, "Error in update worker", e)
            Result.retry()
        }
    }
}