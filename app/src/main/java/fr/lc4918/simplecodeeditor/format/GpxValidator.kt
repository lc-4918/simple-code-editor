package fr.lc4918.simplecodeeditor.format

import fr.lc4918.simplecodeeditor.model.Diagnostic
import fr.lc4918.simplecodeeditor.model.DocumentProblem
import fr.lc4918.simplecodeeditor.model.TreeNode

/**
 * Checks a document that reads as XML against the shape of a GPX track.
 *
 * This is not schema validation. Android carries no implementation of W3C XML
 * Schema, so the official XSD cannot be applied without bringing a parser of
 * our own along; what is checked here is the part of the format that carries
 * the data and that a wrong file gets wrong: the opening element, and the
 * latitude and longitude every point takes.
 *
 * Everything else the schema says, the order of the elements and the ones it
 * forbids, goes unchecked.
 */
object GpxValidator {

    private const val ROOT = "gpx"

    /** The three elements that carry a position. */
    private val POINTS = setOf("wpt", "trkpt", "rtept")

    fun validate(content: String, root: TreeNode): Diagnostic? {
        if (root.localName() != ROOT) {
            return TextPosition.diagnosticAt(
                content,
                DocumentProblem.ROOT_ELEMENT_UNEXPECTED,
                root.offset,
            )
        }
        return GeoAttributes.firstBadPoint(content, root, POINTS)
    }
}

/** What both formats built on XML need to say about a point. */
internal object GeoAttributes {

    private const val LATITUDE = "@lat"
    private const val LONGITUDE = "@lon"

    /** The first point whose latitude or longitude is missing or unusable. */
    fun firstBadPoint(content: String, node: TreeNode, points: Set<String>): Diagnostic? {
        if (node.localName() in points) {
            checkPoint(content, node)?.let { return it }
        }
        return node.children.firstNotNullOfOrNull { firstBadPoint(content, it, points) }
    }

    private fun checkPoint(content: String, point: TreeNode): Diagnostic? {
        val latitude = point.attribute(LATITUDE)
        val longitude = point.attribute(LONGITUDE)
        if (latitude == null || longitude == null) {
            return TextPosition.diagnosticAt(
                content,
                DocumentProblem.COORDINATE_MISSING,
                point.offset,
            )
        }
        return coordinateProblem(content, longitude, latitude, point.offset)
    }

    /** The bounds a position lies within, shared with the GeoJSON rules. */
    fun coordinateProblem(
        content: String,
        longitude: String,
        latitude: String,
        offset: Int,
    ): Diagnostic? {
        val east = longitude.trim().toDoubleOrNull()
        val north = latitude.trim().toDoubleOrNull()
        val problem = when {
            east == null || north == null -> DocumentProblem.COORDINATE_NOT_A_NUMBER
            east < -180.0 || east > 180.0 -> DocumentProblem.LONGITUDE_OUT_OF_RANGE
            north < -90.0 || north > 90.0 -> DocumentProblem.LATITUDE_OUT_OF_RANGE
            else -> return null
        }
        return TextPosition.diagnosticAt(content, problem, offset)
    }

    private fun TreeNode.attribute(name: String): String? =
        children.firstOrNull { it.name.equals(name, ignoreCase = true) }?.value
}

/** The tag without the namespace prefix a document may carry it under. */
internal fun TreeNode.localName(): String = name.substringAfterLast(':').lowercase()
