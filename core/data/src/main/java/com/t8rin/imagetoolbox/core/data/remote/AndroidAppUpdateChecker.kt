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

import com.t8rin.imagetoolbox.core.domain.APP_GITHUB_LINK
import com.t8rin.imagetoolbox.core.domain.remote.AppUpdate
import com.t8rin.imagetoolbox.core.domain.remote.AppUpdateChecker
import com.t8rin.imagetoolbox.core.resources.BuildConfig
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import org.json.JSONObject
import javax.inject.Inject

internal class AndroidAppUpdateChecker @Inject constructor(
    private val client: HttpClient
) : AppUpdateChecker {

    override suspend fun latestUpdateOrNull(): Result<AppUpdate?> = runCatching {
        val repo = APP_GITHUB_LINK.removePrefix("https://github.com/").trim('/')
        val apiUrl = "https://api.github.com/repos/$repo/releases/latest"

        val body = client.get(apiUrl) {
            header("Accept", "application/vnd.github+json")
            header("X-GitHub-Api-Version", "2022-11-28")
        }.bodyAsText()

        val json = JSONObject(body)
        if (json.has("message") && !json.has("tag_name")) {
            val message = json.optString("message")
            error(message.ifBlank { "No release information available" })
        }

        val tag = json.optString("tag_name").ifBlank { return@runCatching null }
        if (!isNewerVersion(tag, BuildConfig.VERSION_NAME)) return@runCatching null

        val assets = json.optJSONArray("assets")
        var apkUrl: String? = null
        if (assets != null) {
            for (i in 0 until assets.length()) {
                val asset = assets.optJSONObject(i) ?: continue
                val name = asset.optString("name")
                val url = asset.optString("browser_download_url")
                if (name.endsWith(".apk", ignoreCase = true) && url.isNotBlank()) {
                    apkUrl = url
                    break
                }
            }
        }

        AppUpdate(
            tag = tag,
            name = json.optString("name").ifBlank { tag },
            notes = json.optString("body"),
            htmlUrl = json.optString("html_url").ifBlank { "$APP_GITHUB_LINK/releases/tag/$tag" },
            apkUrl = apkUrl,
            publishedAt = json.optString("published_at")
        )
    }

    private fun isNewerVersion(remoteTag: String, currentVersion: String): Boolean {
        val remote = normalizeVersion(remoteTag)
        val current = normalizeVersion(currentVersion)
        val max = maxOf(remote.size, current.size)
        for (i in 0 until max) {
            val r = remote.getOrElse(i) { 0L }
            val c = current.getOrElse(i) { 0L }
            if (r > c) return true
            if (r < c) return false
        }
        return false
    }

    private fun normalizeVersion(value: String): List<Long> =
        value.trim()
            .removePrefix("v")
            .removePrefix("V")
            .split(Regex("[^0-9]+"))
            .filter { it.isNotBlank() }
            .mapNotNull { it.toLongOrNull() }
}
