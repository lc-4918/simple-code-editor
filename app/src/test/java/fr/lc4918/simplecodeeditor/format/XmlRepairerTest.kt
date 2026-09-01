package fr.lc4918.simplecodeeditor.format

import org.junit.Assert.assertEquals
import org.junit.Test

class XmlRepairerTest {

    private fun repairedReads(source: String): Boolean =
        XmlTree.read(XmlRepairer.repair(source)).diagnostic == null

    @Test
    fun `a document that reads comes back as it was`() {
        val source = "<gpx><trk><name>walk</name></trk></gpx>"

        assertEquals(source, XmlRepairer.repair(source))
    }

    @Test
    fun `an element left open is closed at the end`() {
        assertEquals("<gpx><trk></trk></gpx>", XmlRepairer.repair("<gpx><trk>"))
    }

    @Test
    fun `elements closed in the wrong order are closed in the right one`() {
        assertEquals("<a><b></b></a>", XmlRepairer.repair("<a><b></a></b>"))
    }

    @Test
    fun `a closing tag with nothing to close is dropped`() {
        assertEquals("<a></a>", XmlRepairer.repair("<a></b></a>"))
    }

    @Test
    fun `an ampersand that opens no entity is written out`() {
        assertEquals("<a>Fish &amp; Chips</a>", XmlRepairer.repair("<a>Fish & Chips</a>"))
    }

    @Test
    fun `an ampersand that opens one is left alone`() {
        val source = "<a>&amp; &lt; &#233; &#x1F600;</a>"

        assertEquals(source, XmlRepairer.repair(source))
    }

    @Test
    fun `a less than sign in text is written out`() {
        assertEquals("<a>1 &lt; 2</a>", XmlRepairer.repair("<a>1 < 2</a>"))
    }

    @Test
    fun `an attribute that lost its quotes gets them back`() {
        assertEquals("""<wpt lat="45.76" lon="4.83"/>""", XmlRepairer.repair("<wpt lat=45.76 lon=4.83/>"))
    }

    @Test
    fun `an attribute that kept its quotes is left alone`() {
        val source = """<a href="x?a=1&amp;b=2">t</a>"""

        assertEquals(source, XmlRepairer.repair(source))
    }

    @Test
    fun `a tag that never closes is closed`() {
        assertEquals("<a><b></b></a>", XmlRepairer.repair("<a><b"))
    }

    @Test
    fun `a comment and character data come through untouched`() {
        val source = "<a><!-- 1 < 2 & 3 --><![CDATA[<b>&</b>]]></a>"

        assertEquals(source, XmlRepairer.repair(source))
    }

    @Test
    fun `a declaration is neither added nor removed`() {
        assertEquals("<a></a>", XmlRepairer.repair("<a>"))

        val declared = "<?xml version=\"1.0\"?><a></a>"
        assertEquals(declared, XmlRepairer.repair(declared))
    }

    @Test
    fun `what comes out of a repair reads`() {
        listOf(
            "<gpx><trk>",
            "<a><b></a></b>",
            "<a>Fish & Chips</a>",
            "<a>1 < 2</a>",
            "<wpt lat=45.76 lon=4.83/>",
            "<a><b",
            "<gpx><wpt lat=1 lon=2><name>x</wpt>",
        ).forEach { source ->
            assertEquals("repaired: " + XmlRepairer.repair(source), true, repairedReads(source))
        }
    }

    @Test
    fun `a document with nothing in it stays that way`() {
        assertEquals("", XmlRepairer.repair(""))
    }
}
