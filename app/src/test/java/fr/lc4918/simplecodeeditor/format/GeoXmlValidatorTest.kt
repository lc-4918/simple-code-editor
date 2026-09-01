package fr.lc4918.simplecodeeditor.format

import fr.lc4918.simplecodeeditor.model.DocumentProblem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GeoXmlValidatorTest {

    private fun gpxProblem(source: String): DocumentProblem? =
        GpxValidator.validate(source, XmlTree.parse(source)!!)?.problem

    private fun kmlProblem(source: String): DocumentProblem? =
        KmlValidator.validate(source, XmlTree.parse(source)!!)?.problem

    @Test
    fun `a track with its points passes`() {
        val source = """
            <gpx><trk><trkseg>
              <trkpt lat="45.76" lon="4.83"/>
              <trkpt lat="45.77" lon="4.84"/>
            </trkseg></trk></gpx>
        """.trimIndent()

        assertNull(gpxProblem(source))
    }

    @Test
    fun `a namespace on the opening element is allowed`() {
        assertNull(gpxProblem("""<gpx:gpx><gpx:wpt lat="0" lon="0"/></gpx:gpx>"""))
    }

    @Test
    fun `a document that does not open on gpx is refused`() {
        assertEquals(
            DocumentProblem.ROOT_ELEMENT_UNEXPECTED,
            gpxProblem("""<tracks><trkpt lat="0" lon="0"/></tracks>"""),
        )
    }

    @Test
    fun `a point without its longitude is refused`() {
        assertEquals(
            DocumentProblem.COORDINATE_MISSING,
            gpxProblem("""<gpx><trkpt lat="45.76"/></gpx>"""),
        )
    }

    @Test
    fun `a coordinate that is not a number is refused`() {
        assertEquals(
            DocumentProblem.COORDINATE_NOT_A_NUMBER,
            gpxProblem("""<gpx><wpt lat="north" lon="4.83"/></gpx>"""),
        )
    }

    @Test
    fun `a latitude past 90 is refused, which is what swapping the two looks like`() {
        assertEquals(
            DocumentProblem.LATITUDE_OUT_OF_RANGE,
            gpxProblem("""<gpx><wpt lat="483" lon="45.76"/></gpx>"""),
        )
    }

    @Test
    fun `a point buried deep in the track is still reached`() {
        val source = """<gpx><trk><trkseg><trkpt lat="0" lon="999"/></trkseg></trk></gpx>"""

        assertEquals(DocumentProblem.LONGITUDE_OUT_OF_RANGE, gpxProblem(source))
    }

    @Test
    fun `a placemark with its coordinates passes`() {
        val source = """
            <kml><Document><Placemark><Point>
              <coordinates>4.83,45.76,180</coordinates>
            </Point></Placemark></Document></kml>
        """.trimIndent()

        assertNull(kmlProblem(source))
    }

    @Test
    fun `several positions in one element are all read`() {
        val source = "<kml><LineString><coordinates>0,0 1,1 999,2</coordinates></LineString></kml>"

        assertEquals(DocumentProblem.LONGITUDE_OUT_OF_RANGE, kmlProblem(source))
    }

    @Test
    fun `a document that does not open on kml is refused`() {
        assertEquals(
            DocumentProblem.ROOT_ELEMENT_UNEXPECTED,
            kmlProblem("<Document><Placemark/></Document>"),
        )
    }

    @Test
    fun `an empty coordinates element is refused`() {
        assertEquals(
            DocumentProblem.COORDINATE_MISSING,
            kmlProblem("<kml><Point><coordinates></coordinates></Point></kml>"),
        )
    }

    @Test
    fun `a position of a single number is refused`() {
        assertEquals(
            DocumentProblem.COORDINATE_MISSING,
            kmlProblem("<kml><Point><coordinates>4.83</coordinates></Point></kml>"),
        )
    }

    @Test
    fun `the report points at the offending line`() {
        val source = "<gpx>\n  <wpt lat=\"0\" lon=\"0\"/>\n  <wpt lat=\"999\" lon=\"0\"/>\n</gpx>"
        val diagnostic = GpxValidator.validate(source, XmlTree.parse(source)!!)!!

        assertEquals(3, diagnostic.line)
    }
}
