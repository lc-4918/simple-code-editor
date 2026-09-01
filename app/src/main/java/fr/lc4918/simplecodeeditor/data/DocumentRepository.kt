package fr.lc4918.simplecodeeditor.data

import fr.lc4918.simplecodeeditor.model.DocumentLocation
import fr.lc4918.simplecodeeditor.model.DocumentSource

/**
 * Reads and writes documents, wherever they live.
 *
 * A [DocumentLocation] comes from the storage picker, which covers the
 * device and every cloud provider installed on it. A plain address is fetched
 * and written over HTTP.
 *
 * Every method throws on failure, and the caller turns that into a message.
 */
interface DocumentRepository {

    suspend fun read(location: DocumentLocation): DocumentSource

    suspend fun write(location: DocumentLocation, content: String)

    suspend fun read(url: String): DocumentSource

    suspend fun write(url: String, content: String)
}
