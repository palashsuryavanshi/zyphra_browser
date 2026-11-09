package com.zyphra.browser.update

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.WorkManager
import com.zyphra.browser.DownloadEngine
import com.zyphra.browser.MainActivity
import com.zyphra.browser.R
import com.google.gson.Gson
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * Manager class for handling update operations including background checks,
 * notifications, and downloads.
 */
class UpdateManager private constructor(private val context: Context) {
    
    companion object {
        private const val TAG = "UpdateManager"
        private const val NOTIFICATION_CHANNEL_ID = "update_channel"
        private const val UPDATE_INFO_KEY = "update_info"
        
        @Volatile
        private var instance: UpdateManager? = null
        
        fun getInstance(context: Context): UpdateManager {
            return instance ?: synchronized(this) {
                instance ?: UpdateManager(context.applicationContext).also { instance = it }
            }
        }
    }
    
    private val notificationManager: NotificationManager by lazy {
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }
    
    /**
     * Initializes the update system by scheduling periodic checks
     */
    fun initialize() {
        createNotificationChannel()
        schedulePeriodicCheck()
    }
    
    /**
     * Schedules periodic update checks
     */
    fun schedulePeriodicCheck() {
        val workRequest = UpdateWorker.getPeriodicRequest()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            UpdateWorker.WORK_NAME,
            androidx.work.ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
        Log.d(TAG, "Periodic update check scheduled")
    }
    
    /**
     * Manually checks for updates and shows result
     */
    suspend fun checkForUpdatesManually(): UpdateCheckerJava.UpdateInfo? {
        val updateChecker = UpdateCheckerJava(context)
        val updateInfo = updateChecker.checkForUpdatesSync()
        
        if (updateInfo != null) {
            saveUpdateInfo(updateInfo)
            showUpdateNotification(context, updateInfo)
        }
        
        updateChecker.recordUpdateCheck()
        return updateInfo
    }
    
    /**
     * Saves update info for later use
     */
    fun saveUpdateInfo(updateInfo: UpdateCheckerJava.UpdateInfo) {
        val prefs = context.getSharedPreferences("update_prefs", Context.MODE_PRIVATE)
        val gson = Gson()
        val json = gson.toJson(updateInfo)
        prefs.edit().putString(UPDATE_INFO_KEY, json).apply()
    }
    
    /**
     * Retrieves saved update info
     */
    fun getSavedUpdateInfo(): UpdateCheckerJava.UpdateInfo? {
        val prefs = context.getSharedPreferences("update_prefs", Context.MODE_PRIVATE)
        val json = prefs.getString(UPDATE_INFO_KEY, null) ?: return null
        
        return try {
            val gson = Gson()
            gson.fromJson(json, UpdateCheckerJava.UpdateInfo::class.java)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse saved update info", e)
            null
        }
    }
    
    /**
     * Shows update notification
     */
    fun showUpdateNotification(context: Context, updateInfo: UpdateCheckerJava.UpdateInfo) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("show_update_dialog", true)
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val notification = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Update Available")
            .setContentText("Zyphra Browser ${updateInfo.newVersion ?: "unknown"} is available")
            .setSmallIcon(R.drawable.ic_gear) // Use existing gear icon
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
        
        notificationManager.notify(1, notification)
    }
    
    /**
     * Enqueues the update APK download using the browser's DownloadEngine.
     * The download will appear in the Downloads screen with progress.
     * Returns the download task ID so progress can be tracked.
     */
    fun enqueueUpdateDownload(updateInfo: UpdateCheckerJava.UpdateInfo): String {
        val fileName = "zyphra-browser-${updateInfo.newVersion ?: "update"}.apk"
        val downloadId = DownloadEngine.enqueue(
            url = updateInfo.asset.browser_download_url,
            name = fileName,
            mime = "application/vnd.android.package-archive",
            folder = "Zyphra Browser",
            headers = emptyMap(),
            userAgent = "Zyphra Browser Update"
        ).id
        
        Log.d(TAG, "Update download enqueued with ID: $downloadId")
        return downloadId
    }

    /**
     * Installs the downloaded APK
     */
    fun installUpdate(apkFile: File) {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(
                Uri.fromFile(apkFile),
                "application/vnd.android.package-archive"
            )
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
        }
        
        context.startActivity(intent)
    }
    
    /**
     * Creates notification channel for Android 8.0+
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "Update Notifications",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for app updates"
                setShowBadge(true)
            }
            
            notificationManager.createNotificationChannel(channel)
        }
    }
}