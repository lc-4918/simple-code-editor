package fr.lc4918.simplecodeeditor.format

/**
 * Rewrites the whitespace of a brace and semicolon language.
 *
 * The scanner walks over the strings, the comments, and for a script over the
 * template literals and the regular expressions, copying each of them exactly
 * as it stands. Anything it cannot scan to the end leaves the document
 * untouched rather than half rewritten.
 */
object CodeFormatter {

    /** What the scanner has to watch out for, which is not the same in both. */
    enum class Dialect {
        /** Style sheets: strings and block comments only. */
        STYLE,

        /** Scripts: also line comments, template literals and regular expressions. */
        SCRIPT,
    }

    fun indent(content: String, dialect: Dialect, width: Int): String {
        val unit = " ".repeat(width)
        val out = StringBuilder(content.length)
        var braceDepth = 0
        var parenDepth = 0
        var pendingSpace = false
        var atLineStart = true
        var lineStart = 0

        fun write(text: String) {
            if (pendingSpace && !atLineStart && out.isNotEmpty()) out.append(' ')
            pendingSpace = false
            out.append(text)
            atLineStart = false
        }

        fun newLine() {
            pendingSpace = false
            if (out.isEmpty()) return
            // Two breaks in a row, when a block ends right after a statement,
            // only reopen the same line at the new depth.
            if (atLineStart) {
                out.setLength(lineStart)
            } else {
                out.append('\n')
                lineStart = out.length
            }
            repeat(braceDepth) { out.append(unit) }
            atLineStart = true
        }

        var index = 0
        while (index < content.length) {
            val character = content[index]

            val span = spanAt(content, index, dialect, out) ?: return content
            if (span > index) {
                val isLineComment = dialect == Dialect.SCRIPT && content.startsWith("//", index)
                write(content.substring(index, span))
                index = span
                // A comment that runs to the end of the line has to keep the
                // line to itself, or it would swallow whatever comes next.
                if (isLineComment) newLine()
                continue
            }

            when {
                character.isWhitespace() -> {
                    pendingSpace = true
                    index++
                }

                character == '{' -> {
                    pendingSpace = true
                    write("{")
                    braceDepth++
                    newLine()
                    index++
                }

                character == '}' -> {
                    braceDepth = (braceDepth - 1).coerceAtLeast(0)
                    newLine()
                    write("}")
                    newLine()
                    index++
                }

                character == ';' && parenDepth == 0 -> {
                    write(";")
                    newLine()
                    index++
                }

                // Inside a block every colon separates a declaration, while
                // outside one it belongs to a selector and must not be spaced.
                character == ':' && dialect == Dialect.STYLE && braceDepth > 0 -> {
                    write(":")
                    pendingSpace = true
                    index++
                }

                else -> {
                    if (character == '(') parenDepth++
                    if (character == ')') parenDepth = (parenDepth - 1).coerceAtLeast(0)
                    write(character.toString())
                    index++
                }
            }
        }
        return out.toString().trimEnd()
    }

    /**
     * Removes the whitespace that carries no meaning.
     *
     * A script keeps its line breaks: dropping them would join two statements
     * that relied on the end of the line to close the first one.
     */
    fun compact(content: String, dialect: Dialect): String {
        val out = StringBuilder(content.length)
        val keepLineBreaks = dialect == Dialect.SCRIPT
        var index = 0

        while (index < content.length) {
            val character = content[index]

            val span = spanAt(content, index, dialect, out) ?: return content
            if (span > index) {
                out.append(content, index, span)
                index = span
                continue
            }

            if (!character.isWhitespace()) {
                out.append(character)
                index++
                continue
            }

            val end = run {
                var cursor = index
                while (cursor < content.length && content[cursor].isWhitespace()) cursor++
                cursor
            }
            val run = content.substring(index, end)
            val before = out.lastOrNull()
            val after = content.getOrNull(end)
            when {
                keepLineBreaks && run.contains('\n') -> out.append('\n')
                before != null && after != null && before.isWordPart() && after.isWordPart() ->
                    out.append(' ')
            }
            index = end
        }
        return out.toString()
    }

    /**
     * End of the span starting at [index] when it opens one, [index] when it
     * does not, and null when a span is left open.
     */
    private fun spanAt(
        content: String,
        index: Int,
        dialect: Dialect,
        written: CharSequence,
    ): Int? {
        val character = content[index]

        if (content.startsWith("/*", index)) {
            val end = content.indexOf("*/", index + 2)
            return if (end < 0) null else end + 2
        }
        if (dialect == Dialect.SCRIPT && content.startsWith("//", index)) {
            val end = content.indexOf('\n', index)
            return if (end < 0) content.length else end
        }
        if (character == '"' || character == '\'') {
            return endOfQuoted(content, index, character)
        }
        if (dialect == Dialect.SCRIPT && character == '`') {
            return endOfQuoted(content, index, '`')
        }
        if (dialect == Dialect.SCRIPT && character == '/' && startsRegex(written)) {
            return endOfRegex(content, index)
        }
        return index
    }

    /** Index just past the closing quote, or null when there is none. */
    private fun endOfQuoted(content: String, start: Int, quote: Char): Int? {
        var index = start + 1
        while (index < content.length) {
            when (content[index]) {
                '\\' -> index += 2
                quote -> return index + 1
                else -> index++
            }
        }
        return null
    }

    /**
     * A slash opens a regular expression unless what precedes it can end a
     * value, in which case it divides.
     */
    private fun startsRegex(written: CharSequence): Boolean {
        val previous = written.lastOrNull { !it.isWhitespace() } ?: return true
        return !(previous.isLetterOrDigit() || previous in "_$)]")
    }

    /** Index just past the closing slash, or null when the line ends first. */
    private fun endOfRegex(content: String, start: Int): Int? {
        var index = start + 1
        var inClass = false
        while (index < content.length) {
            when (content[index]) {
                '\\' -> index++
                '\n' -> return null
                '[' -> inClass = true
                ']' -> inClass = false
                '/' -> if (!inClass) {
                    // The flags that may follow belong to the literal.
                    var end = index + 1
                    while (end < content.length && content[end].isLetter()) end++
                    return end
                }
                else -> Unit
            }
            index++
        }
        return null
    }

    private fun Char.isWordPart(): Boolean =
        isLetterOrDigit() || this == '_' || this == '$' || this == '-' || this == '#' || this == '.'
}
