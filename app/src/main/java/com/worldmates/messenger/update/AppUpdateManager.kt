package com.worldmates.messenger.update

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.util.Log
import androidx.core.content.FileProvider
import com.worldmates.messenger.network.AppUpdateResponse
import com.worldmates.messenger.network.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Manages app update checking, downloading, and installation.
 *
 * Usage:
 *   val manager = AppUpdateManager(context)
 *   manager.checkForUpdate() // checks server for new version
 *   manager.downloadAndInstall(downloadUrl) // downloads APK and triggers install
 */
class AppUpdateManager(private val context: Context) {

    companion object {
        private const val TAG = "AppUpdateManager"
        private const val PREFS_NAME = "app_update_prefs"
        private const val KEY_LAST_CHECK = "last_update_check"
        private const val KEY_DISMISSED_VERSION = "dismissed_version_code"
        private const val CHECK_INTERVAL_MS = 4 * 60 * 60 * 1000L // 4 hours
        private const val APK_FILE_NAME = "worldmates_update.apk"
    }

    data class UpdateInfo(
        val updateAvailable: Boolean = false,
        val forceUpdate: Boolean = false,
        val latestVersionCode: Int = 0,
        val latestVersionName: String = "",
        val downloadUrl: String = "",
        val changelog: String = "",
        val fileSize: Long = 0
    )

    sealed class DownloadState {
        object Idle : DownloadState()
        data class Downloading(val progress: Int) : DownloadState()
        object Downloaded : DownloadState()
        data class Error(val message: String) : DownloadState()
    }

    private val _updateInfo = MutableStateFlow(UpdateInfo())
    val updateInfo: StateFlow<UpdateInfo> = _updateInfo

    private val _downloadState = MutableStateFlow<DownloadState>(DownloadState.Idle)
    val downloadState: StateFlow<DownloadState> = _downloadState

    private val _isChecking = MutableStateFlow(false)
    val isChecking: StateFlow<Boolean> = _isChecking

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private var downloadId: Long = -1

    /**
     * Get current app version code from BuildConfig
     */
    private fun getCurrentVersionCode(): Int {
        return try {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                pInfo.longVersionCode.toInt()
            } else {
                @Suppress("DEPRECATION")
                pInfo.versionCode
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get version code", e)
            0
        }
    }

    /**
     * Get current app version name
     */
    fun getCurrentVersionName(): String {
        return try {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            pInfo.versionName ?: "unknown"
        } catch (e: Exception) {
            "unknown"
        }
    }

    /**
     * Check if enough time has passed since last check
     */
    fun shouldAutoCheck(): Boolean {
        val lastCheck = prefs.getLong(KEY_LAST_CHECK, 0)
        return System.currentTimeMillis() - lastCheck > CHECK_INTERVAL_MS
    }

    /**
     * Check for app updates from server
     */
    suspend fun checkForUpdate(force: Boolean = false): UpdateInfo {
        if (!force && !shouldAutoCheck()) {
            return _updateInfo.value
        }

        _isChecking.value = true
        return try {
            val versionCode = getCurrentVersionCode()
            val versionName = getCurrentVersionName()

            Log.d(TAG, "Checking for update. Current: $versionName ($versionCode)")

            val response: AppUpdateResponse = withContext(Dispatchers.IO) {
                RetrofitClient.apiService.checkAppUpdate(
                    currentVersionCode = versionCode,
                    currentVersionName = versionName
                )
            }

            if (response.apiStatus == 200) {
                val info = UpdateInfo(
                    updateAvailable = response.updateAvailable,
                    forceUpdate = response.forceUpdate,
                    latestVersionCode = response.latestVersionCode,
                    latestVersionName = response.latestVersionName,
                    downloadUrl = response.downloadUrl,
                    changelog = response.changelog,
                    fileSize = response.fileSize
                )
                _updateInfo.value = info

                // Save check time
                prefs.edit().putLong(KEY_LAST_CHECK, System.currentTimeMillis()).apply()

                Log.d(TAG, "Update check result: available=${info.updateAvailable}, " +
                        "latest=${info.latestVersionName} (${info.latestVersionCode})")
                info
            } else {
                Log.e(TAG, "Update check failed: ${response.errorMessage}")
                _updateInfo.value
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to check for update", e)
            _updateInfo.value
        } finally {
            _isChecking.value = false
        }
    }

    /**
     * Check if user dismissed this version's update
     */
    fun isVersionDismissed(versionCode: Int): Boolean {
        return prefs.getInt(KEY_DISMISSED_VERSION, 0) == versionCode
    }

    /**
     * Dismiss the update dialog for this version
     */
    fun dismissVersion(versionCode: Int) {
        prefs.edit().putInt(KEY_DISMISSED_VERSION, versionCode).apply()
    }

    /**
     * Download the APK using DownloadManager and install
     */
    fun downloadAndInstall(downloadUrl: String) {
        if (downloadUrl.isEmpty()) {
            _downloadState.value = DownloadState.Error("Download URL is empty")
            return
        }

        _downloadState.value = DownloadState.Downloading(0)

        // Delete old APK if exists
        val apkFile = File(
            context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),
            APK_FILE_NAME
        )
        if (apkFile.exists()) {
            apkFile.delete()
        }

        try {
            val request = DownloadManager.Request(Uri.parse(downloadUrl))
                .setTitle("WorldMates Messenger")
                .setDescription("Downloading update...")
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setDestinationInExternalFilesDir(
                    context,
                    Environment.DIRECTORY_DOWNLOADS,
                    APK_FILE_NAME
                )
                .setMimeType("application/vnd.android.package-archive")

            val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            downloadId = downloadManager.enqueue(request)

            // Register receiver for download complete
            val receiver = object : BroadcastReceiver() {
                override fun onReceive(ctx: Context?, intent: Intent?) {
                    val id = intent?.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
                    if (id == downloadId) {
                        _downloadState.value = DownloadState.Downloaded
                        context.unregisterReceiver(this)
                        installApk(apkFile)
                    }
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(
                    receiver,
                    IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),
                    Context.RECEIVER_EXPORTED
                )
            } else {
                context.registerReceiver(
                    receiver,
                    IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
                )
            }

            Log.d(TAG, "Download started: $downloadUrl, ID: $downloadId")
        } catch (e: Exception) {
            Log.e(TAG, "Download failed", e)
            _downloadState.value = DownloadState.Error("Download failed: ${e.message}")
        }
    }

    /**
     * Install the downloaded APK
     */
    private fun installApk(apkFile: File) {
        try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                apkFile
            )

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(intent)
            Log.d(TAG, "Install intent launched for: ${apkFile.absolutePath}")
        } catch (e: Exception) {
            Log.e(TAG, "Install failed", e)
            _downloadState.value = DownloadState.Error("Install failed: ${e.message}")
        }
    }

    /**
     * Reset download state
     */
    fun resetDownloadState() {
        _downloadState.value = DownloadState.Idle
    }

    /**
     * Format file size for display
     */
    fun formatFileSize(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> "${bytes / 1024} KB"
            else -> String.format("%.1f MB", bytes / (1024.0 * 1024.0))
        }
    }
}
