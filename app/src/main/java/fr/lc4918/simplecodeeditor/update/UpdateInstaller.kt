package fr.lc4918.simplecodeeditor.update

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.net.toUri
import fr.lc4918.simplecodeeditor.BuildConfig

/**
 * Brings a newer version down and hands it to the installer of the system.
 *
 * The application is not on a store, so nothing else would offer the update.
 * The file goes to the private folder of the application, which needs no
 * storage permission and no provider of our own: the download service knows
 * how to hand back an installable address for its own files.
 */
object UpdateInstaller {

    private const val APK_MIME = "application/vnd.android.package-archive"

    /**
     * False for a development build.
     *
     * It carries another application id and another signature, so the release
     * could not replace it even if it wanted to, and its version name ends in
     * a mark that would read as newer than the release it is compared with.
     */
    val isSupported: Boolean get() = !BuildConfig.DEBUG

    fun download(context: Context, release: ReleaseInfo, title: String): Long {
        val service = context.getSystemService(DownloadManager::class.java)
        val request = DownloadManager.Request(release.apkUrl.toUri()).apply {
            setTitle(title)
            setDestinationInExternalFilesDir(context, null, "simple-code-${release.version}.apk")
            setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            setMimeType(APK_MIME)
        }
        return service.enqueue(request)
    }

    /** What opens the installer on a download that finished, or null. */
    fun installIntent(context: Context, downloadId: Long): Intent? {
        val service = context.getSystemService(DownloadManager::class.java)
        val uri: Uri = service.getUriForDownloadedFile(downloadId) ?: return null
        return Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, APK_MIME)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
        }
    }

    /**
     * Whether the application is allowed to install another.
     *
     * From Android 8 the permission is granted application by application, and
     * without it the installer opens and fails without saying why. Before it,
     * the setting is one of the system as a whole and there is nothing here to
     * ask about.
     */
    fun canInstall(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.O ||
            context.packageManager.canRequestPackageInstalls()

    fun permissionSettings(context: Context): Intent =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Intent(
                Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                "package:${context.packageName}".toUri(),
            )
        } else {
            Intent(Settings.ACTION_SECURITY_SETTINGS)
        }

    /**
     * Drops the update files that are no longer of any use.
     *
     * Nothing takes them away once installed, so without this one stays behind
     * for every update the application ever took. What is newer than the
     * version running is kept: it is the one asked for and not yet installed.
     */
    fun sweep(context: Context, currentVersionCode: Int = BuildConfig.VERSION_CODE) {
        val folder = context.getExternalFilesDir(null) ?: return
        folder.listFiles().orEmpty().forEach { file ->
            val code = versionCodeOf(file.name) ?: return@forEach
            if (code <= currentVersionCode) file.delete()
        }
    }

    /**
     * The version an update file carries in its name, or null when the name is
     * not one of ours: what we did not put there we do not remove.
     */
    internal fun versionCodeOf(name: String): Int? {
        val match = APK_NAME.matchEntire(name) ?: return null
        val (major, minor, patch) = match.destructured
        return major.toInt() * 10_000 + minor.toInt() * 100 + patch.toInt()
    }

    /** Also matches the name the download service gives a second copy. */
    private val APK_NAME = Regex("""^simple-code-v?(\d+)\.(\d+)\.(\d+).*\.apk$""")
}
