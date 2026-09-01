package fr.lc4918.simplecodeeditor.format

import fr.lc4918.simplecodeeditor.model.Diagnostic
import fr.lc4918.simplecodeeditor.model.SyntaxProblem

/** Turns a place in the text into the line and column a reader can look for. */
object TextPosition {

    fun diagnosticAt(content: String, problem: SyntaxProblem, offset: Int): Diagnostic {
        val safeOffset = offset.coerceIn(0, content.length)
        var line = 1
        var lineStart = 0
        for (index in 0 until safeOffset) {
            if (content[index] == '\n') {
                line++
                lineStart = index + 1
            }
        }
        return Diagnostic(
            problem = problem,
            offset = safeOffset,
            line = line,
            column = safeOffset - lineStart + 1,
        )
    }
}
