package com.flydrop.app.data.update

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.compose.runtime.Immutable
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import kotlin.coroutines.coroutineContext

@Immutable
sealed interface UpdateCheck {
    data class UpToDate(val installed: String) : UpdateCheck

    data class Available(val release: AppRelease) : UpdateCheck

    /** No release published yet, so there is nothing to compare against. */
    data object NoReleases : UpdateCheck

    data class Failed(val message: String) : UpdateCheck
}

@Immutable
sealed interface DownloadResult {
    data class Success(val apk: File) : DownloadResult

    data class Failed(val message: String) : DownloadResult
}

/**
 * Checks GitHub Releases for a newer build and fetches its APK.
 *
 * This app is distributed as an APK from GitHub rather than through Play, so
 * updating it means downloading and installing one. Three things keep that from
 * being a hole:
 *
 *  - the release is read over HTTPS from the project's own repository, and a
 *    download URL that is not HTTPS is refused rather than followed;
 *  - the downloaded APK's signing certificate is compared against the running
 *    app's before the installer is ever offered it, so a file that is not a
 *    build of this app by this developer is deleted instead of installed;
 *  - Android itself refuses to replace an installed app with a differently
 *    signed one, which is the guarantee underneath the check above.
 *
 * Nothing is installed silently. The system installer always asks, and the user
 * must have allowed this app to install unknown apps before it can even do that.
 */
class UpdateRepository(private val context: Context) {

    /**
     * Reads the latest release and decides whether it is worth offering.
     *
     * Uses HttpURLConnection and org.json rather than adding an HTTP client and
     * a JSON library for one request and one field.
     */
    suspend fun check(apiUrl: String, installedVersion: String): UpdateCheck =
        withContext(Dispatchers.IO) {
            val body = try {
                readLatestRelease(apiUrl)
            } catch (e: Exception) {
                coroutineContext.ensureActive()
                return@withContext UpdateCheck.Failed(e.networkMessage())
            } ?: return@withContext UpdateCheck.NoReleases

            val release = try {
                parseRelease(body)
            } catch (_: Exception) {
                return@withContext UpdateCheck.Failed("GitHub returned a release this app could not read.")
            }

            classifyUpdate(release, installedVersion)
        }

    /** Null when the repository has no releases yet, which GitHub reports as a 404. */
    private fun readLatestRelease(apiUrl: String): String? {
        val connection = (URL(apiUrl).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            // Pins the response shape; without it GitHub may serve a newer one.
            setRequestProperty("Accept", "application/vnd.github+json")
            setRequestProperty("X-GitHub-Api-Version", "2022-11-28")
            setRequestProperty("User-Agent", "FlyDrop-Android")
            connectTimeout = TIMEOUT_MS
            readTimeout = TIMEOUT_MS
        }
        try {
            return when (val code = connection.responseCode) {
                HttpURLConnection.HTTP_OK ->
                    connection.inputStream.bufferedReader().use { it.readText() }

                HttpURLConnection.HTTP_NOT_FOUND -> null

                HttpURLConnection.HTTP_FORBIDDEN ->
                    throw UpdateException("GitHub is rate limiting this device. Try again later.")

                else -> throw UpdateException("GitHub replied $code.")
            }
        } finally {
            connection.disconnect()
        }
    }

    /**
     * Downloads [release]'s APK, reporting progress as a 0..1 fraction, and
     * refuses anything that is not a build of this app by this developer.
     */
    suspend fun download(
        release: AppRelease,
        onProgress: (Float?) -> Unit,
    ): DownloadResult = withContext(Dispatchers.IO) {
        val url = release.apkUrl
            ?: return@withContext DownloadResult.Failed("That release has no APK to install.")

        // A plaintext URL would let anything on the path swap the file.
        if (!url.startsWith("https://", ignoreCase = true)) {
            return@withContext DownloadResult.Failed("The download link is not secure.")
        }

        val target = try {
            fetch(url, release.apkBytes, onProgress)
        } catch (e: Exception) {
            coroutineContext.ensureActive()
            return@withContext DownloadResult.Failed(e.networkMessage())
        }

        if (!isSignedLikeThisApp(target)) {
            target.delete()
            return@withContext DownloadResult.Failed(
                "That download is not signed by this app's developer, so it was discarded.",
            )
        }
        DownloadResult.Success(target)
    }

    private suspend fun fetch(url: String, expectedBytes: Long, onProgress: (Float?) -> Unit): File {
        val directory = File(context.cacheDir, DIRECTORY).apply {
            // Only one update is ever in flight, so anything already here is a
            // leftover from a previous run and only wastes space.
            deleteRecursively()
            mkdirs()
        }
        val target = File(directory, "update.apk")

        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            instanceFollowRedirects = true
            setRequestProperty("User-Agent", "FlyDrop-Android")
            connectTimeout = TIMEOUT_MS
            readTimeout = DOWNLOAD_TIMEOUT_MS
        }
        try {
            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                throw UpdateException("The download failed (${connection.responseCode}).")
            }
            // Content-Length beats the asset size when present: it is what is
            // actually arriving, redirects and all.
            val total = connection.contentLengthLong.takeIf { it > 0 } ?: expectedBytes
            var written = 0L

            connection.inputStream.use { input ->
                target.outputStream().use { output ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    while (true) {
                        // Lets a cancelled check stop mid-download rather than
                        // pulling megabytes nobody is waiting for any more.
                        coroutineContext.ensureActive()
                        val read = input.read(buffer)
                        if (read < 0) break
                        output.write(buffer, 0, read)
                        written += read
                        onProgress(if (total > 0) (written.toFloat() / total).coerceIn(0f, 1f) else null)
                    }
                }
            }
            return target
        } finally {
            connection.disconnect()
        }
    }

    /**
     * True when [apk] carries the same signing certificate as the running app.
     *
     * Android would refuse the install anyway, but failing here means the user
     * gets a straight answer instead of the system's generic "App not
     * installed", and a file that has no business being installed never reaches
     * the installer at all.
     */
    private fun isSignedLikeThisApp(apk: File): Boolean = runCatching {
        val manager = context.packageManager
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            PackageManager.GET_SIGNING_CERTIFICATES
        } else {
            @Suppress("DEPRECATION")
            PackageManager.GET_SIGNATURES
        }

        val downloaded = manager.getPackageArchiveInfo(apk.path, flags) ?: return false
        if (downloaded.packageName != context.packageName) return false
        val installed = manager.getPackageInfo(context.packageName, flags)

        val downloadedSignatures = downloaded.signatureBytes()
        downloadedSignatures.isNotEmpty() && downloadedSignatures == installed.signatureBytes()
    }.getOrDefault(false)

    @Suppress("DEPRECATION")
    private fun android.content.pm.PackageInfo.signatureBytes(): Set<String> {
        val certificates = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            signingInfo?.let {
                // A rotated key reports its history; only the current signer is
                // meaningful when there are several.
                if (it.hasMultipleSigners()) it.apkContentsSigners else it.signingCertificateHistory
            }
        } else {
            signatures
        }
        return certificates.orEmpty().filterNotNull().map { it.toCharsString() }.toSet()
    }

    /**
     * Hands [apk] to the system installer. It always asks before installing;
     * nothing here can install anything on its own.
     */
    fun install(apk: File): Boolean = runCatching {
        val uri: Uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", apk)
        context.startActivity(
            Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            },
        )
        true
    }.getOrDefault(false)

    /**
     * Whether the user has allowed this app to install others. Sideloaded
     * updates are impossible until they have, so the UI asks first rather than
     * downloading megabytes that cannot be used.
     */
    fun canInstallPackages(): Boolean = context.packageManager.canRequestPackageInstalls()

    /** Opens the system screen where that permission is granted. */
    fun openInstallPermissionSettings(): Boolean = runCatching {
        context.startActivity(
            Intent(
                android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                "package:${context.packageName}".toUri(),
            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
        true
    }.getOrDefault(false)

    private companion object {
        const val TIMEOUT_MS = 15_000
        const val DOWNLOAD_TIMEOUT_MS = 60_000
        const val DIRECTORY = "updates"
    }
}

/** Pure update eligibility decision, kept separate from the GitHub request for testing. */
internal fun classifyUpdate(release: AppRelease, installedVersion: String): UpdateCheck {
    val candidate = AppVersion.parse(release.tag)
    val installed = AppVersion.parse(installedVersion)
    if (candidate == null || installed == null) {
        return UpdateCheck.Failed(
            "The latest release is tagged \"${release.tag}\", which this app " +
                "cannot compare against $installedVersion.",
        )
    }

    if (candidate <= installed) return UpdateCheck.UpToDate(installedVersion)
    if (release.apkUrl.isNullOrBlank()) {
        return UpdateCheck.Failed(
            "A newer release (${release.tag}) exists, but it has no APK to install.",
        )
    }
    return UpdateCheck.Available(release)
}

private class UpdateException(message: String) : Exception(message)

/** Network failures arrive as a zoo of exception types; none of them read well. */
private fun Exception.networkMessage(): String = when (this) {
    is UpdateException -> message.orEmpty()
    is java.net.UnknownHostException -> "No internet connection."
    is java.net.SocketTimeoutException -> "The connection timed out."
    is javax.net.ssl.SSLException -> "The secure connection failed."
    else -> message ?: "Could not reach GitHub (${this::class.simpleName})."
}
