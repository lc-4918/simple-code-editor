package fr.lc4918.simplecodeeditor.update

import fr.lc4918.simplecodeeditor.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * What the release carries about itself.
 *
 * Written by the workflow at each tag and published beside the APK, which is
 * how the application learns of a version it was built before.
 */
data class ReleaseInfo(
    val version: String,
    val versionCode: Int,
    val releaseDate: String,
    val apkUrl: String,
    val changelog: String,
)

/** Where the application looks to learn whether a newer one exists. */
interface UpdateRepository {
    suspend fun latest(): ReleaseInfo?
}

/**
 * The manifest of the last release, read from the address GitHub keeps stable.
 *
 * That address redirects to the asset of the latest release, served from the
 * network of GitHub rather than from its API, which allows sixty requests an
 * hour per address and would run out.
 */
class GithubUpdateRepository(
    private val manifestUrl: String = MANIFEST_URL,
) : UpdateRepository {

    override suspend fun latest(): ReleaseInfo? = withContext(Dispatchers.IO) {
        val body = fetch() ?: return@withContext null
        runCatching {
            val json = JSONObject(body)
            ReleaseInfo(
                version = json.getString("version"),
                versionCode = json.getInt("versionCode"),
                releaseDate = json.optString("releaseDate"),
                apkUrl = json.getString("apkUrl"),
                changelog = json.optString("changelog"),
            )
        }.getOrNull()
    }

    private fun fetch(): String? {
        val connection = (URL(manifestUrl).openConnection() as HttpURLConnection).apply {
            connectTimeout = TIMEOUT_MILLIS
            readTimeout = TIMEOUT_MILLIS
            // The stable address is a redirection, so following it is the
            // whole point of using it.
            instanceFollowRedirects = true
            setRequestProperty("User-Agent", USER_AGENT)
        }
        return try {
            if (connection.responseCode in 200..299) {
                connection.inputStream.use { it.reader().readText() }
            } else {
                null
            }
        } catch (error: Exception) {
            null
        } finally {
            connection.disconnect()
        }
    }

    private companion object {
        const val MANIFEST_URL =
            "https://github.com/lc-4918/simple-code-editor/releases/latest/download/" +
                "latest-release.json"
        const val TIMEOUT_MILLIS = 15_000
        val USER_AGENT = "SimpleCode/${BuildConfig.VERSION_NAME} (Android)"
    }
}
