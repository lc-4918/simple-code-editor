package fr.lc4918.simplecodeeditor.format

/**
 * Rewrites the whitespace between the tags of an XML or HTML document.
 *
 * Only the whitespace that separates tags and text is touched. The inside of a
 * tag, of a comment and of the elements whose content is significant, such as
 * a preformatted block or a script, is copied over as it stands. A document
 * that cannot be scanned to the end is returned untouched.
 */
object MarkupFormatter {

    /** Elements that carry no content and therefore open no level. */
    private val VOID_ELEMENTS = setOf(
        "area", "base", "br", "col", "embed", "hr", "img", "input",
        "link", "meta", "param", "source", "track", "wbr",
    )

    /** Elements whose content is copied over exactly as it is. */
    private val VERBATIM_ELEMENTS = setOf("pre", "textarea", "script", "style")

    fun indent(content: String, width: Int): String =
        rewrite(content, width, compact = false)

    fun compact(content: String): String =
        rewrite(content, width = 0, compact = true)

    private fun rewrite(content: String, width: Int, compact: Boolean): String {
        val unit = " ".repeat(width)
        val out = StringBuilder(content.length)
        var depth = 0
        var index = 0
        var atStart = true
        // Set after verbatim content, so its closing tag stays glued to it.
        var glued = false

        fun separate(level: Int) {
            if (glued) {
                glued = false
                return
            }
            if (compact || atStart) return
            out.append('\n')
            repeat(level) { out.append(unit) }
        }

        while (index < content.length) {
            val character = content[index]

            if (character != '<') {
                val end = content.indexOf('<', index).takeIf { it >= 0 } ?: content.length
                val text = content.substring(index, end)
                if (text.isNotBlank()) {
                    separate(depth)
                    out.append(text.trim())
                    atStart = false
                }
                index = end
                continue
            }

            val tagEnd = endOfTag(content, index) ?: return content
            val tag = content.substring(index, tagEnd + 1)
            val kind = kindOf(tag)

            if (kind == TagKind.CLOSING) depth = (depth - 1).coerceAtLeast(0)
            separate(depth)
            out.append(tag)
            atStart = false
            index = tagEnd + 1

            when (kind) {
                TagKind.OPENING -> {
                    depth++
                    val name = nameOf(tag)
                    if (name in VERBATIM_ELEMENTS) {
                        val closing = "</$name"
                        val start = content.indexOf(closing, index, ignoreCase = true)
                        if (start < 0) return content
                        out.append(content, index, start)
                        index = start
                        glued = true
                    }
                }

                TagKind.CLOSING, TagKind.SELF_CONTAINED -> Unit
            }
        }
        return out.toString()
    }

    private enum class TagKind { OPENING, CLOSING, SELF_CONTAINED }

    private fun kindOf(tag: String): TagKind = when {
        tag.startsWith("</") -> TagKind.CLOSING
        tag.startsWith("<!") || tag.startsWith("<?") -> TagKind.SELF_CONTAINED
        tag.endsWith("/>") -> TagKind.SELF_CONTAINED
        nameOf(tag) in VOID_ELEMENTS -> TagKind.SELF_CONTAINED
        else -> TagKind.OPENING
    }

    private fun nameOf(tag: String): String =
        tag.trimStart('<', '/')
            .takeWhile { !it.isWhitespace() && it != '>' && it != '/' }
            .lowercase()

    /**
     * Index of the bracket closing the tag opened at [start], or null.
     *
     * Comments, declarations and character data end on their own marker, and a
     * bracket inside an attribute value does not close the tag.
     */
    private fun endOfTag(content: String, start: Int): Int? {
        val markers = listOf("<!--" to "-->", "<![CDATA[" to "]]>")
        markers.forEach { (opening, closing) ->
            if (content.startsWith(opening, start)) {
                val end = content.indexOf(closing, start + opening.length)
                return if (end < 0) null else end + closing.length - 1
            }
        }

        var index = start + 1
        var quote: Char? = null
        while (index < content.length) {
            val character = content[index]
            when {
                quote != null -> if (character == quote) quote = null
                character == '"' || character == '\'' -> quote = character
                character == '>' -> return index
            }
            index++
        }
        return null
    }
}
