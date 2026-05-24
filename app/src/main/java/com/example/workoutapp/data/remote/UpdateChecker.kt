package com.example.workoutapp.data.remote

import com.example.workoutapp.BuildConfig
import com.example.workoutapp.data.settings.UpdateCheckPreferences
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

data class UpdateInfo(
    val version: String,
    val downloadUrl: String,
    val changelog: String
)

@Singleton
class UpdateChecker @Inject constructor(
    private val prefs: UpdateCheckPreferences,
    private val okHttpClient: OkHttpClient
) {
    companion object {
        private const val COOLDOWN_MS = 24 * 60 * 60 * 1000L
        private const val GITHUB_API_URL =
            "https://api.github.com/repos/${BuildConfig.GITHUB_REPO_OWNER}/${BuildConfig.GITHUB_REPO_NAME}/releases/latest"
    }

    suspend fun checkForUpdate(currentVersion: String): UpdateInfo? = withContext(Dispatchers.IO) {
        val lastCheck = prefs.lastCheckTimestamp.first()
        if (System.currentTimeMillis() - lastCheck < COOLDOWN_MS) {
            Timber.d("Update check skipped: within cooldown period")
            return@withContext null
        }

        try {
            val request = Request.Builder()
                .url(GITHUB_API_URL)
                .header("Accept", "application/vnd.github.v3+json")
                .build()

            val response = okHttpClient.newCall(request).execute()
            response.use { res ->
                if (!res.isSuccessful) {
                    Timber.w("GitHub API returned ${res.code}")
                    return@withContext null
                }

                val body = res.body?.string() ?: return@withContext null
                val json = JsonParser.parseString(body).asJsonObject
                val tagName = json.get("tag_name").asString
                val latestVersion = tagName.stripVPrefix()
                val htmlUrl = json.get("html_url").asString
                val changelog = json.get("body")?.asString ?: ""

                prefs.setLastCheckTimestamp(System.currentTimeMillis())

                val skipped = prefs.skippedVersion.first()
                if (latestVersion == skipped) {
                    Timber.d("Version $latestVersion was skipped by user")
                    return@withContext null
                }

                if (latestVersion != currentVersion) {
                    UpdateInfo(
                        version = latestVersion,
                        downloadUrl = htmlUrl,
                        changelog = changelog
                    )
                } else {
                    null
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to check for updates")
            null
        }
    }

    private fun String.stripVPrefix(): String = removePrefix("v")
}
