package fr.lc4918.simplecodeeditor.format

/**
 * Rewrites the whitespace of a JSON document without parsing it.
 *
 * Working on the text rather than on a parsed tree keeps the order of the keys
 * and the exact spelling of the numbers, which a round trip through a parser
 * would both lose. Nothing outside whitespace is ever touched, and a document
 * that cannot be scanned to the end, an unterminated string for instance, is
 * returned untouched rather than half rewritten.
 */
object JsonFormatter {

    fun indent(content: String, width: Int): String {
        val unit = " ".repeat(width)
        val out = StringBuilder(content.length + content.length / 4)
        var depth = 0
        var index = 0

        fun newLine() {
            out.append('\n')
            repeat(depth) { out.append(unit) }
        }

        while (index < content.length) {
            val character = content[index]
            when {
                character.isWhitespace() -> index++

                character == '"' -> {
                    val end = endOfString(content, index) ?: return content
                    out.append(content, index, end + 1)
                    index = end + 1
                }

                character == '{' || character == '[' -> {
                    val closing = if (character == '{') '}' else ']'
                    val next = nextSignificant(content, index + 1)
                    out.append(character)
                    // An empty object or array stays on its line, as a pair.
                    if (next != null && content[next] == closing) {
                        out.append(closing)
                        index = next + 1
                    } else {
                        depth++
                        newLine()
                        index++
                    }
                }

                character == '}' || character == ']' -> {
                    depth = (depth - 1).coerceAtLeast(0)
                    newLine()
                    out.append(character)
                    index++
                }

                character == ',' -> {
                    out.append(character)
                    newLine()
                    index++
                }

                character == ':' -> {
                    out.append(": ")
                    index++
                }

                else -> {
                    out.append(character)
                    index++
                }
            }
        }
        return out.toString()
    }

    fun compact(content: String): String {
        val out = StringBuilder(content.length)
        var index = 0
        while (index < content.length) {
            val character = content[index]
            when {
                character.isWhitespace() -> index++

                character == '"' -> {
                    val end = endOfString(content, index) ?: return content
                    out.append(content, index, end + 1)
                    index = end + 1
                }

                else -> {
                    out.append(character)
                    index++
                }
            }
        }
        return out.toString()
    }

    /** Index of the quote closing the string opened at [start], or null. */
    private fun endOfString(content: String, start: Int): Int? {
        var index = start + 1
        while (index < content.length) {
            when (content[index]) {
                '\\' -> index += 2
                '"' -> return index
                else -> index++
            }
        }
        return null
    }

    private fun nextSignificant(content: String, from: Int): Int? {
        var index = from
        while (index < content.length && content[index].isWhitespace()) index++
        return index.takeIf { it < content.length }
    }
}
