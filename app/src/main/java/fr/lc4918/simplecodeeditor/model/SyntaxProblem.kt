package fr.lc4918.simplecodeeditor.model

import androidx.annotation.StringRes
import fr.lc4918.simplecodeeditor.R

/**
 * Why a document could not be read.
 *
 * The reason is an entry of this list rather than a sentence, so that it can be
 * said in the language of the interface and matched on in the tests.
 */
enum class SyntaxProblem(@param:StringRes override val labelRes: Int) : LabelledOption {
    // Shared
    END_OF_DOCUMENT(R.string.problem_end_of_document),
    TRAILING_CONTENT(R.string.problem_trailing_content),

    // JSON
    UNEXPECTED_CHARACTER(R.string.problem_unexpected_character),
    UNTERMINATED_STRING(R.string.problem_unterminated_string),
    EXPECTED_SEPARATOR(R.string.problem_expected_separator),
    EXPECTED_COLON(R.string.problem_expected_colon),
    EXPECTED_KEY(R.string.problem_expected_key),
    BAD_NUMBER(R.string.problem_bad_number),
    BAD_ESCAPE(R.string.problem_bad_escape),
    UNKNOWN_LITERAL(R.string.problem_unknown_literal),

    // XML
    UNTERMINATED_TAG(R.string.problem_unterminated_tag),
    UNCLOSED_ELEMENT(R.string.problem_unclosed_element),
    MISMATCHED_CLOSING_TAG(R.string.problem_mismatched_closing_tag),
    UNEXPECTED_CLOSING_TAG(R.string.problem_unexpected_closing_tag),
    UNTERMINATED_MARKUP(R.string.problem_unterminated_markup),
}

/**
 * A problem, and where in the document it was met.
 *
 * The offset is what the editing surface needs to point at the spot; the line
 * and the column are what is said to the reader.
 */
data class Diagnostic(
    val problem: SyntaxProblem,
    val offset: Int,
    val line: Int,
    val column: Int,
)
