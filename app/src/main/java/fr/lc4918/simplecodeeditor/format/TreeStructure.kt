package fr.lc4918.simplecodeeditor.format

import fr.lc4918.simplecodeeditor.model.DocumentFormat
import fr.lc4918.simplecodeeditor.model.NodeKind
import fr.lc4918.simplecodeeditor.model.Span
import fr.lc4918.simplecodeeditor.model.TreeNode

/**
 * Moves whole nodes around, in the document rather than over it.
 *
 * Like the editing of a single value, every move here is a handful of
 * characters put in another place: what surrounds the node keeps its comments
 * and its layout. What each move has to get right is the punctuation between
 * entries, a comma in JSON and nothing at all in XML, which is where a move
 * that only replaced the node itself would leave a document that no longer
 * reads.
 *
 * Every method answers null when the move has no meaning for the node it was
 * given, which leaves the document as it was.
 */
object TreeStructure {

    /** The text of the node, as it stands in the document. */
    fun textOf(content: String, node: TreeNode): String? {
        val span = node.entrySpan ?: return null
        return content.substring(span.start, span.end)
    }

    /** The subtree on its own, which is what looking closer at it means. */
    fun extract(content: String, node: TreeNode): String? = textOf(content, node)

    /** The document without the node, and without the punctuation it needed. */
    fun remove(content: String, format: DocumentFormat, node: TreeNode): String? {
        val span = node.entrySpan ?: return null
        val widened = if (format == DocumentFormat.JSON) {
            widenOverComma(content, span)
        } else {
            widenOverBlankLine(content, span)
        }
        return content.removeRange(widened.start, widened.end)
    }

    /** The document with a second copy of the node just after it. */
    fun duplicate(content: String, format: DocumentFormat, node: TreeNode): String? {
        val text = textOf(content, node) ?: return null
        val copied = if (format == DocumentFormat.JSON && node.nameSpan != null) {
            renameCopy(content, node, text)
        } else {
            text
        }
        return insertAfter(content, format, node, copied)
    }

    fun insertAfter(
        content: String,
        format: DocumentFormat,
        node: TreeNode,
        text: String,
    ): String? {
        val span = node.entrySpan ?: return null
        val separator = if (format == DocumentFormat.JSON) "," else ""
        val lead = leadOf(content, span.start)
        return content.substring(0, span.end) + separator + lead + text +
            content.substring(span.end)
    }

    fun insertBefore(
        content: String,
        format: DocumentFormat,
        node: TreeNode,
        text: String,
    ): String? {
        val span = node.entrySpan ?: return null
        val separator = if (format == DocumentFormat.JSON) "," else ""
        val lead = leadOf(content, span.start)
        return content.substring(0, span.start) + text + separator + lead +
            content.substring(span.start)
    }

    /**
     * The document with the text put inside the container, as its last child.
     *
     * A container that already holds something gets the text after the last
     * of them; an empty one gets it between its two brackets.
     */
    fun insertInto(
        content: String,
        format: DocumentFormat,
        container: TreeNode,
        text: String,
    ): String? {
        val holding = container.children.lastOrNull { it.entrySpan != null }
        if (holding != null) return insertAfter(content, format, holding, text)

        val span = container.valueSpan ?: container.entrySpan ?: return null
        return when (format) {
            DocumentFormat.JSON -> {
                // Between the brackets of an empty object or array.
                val open = content.indexOf(if (container.kind == NodeKind.ARRAY) '[' else '{', span.start)
                if (open < 0) return null
                content.substring(0, open + 1) + text + content.substring(open + 1)
            }

            else -> {
                val open = content.indexOf('>', container.offset)
                if (open < 0 || content[open - 1] == '/') return null
                content.substring(0, open + 1) + text + content.substring(open + 1)
            }
        }
    }

    /** What a new entry of each shape is written as. */
    fun skeleton(format: DocumentFormat, kind: NodeKind, named: Boolean): String =
        when (format) {
            DocumentFormat.JSON -> {
                val value = when (kind) {
                    NodeKind.OBJECT -> "{}"
                    NodeKind.ARRAY -> "[]"
                    else -> "\"\""
                }
                if (named) "\"$NEW_KEY\": $value" else value
            }

            else -> when (kind) {
                NodeKind.ATTRIBUTE -> "$NEW_KEY=\"\""
                NodeKind.TEXT -> ""
                else -> "<$NEW_KEY></$NEW_KEY>"
            }
        }

    /**
     * A key that no sibling already carries.
     *
     * A duplicated member that kept its key would leave two of them under the
     * same name, which reads but loses one of the two on the way back.
     */
    private fun renameCopy(content: String, node: TreeNode, text: String): String {
        val nameSpan = node.nameSpan ?: return text
        val within = Span(nameSpan.start - node.entrySpan!!.start, nameSpan.end - node.entrySpan.start)
        val renamed = "\"" + DocumentFormatter.escape(node.name + COPY_SUFFIX) + "\""
        return text.substring(0, within.start) + renamed + text.substring(within.end)
    }

    /** A comma on one side of the entry, whichever side carries it. */
    private fun widenOverComma(content: String, span: Span): Span {
        var end = span.end
        while (end < content.length && content[end].isWhitespace()) end++
        if (end < content.length && content[end] == ',') {
            return Span(span.start, end + 1)
        }
        var start = span.start
        while (start > 0 && content[start - 1].isWhitespace()) start--
        if (start > 0 && content[start - 1] == ',') {
            return Span(start - 1, span.end)
        }
        return span
    }

    /** The whitespace that opened the line the entry sits alone on. */
    private fun widenOverBlankLine(content: String, span: Span): Span {
        var start = span.start
        while (start > 0 && content[start - 1] != '\n' && content[start - 1].isWhitespace()) start--
        if (start > 0 && content[start - 1] == '\n') start--
        return Span(start, span.end)
    }

    /**
     * The break and the indentation that put a new entry where the old one is.
     *
     * A container written on one line stays on one line: what is inserted
     * follows the shape of what is already there.
     */
    private fun leadOf(content: String, entryStart: Int): String {
        var start = entryStart
        while (start > 0 && content[start - 1] != '\n') start--
        if (start == 0) return ""
        val indent = content.substring(start, entryStart).takeWhile { it == ' ' || it == '\t' }
        return "\n" + indent
    }

    private const val NEW_KEY = "key"
    private const val COPY_SUFFIX = " copy"
}
