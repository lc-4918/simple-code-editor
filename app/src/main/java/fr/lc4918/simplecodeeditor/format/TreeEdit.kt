package fr.lc4918.simplecodeeditor.format

import fr.lc4918.simplecodeeditor.model.DocumentFormat
import fr.lc4918.simplecodeeditor.model.NodeKind
import fr.lc4918.simplecodeeditor.model.Span
import fr.lc4918.simplecodeeditor.model.TreeNode

/**
 * Changes one name or one value of a document, in the document itself.
 *
 * The tree is read from the text and never written back over it: what is
 * replaced is the handful of characters the name or the value occupies, which
 * leaves the comments, the declaration and the layout exactly as they were.
 */
object TreeEdit {

    /** The document with the text of [span] replaced. */
    fun replace(content: String, span: Span, replacement: String): String =
        content.substring(0, span.start) + replacement + content.substring(span.end)

    /**
     * The document with the value of [node] set to what was typed.
     *
     * Null when the node has no value written in the document to replace,
     * which is the case of an object, an array and an element.
     */
    fun withValue(
        content: String,
        format: DocumentFormat,
        node: TreeNode,
        typed: String,
    ): String? {
        val span = node.valueSpan ?: return null
        if (node.children.isNotEmpty() || node.kind == NodeKind.OBJECT ||
            node.kind == NodeKind.ARRAY || node.kind == NodeKind.ELEMENT
        ) {
            return null
        }
        val written = when (format) {
            DocumentFormat.JSON -> jsonLiteral(typed)
            else -> escapeXmlText(typed, insideAttribute = node.kind == NodeKind.ATTRIBUTE)
        }
        return replace(content, span, written)
    }

    /** The document with the name of [node] set to what was typed. */
    fun withName(
        content: String,
        format: DocumentFormat,
        node: TreeNode,
        typed: String,
    ): String? {
        val span = node.nameSpan ?: return null
        val cleaned = typed.trim()
        if (cleaned.isEmpty()) return null
        val written = when {
            format == DocumentFormat.JSON -> "\"" + DocumentFormatter.escape(cleaned) + "\""
            // The prefix marks an attribute in the view and belongs to the
            // view, not to the document.
            node.kind == NodeKind.ATTRIBUTE -> cleaned.removePrefix(ATTRIBUTE_PREFIX)
            else -> cleaned
        }
        // The closing tag carries the name too, and has to follow.
        if (format != DocumentFormat.JSON && node.kind == NodeKind.ELEMENT) {
            return renameElement(content, node, written)
        }
        return replace(content, span, written)
    }

    /**
     * What JSON writes for the text that was typed.
     *
     * The type follows what was typed, which is how someone turns a value
     * into a number or into null without saying so. Text that would be read
     * as one of those and is meant as a string is written between quotes, and
     * comes back out without them.
     */
    fun jsonLiteral(typed: String): String {
        val trimmed = typed.trim()
        return when {
            trimmed == "true" || trimmed == "false" || trimmed == "null" -> trimmed
            trimmed.length > 1 && trimmed.startsWith('"') && trimmed.endsWith('"') ->
                "\"" + DocumentFormatter.escape(trimmed.substring(1, trimmed.length - 1)) + "\""

            isJsonNumber(trimmed) -> trimmed
            else -> "\"" + DocumentFormatter.escape(typed) + "\""
        }
    }

    /** The kind JSON would give the text that was typed. */
    fun jsonKindOf(typed: String): NodeKind {
        val trimmed = typed.trim()
        return when {
            trimmed == "true" || trimmed == "false" -> NodeKind.BOOLEAN
            trimmed == "null" -> NodeKind.NULL
            trimmed.length > 1 && trimmed.startsWith('"') && trimmed.endsWith('"') ->
                NodeKind.STRING

            isJsonNumber(trimmed) -> NodeKind.NUMBER
            else -> NodeKind.STRING
        }
    }

    private fun isJsonNumber(text: String): Boolean =
        NUMBER.matches(text) && text.toDoubleOrNull() != null

    /** Both tags carry the name, so both have to change together. */
    private fun renameElement(content: String, node: TreeNode, written: String): String? {
        val span = node.nameSpan ?: return null
        val opening = replace(content, span, written)
        val shift = written.length - (span.end - span.start)

        // An element that closes itself has no second tag to follow.
        val tagEnd = opening.indexOf('>', span.start)
        if (tagEnd > 0 && opening[tagEnd - 1] == '/') return opening

        val closing = "</" + node.name
        val at = opening.indexOf(closing, span.end + shift)
        if (at < 0) return opening
        return replace(opening, Span(at, at + closing.length), "</$written")
    }

    private fun escapeXmlText(text: String, insideAttribute: Boolean): String {
        val escaped = text
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
        return if (insideAttribute) escaped.replace("\"", "&quot;") else escaped
    }

    private const val ATTRIBUTE_PREFIX = "@"

    private val NUMBER = Regex("""-?\d+(\.\d+)?([eE][+-]?\d+)?""")
}
