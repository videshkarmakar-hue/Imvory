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

package com.t8rin.imagetoolbox.feature.settings.presentation.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.t8rin.imagetoolbox.core.domain.remote.AppUpdateManager
import com.t8rin.imagetoolbox.core.domain.remote.AppUpdateStatus
import com.t8rin.imagetoolbox.core.resources.Icons
import com.t8rin.imagetoolbox.core.resources.R
import com.t8rin.imagetoolbox.core.resources.icons.MobileArrowDown
import com.t8rin.imagetoolbox.core.ui.widget.modifier.ShapeDefaults
import com.t8rin.imagetoolbox.core.ui.widget.preferences.PreferenceRow

@Composable
fun AppUpdatesSettingItem(
    appUpdateManager: AppUpdateManager,
    shape: Shape = ShapeDefaults.center,
    modifier: Modifier = Modifier.padding(horizontal = 8.dp)
) {
    val status by appUpdateManager.status.collectAsStateWithLifecycle()

    val subtitle = when (status) {
        AppUpdateStatus.Idle -> stringResource(R.string.app_updates_sub)
        AppUpdateStatus.Checking -> stringResource(R.string.checking_for_updates)
        AppUpdateStatus.UpToDate -> stringResource(R.string.app_up_to_date)
        is AppUpdateStatus.Available -> stringResource(R.string.app_update_available_tap_to_update)
        is AppUpdateStatus.NoDirectApk -> stringResource(R.string.no_direct_apk_sub)
        is AppUpdateStatus.Downloading -> {
            val percent = ((status as AppUpdateStatus.Downloading).progress.currentPercent * 100)
                .toInt().coerceIn(0, 100)
            stringResource(R.string.downloading_update_percent, percent)
        }
        is AppUpdateStatus.Downloaded -> stringResource(R.string.update_downloaded_install)
        is AppUpdateStatus.Error -> (status as AppUpdateStatus.Error).message
            ?: stringResource(R.string.something_went_wrong)
    }

    val onClick: (() -> Unit)? = when (val s = status) {
        AppUpdateStatus.Idle, AppUpdateStatus.UpToDate, is AppUpdateStatus.Error -> {
            { appUpdateManager.checkForUpdate() }
        }
        is AppUpdateStatus.Available -> {
            { appUpdateManager.downloadUpdate(s.update) }
        }
        is AppUpdateStatus.NoDirectApk -> {
            { appUpdateManager.checkForUpdate() }
        }
        is AppUpdateStatus.Downloaded -> {
            { appUpdateManager.installUpdate(s.apkPath) }
        }
        AppUpdateStatus.Checking, is AppUpdateStatus.Downloading -> null
    }

    PreferenceRow(
        shape = shape,
        modifier = modifier,
        title = stringResource(R.string.app_updates),
        subtitle = subtitle,
        startIcon = Icons.Rounded.MobileArrowDown,
        onClick = onClick,
        enabled = onClick != null,
        endContent = when (status) {
            is AppUpdateStatus.Available,
            is AppUpdateStatus.Downloaded -> {
                {
                    Text(
                        text = stringResource(R.string.update_now),
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
            else -> null
        }
    )
}