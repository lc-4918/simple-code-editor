package fr.lc4918.simplecodeeditor.format

import fr.lc4918.simplecodeeditor.model.NodeKind
import fr.lc4918.simplecodeeditor.model.Span
import fr.lc4918.simplecodeeditor.model.DocumentProblem
import fr.lc4918.simplecodeeditor.model.TreeNode

/**
 * Reads a JSON document into the tree the hierarchical view shows.
 *
 * The reader is strict and stops at the first problem, saying which one and
 * where: a document it cannot read to the end is refused whole rather than
 * shown by halves. Numbers are kept as they were written rather than turned
 * into a number and back, which would change how they read.
 */
object JsonTree {

    /** The tree, or the first problem met and where. */
    fun read(content: String): TreeReading {
        val reader = Reader(content)
        return try {
            reader.skipWhitespace()
            val root = reader.readValue(name = "")
            reader.skipWhitespace()
            if (!reader.atEnd) reader.refuse(DocumentProblem.TRAILING_CONTENT)
            TreeReading.Tree(root)
        } catch (problem: SyntaxException) {
            TreeReading.Refused(
                TextPosition.diagnosticAt(content, problem.problem, problem.offset),
            )
        }
    }

    fun parse(content: String): TreeNode? = read(content).root

    private class Reader(private val text: String) {
        private var index = 0

        val atEnd: Boolean get() = index >= text.length

        fun skipWhitespace() {
            while (index < text.length && text[index].isWhitespace()) index++
        }

        fun readValue(name: String, nameSpan: Span? = null): TreeNode {
            skipWhitespace()
            val at = index
            val node = when (val character = peek()) {
                '{' -> readObject(name, at)
                '[' -> readArray(name, at)
                '"' -> TreeNode(name, NodeKind.STRING, readString(), offset = at)
                't', 'f' ->
                    TreeNode(name, NodeKind.BOOLEAN, readLiteral("true", "false"), offset = at)

                'n' -> TreeNode(name, NodeKind.NULL, readLiteral("null"), offset = at)
                else ->
                    if (character == '-' || character.isDigit()) {
                        TreeNode(name, NodeKind.NUMBER, readNumber(), offset = at)
                    } else {
                        refuse(DocumentProblem.UNEXPECTED_CHARACTER)
                    }
            }
            // The whole of what was read, quotes of a string included, which
            // is what has to make way for another value.
            return node.copy(nameSpan = nameSpan, valueSpan = Span(at, index))
        }

        private fun readObject(name: String, at: Int): TreeNode {
            expect('{')
            val children = mutableListOf<TreeNode>()
            skipWhitespace()
            if (peek() == '}') {
                index++
                return TreeNode(name, NodeKind.OBJECT, children = children, offset = at)
            }
            while (true) {
                skipWhitespace()
                val keyAt = index
                val key = readString()
                val keySpan = Span(keyAt, index)
                skipWhitespace()
                expect(':')
                children.add(readValue(key, keySpan))
                skipWhitespace()
                when (peek()) {
                    ',' -> index++
                    '}' -> {
                        index++
                        return TreeNode(name, NodeKind.OBJECT, children = children, offset = at)
                    }

                    else -> refuse(DocumentProblem.EXPECTED_SEPARATOR)
                }
            }
        }

        private fun readArray(name: String, at: Int): TreeNode {
            expect('[')
            val children = mutableListOf<TreeNode>()
            skipWhitespace()
            if (peek() == ']') {
                index++
                return TreeNode(name, NodeKind.ARRAY, children = children, offset = at)
            }
            while (true) {
                children.add(readValue(children.size.toString()))
                skipWhitespace()
                when (peek()) {
                    ',' -> index++
                    ']' -> {
                        index++
                        return TreeNode(name, NodeKind.ARRAY, children = children, offset = at)
                    }

                    else -> refuse(DocumentProblem.EXPECTED_SEPARATOR)
                }
            }
        }

        /** The text of the string, with its escapes turned back into characters. */
        private fun readString(): String {
            val start = index
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
            refuse(DocumentProblem.UNTERMINATED_STRING, at = start)
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
                    if (index + 4 > text.length) refuse(DocumentProblem.BAD_ESCAPE)
                    val code = text.substring(index, index + 4).toIntOrNull(16)
                        ?: refuse(DocumentProblem.BAD_ESCAPE)
                    index += 4
                    code.toChar()
                }

                else -> refuse(DocumentProblem.BAD_ESCAPE)
            }
        }

        private fun readNumber(): String {
            val start = index
            if (peek() == '-') index++
            while (index < text.length && (text[index].isDigit() || text[index] in ".eE+-")) index++
            val number = text.substring(start, index)
            if (number.toDoubleOrNull() == null) refuse(DocumentProblem.BAD_NUMBER, at = start)
            return number
        }

        private fun readLiteral(vararg options: String): String {
            options.forEach { option ->
                if (text.startsWith(option, index)) {
                    index += option.length
                    return option
                }
            }
            refuse(DocumentProblem.UNKNOWN_LITERAL)
        }

        private fun peek(): Char =
            if (index < text.length) text[index] else refuse(DocumentProblem.END_OF_DOCUMENT)

        private fun expect(character: Char) {
            val expected = when (character) {
                ':' -> DocumentProblem.EXPECTED_COLON
                '"' -> DocumentProblem.EXPECTED_KEY
                else -> DocumentProblem.EXPECTED_SEPARATOR
            }
            if (peek() != character) refuse(expected)
            index++
        }

        fun refuse(problem: DocumentProblem, at: Int = index): Nothing =
            throw SyntaxException(problem, at)
    }
}
