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

package com.t8rin.imagetoolbox.feature.root.presentation.components.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Alignment
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.t8rin.imagetoolbox.core.domain.remote.AppUpdate
import com.t8rin.imagetoolbox.core.domain.remote.AppUpdateStatus
import com.t8rin.imagetoolbox.core.resources.Icons
import com.t8rin.imagetoolbox.core.resources.R
import com.t8rin.imagetoolbox.core.resources.icons.MobileArrowDown
import com.t8rin.imagetoolbox.core.resources.icons.WarningAmber
import com.t8rin.imagetoolbox.core.ui.widget.enhanced.EnhancedAlertDialog
import com.t8rin.imagetoolbox.core.ui.widget.enhanced.EnhancedButton
import com.t8rin.imagetoolbox.core.ui.widget.enhanced.EnhancedLoadingIndicator

@Composable
internal fun AppUpdateDialog(
    status: AppUpdateStatus,
    onDownload: (AppUpdate) -> Unit,
    onInstall: (String) -> Unit,
    onRetry: () -> Unit
) {
    val update = when (status) {
        is AppUpdateStatus.Available -> status.update
        is AppUpdateStatus.NoDirectApk -> status.update
        is AppUpdateStatus.Downloading -> status.update
        is AppUpdateStatus.Downloaded -> status.update
        is AppUpdateStatus.Error -> status.update
        else -> null
    }

    if (update == null) return

    EnhancedAlertDialog(
        visible = true,
        onDismissRequest = {},
        icon = {
            Icon(
                imageVector = if (status is AppUpdateStatus.NoDirectApk) Icons.Rounded.WarningAmber else Icons.Rounded.MobileArrowDown,
                contentDescription = null
            )
        },
        title = { Text(stringResource(R.string.app_updates)) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = update?.name ?: update?.tag.orEmpty(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = stringResource(R.string.app_update_available_sub),
                    style = MaterialTheme.typography.bodyMedium
                )

                when (status) {
                    is AppUpdateStatus.NoDirectApk -> {
                        Text(
                            text = stringResource(R.string.no_direct_apk_sub),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                    }

                    is AppUpdateStatus.Downloading -> {
                        val percent = (status.progress.currentPercent * 100).toInt().coerceIn(0, 100)
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            EnhancedLoadingIndicator(progress = status.progress.currentPercent.coerceIn(0f, 1f))
                        }
                        Text(
                            text = stringResource(R.string.downloading_update_percent, percent),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                    is AppUpdateStatus.Downloaded -> {
                        Text(
                            text = stringResource(R.string.update_downloaded_install),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                    is AppUpdateStatus.Error -> {
                        Text(
                            text = status.message ?: stringResource(R.string.something_went_wrong),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                    }

                    else -> Unit
                }

                val notes = update?.notes?.trim().orEmpty()
                if (notes.isNotEmpty()) {
                    Text(
                        text = notes,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            when (status) {
                is AppUpdateStatus.Available -> {
                    EnhancedButton(onClick = { onDownload(status.update) }) {
                        Text(stringResource(R.string.update_now))
                    }
                }

                is AppUpdateStatus.Downloaded -> {
                    EnhancedButton(onClick = { onInstall(status.apkPath) }) {
                        Text(stringResource(R.string.install_update))
                    }
                }

                is AppUpdateStatus.NoDirectApk -> {
                    EnhancedButton(onClick = onRetry) {
                        Text(stringResource(R.string.check_again))
                    }
                }

                is AppUpdateStatus.Error -> {
                    EnhancedButton(onClick = onRetry) {
                        Text(stringResource(R.string.retry))
                    }
                }

                else -> Unit
            }
        },
        dismissButton = {}
    )
}
