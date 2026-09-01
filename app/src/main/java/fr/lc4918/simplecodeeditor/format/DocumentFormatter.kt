package fr.lc4918.simplecodeeditor.format

import fr.lc4918.simplecodeeditor.model.DocumentFormat

/**
 * Lays a document out, or squeezes it, according to its format.
 *
 * A format with no layout of its own, such as CSV, is handed back unchanged,
 * which matches the toolbar not offering the two tools for it.
 */
object DocumentFormatter {

    fun indent(content: String, format: DocumentFormat, width: Int): String = when (format) {
        DocumentFormat.JSON -> JsonFormatter.indent(content, width)
        DocumentFormat.XML -> MarkupFormatter.indent(content, width)
        DocumentFormat.HTML -> MarkupFormatter.indent(content, width, html = true)
        DocumentFormat.CSS -> CodeFormatter.indent(content, CodeFormatter.Dialect.STYLE, width)
        DocumentFormat.JAVASCRIPT ->
            CodeFormatter.indent(content, CodeFormatter.Dialect.SCRIPT, width)

        DocumentFormat.CSV, DocumentFormat.MARKDOWN, DocumentFormat.PLAIN_TEXT -> content
    }

    fun compact(content: String, format: DocumentFormat): String = when (format) {
        DocumentFormat.JSON -> JsonFormatter.compact(content)
        DocumentFormat.XML -> MarkupFormatter.compact(content)
        DocumentFormat.HTML -> MarkupFormatter.compact(content, html = true)
        DocumentFormat.CSS -> CodeFormatter.compact(content, CodeFormatter.Dialect.STYLE)
        DocumentFormat.JAVASCRIPT -> CodeFormatter.compact(content, CodeFormatter.Dialect.SCRIPT)
        DocumentFormat.CSV, DocumentFormat.MARKDOWN, DocumentFormat.PLAIN_TEXT -> content
    }

    /**
     * The document as it would be written inside a string literal.
     *
     * The surrounding quotes are left out, so the result can be pasted between
     * the quotes the target language already has.
     */
    fun escape(content: String): String {
        val out = StringBuilder(content.length + content.length / 8)
        content.forEach { character ->
            when (character) {
                '\\' -> out.append("\\\\")
                '"' -> out.append("\\\"")
                '\n' -> out.append("\\n")
                '\r' -> out.append("\\r")
                '\t' -> out.append("\\t")
                '\b' -> out.append("\\b")
                '\u000C' -> out.append("\\f")
                else ->
                    if (character < ' ') {
                        out.append("\\u%04x".format(character.code))
                    } else {
                        out.append(character)
                    }
            }
        }
        return out.toString()
    }
}
