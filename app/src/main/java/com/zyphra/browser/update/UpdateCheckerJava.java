package com.zyphra.browser.update;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import com.zyphra.browser.github.GithubApiService;
import com.zyphra.browser.github.Release;
import com.zyphra.browser.github.Asset;

import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Java utility class for checking app updates from GitHub Releases.
 * Using Java to avoid Kotlin platform type issues with Retrofit.
 */
public class UpdateCheckerJava {
    
    private static final String TAG = "UpdateCheckerJava";
    private static final String PREFS_NAME = "update_prefs";
    private static final String LAST_CHECK_KEY = "last_check_time";
    private static final long DAY_IN_MILLIS = 24 * 60 * 60 * 1000L;
    
    private final Context context;
    
    // ABI mapping for GitHub release assets
    private static final Map<String, List<String>> ABI_MAPPING = new HashMap<>();
    static {
        ABI_MAPPING.put("arm64-v8a", Arrays.asList("arm64", "arm64-v8a", "aarch64"));
        ABI_MAPPING.put("armeabi-v7a", Arrays.asList("armeabi-v7a", "arm", "armeabi"));
        ABI_MAPPING.put("x86_64", Arrays.asList("x86_64", "x64"));
        ABI_MAPPING.put("x86", Arrays.asList("x86", "x86"));
    }
    
    public UpdateCheckerJava(Context context) {
        this.context = context.getApplicationContext();
    }
    
    /**
     * Checks if an update check is needed (once per day)
     */
    public boolean isUpdateCheckNeeded() {
        android.content.SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        long lastCheckTime = prefs.getLong(LAST_CHECK_KEY, 0);
        long currentTime = System.currentTimeMillis();
        return (currentTime - lastCheckTime) >= DAY_IN_MILLIS;
    }
    
    /**
     * Records that an update check was performed
     */
    public void recordUpdateCheck() {
        android.content.SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putLong(LAST_CHECK_KEY, System.currentTimeMillis()).apply();
    }
    
    /**
     * Gets the current device ABI
     */
    public String getCurrentAbi() {
        try {
            if (Build.SUPPORTED_ABIS.length > 0) {
                return Build.SUPPORTED_ABIS[0];
            } else {
                return "arm64-v8a";
            }
        } catch (Exception e) {
            Log.w(TAG, "Failed to get ABI, defaulting to arm64-v8a", e);
            return "arm64-v8a";
        }
    }
    
    /**
     * Finds the correct APK asset for the current device ABI
     */
    public Asset findMatchingAsset(Release release, String abi) {
        List<String> abiPatterns = ABI_MAPPING.get(abi);
        if (abiPatterns == null) return null;
        
        for (Asset asset : release.getAssets()) {
            for (String pattern : abiPatterns) {
                String assetName = asset.getName();
                if (assetName.toLowerCase(Locale.US).contains(pattern.toLowerCase(Locale.US)) &&
                    assetName.toLowerCase(Locale.US).endsWith(".apk") &&
                    !assetName.toLowerCase(Locale.US).contains("universal")) {
                    return asset;
                }
            }
        }
        return null;
    }
    
    /**
     * Compares version strings (semantic versioning)
     */
    public boolean isNewerVersion(String currentVersion, String newVersion) {
        if (newVersion == null) return false;
        String[] currentParts = currentVersion.split("\\.");
        String[] newParts = newVersion.split("\\.");
        
        int maxLen = Math.max(currentParts.length, newParts.length);
        for (int i = 0; i < maxLen; i++) {
            int currentPart = i < currentParts.length ? parseIntSafe(currentParts[i]) : 0;
            int newPart = i < newParts.length ? parseIntSafe(newParts[i]) : 0;
            
            if (newPart > currentPart) return true;
            if (newPart < currentPart) return false;
        }
        return false;
    }
    
    private int parseIntSafe(String s) {
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
    
    /**
     * Strips 'v' prefix from version string
     */
    public String stripVPrefix(String version) {
        if (version == null) return "";
        return version.startsWith("v") ? version.substring(1) : version;
    }
    
    /**
     * Parses GitHub release date string to timestamp
     */
    private long parseReleaseDate(String dateString) {
        try {
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
            format.setTimeZone(TimeZone.getTimeZone("UTC"));
            java.util.Date date = format.parse(dateString);
            return date != null ? date.getTime() : 0L;
        } catch (Exception e) {
            Log.w(TAG, "Failed to parse date: " + dateString, e);
            return 0L;
        }
    }
    
    /**
     * Checks for updates and returns update information if available
     * This is a synchronous version for use in Worker
     */
    public UpdateInfo checkForUpdatesSync() {
        try {
            GithubApiService githubApi = GithubApiService.Companion.create();
            retrofit2.Response<List<Release>> response = githubApi.getReleases().execute();
            
            if (!response.isSuccessful()) {
                Log.e(TAG, "Failed to fetch releases: " + response.code());
                return null;
            }
            
            List<Release> releaseList = response.body();
            if (releaseList == null || releaseList.isEmpty()) {
                Log.w(TAG, "No releases found");
                return null;
            }
            
            // Filter out prereleases and get the latest stable release
            List<Release> stableReleases = new ArrayList<>();
            for (Release release : releaseList) {
                if (!release.getPrerelease()) {
                    stableReleases.add(release);
                }
            }
            
            if (stableReleases.isEmpty()) {
                Log.w(TAG, "No stable releases found");
                return null;
            }
            
            Release latestRelease = null;
            long latestDate = 0;
            for (Release release : stableReleases) {
                long date = parseReleaseDate(release.getPublished_at());
                if (date > latestDate) {
                    latestDate = date;
                    latestRelease = release;
                }
            }
            
            if (latestRelease == null) {
                return null;
            }
            
            String currentVersion;
            try {
                currentVersion = context.getPackageManager()
                    .getPackageInfo(context.getPackageName(), 0).versionName;
            } catch (PackageManager.NameNotFoundException e) {
                currentVersion = "1.0";
            }
            
            String tagName = latestRelease.getTag_name();
            String newVersion = stripVPrefix(tagName);
            
            if (isNewerVersion(currentVersion, newVersion)) {
                String abi = getCurrentAbi();
                Asset matchingAsset = findMatchingAsset(latestRelease, abi);
                
                if (matchingAsset != null) {
                    Log.d(TAG, "Update available: " + latestRelease.getTag_name());
                    return new UpdateInfo(latestRelease, matchingAsset, currentVersion, newVersion);
                } else {
                    Log.w(TAG, "No matching APK found for ABI: " + abi);
                }
            }
            
            Log.d(TAG, "No updates available");
            return null;
            
        } catch (Exception e) {
            Log.e(TAG, "Error checking for updates", e);
            return null;
        }
    }
    
    /**
     * Data class containing update information
     */
    public static class UpdateInfo {
        public final Release release;
        public final Asset asset;
        public final String currentVersion;
        public final String newVersion;
        
        public UpdateInfo(Release release, Asset asset, String currentVersion, String newVersion) {
            this.release = release;
            this.asset = asset;
            this.currentVersion = currentVersion;
            this.newVersion = newVersion;
        }
    }
}