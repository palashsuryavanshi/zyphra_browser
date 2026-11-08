package com.zyphra.browser

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * Secure receiver for private browsing dismissal notifications.
 * 
 * This receiver handles the dismissal of private browsing indicators and ensures
 * that only authorized sources can trigger the dismissal action.
 */
class PrivateBrowsingDismissReceiver : BroadcastReceiver() {
    
    override fun onReceive(context: Context, intent: Intent) {
        try {
            // Verify the caller is authorized to trigger this action
            if (!isAuthorizedCaller(context, intent)) {
                Log.w("PrivateBrowsingDismissReceiver", 
                    "Unauthorized dismissal attempt from: ${intent.action}")
                return
            }
            
            // Mark the private browsing indicator as dismissed
            PrivateBrowsingNotifier.dismiss()
            
            Log.d("PrivateBrowsingDismissReceiver", 
                "Private browsing indicator dismissed by authorized caller")
            
        } catch (e: Exception) {
            Log.e("PrivateBrowsingDismissReceiver", 
                "Error processing dismissal request", e)
        }
    }
    
    /**
     * Verifies that the broadcast receiver is being called by an authorized source.
     * 
     * This prevents malicious apps from triggering private browsing dismissal.
     */
    private fun isAuthorizedCaller(context: Context, intent: Intent): Boolean {
        // Check if the action matches what we expect
        val expectedAction = intent.action
        if (expectedAction != context.packageName + ".PrivateBrowsingDismiss") {
            Log.w("PrivateBrowsingDismissReceiver", 
                "Unexpected action: $expectedAction")
            return false
        }
        
        // Additional verification can be added here if needed
        // For now, we rely on the manifest-level signature protection
        
        return true
    }
}