package fr.lc4918.simplecodeeditor.format

import org.junit.Assert.assertEquals
import org.junit.Test

class MarkupFormatterTest {

    @Test
    fun `nested elements gain one level each`() {
        val formatted = MarkupFormatter.indent("<gpx><trk><name>walk</name></trk></gpx>", width = 2)

        assertEquals(
            """
            <gpx>
              <trk>
                <name>
                  walk
                </name>
              </trk>
            </gpx>
            """.trimIndent(),
            formatted,
        )
    }

    @Test
    fun `a self closing tag opens no level`() {
        val formatted = MarkupFormatter.indent("<a><b/><c/></a>", width = 2)

        assertEquals("<a>\n  <b/>\n  <c/>\n</a>", formatted)
    }

    @Test
    fun `a void element opens no level`() {
        val formatted = MarkupFormatter.indent("<p><br><img src=\"a.png\"></p>", width = 2)

        assertEquals("<p>\n  <br>\n  <img src=\"a.png\">\n</p>", formatted)
    }

    @Test
    fun `a bracket inside an attribute does not end the tag`() {
        val formatted = MarkupFormatter.indent("<a title=\"a > b\"><b/></a>", width = 2)

        assertEquals("<a title=\"a > b\">\n  <b/>\n</a>", formatted)
    }

    @Test
    fun `a declaration and a comment stay on their own line`() {
        val formatted = MarkupFormatter.indent("<?xml version=\"1.0\"?><!-- note --><a/>", width = 2)

        assertEquals("<?xml version=\"1.0\"?>\n<!-- note -->\n<a/>", formatted)
    }

    @Test
    fun `the content of a preformatted block is left as it is`() {
        val source = "<div><pre>  kept\n   as is</pre></div>"
        val formatted = MarkupFormatter.indent(source, width = 2)

        assertEquals("<div>\n  <pre>  kept\n   as is</pre>\n</div>", formatted)
    }

    @Test
    fun `compacting drops the whitespace between tags`() {
        val source = """
            <gpx>
              <trk>
                <name>walk</name>
              </trk>
            </gpx>
        """.trimIndent()

        assertEquals("<gpx><trk><name>walk</name></trk></gpx>", MarkupFormatter.compact(source))
    }

    @Test
    fun `an unterminated tag leaves the document untouched`() {
        val broken = "<a><b"

        assertEquals(broken, MarkupFormatter.indent(broken, width = 2))
    }

    @Test
    fun `indenting is stable when run twice`() {
        val once = MarkupFormatter.indent("<a><b>text</b><c/></a>", width = 2)

        assertEquals(once, MarkupFormatter.indent(once, width = 2))
    }
}
