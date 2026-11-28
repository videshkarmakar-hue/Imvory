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

package com.t8rin.imagetoolbox.core.domain.remote

sealed interface AppUpdateStatus {
    data object Idle : AppUpdateStatus
    data object Checking : AppUpdateStatus
    data class Available(val update: AppUpdate) : AppUpdateStatus
    data object UpToDate : AppUpdateStatus
    data class NoDirectApk(val update: AppUpdate) : AppUpdateStatus
    data class Downloading(val update: AppUpdate, val progress: DownloadProgress) : AppUpdateStatus
    data class Downloaded(val update: AppUpdate, val apkPath: String) : AppUpdateStatus
    data class Error(val message: String?, val update: AppUpdate? = null) : AppUpdateStatus
}
