package fr.lc4918.simplecodeeditor.format

import fr.lc4918.simplecodeeditor.model.DocumentFormat
import org.junit.Assert.assertEquals
import org.junit.Test

class DocumentFormatterTest {

    @Test
    fun `each format is laid out by its own rules`() {
        assertEquals(
            "{\n  \"a\": 1\n}",
            DocumentFormatter.indent("""{"a":1}""", DocumentFormat.JSON, width = 2),
        )
        assertEquals(
            "<a>\n  <b/>\n</a>",
            DocumentFormatter.indent("<a><b/></a>", DocumentFormat.XML, width = 2),
        )
        assertEquals(
            "a {\n  b: c;\n}",
            DocumentFormatter.indent("a{b:c;}", DocumentFormat.CSS, width = 2),
        )
    }

    @Test
    fun `a format without a layout of its own is handed back unchanged`() {
        val rows = "name,city\nada,lyon"

        assertEquals(rows, DocumentFormatter.indent(rows, DocumentFormat.CSV, width = 2))
        assertEquals(rows, DocumentFormatter.compact(rows, DocumentFormat.CSV))
    }

    @Test
    fun `escaping produces what would sit between two quotes`() {
        assertEquals(
            "{\\\"a\\\": 1}\\nend",
            DocumentFormatter.escape("{\"a\": 1}\nend"),
        )
    }

    @Test
    fun `a backslash is doubled`() {
        assertEquals("a\\\\b", DocumentFormatter.escape("a\\b"))
    }

    @Test
    fun `a control character is escaped by its code`() {
        assertEquals("\\u0001", DocumentFormatter.escape("\u0001"))
    }
}
