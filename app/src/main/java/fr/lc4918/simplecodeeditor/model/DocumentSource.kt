package fr.lc4918.simplecodeeditor.model

/**
 * What a location gave back when the document was read from it.
 *
 * The name and the MIME type are whatever the provider reported, so both can be
 * missing and neither is trusted beyond feeding the format detection.
 */
data class DocumentSource(
    val name: String?,
    val mimeType: String?,
    val content: String,
)
