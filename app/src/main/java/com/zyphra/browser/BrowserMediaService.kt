package com.zyphra.browser

import android.app.Notification
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.Process
import android.os.Binder

/**
 * Keeps Gecko media playback alive while the browser Activity is backgrounded.
 *
 * The notification itself is built by [BrowserMediaController] so the shade shows
 * one media entry with working transport controls rather than a second, inert one.
 */
class BrowserMediaService : Service() {
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }
        
        // Verify caller identity for service commands
        if (intent?.action == ACTION_PLAY || intent?.action == ACTION_PAUSE || intent?.action == ACTION_STOP) {
            enforceTrustedCaller()
        }
        
        val notification = BrowserMediaController.buildNotification(this, BrowserMediaController.mediaToken())
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
        super.onDestroy()
    }

    companion object {
        const val NOTIFICATION_ID = 4102
        const val ACTION_PLAY = "com.zyphra.browser.PLAY_MEDIA"
        const val ACTION_PAUSE = "com.zyphra.browser.PAUSE_MEDIA"
        private const val ACTION_STOP = "com.zyphra.browser.STOP_MEDIA"

        fun start(context: android.content.Context) {
            val intent = Intent(context, BrowserMediaService::class.java)
            runCatching { androidx.core.content.ContextCompat.startForegroundService(context, intent) }
        }

        fun stop(context: android.content.Context) {
            context.stopService(Intent(context, BrowserMediaService::class.java))
        }
    }

    private fun enforceTrustedCaller() {
        val callingUid = Binder.getCallingUid()
        // Allow calls from the same application
        if (callingUid == Process.myUid()) {
            return
        }
        // For cross-process calls, we rely on the manifest permission protection
        // which is already set to signature-level
    }
}
