package com.zyphra.browser

import android.content.Context
import android.content.pm.PackageManager
import android.os.Binder
import android.os.Process
import android.util.Log

/**
 * Utility for verifying caller identity and signatures for secure IPC.
 * 
 * This class provides cryptographic verification of calling applications
 * to prevent unauthorized access to sensitive services and components.
 */
object CallerVerifier {
    private const val TAG = "ZyphraCallerVerifier"
    
    /**
     * Verifies that the current caller is authorized to access sensitive functionality.
     * 
     * @param context Application context for package manager access
     * @return true if caller is authorized, false otherwise
     */
    fun isCallerAuthorized(context: Context): Boolean {
        val callingUid = Binder.getCallingUid()
        
        // Allow calls from the same application
        if (callingUid == Process.myUid()) {
            return true
        }
        
        // For external callers, verify they have the required permission
        // The manifest-level signature protection provides the primary security
        // This method provides additional runtime verification
        return hasRequiredPermission(context, callingUid)
    }
    
    /**
     * Enforces that only authorized callers can access the service.
     * Throws SecurityException if caller is not authorized.
     */
    fun enforceAuthorizedCaller(context: Context, operation: String = "this operation") {
        if (!isCallerAuthorized(context)) {
            val callingUid = Binder.getCallingUid()
            Log.w(TAG, "Unauthorized access attempt from UID $callingUid for: $operation")
            throw SecurityException("Unauthorized caller for: $operation")
        }
    }
    
    /**
     * Checks if the calling UID has the required signature-level permission.
     */
    private fun hasRequiredPermission(context: Context, callingUid: Int): Boolean {
        val pm = context.packageManager
        
        try {
            // Get packages for the calling UID
            val packages = pm.getPackagesForUid(callingUid) ?: return false
            
            // Check if any package has the required signature permission
            for (packageName in packages) {
                try {
                    val packageInfo = pm.getPackageInfo(
                        packageName, 
                        PackageManager.GET_SIGNATURES
                    )
                    
                    // Verify the package is signed with the same certificate as this app
                    val packageSignatures = packageInfo.signatures
                    if (packageSignatures != null) {
                        for (signature in packageSignatures) {
                            val ourSignatures = pm.getPackageInfo(
                                context.packageName, 
                                PackageManager.GET_SIGNATURES
                            ).signatures
                            
                            for (ourSignature in ourSignatures ?: emptyArray()) {
                                if (signature.toByteArray().contentEquals(ourSignature.toByteArray())) {
                                    return true
                                }
                            }
                        }
                    }
                } catch (e: PackageManager.NameNotFoundException) {
                    Log.w(TAG, "Package not found for UID $callingUid: $packageName", e)
                    continue
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error verifying caller permissions", e)
        }
        
        return false
    }
    
    /**
     * Gets the calling UID for logging and debugging purposes.
     */
    fun getCallingUid(): Int = Binder.getCallingUid()
    
    /**
     * Checks if the current call is from within the same process.
     */
    fun isSameProcess(): Boolean = Binder.getCallingUid() == Process.myUid()
}