package fr.lc4918.simplecodeeditor.model

/**
 * The document currently held by the editor.
 *
 * [origin] is null for a document that has never been read from or written to
 * storage, which is what tells the Save action whether it can overwrite in place
 * or has to ask for a destination.
 */
data class EditorDocument(
    val name: String,
    val content: String,
    val format: DocumentFormat,
    val origin: DocumentLocation? = null,
    val isModified: Boolean = false,
) {
    val capabilities: FormatCapabilities
        get() = format.capabilities

    /** File name including the extension matching the current format. */
    fun fileName(): String {
        val extension = format.defaultExtension
        val hasKnownExtension = name.substringAfterLast('.', "")
            .lowercase() in format.extensions
        return if (hasKnownExtension) name else "$name.$extension"
    }

    companion object {
        const val DEFAULT_NAME = "document"

        fun empty(format: DocumentFormat = DocumentFormat.JSON): EditorDocument =
            EditorDocument(name = DEFAULT_NAME, content = "", format = format)
    }
}
