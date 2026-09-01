package fr.lc4918.simplecodeeditor.format

import fr.lc4918.simplecodeeditor.model.NodeKind
import fr.lc4918.simplecodeeditor.model.SyntaxProblem
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
            if (!reader.atEnd) reader.refuse(SyntaxProblem.TRAILING_CONTENT)
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
                        refuse(SyntaxProblem.UNEXPECTED_CHARACTER)
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

                    else -> refuse(SyntaxProblem.EXPECTED_SEPARATOR)
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

                    else -> refuse(SyntaxProblem.EXPECTED_SEPARATOR)
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
            refuse(SyntaxProblem.UNTERMINATED_STRING, at = start)
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
                    if (index + 4 > text.length) refuse(SyntaxProblem.BAD_ESCAPE)
                    val code = text.substring(index, index + 4).toIntOrNull(16)
                        ?: refuse(SyntaxProblem.BAD_ESCAPE)
                    index += 4
                    code.toChar()
                }

                else -> refuse(SyntaxProblem.BAD_ESCAPE)
            }
        }

        private fun readNumber(): String {
            val start = index
            if (peek() == '-') index++
            while (index < text.length && (text[index].isDigit() || text[index] in ".eE+-")) index++
            val number = text.substring(start, index)
            if (number.toDoubleOrNull() == null) refuse(SyntaxProblem.BAD_NUMBER, at = start)
            return number
        }

        private fun readLiteral(vararg options: String): String {
            options.forEach { option ->
                if (text.startsWith(option, index)) {
                    index += option.length
                    return option
                }
            }
            refuse(SyntaxProblem.UNKNOWN_LITERAL)
        }

        private fun peek(): Char =
            if (index < text.length) text[index] else refuse(SyntaxProblem.END_OF_DOCUMENT)

        private fun expect(character: Char) {
            val expected = when (character) {
                ':' -> SyntaxProblem.EXPECTED_COLON
                '"' -> SyntaxProblem.EXPECTED_KEY
                else -> SyntaxProblem.EXPECTED_SEPARATOR
            }
            if (peek() != character) refuse(expected)
            index++
        }

        fun refuse(problem: SyntaxProblem, at: Int = index): Nothing =
            throw SyntaxException(problem, at)
    }
}
