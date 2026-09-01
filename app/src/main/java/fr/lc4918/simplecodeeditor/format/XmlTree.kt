package fr.lc4918.simplecodeeditor.format

import fr.lc4918.simplecodeeditor.model.NodeKind
import fr.lc4918.simplecodeeditor.model.TreeNode

/**
 * Reads an XML or HTML document into the tree the hierarchical view shows.
 *
 * Attributes hang under their element as leaves, prefixed so they cannot be
 * mistaken for a child element. Comments, declarations and character data are
 * skipped: they carry no place in the hierarchy the view draws. A document
 * whose tags do not close leaves the view with nothing to show.
 */
object XmlTree {

    /** Elements that hold nothing and therefore never look for a closing tag. */
    private val VOID_ELEMENTS = setOf(
        "area", "base", "br", "col", "embed", "hr", "img", "input",
        "link", "meta", "param", "source", "track", "wbr",
    )

    private const val ATTRIBUTE_PREFIX = "@"

    fun parse(content: String): TreeNode? = runCatching {
        val reader = Reader(content)
        val roots = reader.readNodes(closingFor = null)
        when {
            roots.isEmpty() -> null
            roots.size == 1 -> roots.single()
            // Several roots, which HTML fragments often have, hang under one.
            else -> TreeNode(name = "", kind = NodeKind.ELEMENT, children = roots)
        }
    }.getOrNull()

    private class Reader(private val text: String) {
        private var index = 0

        /**
         * Everything up to the closing tag of [closingFor], or to the end of
         * the document when there is none to look for.
         */
        fun readNodes(closingFor: String?): List<TreeNode> {
            val nodes = mutableListOf<TreeNode>()
            while (index < text.length) {
                if (text[index] != '<') {
                    readText()?.let(nodes::add)
                    continue
                }
                if (text.startsWith("</", index)) {
                    val name = closingTagName()
                    if (closingFor == null) fail("Unexpected closing tag")
                    if (!name.equals(closingFor, ignoreCase = true)) fail("Mismatched closing tag")
                    return nodes
                }
                if (isSkippable()) {
                    skipMarkup()
                    continue
                }
                nodes.add(readElement())
            }
            if (closingFor != null) fail("Unclosed element")
            return nodes
        }

        private fun readText(): TreeNode? {
            val end = text.indexOf('<', index).takeIf { it >= 0 } ?: text.length
            val raw = text.substring(index, end)
            index = end
            return raw.trim()
                .takeIf { it.isNotEmpty() }
                ?.let { TreeNode(name = "", kind = NodeKind.TEXT, value = it) }
        }

        private fun readElement(): TreeNode {
            val end = endOfTag() ?: fail("Unterminated tag")
            val tag = text.substring(index, end + 1)
            index = end + 1

            val name = tagName(tag)
            val attributes = attributesOf(tag)
            val selfContained = tag.endsWith("/>") || name.lowercase() in VOID_ELEMENTS
            val children = if (selfContained) emptyList() else readNodes(closingFor = name)

            return TreeNode(
                name = name,
                kind = NodeKind.ELEMENT,
                children = attributes + children,
            )
        }

        private fun closingTagName(): String {
            val end = endOfTag() ?: fail("Unterminated closing tag")
            val name = text.substring(index + 2, end).trim()
            index = end + 1
            return name
        }

        private fun isSkippable(): Boolean =
            text.startsWith("<!", index) || text.startsWith("<?", index)

        private fun skipMarkup() {
            listOf("<!--" to "-->", "<![CDATA[" to "]]>").forEach { (opening, closing) ->
                if (text.startsWith(opening, index)) {
                    val end = text.indexOf(closing, index + opening.length)
                    if (end < 0) fail("Unterminated markup")
                    index = end + closing.length
                    return
                }
            }
            val end = endOfTag() ?: fail("Unterminated declaration")
            index = end + 1
        }

        /** Index of the bracket closing the tag, ignoring the ones in a value. */
        private fun endOfTag(): Int? {
            var cursor = index + 1
            var quote: Char? = null
            while (cursor < text.length) {
                val character = text[cursor]
                when {
                    quote != null -> if (character == quote) quote = null
                    character == '"' || character == '\'' -> quote = character
                    character == '>' -> return cursor
                }
                cursor++
            }
            return null
        }

        private fun fail(reason: String): Nothing = throw IllegalArgumentException(reason)
    }

    private fun tagName(tag: String): String =
        tag.trimStart('<').takeWhile { !it.isWhitespace() && it != '>' && it != '/' }

    private fun attributesOf(tag: String): List<TreeNode> {
        val body = tag.trim('<', '>', '/').substringAfter(tagName(tag), missingDelimiterValue = "")
        return Regex("""([\w:.-]+)\s*=\s*("([^"]*)"|'([^']*)')""")
            .findAll(body)
            .map { match ->
                TreeNode(
                    name = ATTRIBUTE_PREFIX + match.groupValues[1],
                    kind = NodeKind.ATTRIBUTE,
                    value = match.groupValues[3].ifEmpty { match.groupValues[4] },
                )
            }
            .toList()
    }
}
