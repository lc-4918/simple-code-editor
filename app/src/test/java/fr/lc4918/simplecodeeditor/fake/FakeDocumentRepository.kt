package fr.lc4918.simplecodeeditor.fake

import fr.lc4918.simplecodeeditor.data.DocumentRepository
import fr.lc4918.simplecodeeditor.model.DocumentLocation
import fr.lc4918.simplecodeeditor.model.DocumentSource
import java.io.IOException

/** In-memory storage, so the view model can be tested without a device. */
class FakeDocumentRepository : DocumentRepository {

    /** Content keyed by location or by address, as it would be found there. */
    val stored = mutableMapOf<String, DocumentSource>()

    /** When set, every call fails with it. */
    var failure: Throwable? = null

    override suspend fun read(location: DocumentLocation): DocumentSource = read(location.value)

    override suspend fun write(location: DocumentLocation, content: String) {
        write(location.value, content)
    }

    override suspend fun read(url: String): DocumentSource {
        failure?.let { throw it }
        return stored[url] ?: throw IOException("Nothing at $url")
    }

    override suspend fun write(url: String, content: String) {
        failure?.let { throw it }
        stored[url] = DocumentSource(
            name = stored[url]?.name,
            mimeType = stored[url]?.mimeType,
            content = content,
        )
    }
}
