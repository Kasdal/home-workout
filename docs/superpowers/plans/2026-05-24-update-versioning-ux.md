# Auto-Update, Versioning, and UX Improvements — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add in-app update checking via GitHub Releases API, a "What's New" dialog on version bump, a revamped About screen, and enhanced onboarding with auto-triggered tutorial using screenshots.

**Architecture:** A new `UpdateChecker` class polls the GitHub Releases API (OkHttp) with a 24h DataStore cache. `MainViewModel` orchestrates the update check and version-change detection, exposing state flows for the bottom sheet and dialog. The About screen gets check-for-updates, feedback, and licenses sections. Onboarding redirects new users to the tutorial, which now shows app screenshots instead of emojis.

**Tech Stack:** Kotlin, Compose, Hilt, OkHttp (existing), DataStore Preferences (existing), GitHub Releases API

---

### Task 1: BuildConfig fields for GitHub repo info

**Files:**
- Modify: `app/build.gradle.kts:34-34` (inside `defaultConfig`)

- [ ] **Step 1: Add BuildConfig fields**

In `app/build.gradle.kts`, inside the `defaultConfig` block, after `versionName = rootProject.version.toString()`:

```kotlin
buildConfigField("String", "GITHUB_REPO_OWNER", "\"Kasdal\"")
buildConfigField("String", "GITHUB_REPO_NAME", "\"home-workout\"")
```

- [ ] **Step 2: Verify BuildConfig fields are generated**

```bash
./gradlew.bat :app:assembleDebug --stacktrace
```

Expected: PASS (build succeeds)

- [ ] **Step 3: Commit**

```bash
git add app/build.gradle.kts
git commit -m "feat: add BuildConfig fields for GitHub repo info"
```

---

### Task 2: UpdateCheckPreferences DataStore

**Files:**
- Create: `app/src/main/java/com/example/workoutapp/data/settings/UpdateCheckPreferences.kt`

- [ ] **Step 1: Write the failing test**

Create `app/src/test/java/com/example/workoutapp/data/settings/UpdateCheckPreferencesTest.kt`:

```kotlin
package com.example.workoutapp.data.settings

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

private val Context.testDataStore by preferencesDataStore(name = "test_update_check_prefs")

class UpdateCheckPreferencesTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Test
    fun `defaults return null or zero`() = runTest {
        val prefs = UpdateCheckPreferences(context)
        assertEquals(0L, prefs.lastCheckTimestamp.first())
        assertNull(prefs.lastSeenVersion.first())
        assertNull(prefs.skippedVersion.first())
        assertNull(prefs.cachedChangelogMd.first())
        assertNull(prefs.cachedChangelogVersion.first())
    }

    @Test
    fun `setLastCheckTimestamp stores and retrieves value`() = runTest {
        val prefs = UpdateCheckPreferences(context)
        val timestamp = 1717000000000L
        prefs.setLastCheckTimestamp(timestamp)
        assertEquals(timestamp, prefs.lastCheckTimestamp.first())
    }

    @Test
    fun `setLastSeenVersion stores and retrieves value`() = runTest {
        val prefs = UpdateCheckPreferences(context)
        prefs.setLastSeenVersion("1.2.0")
        assertEquals("1.2.0", prefs.lastSeenVersion.first())
    }

    @Test
    fun `setSkippedVersion stores and retrieves value`() = runTest {
        val prefs = UpdateCheckPreferences(context)
        prefs.setSkippedVersion("1.3.0")
        assertEquals("1.3.0", prefs.skippedVersion.first())
    }

    @Test
    fun `setCachedChangelog stores both md and version`() = runTest {
        val prefs = UpdateCheckPreferences(context)
        prefs.setCachedChangelog("1.2.0", "## What's New\n- Feature A\n- Feature B")
        assertEquals("1.2.0", prefs.cachedChangelogVersion.first())
        assertEquals("## What's New\n- Feature A\n- Feature B", prefs.cachedChangelogMd.first())
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

```bash
./gradlew.bat :app:testDebugUnitTest --tests "com.example.workoutapp.data.settings.UpdateCheckPreferencesTest"
```

Expected: FAIL (class not found)

- [ ] **Step 3: Write UpdateCheckPreferences implementation**

Create `app/src/main/java/com/example/workoutapp/data/settings/UpdateCheckPreferences.kt`:

```kotlin
package com.example.workoutapp.data.settings

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

private val Context.updateCheckDataStore by preferencesDataStore(name = "update_check_preferences")

@Singleton
class UpdateCheckPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val lastCheckTimestampKey = longPreferencesKey("last_check_timestamp")
    private val lastSeenVersionKey = stringPreferencesKey("last_seen_version")
    private val skippedVersionKey = stringPreferencesKey("skipped_version")
    private val cachedChangelogMdKey = stringPreferencesKey("cached_changelog_md")
    private val cachedChangelogVersionKey = stringPreferencesKey("cached_changelog_version")

    val lastCheckTimestamp: Flow<Long> = context.updateCheckDataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[lastCheckTimestampKey] ?: 0L }

    val lastSeenVersion: Flow<String?> = context.updateCheckDataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[lastSeenVersionKey] }

    val skippedVersion: Flow<String?> = context.updateCheckDataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[skippedVersionKey] }

    val cachedChangelogMd: Flow<String?> = context.updateCheckDataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[cachedChangelogMdKey] }

    val cachedChangelogVersion: Flow<String?> = context.updateCheckDataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[cachedChangelogVersionKey] }

    suspend fun setLastCheckTimestamp(timestamp: Long) {
        context.updateCheckDataStore.edit { it[lastCheckTimestampKey] = timestamp }
    }

    suspend fun setLastSeenVersion(version: String) {
        context.updateCheckDataStore.edit { it[lastSeenVersionKey] = version }
    }

    suspend fun setSkippedVersion(version: String) {
        context.updateCheckDataStore.edit { it[skippedVersionKey] = version }
    }

    suspend fun setCachedChangelog(version: String, changelogMd: String) {
        context.updateCheckDataStore.edit {
            it[cachedChangelogVersionKey] = version
            it[cachedChangelogMdKey] = changelogMd
        }
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

```bash
./gradlew.bat :app:testDebugUnitTest --tests "com.example.workoutapp.data.settings.UpdateCheckPreferencesTest"
```

Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/example/workoutapp/data/settings/UpdateCheckPreferences.kt app/src/test/java/com/example/workoutapp/data/settings/UpdateCheckPreferencesTest.kt
git commit -m "feat: add UpdateCheckPreferences DataStore for update state"
```

---

### Task 3: UpdateChecker with GitHub Releases API

**Files:**
- Create: `app/src/main/java/com/example/workoutapp/data/remote/UpdateChecker.kt`
- Create: `app/src/test/java/com/example/workoutapp/data/remote/UpdateCheckerTest.kt`

- [ ] **Step 1: Write the failing test**

Create `app/src/test/java/com/example/workoutapp/data/remote/UpdateCheckerTest.kt`:

```kotlin
package com.example.workoutapp.data.remote

import com.example.workoutapp.data.settings.UpdateCheckPreferences
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody
import okhttp3.MediaType.Companion.toMediaType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateCheckerTest {

    private val prefs = mockk<UpdateCheckPreferences>(relaxed = true)
    private val client = mockk<OkHttpClient>()
    private val checker = UpdateChecker(prefs, client)

    @Test
    fun `checkForUpdate returns null when version is current`() = runTest {
        coEvery { prefs.lastCheckTimestamp } returns flowOf(0L)
        coEvery { prefs.lastSeenVersion } returns flowOf("1.5.0")
        coEvery { prefs.skippedVersion } returns flowOf(null)

        val mockResponse = mockk<Response>()
        val mockBody = mockk<ResponseBody>()
        every { mockResponse.isSuccessful } returns true
        every { mockResponse.body } returns mockBody
        every { mockBody.string() } returns """
            {
                "tag_name": "v1.5.0",
                "name": "v1.5.0",
                "html_url": "https://github.com/Kasdal/home-workout/releases/tag/v1.5.0",
                "body": "Release notes here"
            }
        """.trimIndent()
        every { mockResponse.close() } returns Unit

        val call = mockk<okhttp3.Call>()
        every { client.newCall(any<Request>()) } returns call
        every { call.execute() } returns mockResponse

        val result = checker.checkForUpdate("1.5.0")
        assertNull(result)
    }

    @Test
    fun `checkForUpdate returns UpdateInfo when newer version available`() = runTest {
        coEvery { prefs.lastCheckTimestamp } returns flowOf(0L)
        coEvery { prefs.lastSeenVersion } returns flowOf("1.0.0")
        coEvery { prefs.skippedVersion } returns flowOf(null)

        val mockResponse = mockk<Response>()
        val mockBody = mockk<ResponseBody>()
        every { mockResponse.isSuccessful } returns true
        every { mockResponse.body } returns mockBody
        every { mockBody.string() } returns """
            {
                "tag_name": "v1.5.0",
                "name": "v1.5.0",
                "html_url": "https://github.com/Kasdal/home-workout/releases/tag/v1.5.0",
                "body": "Release notes here"
            }
        """.trimIndent()
        every { mockResponse.close() } returns Unit

        val call = mockk<okhttp3.Call>()
        every { client.newCall(any<Request>()) } returns call
        every { call.execute() } returns mockResponse

        val result = checker.checkForUpdate("1.0.0")
        assertEquals("1.5.0", result?.version)
        assertEquals("https://github.com/Kasdal/home-workout/releases/tag/v1.5.0", result?.downloadUrl)
        assertEquals("Release notes here", result?.changelog)
    }

    @Test
    fun `checkForUpdate skips when within 24h cooldown`() = runTest {
        val recentTimestamp = System.currentTimeMillis() - 12 * 60 * 60 * 1000 // 12 hours ago
        coEvery { prefs.lastCheckTimestamp } returns flowOf(recentTimestamp)

        val result = checker.checkForUpdate("1.0.0")
        assertNull(result)
    }

    @Test
    fun `checkForUpdate skips version user chose to skip`() = runTest {
        coEvery { prefs.lastCheckTimestamp } returns flowOf(0L)
        coEvery { prefs.skippedVersion } returns flowOf("1.5.0")

        val mockResponse = mockk<Response>()
        val mockBody = mockk<ResponseBody>()
        every { mockResponse.isSuccessful } returns true
        every { mockResponse.body } returns mockBody
        every { mockBody.string() } returns """
            {
                "tag_name": "v1.5.0",
                "name": "v1.5.0",
                "html_url": "https://github.com/Kasdal/home-workout/releases/tag/v1.5.0",
                "body": "Release notes"
            }
        """.trimIndent()
        every { mockResponse.close() } returns Unit

        val call = mockk<okhttp3.Call>()
        every { client.newCall(any<Request>()) } returns call
        every { call.execute() } returns mockResponse

        val result = checker.checkForUpdate("1.0.0")
        assertNull(result)
    }

    @Test
    fun `checkForUpdate handles API error gracefully`() = runTest {
        coEvery { prefs.lastCheckTimestamp } returns flowOf(0L)
        coEvery { prefs.skippedVersion } returns flowOf(null)

        val mockResponse = mockk<Response>()
        every { mockResponse.isSuccessful } returns false
        every { mockResponse.code } returns 403
        every { mockResponse.close() } returns Unit

        val call = mockk<okhttp3.Call>()
        every { client.newCall(any<Request>()) } returns call
        every { call.execute() } returns mockResponse

        val result = checker.checkForUpdate("1.0.0")
        assertNull(result)
    }

    @Test
    fun `stripVPrefix removes leading v from tag name`() = runTest {
        coEvery { prefs.lastCheckTimestamp } returns flowOf(0L)
        coEvery { prefs.lastSeenVersion } returns flowOf("1.0.0")
        coEvery { prefs.skippedVersion } returns flowOf(null)

        val mockResponse = mockk<Response>()
        val mockBody = mockk<ResponseBody>()
        every { mockResponse.isSuccessful } returns true
        every { mockResponse.body } returns mockBody
        every { mockBody.string() } returns """
            {
                "tag_name": "v2.0.0-beta",
                "name": "v2.0.0-beta",
                "html_url": "https://github.com/Kasdal/home-workout/releases/tag/v2.0.0-beta",
                "body": "Beta release"
            }
        """.trimIndent()
        every { mockResponse.close() } returns Unit

        val call = mockk<okhttp3.Call>()
        every { client.newCall(any<Request>()) } returns call
        every { call.execute() } returns mockResponse

        val result = checker.checkForUpdate("1.0.0")
        assertEquals("2.0.0-beta", result?.version)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

```bash
./gradlew.bat :app:testDebugUnitTest --tests "com.example.workoutapp.data.remote.UpdateCheckerTest"
```

Expected: FAIL (class not found)

- [ ] **Step 3: Write UpdateChecker implementation**

Create `app/src/main/java/com/example/workoutapp/data/remote/UpdateChecker.kt`:

```kotlin
package com.example.workoutapp.data.remote

import com.example.workoutapp.BuildConfig
import com.example.workoutapp.data.settings.UpdateCheckPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
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
        private const val COOLDOWN_MS = 24 * 60 * 60 * 1000L // 24 hours
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
                val json = JSONObject(body)
                val tagName = json.getString("tag_name")
                val latestVersion = tagName.stripVPrefix()
                val htmlUrl = json.getString("html_url")
                val changelog = json.optString("body", "")

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
```

- [ ] **Step 4: Run test to verify it passes**

```bash
./gradlew.bat :app:testDebugUnitTest --tests "com.example.workoutapp.data.remote.UpdateCheckerTest"
```

Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/example/workoutapp/data/remote/UpdateChecker.kt app/src/test/java/com/example/workoutapp/data/remote/UpdateCheckerTest.kt
git commit -m "feat: add UpdateChecker with GitHub Releases API integration"
```

---

### Task 4: UpdateAvailableBottomSheet

**Files:**
- Create: `app/src/main/java/com/example/workoutapp/ui/components/UpdateAvailableBottomSheet.kt`

- [ ] **Step 1: Write the composable**

Create `app/src/main/java/com/example/workoutapp/ui/components/UpdateAvailableBottomSheet.kt`:

```kotlin
package com.example.workoutapp.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.workoutapp.data.remote.UpdateInfo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpdateAvailableBottomSheet(
    updateInfo: UpdateInfo,
    onDownload: () -> Unit,
    onRemindLater: () -> Unit,
    onSkip: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = "Update Available",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Version ${updateInfo.version} is now available.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))

            if (updateInfo.changelog.isNotBlank()) {
                HorizontalDivider()
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "What's New",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = updateInfo.changelog.take(300).replace("##", "").replace("**", "").trim(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider()
            }

            Spacer(modifier = Modifier.height(20.dp))
            Button(
                onClick = onDownload,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Download Update")
            }
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(
                onClick = onRemindLater,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Remind Me Later")
            }
            Spacer(modifier = Modifier.height(4.dp))
            TextButton(
                onClick = onSkip,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Skip This Version")
            }
        }
    }
}
```

- [ ] **Step 2: Build to verify compilation**

```bash
./gradlew.bat :app:assembleDebug --stacktrace
```

Expected: PASS

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/example/workoutapp/ui/components/UpdateAvailableBottomSheet.kt
git commit -m "feat: add UpdateAvailableBottomSheet composable"
```

---

### Task 5: WhatsNewDialog

**Files:**
- Create: `app/src/main/java/com/example/workoutapp/ui/components/WhatsNewDialog.kt`
- Create: `app/src/main/res/values/changelog.xml`

- [ ] **Step 1: Create fallback changelog resource**

Create `app/src/main/res/values/changelog.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="changelog_fallback">Thank you for updating! Here\'s what changed in this release.\n\nCheck the GitHub Releases page for full details.</string>
</resources>
```

- [ ] **Step 2: Write the dialog composable**

Create `app/src/main/java/com/example/workoutapp/ui/components/WhatsNewDialog.kt`:

```kotlin
package com.example.workoutapp.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun WhatsNewDialog(
    version: String,
    changelog: String,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "What\'s New in v$version",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = changelog
                        .replace("##", "")
                        .replace("**", "")
                        .trim(),
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Got it")
            }
        }
    )
}
```

- [ ] **Step 3: Build to verify compilation**

```bash
./gradlew.bat :app:assembleDebug --stacktrace
```

Expected: PASS

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/example/workoutapp/ui/components/WhatsNewDialog.kt app/src/main/res/values/changelog.xml
git commit -m "feat: add WhatsNewDialog and fallback changelog resource"
```

---

### Task 6: Integrate update check and What's New into MainViewModel

**Files:**
- Modify: `app/src/main/java/com/example/workoutapp/MainViewModel.kt`
- Modify: `app/src/main/java/com/example/workoutapp/MainActivity.kt`

- [ ] **Step 1: Update MainViewModel**

Replace `app/src/main/java/com/example/workoutapp/MainViewModel.kt`:

```kotlin
package com.example.workoutapp

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.workoutapp.data.remote.UpdateChecker
import com.example.workoutapp.data.remote.UpdateInfo
import com.example.workoutapp.data.settings.LegacySettingsBootstrapper
import com.example.workoutapp.data.settings.LocalAppPreferencesRepository
import com.example.workoutapp.data.settings.UpdateCheckPreferences
import com.example.workoutapp.domain.startup.AppEntryState
import com.example.workoutapp.domain.startup.AppLaunchCoordinator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val legacySettingsBootstrapper: LegacySettingsBootstrapper,
    private val localAppPreferencesRepository: LocalAppPreferencesRepository,
    private val updateCheckPreferences: UpdateCheckPreferences,
    private val updateChecker: UpdateChecker,
    appLaunchCoordinator: AppLaunchCoordinator
) : ViewModel() {

    val appEntryState: StateFlow<AppEntryState?> = appLaunchCoordinator.appEntryState()
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val themeMode = localAppPreferencesRepository.settings

    private val _updateInfo = kotlinx.coroutines.flow.MutableStateFlow<UpdateInfo?>(null)
    val updateInfo: StateFlow<UpdateInfo?> = _updateInfo

    private val _showWhatsNew = kotlinx.coroutines.flow.MutableStateFlow(false)
    val showWhatsNew: StateFlow<Boolean> = _showWhatsNew

    private val _whatsNewChangelog = kotlinx.coroutines.flow.MutableStateFlow("")
    val whatsNewChangelog: StateFlow<String> = _whatsNewChangelog

    init {
        migrateLegacyThemeIfNeeded()
    }

    private fun migrateLegacyThemeIfNeeded() {
        viewModelScope.launch {
            legacySettingsBootstrapper.seedFromLegacySettingsIfPresent()
        }
    }

    fun onAppReady() {
        viewModelScope.launch {
            checkForUpdate()
            checkWhatNew()
        }
    }

    private suspend fun checkForUpdate() {
        val currentVersion = BuildConfig.VERSION_NAME
        val result = updateChecker.checkForUpdate(currentVersion)
        if (result != null) {
            updateCheckPreferences.setCachedChangelog(result.version, result.changelog)
            _updateInfo.value = result
        }
    }

    private suspend fun checkWhatNew() {
        val currentVersion = BuildConfig.VERSION_NAME
        val lastSeen = updateCheckPreferences.lastSeenVersion
            .kotlinx.coroutines.flow.first()
        if (lastSeen != currentVersion) {
            updateCheckPreferences.setLastSeenVersion(currentVersion)
            val cachedMd = updateCheckPreferences.cachedChangelogMd
                .kotlinx.coroutines.flow.first()
            val fallback = localAppPreferencesRepository.context()
                .getString(R.string.changelog_fallback)
            _whatsNewChangelog.value = cachedMd ?: fallback
            _showWhatsNew.value = true
        }
    }

    fun dismissUpdateSheet() {
        _updateInfo.value = null
    }

    fun skipVersion(version: String) {
        viewModelScope.launch {
            updateCheckPreferences.setSkippedVersion(version)
            _updateInfo.value = null
        }
    }

    fun dismissWhatsNew() {
        _showWhatsNew.value = false
    }
}
```

Wait — `LocalAppPreferencesRepository` doesn't expose `context()`. Let me use the string resource differently. I'll inject `@ApplicationContext` directly into `MainViewModel` instead, or use `R.string.changelog_fallback` directly.

- [ ] **Step 1 (corrected): Update MainViewModel**

Replace `app/src/main/java/com/example/workoutapp/MainViewModel.kt`:

```kotlin
package com.example.workoutapp

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.workoutapp.data.remote.UpdateChecker
import com.example.workoutapp.data.remote.UpdateInfo
import com.example.workoutapp.data.settings.LegacySettingsBootstrapper
import com.example.workoutapp.data.settings.LocalAppPreferencesRepository
import com.example.workoutapp.data.settings.UpdateCheckPreferences
import com.example.workoutapp.domain.startup.AppEntryState
import com.example.workoutapp.domain.startup.AppLaunchCoordinator
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val legacySettingsBootstrapper: LegacySettingsBootstrapper,
    private val localAppPreferencesRepository: LocalAppPreferencesRepository,
    private val updateCheckPreferences: UpdateCheckPreferences,
    private val updateChecker: UpdateChecker,
    @ApplicationContext private val appContext: Context,
    appLaunchCoordinator: AppLaunchCoordinator
) : ViewModel() {

    val appEntryState: StateFlow<AppEntryState?> = appLaunchCoordinator.appEntryState()
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val themeMode = localAppPreferencesRepository.settings

    private val _updateInfo = MutableStateFlow<UpdateInfo?>(null)
    val updateInfo: StateFlow<UpdateInfo?> = _updateInfo

    private val _showWhatsNew = MutableStateFlow(false)
    val showWhatsNew: StateFlow<Boolean> = _showWhatsNew

    private val _whatsNewChangelog = MutableStateFlow("")
    val whatsNewChangelog: StateFlow<String> = _whatsNewChangelog

    init {
        migrateLegacyThemeIfNeeded()
    }

    private fun migrateLegacyThemeIfNeeded() {
        viewModelScope.launch {
            legacySettingsBootstrapper.seedFromLegacySettingsIfPresent()
        }
    }

    fun onAppReady() {
        viewModelScope.launch {
            checkForUpdate()
            checkWhatNew()
        }
    }

    private suspend fun checkForUpdate() {
        val result = updateChecker.checkForUpdate(BuildConfig.VERSION_NAME)
        if (result != null) {
            updateCheckPreferences.setCachedChangelog(result.version, result.changelog)
            _updateInfo.value = result
        }
    }

    private suspend fun checkWhatNew() {
        val currentVersion = BuildConfig.VERSION_NAME
        val lastSeen = updateCheckPreferences.lastSeenVersion.first()
        if (lastSeen != currentVersion) {
            updateCheckPreferences.setLastSeenVersion(currentVersion)
            val cachedMd = updateCheckPreferences.cachedChangelogMd.first()
            val fallback = appContext.getString(R.string.changelog_fallback)
            _whatsNewChangelog.value = cachedMd ?: fallback
            _showWhatsNew.value = true
        }
    }

    fun dismissUpdateSheet() {
        _updateInfo.value = null
    }

    fun skipVersion(version: String) {
        viewModelScope.launch {
            updateCheckPreferences.setSkippedVersion(version)
            _updateInfo.value = null
        }
    }

    fun dismissWhatsNew() {
        _showWhatsNew.value = false
    }
}
```

- [ ] **Step 2: Update MainActivity to wire bottom sheet and dialog**

Modify `app/src/main/java/com/example/workoutapp/MainActivity.kt`:

Add imports at the top (after existing imports):
```kotlin
import android.content.Intent
import android.net.Uri
import com.example.workoutapp.data.remote.UpdateInfo
import com.example.workoutapp.ui.components.UpdateAvailableBottomSheet
import com.example.workoutapp.ui.components.WhatsNewDialog
```

Modify the `AppEntryState.Ready` block to add `LaunchedEffect` for `onAppReady` and overlay the bottom sheet and dialog.

Find this block in `MainActivity.kt` (around line 59):
```kotlin
is AppEntryState.Ready -> {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        val navController = rememberNavController()
        NavHost(
```

Add after `val navController = rememberNavController()`:
```kotlin
LaunchedEffect(Unit) {
    mainViewModel.onAppReady()
}
```

Add after the closing `}` of `NavHost(...)` and before the closing `}` of `Surface(...)`:

```kotlin
                    val updateInfo by mainViewModel.updateInfo.collectAsState()
                    val showWhatsNew by mainViewModel.showWhatsNew.collectAsState()
                    val whatsNewChangelog by mainViewModel.whatsNewChangelog.collectAsState()

                    updateInfo?.let { info ->
                        UpdateAvailableBottomSheet(
                            updateInfo = info,
                            onDownload = {
                                mainViewModel.dismissUpdateSheet()
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(info.downloadUrl))
                                startActivity(intent)
                            },
                            onRemindLater = { mainViewModel.dismissUpdateSheet() },
                            onSkip = { mainViewModel.skipVersion(info.version) },
                            onDismiss = { mainViewModel.dismissUpdateSheet() }
                        )
                    }

                    if (showWhatsNew) {
                        WhatsNewDialog(
                            version = BuildConfig.VERSION_NAME,
                            changelog = whatsNewChangelog,
                            onDismiss = { mainViewModel.dismissWhatsNew() }
                        )
                    }
```

- [ ] **Step 3: Build to verify compilation**

```bash
./gradlew.bat :app:assembleDebug --stacktrace
```

Expected: PASS

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/example/workoutapp/MainViewModel.kt app/src/main/java/com/example/workoutapp/MainActivity.kt
git commit -m "feat: integrate update check and What's New into app launch"
```

---

### Task 7: About screen revamp

**Files:**
- Modify: `app/src/main/java/com/example/workoutapp/ui/about/AboutScreen.kt`
- Modify: `app/src/main/java/com/example/workoutapp/ui/settings/SettingsScreen.kt` (make About row clickable, update version format)

- [ ] **Step 1: Rewrite AboutScreen**

Replace `app/src/main/java/com/example/workoutapp/ui/about/AboutScreen.kt`:

```kotlin
package com.example.workoutapp.ui.about

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.workoutapp.BuildConfig

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(navController: NavController) {
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("About") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Workout Tracker",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "v${BuildConfig.VERSION_NAME} (build ${BuildConfig.VERSION_CODE})",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column {
                    ListItem(
                        headlineContent = { Text("Check for Updates") },
                        supportingContent = { Text("See if a newer version is available on GitHub") },
                        leadingContent = { Icon(Icons.Default.Refresh, null) },
                        modifier = Modifier.padding(0.dp)
                    )
                    ListItem(
                        headlineContent = { Text("Send Feedback") },
                        supportingContent = { Text("Report a bug or suggest a feature") },
                        leadingContent = { Icon(Icons.Default.Email, null) },
                        modifier = Modifier.padding(0.dp),
                        onClick = {
                            val intent = Intent(Intent.ACTION_SENDTO).apply {
                                data = Uri.parse("mailto:")
                                putExtra(Intent.EXTRA_EMAIL, arrayOf("a@b.com"))
                                putExtra(Intent.EXTRA_SUBJECT, "Workout Tracker v${BuildConfig.VERSION_NAME} Feedback")
                            }
                            context.startActivity(intent)
                        }
                    )
                    ListItem(
                        headlineContent = { Text("Licenses") },
                        supportingContent = { Text("Open source libraries used in this app") },
                        leadingContent = { Icon(Icons.Default.Security, null) },
                        modifier = Modifier.padding(0.dp)
                    )
                }
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Developed by Milan Ples",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "\u00A9 2025",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
```

- [ ] **Step 2: Update SettingsScreen to link to About and show full version**

Modify `ui/settings/SettingsScreen.kt` — replace the About section (lines 329-340):

Replace:
```kotlin
            // About Section
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "About",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Workout Tracker v${com.example.workoutapp.BuildConfig.VERSION_NAME}")
                    Text("Developed by Milan Ples @2025")
                }
            }
```

With:
```kotlin
            // About Section
            Card(
                modifier = Modifier.fillMaxWidth(),
                onClick = { navController.navigate(Screen.About.route) }
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "About",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Workout Tracker v${com.example.workoutapp.BuildConfig.VERSION_NAME} (build ${com.example.workoutapp.BuildConfig.VERSION_CODE})")
                    Text("Developed by Milan Ples @2025")
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Tap for more info",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
```

Add the import at the top of SettingsScreen:
```kotlin
import com.example.workoutapp.ui.navigation.Screen
```

- [ ] **Step 3: Build to verify compilation**

```bash
./gradlew.bat :app:assembleDebug --stacktrace
```

Expected: PASS

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/example/workoutapp/ui/about/AboutScreen.kt app/src/main/java/com/example/workoutapp/ui/settings/SettingsScreen.kt
git commit -m "feat: revamp About screen and add Settings link"
```

---

### Task 8: CI workflow — extract CHANGELOG.md for releases

**Files:**
- Modify: `.github/workflows/release.yml`

- [ ] **Step 1: Add changelog extraction step to release job**

In `.github/workflows/release.yml`, replace the existing `Create Release` step (lines 91-113) with:

```yaml
      - name: Extract changelog for this version
        id: changelog
        run: |
          VERSION="${{ steps.version.outputs.VERSION }}"
          CHANGELOG=$(awk -v ver="## [${VERSION}]" '
            $0 ~ ver { found=1; next }
            found && /^## \[/ { exit }
            found { print }
          ' CHANGELOG.md || true)

          if [ -z "$CHANGELOG" ]; then
            CHANGELOG=$(awk -v ver="## [Unreleased]" '
              $0 ~ ver { found=1; next }
              found && /^## \[/ { exit }
              found { print }
            ' CHANGELOG.md || true)
          fi

          if [ -z "$CHANGELOG" ]; then
            CHANGELOG="See the commit history for details."
          fi

          {
            echo 'CHANGELOG<<EOF'
            echo "$CHANGELOG"
            echo EOF
          } >> $GITHUB_OUTPUT

      - name: Create Release
        uses: softprops/action-gh-release@v1
        with:
          name: Release ${{ steps.version.outputs.VERSION }}
          body: |
            ## Workout Tracker v${{ steps.version.outputs.VERSION }}

            ${{ steps.changelog.outputs.CHANGELOG }}

            ### Downloads
            - **app-debug.apk**: Debug build for testing
            - **app-release-unsigned.apk**: Release build (unsigned)

            ### Installation
            Download the APK and install on your Android device.
            You may need to enable "Install from Unknown Sources" in your device settings.
          files: |
            ./artifacts/app-debug.apk
            ./artifacts/app-release-unsigned.apk
          draft: false
          prerelease: false
        env:
          GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}
```

- [ ] **Step 2: Commit**

```bash
git add .github/workflows/release.yml
git commit -m "ci: extract CHANGELOG.md section for GitHub Release body"
```

---

### Task 9: Onboarding — auto-trigger tutorial for new users

**Files:**
- Modify: `app/src/main/java/com/example/workoutapp/ui/onboarding/OnboardingScreen.kt`
- Modify: `app/src/main/java/com/example/workoutapp/ui/tutorial/TutorialScreen.kt`
- Modify: `app/src/main/java/com/example/workoutapp/ui/navigation/Screen.kt`
- Modify: `app/src/main/java/com/example/workoutapp/MainActivity.kt`

- [ ] **Step 1: Update Screen.kt to accept navigation argument**

In `app/src/main/java/com/example/workoutapp/ui/navigation/Screen.kt`, change the Tutorial route:

```kotlin
object Tutorial : Screen("tutorial?fromOnboarding={fromOnboarding}")
```

- [ ] **Step 2: Change post-metrics navigation to pass argument**

In `OnboardingScreen.kt`, change lines 97-101 from:
```kotlin
                    viewModel.saveMetrics(w, h, a, gender) {
                        navController.navigate(Screen.Workout.route) {
                            popUpTo(Screen.Onboarding.route) { inclusive = true }
                        }
                    }
```

To:
```kotlin
                    viewModel.saveMetrics(w, h, a, gender) {
                        navController.navigate("tutorial?fromOnboarding=true") {
                            popUpTo(Screen.Onboarding.route) { inclusive = true }
                        }
                    }
```

- [ ] **Step 3: Update TutorialScreen to accept fromOnboarding parameter**

In `TutorialScreen.kt`, change the function signature from:
```kotlin
fun TutorialScreen(
    navController: NavController,
    onComplete: () -> Unit = {}
)
```

To:
```kotlin
fun TutorialScreen(
    navController: NavController,
    onComplete: () -> Unit = {},
    fromOnboarding: Boolean = false
)
```

Add import at top of TutorialScreen.kt:
```kotlin
import com.example.workoutapp.ui.navigation.Screen
```

Change the "Done" button `onClick` (lines 157-163) from:
```kotlin
                            } else {
                                onComplete()
                                navController.navigateUp()
                            }
```

To:
```kotlin
                            } else {
                                onComplete()
                                if (fromOnboarding) {
                                    navController.navigate(Screen.Workout.route) {
                                        popUpTo(0) { inclusive = true }
                                    }
                                } else {
                                    navController.navigateUp()
                                }
                            }
```

- [ ] **Step 4: Update MainActivity.kt Tutorial composable to extract argument**

In `MainActivity.kt`, add the imports:
```kotlin
import androidx.navigation.NavType
import androidx.navigation.compose.navArgument
```

Replace the Tutorial composable (line 84-86) from:
```kotlin
                                    composable(Screen.Tutorial.route) {
                                        com.example.workoutapp.ui.tutorial.TutorialScreen(navController = navController)
                                    }
```

To:
```kotlin
                                    composable(
                                        route = Screen.Tutorial.route,
                                        arguments = listOf(navArgument("fromOnboarding") {
                                            type = NavType.BoolType
                                            defaultValue = false
                                        })
                                    ) { backStackEntry ->
                                        val fromOnboarding = backStackEntry.arguments?.getBoolean("fromOnboarding") ?: false
                                        com.example.workoutapp.ui.tutorial.TutorialScreen(
                                            navController = navController,
                                            fromOnboarding = fromOnboarding
                                        )
                                    }
```

- [ ] **Step 5: Build to verify compilation**

```bash
./gradlew.bat :app:assembleDebug --stacktrace
```

Expected: PASS

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/example/workoutapp/ui/onboarding/OnboardingScreen.kt app/src/main/java/com/example/workoutapp/ui/tutorial/TutorialScreen.kt app/src/main/java/com/example/workoutapp/ui/navigation/Screen.kt app/src/main/java/com/example/workoutapp/MainActivity.kt
git commit -m "feat: auto-trigger tutorial after onboarding with nav argument"
```

---

### Task 10: Tutorial screenshots (replace emojis)

**Files:**
- Modify: `app/src/main/java/com/example/workoutapp/ui/tutorial/TutorialScreen.kt`
- Create: 9 screenshot assets in `app/src/main/res/drawable/` (placeholder images)

- [ ] **Step 1: Create placeholder drawable resources**

Since actual screenshots aren't available in this plan, create placeholder vector drawables as temporary stand-ins. The user can replace them with actual screenshots later.

For each of the 9 pages, create a simple placeholder XML drawable. Create `app/src/main/res/drawable/tutorial_page_1.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="360dp"
    android:height="640dp"
    android:viewportWidth="360"
    android:viewportHeight="640">
    <path
        android:fillColor="#1C1C1E"
        android:pathData="M0,0h360v640H0z"/>
    <path
        android:fillColor="#00E676"
        android:pathData="M130,300h100v40h-100z"/>
    <path
        android:fillColor="#FFFFFF"
        android:pathData="M140,310 L150,320 L170,300"
        android:strokeWidth="3"
        android:strokeColor="#FFFFFF"/>
</vector>
```

Create 8 more similar placeholder files (`tutorial_page_2.xml` through `tutorial_page_9.xml`) with varied shapes to distinguish them. Use `@color/neon_green` if defined, or the hex `#00E676`.

- [ ] **Step 2: Update TutorialPage data class and TutorialPageContent**

In `TutorialScreen.kt`, update the `TutorialPage` data class to support images:

```kotlin
data class TutorialPage(
    val title: String,
    val description: String,
    val emoji: String = "",
    val imageRes: Int? = null
)
```

Update the pages list to use images instead of emojis:
```kotlin
    val pages = listOf(
        TutorialPage(
            title = "Workout Library",
            description = "Create exercises and manage your workout library with custom sets, reps, and weights.",
            imageRes = R.drawable.tutorial_page_1
        ),
        TutorialPage(
            title = "Exercise Wizard",
            description = "Add exercises with sets, reps, weights, hold timers, or ESP sensor support.",
            imageRes = R.drawable.tutorial_page_2
        ),
        TutorialPage(
            title = "Active Session",
            description = "Start a session and work through your exercises one by one.",
            imageRes = R.drawable.tutorial_page_3
        ),
        TutorialPage(
            title = "Hold to Complete",
            description = "Hold the exercise card to mark a set as complete during your session.",
            imageRes = R.drawable.tutorial_page_4
        ),
        TutorialPage(
            title = "Rest Timer",
            description = "Auto-countdown between sets with configurable rest periods.",
            imageRes = R.drawable.tutorial_page_5
        ),
        TutorialPage(
            title = "Weight Adjustment",
            description = "Adjust weight in 5kg increments during your session for quick changes.",
            imageRes = R.drawable.tutorial_page_6
        ),
        TutorialPage(
            title = "History Dashboard",
            description = "Track your progress with volume charts, personal records, and workout streaks.",
            imageRes = R.drawable.tutorial_page_7
        ),
        TutorialPage(
            title = "Rest Day Calendar",
            description = "Mark rest days and see your weekly and monthly consistency stats.",
            imageRes = R.drawable.tutorial_page_8
        ),
        TutorialPage(
            title = "Settings & Customization",
            description = "Customize sounds, timers, theme mode, and ESP sensor support.",
            imageRes = R.drawable.tutorial_page_9
        )
    )
```

- [ ] **Step 3: Update TutorialPageContent to render images**

Replace:
```kotlin
@Composable
private fun TutorialPageContent(page: TutorialPage) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Animated illustration based on page
        AnimatedIllustration(page.emoji)
        
        Spacer(modifier = Modifier.height(32.dp))
        Text(
            text = page.title,
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = page.description,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
```

With:
```kotlin
@Composable
private fun TutorialPageContent(page: TutorialPage) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (page.imageRes != null) {
            androidx.compose.foundation.Image(
                painter = androidx.compose.ui.res.painterResource(id = page.imageRes),
                contentDescription = page.title,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(320.dp),
                contentScale = androidx.compose.ui.layout.ContentScale.Fit
            )
        } else {
            AnimatedIllustration(page.emoji)
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = page.title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = page.description,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
```

- [ ] **Step 4: Build to verify compilation**

```bash
./gradlew.bat :app:assembleDebug --stacktrace
```

Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/example/workoutapp/ui/tutorial/TutorialScreen.kt app/src/main/res/drawable/tutorial_page_*.xml
git commit -m "feat: replace tutorial emojis with screenshot images"
```

---

### Task 11: Final verification build

- [ ] **Step 1: Run full build**

```bash
./gradlew.bat :app:assembleDebug --stacktrace
```

Expected: PASS (BUILD SUCCESSFUL)

- [ ] **Step 2: Run unit tests**

```bash
./gradlew.bat :app:testDebugUnitTest --stacktrace
```

Expected: PASS (all tests green)

- [ ] **Step 3: Verify version display**

```bash
./gradlew.bat signingReport 2>$null | Select-String "VERSION"
```

Expected: shows current version info
```

---

