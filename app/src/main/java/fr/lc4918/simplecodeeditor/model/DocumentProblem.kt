package fr.lc4918.simplecodeeditor.model

import androidx.annotation.StringRes
import fr.lc4918.simplecodeeditor.R

/**
 * Why a document is not what it claims to be.
 *
 * Some entries are about the grammar, which stops the reading outright, and
 * others about the rules a derived format adds on top of a document that reads
 * perfectly well. The reason is an entry of this list rather than a sentence,
 * so that it can be said in the language of the interface and matched on in
 * the tests.
 */
enum class DocumentProblem(
    @param:StringRes override val labelRes: Int,
    /**
     * True when the reading itself stopped here.
     *
     * Repairing rewrites a document until it can be read, so it has something
     * to offer these and nothing to offer the others: a document that breaks
     * a rule of GeoJSON reads perfectly well, and handing it to a repairer
     * would give it back unchanged.
     */
    val stopsTheReading: Boolean = true,
) : LabelledOption {
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

    // Rules a derived format adds on top of a document that reads
    GEOJSON_UNKNOWN_TYPE(R.string.problem_geojson_unknown_type, stopsTheReading = false),
    GEOJSON_MISSING_MEMBER(R.string.problem_geojson_missing_member, stopsTheReading = false),
    GEOJSON_BAD_POSITION(R.string.problem_geojson_bad_position, stopsTheReading = false),
    GEOJSON_COORDINATES_SHAPE(R.string.problem_geojson_coordinates_shape, stopsTheReading = false),
    GEOJSON_SHORT_LINE(R.string.problem_geojson_short_line, stopsTheReading = false),
    GEOJSON_RING_NOT_CLOSED(R.string.problem_geojson_ring_not_closed, stopsTheReading = false),
    ROOT_ELEMENT_UNEXPECTED(R.string.problem_root_element_unexpected, stopsTheReading = false),
    COORDINATE_MISSING(R.string.problem_coordinate_missing, stopsTheReading = false),
    COORDINATE_NOT_A_NUMBER(R.string.problem_coordinate_not_a_number, stopsTheReading = false),
    LONGITUDE_OUT_OF_RANGE(R.string.problem_longitude_out_of_range, stopsTheReading = false),
    LATITUDE_OUT_OF_RANGE(R.string.problem_latitude_out_of_range, stopsTheReading = false),
}

/**
 * A problem, and where in the document it was met.
 *
 * The offset is what the editing surface needs to point at the spot; the line
 * and the column are what is said to the reader.
 */
data class Diagnostic(
    val problem: DocumentProblem,
    val offset: Int,
    val line: Int,
    val column: Int,
)
