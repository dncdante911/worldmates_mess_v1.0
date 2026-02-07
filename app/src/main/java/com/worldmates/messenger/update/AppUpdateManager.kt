package com.worldmates.messenger.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
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

private const val TAG = "AppUpdateManager"

data class UpdateState(
    val hasUpdate: Boolean = false,
    val latestVersion: String? = null,
    val isMandatory: Boolean = false,
    val changelog: List<String> = emptyList(),
    val apkUrl: String? = null,
    val publishedAt: String? = null,
    val error: String? = null,
    val checkedAtMillis: Long? = null
)

object AppUpdateManager {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _state = MutableStateFlow(UpdateState())
    val state: StateFlow<UpdateState> = _state.asStateFlow()

    private var periodicJob: Job? = null
    private var snoozedUntilMillis: Long = 0L

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

    suspend fun checkForUpdates(force: Boolean = false): UpdateState {
        if (!force && _state.value.checkedAtMillis != null) {
            val elapsed = System.currentTimeMillis() - (_state.value.checkedAtMillis ?: 0)
            if (elapsed < 5 * 60_000) return _state.value
        }

        return try {
            val apiResponse = fetchUpdateResponse()
            val updateInfo = apiResponse.data

            if (!apiResponse.success && updateInfo == null) {
                return updateFailure(apiResponse.message ?: "Update endpoint returned invalid payload")
            }

            val isUpdateAvailable = updateInfo != null && isNewerVersion(updateInfo)
            val isSnoozed = System.currentTimeMillis() < snoozedUntilMillis

            val newState = UpdateState(
                hasUpdate = isUpdateAvailable && !isSnoozed,
                latestVersion = updateInfo?.latestVersion,
                isMandatory = updateInfo?.isMandatory ?: false,
                changelog = updateInfo?.changelog.orEmpty(),
                apkUrl = updateInfo?.apkUrl,
                publishedAt = updateInfo?.publishedAt,
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

    private suspend fun fetchUpdateResponse(): AppUpdateResponse {
        return try {
            RetrofitClient.apiService.checkMobileUpdate()
        } catch (routerError: Exception) {
            Log.w(TAG, "Router update endpoint failed, fallback to direct endpoint: ${routerError.message}")
            RetrofitClient.apiService.checkMobileUpdateDirect()
        }
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

    fun snoozePrompt(hours: Int = 12) {
        snoozedUntilMillis = System.currentTimeMillis() + hours * 60L * 60_000L
        _state.value = _state.value.copy(hasUpdate = false)
    }

    fun openUpdateUrl(context: Context) {
        val url = _state.value.apkUrl ?: return
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
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
