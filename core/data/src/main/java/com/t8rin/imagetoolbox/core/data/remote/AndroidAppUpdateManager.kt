/*
 * Imvory is an image editor for android
 * Copyright (c) 2026 T8RIN (Malik Mukhametzyanov)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * You should have received a copy of the Apache License
 * along with this program.  If not, see <http://www.apache.org/licenses/LICENSE-2.0>.
 */

package com.t8rin.imagetoolbox.core.data.remote

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import com.t8rin.imagetoolbox.core.domain.coroutines.DispatchersHolder
import com.t8rin.imagetoolbox.core.domain.remote.AppUpdate
import com.t8rin.imagetoolbox.core.domain.remote.AppUpdateChecker
import com.t8rin.imagetoolbox.core.domain.remote.AppUpdateManager
import com.t8rin.imagetoolbox.core.domain.remote.AppUpdateStatus
import com.t8rin.imagetoolbox.core.domain.remote.DownloadManager
import com.t8rin.imagetoolbox.core.domain.remote.DownloadProgress
import com.t8rin.imagetoolbox.core.resources.R
import com.t8rin.imagetoolbox.core.utils.appContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class AndroidAppUpdateManager @Inject constructor(
    private val appUpdateChecker: AppUpdateChecker,
    private val downloadManager: DownloadManager,
    dispatchersHolder: DispatchersHolder
) : AppUpdateManager, DispatchersHolder by dispatchersHolder {

    private val scope = CoroutineScope(SupervisorJob() + defaultDispatcher)

    private val _status = MutableStateFlow<AppUpdateStatus>(AppUpdateStatus.Idle)
    override val status: StateFlow<AppUpdateStatus> = _status.asStateFlow()

    override fun checkForUpdate(silent: Boolean) {
        val current = _status.value
        if (current is AppUpdateStatus.Checking || current is AppUpdateStatus.Downloading) return
        scope.launch {
            if (!silent) _status.value = AppUpdateStatus.Checking
            appUpdateChecker.latestUpdateOrNull()
                .onSuccess { update ->
                    when {
                        update == null -> _status.value = AppUpdateStatus.UpToDate
                        !update.hasDirectApk -> _status.value = AppUpdateStatus.NoDirectApk(update)
                        else -> {
                            _status.value = AppUpdateStatus.Available(update)
                            downloadUpdate(update)
                        }
                    }
                }
                .onFailure { e ->
                    if (!silent) {
                        _status.value = AppUpdateStatus.Error(e.message ?: e::class.simpleName)
                    }
                }
        }
    }

    override fun downloadUpdate(update: AppUpdate) {
        val apkUrl = update.apkUrl ?: run {
            _status.value = AppUpdateStatus.NoDirectApk(update)
            return
        }
        val current = _status.value
        if (current is AppUpdateStatus.Downloading) return

        scope.launch {
            val updatesDir = File(appContext.cacheDir, "updates").apply { mkdirs() }
            val apkFile = File(updatesDir, "imvory-update-${sanitize(update.tag)}.apk")
            if (apkFile.exists()) apkFile.delete()

            _status.value = AppUpdateStatus.Downloading(update, DownloadProgress(0f, -1))
            downloadManager.download(
                url = apkUrl,
                destinationPath = apkFile.absolutePath,
                onProgress = { progress ->
                    _status.value = AppUpdateStatus.Downloading(update, progress)
                },
                onFinish = { error ->
                    if (error == null && apkFile.exists()) {
                        _status.value = AppUpdateStatus.Downloaded(update, apkFile.absolutePath)
                        installUpdate(apkFile.absolutePath)
                    } else if (error != null) {
                        _status.value = AppUpdateStatus.Error(
                            message = error.message ?: error::class.simpleName,
                            update = update
                        )
                    }
                }
            )
        }
    }

    override fun installUpdate(apkPath: String) {
        runCatching {
            val file = File(apkPath)
            if (!file.exists()) {
                val update = (status.value as? AppUpdateStatus.Downloaded)?.update
                    ?: (status.value as? AppUpdateStatus.Error)?.update
                _status.value = AppUpdateStatus.Error("Update file not found", update)
                return
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
                !appContext.packageManager.canRequestPackageInstalls()
            ) {
                val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                    data = Uri.parse("package:${appContext.packageName}")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                appContext.startActivity(intent)
                return
            }

            val authority = appContext.getString(R.string.file_provider)
            val uri = FileProvider.getUriForFile(appContext, authority, file)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            appContext.startActivity(intent)
        }.onFailure { e ->
            _status.value = AppUpdateStatus.Error(e.message ?: e::class.simpleName)
        }
    }

    override fun clearError() {
        if (_status.value is AppUpdateStatus.Error) {
            _status.value = AppUpdateStatus.Idle
        }
    }

    private fun sanitize(tag: String): String = tag.replace(Regex("[^A-Za-z0-9._-]"), "_")
}
