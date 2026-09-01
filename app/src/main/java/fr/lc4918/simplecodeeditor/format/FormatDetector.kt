package fr.lc4918.simplecodeeditor.format

import fr.lc4918.simplecodeeditor.model.DocumentFormat

/**
 * Works out which format a document uses.
 *
 * The file name is trusted first, then the MIME type reported by the provider,
 * and only then the content itself. Content sniffing stays deliberately cheap:
 * it looks at the opening characters and at a handful of shape rules rather than
 * trying to fully parse the document.
 */
object FormatDetector {

    private const val SNIFF_LIMIT = 8192
    private val CSV_DELIMITERS = listOf(',', ';', '\t', '|')

    /**
     * @param fileName name of the file, with or without a path
     * @param mimeType MIME type reported by the content provider, if any
     * @param content the document text, used only when the two above fail
     */
    fun detect(
        fileName: String? = null,
        mimeType: String? = null,
        content: String? = null,
    ): DocumentFormat {
        fileName?.let { name ->
            val extension = name.substringAfterLast('/').substringAfterLast('.', "")
            if (extension.isNotEmpty()) {
                DocumentFormat.fromExtension(extension)?.let { return it }
            }
        }

        mimeType?.let { type ->
            DocumentFormat.fromMimeType(type)?.let { return it }
        }

        return content?.let(::detectFromContent) ?: DocumentFormat.PLAIN_TEXT
    }

    /** Best guess based on the document text alone. */
    fun detectFromContent(content: String): DocumentFormat {
        val sample = content.take(SNIFF_LIMIT)
        val trimmed = sample.trimStart()
        if (trimmed.isEmpty()) return DocumentFormat.PLAIN_TEXT

        if (looksLikeMarkup(trimmed)) {
            return if (looksLikeHtml(trimmed)) DocumentFormat.HTML else DocumentFormat.XML
        }
        if (looksLikeJson(trimmed)) return DocumentFormat.JSON
        if (looksLikeCsv(sample)) return DocumentFormat.CSV
        if (looksLikeCss(sample)) return DocumentFormat.CSS
        if (looksLikeJavaScript(sample)) return DocumentFormat.JAVASCRIPT

        return DocumentFormat.PLAIN_TEXT
    }

    private fun looksLikeMarkup(trimmed: String): Boolean = trimmed.startsWith("<")

    private fun looksLikeHtml(trimmed: String): Boolean {
        val head = trimmed.take(512).lowercase()
        return head.startsWith("<!doctype html") ||
            head.contains("<html") ||
            head.contains("<head") ||
            head.contains("<body")
    }

    private fun looksLikeJson(trimmed: String): Boolean {
        val first = trimmed.first()
        if (first != '{' && first != '[') return false
        val last = trimmed.trimEnd().lastOrNull() ?: return false
        // A truncated sample cannot be balanced, so only require a closing
        // bracket when the whole document fits in the sample.
        return if (trimmed.length < SNIFF_LIMIT) {
            (first == '{' && last == '}') || (first == '[' && last == ']')
        } else {
            true
        }
    }

    private fun looksLikeCsv(sample: String): Boolean {
        val lines = sample.lineSequence()
            .filter { it.isNotBlank() }
            .take(10)
            .toList()
        if (lines.size < 2) return false

        return CSV_DELIMITERS.any { delimiter ->
            val counts = lines.map { line -> countOutsideQuotes(line, delimiter) }
            counts.first() > 0 && counts.all { it == counts.first() }
        }
    }

    private fun countOutsideQuotes(line: String, delimiter: Char): Int {
        var inQuotes = false
        var count = 0
        for (character in line) {
            when {
                character == '"' -> inQuotes = !inQuotes
                character == delimiter && !inQuotes -> count++
            }
        }
        return count
    }

    private fun looksLikeCss(sample: String): Boolean {
        val withoutComments = sample.replace(Regex("/\\*.*?\\*/", RegexOption.DOT_MATCHES_ALL), "")
        return Regex("[^{}]+\\{[^{}]*[a-zA-Z-]+\\s*:[^{};]+;?[^{}]*}")
            .containsMatchIn(withoutComments)
    }

    private fun looksLikeJavaScript(sample: String): Boolean =
        Regex("""\b(function|const|let|var|class|import|export|return)\b""")
            .containsMatchIn(sample) ||
            sample.contains("=>")
}
