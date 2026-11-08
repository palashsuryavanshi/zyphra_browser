package com.zyphra.browser

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * Secure receiver for media control actions.
 * 
 * This receiver handles media playback control commands and ensures
 * that only authorized sources can control media playback.
 */
class MediaActionReceiver : BroadcastReceiver() {
    
    override fun onReceive(context: Context, intent: Intent) {
        try {
            val action = intent.action ?: return
            
            // Verify caller authorization before executing any media action
            if (!CallerVerifier.isCallerAuthorized(context)) {
                Log.w("MediaActionReceiver", 
                    "Unauthorized media action attempt: $action")
                return
            }
            
            // Route the action to the appropriate handler
            when (action) {
                BrowserMediaService.ACTION_PLAY -> handlePlayAction(context, intent)
                BrowserMediaService.ACTION_PAUSE -> handlePauseAction(context, intent)
                BrowserMediaService.ACTION_STOP -> handleStopAction(context, intent)
                else -> Log.w("MediaActionReceiver", "Unknown action: $action")
            }
            
        } catch (e: Exception) {
            Log.e("MediaActionReceiver", "Error processing media action", e)
        }
    }
    
    /**
     * Handles the play action.
     */
    private fun handlePlayAction(context: Context, intent: Intent) {
        Log.d("MediaActionReceiver", "Processing play action")
        BrowserMediaService.start(context)
        // Additional play logic can be added here
    }
    
    /**
     * Handles the pause action.
     */
    private fun handlePauseAction(context: Context, intent: Intent) {
        Log.d("MediaActionReceiver", "Processing pause action")
        // Additional pause logic can be added here
    }
    
    /**
     * Handles the stop action.
     */
    private fun handleStopAction(context: Context, intent: Intent) {
        Log.d("MediaActionReceiver", "Processing stop action")
        BrowserMediaService.stop(context)
        // Additional stop logic can be added here
    }
}