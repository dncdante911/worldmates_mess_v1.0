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
import com.worldmates.messenger.BuildConfig
import com.worldmates.messenger.data.model.AppUpdateInfo
import com.worldmates.messenger.data.model.AppUpdateResponse
import com.worldmates.messenger.network.RetrofitClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File

private const val TAG = "AppUpdateManager"

data class UpdateState(
    val hasUpdate: Boolean = false,
    val latestVersion: String? = null,
    val latestVersionCode: Int = 0,
    val isMandatory: Boolean = false,
    val changelog: List<String> = emptyList(),
    val apkUrl: String? = null,
    val publishedAt: String? = null,
    val error: String? = null,
    val checkedAtMillis: Long? = null,
    val downloadState: DownloadState = DownloadState.Idle
)

sealed class DownloadState {
    object Idle : DownloadState()
    object Downloading : DownloadState()
    object Downloaded : DownloadState()
    data class Error(val message: String) : DownloadState()
}

object AppUpdateManager {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _state = MutableStateFlow(UpdateState())
    val state: StateFlow<UpdateState> = _state.asStateFlow()

    private var periodicJob: Job? = null
    private var snoozedUntilMillis: Long = 0L
    private var downloadId: Long = -1
    private const val APK_FILE_NAME = "worldmates_update.apk"

    /**
     * Start periodic background update checks.
     * Called from WMApplication.onCreate().
     */
    fun startPeriodicChecks(intervalMinutes: Long = 30L) {
        if (periodicJob?.isActive == true) return

        periodicJob = scope.launch {
            while (isActive) {
                runCatching { checkForUpdates() }
                    .onFailure { e ->
                        Log.w(TAG, "Background update check failed: ${e.message}")
                    }
                delay(intervalMinutes * 60_000)
            }
        }
    }

    /**
     * Check for updates from server.
     * Uses router endpoint with fallback to direct endpoint.
     */
    suspend fun checkForUpdates(force: Boolean = false): UpdateState {
        if (!force && _state.value.checkedAtMillis != null) {
            val elapsed = System.currentTimeMillis() - (_state.value.checkedAtMillis ?: 0)
            if (elapsed < 5 * 60_000) return _state.value
        }

        return try {
            val response = fetchUpdateResponse()
            val info = response.data

            if (!response.success && info == null) {
                return updateFailure(response.message ?: "Update endpoint returned invalid payload")
            }

            val updateAvailable = info != null && isNewerVersion(info)
            val isSnoozed = System.currentTimeMillis() < snoozedUntilMillis

            val newState = UpdateState(
                hasUpdate = updateAvailable && !isSnoozed,
                latestVersion = info?.latestVersion,
                latestVersionCode = info?.versionCode ?: 0,
                isMandatory = info?.isMandatory ?: false,
                changelog = info?.changelog.orEmpty(),
                apkUrl = info?.apkUrl,
                publishedAt = info?.publishedAt,
                error = null,
                checkedAtMillis = System.currentTimeMillis()
            )

            _state.value = newState
            Log.d(TAG, "Update check complete: hasUpdate=${newState.hasUpdate}, latest=${newState.latestVersion}")
            newState
        } catch (e: Exception) {
            updateFailure(e.message ?: "Unknown error while checking updates", e)
        }
    }

    /**
     * Fetch update info directly from endpoint (bypasses WoWonder router).
     */
    private suspend fun fetchUpdateResponse(): AppUpdateResponse {
        return RetrofitClient.apiService.checkMobileUpdate()
    }

    private fun updateFailure(message: String, throwable: Throwable? = null): UpdateState {
        val failed = _state.value.copy(
            error = message,
            checkedAtMillis = System.currentTimeMillis()
        )
        _state.value = failed

        if (throwable != null) {
            Log.e(TAG, "Update check failed", throwable)
        } else {
            Log.w(TAG, "Update check failed: $message")
        }

        return failed
    }

    /**
     * Snooze update prompt for specified hours.
     */
    fun snoozePrompt(hours: Int = 12) {
        snoozedUntilMillis = System.currentTimeMillis() + hours * 60L * 60_000L
        _state.value = _state.value.copy(hasUpdate = false)
    }

    /**
     * Open update URL in browser (fallback).
     */
    fun openUpdateUrl(context: Context) {
        val url = _state.value.apkUrl ?: return
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    /**
     * Download APK via DownloadManager and trigger install.
     */
    fun downloadAndInstall(context: Context) {
        val downloadUrl = _state.value.apkUrl
        if (downloadUrl.isNullOrEmpty()) {
            _state.value = _state.value.copy(
                downloadState = DownloadState.Error("Download URL is empty")
            )
            return
        }

        _state.value = _state.value.copy(downloadState = DownloadState.Downloading)

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

            val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            downloadId = dm.enqueue(request)

            val receiver = object : BroadcastReceiver() {
                override fun onReceive(ctx: Context?, intent: Intent?) {
                    val id = intent?.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
                    if (id == downloadId) {
                        _state.value = _state.value.copy(downloadState = DownloadState.Downloaded)
                        context.unregisterReceiver(this)
                        installApk(context, apkFile)
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
            _state.value = _state.value.copy(
                downloadState = DownloadState.Error("Download failed: ${e.message}")
            )
        }
    }

    /**
     * Install downloaded APK via FileProvider.
     */
    private fun installApk(context: Context, apkFile: File) {
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
            Log.d(TAG, "Install intent launched")
        } catch (e: Exception) {
            Log.e(TAG, "Install failed", e)
            _state.value = _state.value.copy(
                downloadState = DownloadState.Error("Install failed: ${e.message}")
            )
        }
    }

    /**
     * Reset download state.
     */
    fun resetDownloadState() {
        _state.value = _state.value.copy(downloadState = DownloadState.Idle)
    }

    /**
     * Get current app version name.
     */
    fun getCurrentVersionName(): String {
        return BuildConfig.VERSION_NAME
    }

    private fun isNewerVersion(updateInfo: AppUpdateInfo): Boolean {
        val currentCode = BuildConfig.VERSION_CODE
        if (updateInfo.versionCode > currentCode) return true
        return compareVersions(updateInfo.latestVersion, BuildConfig.VERSION_NAME) > 0
    }

    private fun compareVersions(latest: String, current: String): Int {
        val a = latest.split(".")
        val b = current.split(".")
        val max = maxOf(a.size, b.size)
        repeat(max) { i ->
            val ai = a.getOrNull(i)?.filter { it.isDigit() }?.toIntOrNull() ?: 0
            val bi = b.getOrNull(i)?.filter { it.isDigit() }?.toIntOrNull() ?: 0
            if (ai != bi) return ai.compareTo(bi)
        }
        return 0
    }
}
