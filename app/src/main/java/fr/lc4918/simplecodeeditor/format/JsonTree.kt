package fr.lc4918.simplecodeeditor.format

import fr.lc4918.simplecodeeditor.model.NodeKind
import fr.lc4918.simplecodeeditor.model.TreeNode

/**
 * Reads a JSON document into the tree the hierarchical view shows.
 *
 * The reader is strict: anything it cannot read leaves it with nothing to
 * show, and the view says so rather than showing half a document. Numbers are
 * kept as they were written rather than turned into a number and back, which
 * would change how they read.
 */
object JsonTree {

    fun parse(content: String): TreeNode? {
        val reader = Reader(content)
        return runCatching {
            reader.skipWhitespace()
            val root = reader.readValue(name = "")
            reader.skipWhitespace()
            if (!reader.atEnd) throw IllegalArgumentException("Trailing content")
            root
        }.getOrNull()
    }

    private class Reader(private val text: String) {
        private var index = 0

        val atEnd: Boolean get() = index >= text.length

        fun skipWhitespace() {
            while (index < text.length && text[index].isWhitespace()) index++
        }

        fun readValue(name: String): TreeNode {
            skipWhitespace()
            return when (val character = peek()) {
                '{' -> readObject(name)
                '[' -> readArray(name)
                '"' -> TreeNode(name, NodeKind.STRING, value = readString())
                't', 'f' -> TreeNode(name, NodeKind.BOOLEAN, value = readLiteral("true", "false"))
                'n' -> TreeNode(name, NodeKind.NULL, value = readLiteral("null"))
                else ->
                    if (character == '-' || character.isDigit()) {
                        TreeNode(name, NodeKind.NUMBER, value = readNumber())
                    } else {
                        fail("Unexpected character")
                    }
            }
        }

        private fun readObject(name: String): TreeNode {
            expect('{')
            val children = mutableListOf<TreeNode>()
            skipWhitespace()
            if (peek() == '}') {
                index++
                return TreeNode(name, NodeKind.OBJECT, children = children)
            }
            while (true) {
                skipWhitespace()
                val key = readString()
                skipWhitespace()
                expect(':')
                children.add(readValue(key))
                skipWhitespace()
                when (peek()) {
                    ',' -> index++
                    '}' -> {
                        index++
                        return TreeNode(name, NodeKind.OBJECT, children = children)
                    }

                    else -> fail("Expected a comma or a closing brace")
                }
            }
        }

        private fun readArray(name: String): TreeNode {
            expect('[')
            val children = mutableListOf<TreeNode>()
            skipWhitespace()
            if (peek() == ']') {
                index++
                return TreeNode(name, NodeKind.ARRAY, children = children)
            }
            while (true) {
                children.add(readValue(children.size.toString()))
                skipWhitespace()
                when (peek()) {
                    ',' -> index++
                    ']' -> {
                        index++
                        return TreeNode(name, NodeKind.ARRAY, children = children)
                    }

                    else -> fail("Expected a comma or a closing bracket")
                }
            }
        }

        /** The text of the string, with its escapes turned back into characters. */
        private fun readString(): String {
            expect('"')
            val out = StringBuilder()
            while (index < text.length) {
                when (val character = text[index]) {
                    '"' -> {
                        index++
                        return out.toString()
                    }

                    '\\' -> {
                        index++
                        out.append(readEscape())
                    }

                    else -> {
                        out.append(character)
                        index++
                    }
                }
            }
            fail("Unterminated string")
        }

        private fun readEscape(): Char {
            val character = peek()
            index++
            return when (character) {
                '"' -> '"'
                '\\' -> '\\'
                '/' -> '/'
                'b' -> '\b'
                'f' -> '\u000C'
                'n' -> '\n'
                'r' -> '\r'
                't' -> '\t'
                'u' -> {
                    if (index + 4 > text.length) fail("Truncated escape")
                    val code = text.substring(index, index + 4).toIntOrNull(16)
                        ?: fail("Bad escape")
                    index += 4
                    code.toChar()
                }

                else -> fail("Unknown escape")
            }
        }

        private fun readNumber(): String {
            val start = index
            if (peek() == '-') index++
            while (index < text.length && (text[index].isDigit() || text[index] in ".eE+-")) index++
            val number = text.substring(start, index)
            if (number.toDoubleOrNull() == null) fail("Bad number")
            return number
        }

        private fun readLiteral(vararg options: String): String {
            options.forEach { option ->
                if (text.startsWith(option, index)) {
                    index += option.length
                    return option
                }
            }
            fail("Unknown literal")
        }

        private fun peek(): Char = if (index < text.length) text[index] else fail("End of document")

        private fun expect(character: Char) {
            if (peek() != character) fail("Expected $character")
            index++
        }

        private fun fail(reason: String): Nothing = throw IllegalArgumentException(reason)
    }
}
