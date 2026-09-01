package fr.lc4918.simplecodeeditor.format

import fr.lc4918.simplecodeeditor.model.DocumentFormat
import org.junit.Assert.assertEquals
import org.junit.Test

class FormatDetectorTest {

    @Test
    fun `file name wins over content`() {
        val format = FormatDetector.detect(
            fileName = "route.gpx",
            content = "{ \"a\": 1 }",
        )
        assertEquals(DocumentFormat.XML, format)
    }

    @Test
    fun `derived extensions map to their base format`() {
        assertEquals(DocumentFormat.JSON, FormatDetector.detect(fileName = "cities.geojson"))
        assertEquals(DocumentFormat.XML, FormatDetector.detect(fileName = "places.kml"))
        assertEquals(DocumentFormat.XML, FormatDetector.detect(fileName = "track.gpx"))
    }

    @Test
    fun `path prefixes are ignored`() {
        assertEquals(
            DocumentFormat.CSS,
            FormatDetector.detect(fileName = "/storage/emulated/0/site/main.css"),
        )
    }

    @Test
    fun `mime type is used when the name has no extension`() {
        assertEquals(
            DocumentFormat.JSON,
            FormatDetector.detect(fileName = "download", mimeType = "application/json; charset=utf-8"),
        )
    }

    @Test
    fun `json object and array are detected from content`() {
        assertEquals(DocumentFormat.JSON, FormatDetector.detectFromContent("{\"a\": 1}"))
        assertEquals(DocumentFormat.JSON, FormatDetector.detectFromContent("  [1, 2, 3]  "))
    }

    @Test
    fun `html is told apart from xml`() {
        assertEquals(
            DocumentFormat.HTML,
            FormatDetector.detectFromContent("<!DOCTYPE html><html><body>hi</body></html>"),
        )
        assertEquals(
            DocumentFormat.XML,
            FormatDetector.detectFromContent("<?xml version=\"1.0\"?><root><item/></root>"),
        )
    }

    @Test
    fun `csv needs a stable delimiter count across lines`() {
        val csv = "name,city,age\nada,london,36\ngrace,new york,45"
        assertEquals(DocumentFormat.CSV, FormatDetector.detectFromContent(csv))

        val ragged = "name,city,age\nada,london\ngrace"
        assertEquals(DocumentFormat.PLAIN_TEXT, FormatDetector.detectFromContent(ragged))
    }

    @Test
    fun `quoted delimiters do not count as separators`() {
        val csv = "name,city\n\"doe, john\",london\n\"roe, jane\",paris"
        assertEquals(DocumentFormat.CSV, FormatDetector.detectFromContent(csv))
    }

    @Test
    fun `css and javascript are detected from content`() {
        assertEquals(
            DocumentFormat.CSS,
            FormatDetector.detectFromContent("body { margin: 0; padding: 0; }"),
        )
        assertEquals(
            DocumentFormat.JAVASCRIPT,
            FormatDetector.detectFromContent("const total = items.reduce((a, b) => a + b, 0)"),
        )
    }

    @Test
    fun `unknown content falls back to plain text`() {
        assertEquals(DocumentFormat.PLAIN_TEXT, FormatDetector.detectFromContent("just a note"))
        assertEquals(DocumentFormat.PLAIN_TEXT, FormatDetector.detectFromContent("   "))
    }
}
