package fr.lc4918.simplecodeeditor.data

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import fr.lc4918.simplecodeeditor.model.DocumentLocation
import fr.lc4918.simplecodeeditor.model.DocumentSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/** Storage access framework for locations, and HTTP for addresses. */
class AndroidDocumentRepository(private val context: Context) : DocumentRepository {

    override suspend fun read(location: DocumentLocation): DocumentSource =
        withContext(Dispatchers.IO) {
            val uri = Uri.parse(location.value)
            val resolver = context.contentResolver
            val content = resolver.openInputStream(uri)
                ?.use { stream -> stream.reader().readText() }
                ?: throw IOException("No content at $uri")

            DocumentSource(
                name = displayName(uri),
                mimeType = resolver.getType(uri),
                content = content,
            )
        }

    override suspend fun write(location: DocumentLocation, content: String) =
        withContext(Dispatchers.IO) {
            // Truncating matters: without it a shorter document would leave the
            // tail of the previous one behind.
            val stream = context.contentResolver.openOutputStream(Uri.parse(location.value), "wt")
                ?: throw IOException("Cannot write to ${location.value}")
            stream.use { it.writer().apply { write(content) }.flush() }
        }

    override suspend fun read(url: String): DocumentSource = withContext(Dispatchers.IO) {
        val connection = openConnection(url, "GET")
        try {
            val code = connection.responseCode
            if (code !in 200..299) throw IOException("HTTP $code for $url")
            DocumentSource(
                name = URL(url).path.substringAfterLast('/').ifEmpty { null },
                mimeType = connection.contentType,
                content = connection.inputStream.use { it.reader().readText() },
            )
        } finally {
            connection.disconnect()
        }
    }

    override suspend fun write(url: String, content: String) = withContext(Dispatchers.IO) {
        val connection = openConnection(url, "PUT")
        try {
            connection.doOutput = true
            connection.outputStream.use { it.writer().apply { write(content) }.flush() }
            val code = connection.responseCode
            if (code !in 200..299) throw IOException("HTTP $code for $url")
        } finally {
            connection.disconnect()
        }
    }

    private fun openConnection(url: String, method: String): HttpURLConnection {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.requestMethod = method
        connection.connectTimeout = TIMEOUT_MILLIS
        connection.readTimeout = TIMEOUT_MILLIS
        return connection
    }

    /** The name the provider shows for the document, when it exposes one. */
    private fun displayName(uri: Uri): String? {
        val projection = arrayOf(OpenableColumns.DISPLAY_NAME)
        context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
            val column = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (column >= 0 && cursor.moveToFirst()) return cursor.getString(column)
        }
        return uri.lastPathSegment?.substringAfterLast('/')
    }

    private companion object {
        const val TIMEOUT_MILLIS = 15_000
    }
}
