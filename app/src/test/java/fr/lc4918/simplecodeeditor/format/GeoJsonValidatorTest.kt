package fr.lc4918.simplecodeeditor.format

import fr.lc4918.simplecodeeditor.model.DocumentProblem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GeoJsonValidatorTest {

    private fun problemIn(source: String): DocumentProblem? {
        val root = JsonTree.parse(source)!!
        return GeoJsonValidator.validate(source, root)?.problem
    }

    @Test
    fun `a collection of features passes`() {
        val source = """
            {"type":"FeatureCollection","features":[
              {"type":"Feature","properties":{},"geometry":{"type":"Point","coordinates":[4.83,45.76]}}
            ]}
        """.trimIndent()

        assertNull(problemIn(source))
    }

    @Test
    fun `a bare geometry passes`() {
        assertNull(problemIn("""{"type":"Point","coordinates":[4.83,45.76]}"""))
    }

    @Test
    fun `a height is allowed on a position`() {
        assertNull(problemIn("""{"type":"Point","coordinates":[4.83,45.76,180]}"""))
    }

    @Test
    fun `a type GeoJSON does not define is refused`() {
        assertEquals(
            DocumentProblem.GEOJSON_UNKNOWN_TYPE,
            problemIn("""{"type":"Circle","coordinates":[0,0]}"""),
        )
    }

    @Test
    fun `a feature without its geometry is refused`() {
        assertEquals(
            DocumentProblem.GEOJSON_MISSING_MEMBER,
            problemIn("""{"type":"Feature","properties":{}}"""),
        )
    }

    @Test
    fun `a feature may hold no geometry at all`() {
        assertNull(problemIn("""{"type":"Feature","properties":null,"geometry":null}"""))
    }

    @Test
    fun `a collection without its features is refused`() {
        assertEquals(
            DocumentProblem.GEOJSON_MISSING_MEMBER,
            problemIn("""{"type":"FeatureCollection"}"""),
        )
    }

    @Test
    fun `a geometry without its coordinates is refused`() {
        assertEquals(DocumentProblem.GEOJSON_MISSING_MEMBER, problemIn("""{"type":"Point"}"""))
    }

    @Test
    fun `a position of one number is refused`() {
        assertEquals(
            DocumentProblem.GEOJSON_BAD_POSITION,
            problemIn("""{"type":"Point","coordinates":[4.83]}"""),
        )
    }

    @Test
    fun `a position holding text is refused`() {
        assertEquals(
            DocumentProblem.GEOJSON_BAD_POSITION,
            problemIn("""{"type":"Point","coordinates":["4.83",45.76]}"""),
        )
    }

    @Test
    fun `coordinates that are not nested as the geometry takes are refused`() {
        assertEquals(
            DocumentProblem.GEOJSON_COORDINATES_SHAPE,
            problemIn("""{"type":"LineString","coordinates":[4.83,45.76]}"""),
        )
    }

    @Test
    fun `a longitude past 180 is refused, and named as such`() {
        assertEquals(
            DocumentProblem.LONGITUDE_OUT_OF_RANGE,
            problemIn("""{"type":"Point","coordinates":[181,45]}"""),
        )
    }

    @Test
    fun `a latitude past 90 is refused, which is what swapping the two looks like`() {
        assertEquals(
            DocumentProblem.LATITUDE_OUT_OF_RANGE,
            problemIn("""{"type":"Point","coordinates":[45.76,4.83e2]}"""),
        )
    }

    @Test
    fun `a line of a single position is refused`() {
        assertEquals(
            DocumentProblem.GEOJSON_SHORT_LINE,
            problemIn("""{"type":"LineString","coordinates":[[4.8,45.7]]}"""),
        )
    }

    @Test
    fun `a ring that does not close is refused`() {
        val source = """{"type":"Polygon","coordinates":[[[0,0],[1,0],[1,1],[0,1]]]}"""

        assertEquals(DocumentProblem.GEOJSON_RING_NOT_CLOSED, problemIn(source))
    }

    @Test
    fun `a ring that closes passes`() {
        val source = """{"type":"Polygon","coordinates":[[[0,0],[1,0],[1,1],[0,0]]]}"""

        assertNull(problemIn(source))
    }

    @Test
    fun `a ring of three positions is refused however it ends`() {
        val source = """{"type":"Polygon","coordinates":[[[0,0],[1,0],[0,0]]]}"""

        assertEquals(DocumentProblem.GEOJSON_RING_NOT_CLOSED, problemIn(source))
    }

    @Test
    fun `a collection of geometries is walked through`() {
        val source = """
            {"type":"GeometryCollection","geometries":[
              {"type":"Point","coordinates":[0,0]},
              {"type":"Point","coordinates":[999,0]}
            ]}
        """.trimIndent()

        assertEquals(DocumentProblem.LONGITUDE_OUT_OF_RANGE, problemIn(source))
    }

    @Test
    fun `a rule of the format is not something repairing could address`() {
        DocumentProblem.entries
            .filter { it.name.startsWith("GEOJSON_") || it.name.startsWith("COORDINATE") }
            .forEach { problem -> assertEquals(false, problem.stopsTheReading) }
    }

    @Test
    fun `a grammar problem is something repairing could address`() {
        assertEquals(true, DocumentProblem.UNEXPECTED_CHARACTER.stopsTheReading)
        assertEquals(true, DocumentProblem.UNTERMINATED_STRING.stopsTheReading)
    }

    @Test
    fun `the report points at the offending line`() {
        val source = "{\n  \"type\": \"Point\",\n  \"coordinates\": [999, 0]\n}"
        val diagnostic = GeoJsonValidator.validate(source, JsonTree.parse(source)!!)!!

        assertEquals(3, diagnostic.line)
    }
}
