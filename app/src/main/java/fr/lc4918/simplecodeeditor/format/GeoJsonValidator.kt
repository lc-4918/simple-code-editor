package fr.lc4918.simplecodeeditor.format

import fr.lc4918.simplecodeeditor.model.Diagnostic
import fr.lc4918.simplecodeeditor.model.DocumentProblem
import fr.lc4918.simplecodeeditor.model.NodeKind
import fr.lc4918.simplecodeeditor.model.TreeNode

/**
 * Checks a document that reads as JSON against the rules of GeoJSON.
 *
 * Follows RFC 7946 on the points a reader can check without knowing what the
 * data means: the nine types, the members each one requires, the shape of the
 * coordinates each geometry takes, and the bounds a position lies within.
 *
 * Left out on purpose, since a document that breaks them is still usable and
 * a reader cannot tell intent from mistake: the winding order of the rings,
 * the crossing of the antimeridian, and the bounding box.
 *
 * Only the first broken rule is reported, as with the readers.
 */
object GeoJsonValidator {

    /** How many arrays wrap the positions of each geometry. */
    private val GEOMETRIES = mapOf(
        "Point" to 0,
        "MultiPoint" to 1,
        "LineString" to 1,
        "MultiLineString" to 2,
        "Polygon" to 2,
        "MultiPolygon" to 3,
    )

    private const val MIN_RING_POSITIONS = 4
    private const val MIN_LINE_POSITIONS = 2

    fun validate(content: String, root: TreeNode): Diagnostic? {
        val problem = checkRoot(root) ?: return null
        return TextPosition.diagnosticAt(content, problem.problem, problem.offset)
    }

    /** A rule broken, and the node it was broken at. */
    private data class Broken(val problem: DocumentProblem, val offset: Int)

    private fun checkRoot(node: TreeNode): Broken? {
        if (node.kind != NodeKind.OBJECT) {
            return Broken(DocumentProblem.GEOJSON_UNKNOWN_TYPE, node.offset)
        }
        return when (node.typeName()) {
            "FeatureCollection" -> {
                val features = node.member("features")
                    ?: return Broken(DocumentProblem.GEOJSON_MISSING_MEMBER, node.offset)
                if (features.kind != NodeKind.ARRAY) {
                    return Broken(DocumentProblem.GEOJSON_MISSING_MEMBER, features.offset)
                }
                features.children.firstNotNullOfOrNull(::checkFeature)
            }

            "Feature" -> checkFeature(node)
            "GeometryCollection", in GEOMETRIES.keys -> checkGeometry(node)
            else -> Broken(
                DocumentProblem.GEOJSON_UNKNOWN_TYPE,
                node.member("type")?.offset ?: node.offset,
            )
        }
    }

    private fun checkFeature(node: TreeNode): Broken? {
        if (node.kind != NodeKind.OBJECT || node.typeName() != "Feature") {
            return Broken(DocumentProblem.GEOJSON_UNKNOWN_TYPE, node.offset)
        }
        // Both are required, and both may be null, which is what a feature
        // with nothing to say about itself looks like.
        val geometry = node.member("geometry")
            ?: return Broken(DocumentProblem.GEOJSON_MISSING_MEMBER, node.offset)
        node.member("properties")
            ?: return Broken(DocumentProblem.GEOJSON_MISSING_MEMBER, node.offset)

        return if (geometry.kind == NodeKind.NULL) null else checkGeometry(geometry)
    }

    private fun checkGeometry(node: TreeNode): Broken? {
        if (node.kind != NodeKind.OBJECT) {
            return Broken(DocumentProblem.GEOJSON_UNKNOWN_TYPE, node.offset)
        }
        val type = node.typeName()
        if (type == "GeometryCollection") {
            val geometries = node.member("geometries")
                ?: return Broken(DocumentProblem.GEOJSON_MISSING_MEMBER, node.offset)
            if (geometries.kind != NodeKind.ARRAY) {
                return Broken(DocumentProblem.GEOJSON_MISSING_MEMBER, geometries.offset)
            }
            return geometries.children.firstNotNullOfOrNull(::checkGeometry)
        }

        val depth = GEOMETRIES[type]
            ?: return Broken(
                DocumentProblem.GEOJSON_UNKNOWN_TYPE,
                node.member("type")?.offset ?: node.offset,
            )
        val coordinates = node.member("coordinates")
            ?: return Broken(DocumentProblem.GEOJSON_MISSING_MEMBER, node.offset)

        return checkCoordinates(coordinates, depth, type)
    }

    private fun checkCoordinates(node: TreeNode, depth: Int, type: String?): Broken? {
        if (depth == 0) return checkPosition(node)
        if (node.kind != NodeKind.ARRAY) {
            return Broken(DocumentProblem.GEOJSON_COORDINATES_SHAPE, node.offset)
        }

        // The rules that count the positions apply to the array that holds
        // them, which is the one at depth one.
        if (depth == 1) {
            val isRing = type == "Polygon" || type == "MultiPolygon"
            if (isRing) {
                checkRing(node)?.let { return it }
            } else if (type == "LineString" || type == "MultiLineString") {
                if (node.children.size < MIN_LINE_POSITIONS) {
                    return Broken(DocumentProblem.GEOJSON_SHORT_LINE, node.offset)
                }
            }
        }
        return node.children.firstNotNullOfOrNull { checkCoordinates(it, depth - 1, type) }
    }

    private fun checkRing(ring: TreeNode): Broken? {
        if (ring.children.size < MIN_RING_POSITIONS) {
            return Broken(DocumentProblem.GEOJSON_RING_NOT_CLOSED, ring.offset)
        }
        val first = ring.children.first().positionValues()
        val last = ring.children.last().positionValues()
        return if (first != null && first == last) {
            null
        } else {
            Broken(DocumentProblem.GEOJSON_RING_NOT_CLOSED, ring.offset)
        }
    }

    private fun checkPosition(node: TreeNode): Broken? {
        // A number where a position was expected is not a bad position, it is
        // one array too few: saying so points at the right mistake.
        if (node.kind != NodeKind.ARRAY) {
            return Broken(DocumentProblem.GEOJSON_COORDINATES_SHAPE, node.offset)
        }
        val numbers = node.positionValues()
            ?: return Broken(DocumentProblem.GEOJSON_BAD_POSITION, node.offset)

        val longitude = numbers[0]
        val latitude = numbers[1]
        return when {
            longitude < -180.0 || longitude > 180.0 ->
                Broken(DocumentProblem.LONGITUDE_OUT_OF_RANGE, node.children[0].offset)

            latitude < -90.0 || latitude > 90.0 ->
                Broken(DocumentProblem.LATITUDE_OUT_OF_RANGE, node.children[1].offset)

            else -> null
        }
    }

    /** The numbers of a position, or null when this is not one. */
    private fun TreeNode.positionValues(): List<Double>? {
        if (kind != NodeKind.ARRAY) return null
        if (children.size !in 2..3) return null
        val numbers = children.map { child ->
            if (child.kind != NodeKind.NUMBER) return null
            child.value?.toDoubleOrNull() ?: return null
        }
        return numbers
    }

    private fun TreeNode.member(name: String): TreeNode? = children.firstOrNull { it.name == name }

    private fun TreeNode.typeName(): String? =
        member("type")?.takeIf { it.kind == NodeKind.STRING }?.value
}
