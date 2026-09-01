package fr.lc4918.simplecodeeditor.format

/**
 * Makes a broken XML document readable again, as far as guessing allows.
 *
 * There is no jsonrepair for XML to lean on, so this covers the faults that
 * actually turn up in files rather than every fault there is: an ampersand
 * that opens no entity, a bare less than sign in text, an attribute whose
 * value lost its quotes, a closing tag that closes the wrong element, one that
 * closes nothing at all, and elements left open at the end.
 *
 * What it will not do is add an XML declaration to a document that lacks one.
 * A document without one is well formed, so there is nothing there to repair,
 * and writing one in would change the file rather than fix it.
 *
 * The result is a best effort: it is handed back and read again, and the
 * reading is what says whether the guess worked.
 */
object XmlRepairer {

    fun repair(content: String): String {
        val out = StringBuilder(content.length + PADDING)
        val open = ArrayDeque<String>()
        var index = 0

        while (index < content.length) {
            val character = content[index]
            if (character == '&') {
                val entity = entityEnd(content, index)
                if (entity == null) {
                    out.append("&amp;")
                    index++
                } else {
                    out.append(content, index, entity)
                    index = entity
                }
                continue
            }
            if (character != '<') {
                out.append(character)
                index++
                continue
            }

            val verbatim = verbatimEnd(content, index)
            if (verbatim != null) {
                out.append(content, index, verbatim)
                index = verbatim
                continue
            }

            if (content.startsWith("</", index)) {
                index = closeTag(content, index, out, open)
                continue
            }

            if (!opensAName(content, index)) {
                // A less than sign with no name after it is text, whatever it
                // looks like, and text is where it belongs.
                out.append("&lt;")
                index++
                continue
            }

            index = openTag(content, index, out, open)
        }

        while (open.isNotEmpty()) out.append("</").append(open.removeLast()).append(">")
        return out.toString()
    }

    /** Just past a comment, a declaration or character data, which are copied whole. */
    private fun verbatimEnd(content: String, index: Int): Int? {
        listOf("<!--" to "-->", "<![CDATA[" to "]]>").forEach { (opening, closing) ->
            if (content.startsWith(opening, index)) {
                val end = content.indexOf(closing, index + opening.length)
                return if (end < 0) content.length else end + closing.length
            }
        }
        if (content.startsWith("<?", index) || content.startsWith("<!", index)) {
            val end = endOfTag(content, index)
            return if (end == null) content.length else end + 1
        }
        return null
    }

    private fun openTag(
        content: String,
        index: Int,
        out: StringBuilder,
        open: ArrayDeque<String>,
    ): Int {
        val end = endOfTag(content, index)
        val tag = if (end == null) {
            // Nothing closes it, so it is closed here.
            content.substring(index) + ">"
        } else {
            content.substring(index, end + 1)
        }
        val repaired = quoteAttributes(tag)
        out.append(repaired)
        if (!repaired.endsWith("/>")) open.addLast(nameOf(tag))
        return if (end == null) content.length else end + 1
    }

    /**
     * Writes the closing tag, and whatever has to close before it can.
     *
     * A tag that closes an element still open further up closes the ones
     * opened since; one that closes nothing at all is dropped.
     */
    private fun closeTag(
        content: String,
        index: Int,
        out: StringBuilder,
        open: ArrayDeque<String>,
    ): Int {
        val end = endOfTag(content, index)
        val past = if (end == null) content.length else end + 1
        val name = content.substring(index + 2, (end ?: content.length)).trim().substringBefore(' ')

        if (!open.contains(name)) return past

        while (open.isNotEmpty() && open.last() != name) {
            out.append("</").append(open.removeLast()).append(">")
        }
        open.removeLast()
        out.append("</").append(name).append(">")
        return past
    }

    /**
     * Just past the entity opened here, or null when nothing is opened.
     *
     * An ampersand that already opens one is left alone: escaping it again
     * would turn a document that says one thing into one that says another.
     */
    private fun entityEnd(content: String, index: Int): Int? {
        val match = ENTITY.matchAt(content, index) ?: return null
        return index + match.value.length
    }

    private fun opensAName(content: String, index: Int): Boolean {
        val next = content.getOrNull(index + 1) ?: return false
        return next.isLetter() || next == '_' || next == ':'
    }

    private fun nameOf(tag: String): String =
        tag.trimStart('<').takeWhile { !it.isWhitespace() && it != '>' && it != '/' }

    /**
     * A value that lost its quotes gets them back.
     *
     * Walked through rather than matched all at once: a pattern loose enough
     * to catch a bare value also catches what looks like one inside a value
     * that has its quotes, and would quote a piece of a web address.
     */
    private fun quoteAttributes(tag: String): String {
        val out = StringBuilder(tag.length + PADDING)
        var index = 0
        // The opening bracket and the name of the element.
        while (index < tag.length && tag[index] != ' ' && tag[index] != '\t' && tag[index] != '\n') {
            out.append(tag[index])
            index++
        }

        while (index < tag.length) {
            val character = tag[index]
            if (character.isWhitespace() || character == '/' || character == '>') {
                out.append(character)
                index++
                continue
            }

            val name = tag.substring(index).takeWhile { it.isLetterOrDigit() || it in "_:.-" }
            if (name.isEmpty()) {
                out.append(character)
                index++
                continue
            }
            out.append(name)
            index += name.length

            var after = index
            while (after < tag.length && tag[after].isWhitespace()) after++
            if (after >= tag.length || tag[after] != '=') continue

            out.append(tag, index, after + 1)
            index = after + 1
            while (index < tag.length && tag[index].isWhitespace()) index++
            if (index >= tag.length) break

            val quote = tag[index]
            if (quote == '"' || quote == '\'') {
                val end = tag.indexOf(quote, index + 1)
                val past = if (end < 0) tag.length else end + 1
                out.append(tag, index, past)
                index = past
            } else {
                val value = tag.substring(index).takeWhile { !it.isWhitespace() && it != '>' && it != '/' }
                out.append('"').append(value).append('"')
                index += value.length
            }
        }
        return out.toString()
    }

    private fun endOfTag(content: String, start: Int): Int? {
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

    private const val PADDING = 32

    private val ENTITY = Regex("""&([a-zA-Z][a-zA-Z0-9]*|#\d+|#[xX][0-9a-fA-F]+);""")
}
