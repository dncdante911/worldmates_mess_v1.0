package com.worldmates.messenger.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import com.worldmates.messenger.BuildConfig
import com.worldmates.messenger.data.model.AppUpdateInfo
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
            val response = runCatching {
                RetrofitClient.apiService.checkMobileUpdate()
            }.getOrElse {
                Log.w(TAG, "Router update endpoint failed, fallback to direct endpoint: ${it.message}")
                RetrofitClient.apiService.checkMobileUpdateDirect()
            }

            val info = response.data
            val updateAvailable = info != null && isNewerVersion(info)

            if (!response.success && info == null) {
                val failed = _state.value.copy(
                    error = response.message ?: "Update endpoint returned invalid payload",
                    checkedAtMillis = System.currentTimeMillis()
                )
                _state.value = failed
                return failed
            }

            val isSnoozed = System.currentTimeMillis() < snoozedUntilMillis
            val newState = UpdateState(
                hasUpdate = updateAvailable && !isSnoozed,
                latestVersion = info?.latestVersion,
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
            val failed = _state.value.copy(
                error = e.message,
                checkedAtMillis = System.currentTimeMillis()
            )
            _state.value = failed
            Log.e(TAG, "Update check failed", e)
            failed
        }
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
