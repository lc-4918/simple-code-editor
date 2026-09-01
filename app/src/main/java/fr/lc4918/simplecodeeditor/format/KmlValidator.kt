package fr.lc4918.simplecodeeditor.format

import fr.lc4918.simplecodeeditor.model.Diagnostic
import fr.lc4918.simplecodeeditor.model.DocumentProblem
import fr.lc4918.simplecodeeditor.model.NodeKind
import fr.lc4918.simplecodeeditor.model.TreeNode

/**
 * Checks a document that reads as XML against the shape of a KML file.
 *
 * As with GPX this is not schema validation, for the same reason: what is
 * checked is the opening element and the coordinates, which KML writes as
 * text rather than as attributes, one position per whitespace separated
 * group of two or three numbers separated by commas.
 */
object KmlValidator {

    private const val ROOT = "kml"
    private const val COORDINATES = "coordinates"

    fun validate(content: String, root: TreeNode): Diagnostic? {
        if (root.localName() != ROOT) {
            return TextPosition.diagnosticAt(
                content,
                DocumentProblem.ROOT_ELEMENT_UNEXPECTED,
                root.offset,
            )
        }
        return firstBadCoordinates(content, root)
    }

    private fun firstBadCoordinates(content: String, node: TreeNode): Diagnostic? {
        if (node.localName() == COORDINATES) {
            checkCoordinates(content, node)?.let { return it }
        }
        return node.children.firstNotNullOfOrNull { firstBadCoordinates(content, it) }
    }

    private fun checkCoordinates(content: String, node: TreeNode): Diagnostic? {
        val text = node.children.firstOrNull { it.kind == NodeKind.TEXT }?.value
        if (text.isNullOrBlank()) {
            return TextPosition.diagnosticAt(
                content,
                DocumentProblem.COORDINATE_MISSING,
                node.offset,
            )
        }
        text.split(Regex("""\s+""")).filter { it.isNotBlank() }.forEach { group ->
            val numbers = group.split(',')
            if (numbers.size !in 2..3) {
                return TextPosition.diagnosticAt(
                    content,
                    DocumentProblem.COORDINATE_MISSING,
                    node.offset,
                )
            }
            // Longitude first, which is the order KML writes and the one a
            // file converted from somewhere else often gets backwards.
            GeoAttributes.coordinateProblem(content, numbers[0], numbers[1], node.offset)
                ?.let { return it }
        }
        return null
    }
}
