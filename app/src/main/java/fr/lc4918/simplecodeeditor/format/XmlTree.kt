package fr.lc4918.simplecodeeditor.format

import fr.lc4918.simplecodeeditor.model.NodeKind
import fr.lc4918.simplecodeeditor.model.Span
import fr.lc4918.simplecodeeditor.model.DocumentProblem
import fr.lc4918.simplecodeeditor.model.TreeNode

/**
 * Reads an XML or HTML document into the tree the hierarchical view shows.
 *
 * Attributes hang under their element as leaves, prefixed so they cannot be
 * mistaken for a child element. Comments, declarations and character data are
 * skipped: they carry no place in the hierarchy the view draws.
 *
 * The reader is strict and stops at the first problem, saying which one and
 * where, rather than guessing what a document whose tags do not close meant.
 */
object XmlTree {

    /**
     * Elements HTML closes on their own behalf.
     *
     * They belong to HTML and to nothing else: XML has no such list, and a
     * document that uses one of these names for an ordinary element, which
     * GPX does with link, would have it closed under its feet.
     */
    private val VOID_ELEMENTS = setOf(
        "area", "base", "br", "col", "embed", "hr", "img", "input",
        "link", "meta", "param", "source", "track", "wbr",
    )

    private const val ATTRIBUTE_PREFIX = "@"

    /**
     * The tree, or the first problem met and where.
     *
     * @param html whether the elements HTML closes on their own behalf apply
     */
    fun read(content: String, html: Boolean = false): TreeReading {
        val reader = Reader(content, html)
        return try {
            val roots = reader.readNodes(closingFor = null)
            when {
                roots.isEmpty() -> throw SyntaxException(DocumentProblem.END_OF_DOCUMENT, 0)
                roots.size == 1 -> TreeReading.Tree(roots.single())
                // Several roots, which HTML fragments often have, hang under one.
                else -> TreeReading.Tree(
                    TreeNode(name = "", kind = NodeKind.ELEMENT, children = roots, offset = 0),
                )
            }
        } catch (problem: SyntaxException) {
            TreeReading.Refused(
                TextPosition.diagnosticAt(content, problem.problem, problem.offset),
            )
        }
    }

    fun parse(content: String, html: Boolean = false): TreeNode? = read(content, html).root

    private class Reader(private val text: String, private val html: Boolean) {
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
                    val at = index
                    val name = closingTagName()
                    if (closingFor == null) refuse(DocumentProblem.UNEXPECTED_CLOSING_TAG, at)
                    if (!name.equals(closingFor, ignoreCase = true)) {
                        refuse(DocumentProblem.MISMATCHED_CLOSING_TAG, at)
                    }
                    return nodes
                }
                if (isSkippable()) {
                    skipMarkup()
                    continue
                }
                nodes.add(readElement())
            }
            if (closingFor != null) refuse(DocumentProblem.UNCLOSED_ELEMENT, text.length)
            return nodes
        }

        private fun readText(): TreeNode? {
            val at = index
            val end = text.indexOf('<', index).takeIf { it >= 0 } ?: text.length
            val raw = text.substring(index, end)
            index = end
            val trimmed = raw.trim()
            if (trimmed.isEmpty()) return null
            // The span skips the whitespace the trimming dropped, so that
            // replacing the text leaves the layout around it alone.
            val start = at + raw.indexOf(trimmed)
            return TreeNode(
                name = "",
                kind = NodeKind.TEXT,
                value = trimmed,
                offset = at,
                valueSpan = Span(start, start + trimmed.length),
            )
        }

        private fun readElement(): TreeNode {
            val at = index
            val end = endOfTag() ?: refuse(DocumentProblem.UNTERMINATED_TAG, index)
            val tag = text.substring(index, end + 1)
            index = end + 1

            val name = tagName(tag)
            // Just past the opening bracket, which is where the name is written.
            val nameSpan = Span(at + 1, at + 1 + name.length)
            val attributes = attributesOf(tag, at)
            val selfContained =
                tag.endsWith("/>") || (html && name.lowercase() in VOID_ELEMENTS)
            val children = if (selfContained) emptyList() else readNodes(closingFor = name)

            return TreeNode(
                name = name,
                kind = NodeKind.ELEMENT,
                children = attributes + children,
                offset = at,
                nameSpan = nameSpan,
            )
        }

        private fun closingTagName(): String {
            val end = endOfTag() ?: refuse(DocumentProblem.UNTERMINATED_TAG, index)
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
                    if (end < 0) refuse(DocumentProblem.UNTERMINATED_MARKUP, index)
                    index = end + closing.length
                    return
                }
            }
            val end = endOfTag() ?: refuse(DocumentProblem.UNTERMINATED_MARKUP, index)
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

        private fun refuse(problem: DocumentProblem, at: Int): Nothing =
            throw SyntaxException(problem, at)
    }

    private fun tagName(tag: String): String =
        tag.trimStart('<').takeWhile { !it.isWhitespace() && it != '>' && it != '/' }

    private fun attributesOf(tag: String, at: Int): List<TreeNode> {
        // Searched in the tag itself rather than in a trimmed copy, so that
        // the places the matches report are places in the document.
        val nameLength = tagName(tag).length
        return Regex("""([\w:.-]+)\s*=\s*("([^"]*)"|'([^']*)')""")
            .findAll(tag, startIndex = 1 + nameLength)
            .map { match ->
                val nameGroup = match.groups[1]!!
                val quoted = match.groups[2]!!
                TreeNode(
                    name = ATTRIBUTE_PREFIX + match.groupValues[1],
                    kind = NodeKind.ATTRIBUTE,
                    value = match.groupValues[3].ifEmpty { match.groupValues[4] },
                    offset = at + match.range.first,
                    nameSpan = Span(
                        at + nameGroup.range.first,
                        at + nameGroup.range.last + 1,
                    ),
                    // Inside the quotes, which stay where they are.
                    valueSpan = Span(
                        at + quoted.range.first + 1,
                        at + quoted.range.last,
                    ),
                )
            }
            .toList()
    }
}
