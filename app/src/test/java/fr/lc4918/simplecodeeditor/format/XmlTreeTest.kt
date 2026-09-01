package fr.lc4918.simplecodeeditor.format

import fr.lc4918.simplecodeeditor.model.NodeKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class XmlTreeTest {

    @Test
    fun `elements become nested nodes`() {
        val root = XmlTree.parse("<gpx><trk><name>walk</name></trk></gpx>")!!

        assertEquals("gpx", root.name)
        assertEquals("trk", root.children.single().name)
        assertEquals("name", root.children.single().children.single().name)
    }

    @Test
    fun `the text of an element hangs under it`() {
        val root = XmlTree.parse("<name>walk</name>")!!
        val text = root.children.single()

        assertEquals(NodeKind.TEXT, text.kind)
        assertEquals("walk", text.value)
    }

    @Test
    fun `attributes hang under their element, marked as such`() {
        val root = XmlTree.parse("""<trkpt lat="45.1" lon="1.2"/>""")!!

        assertEquals(listOf("@lat", "@lon"), root.children.map { it.name })
        assertEquals(listOf("45.1", "1.2"), root.children.map { it.value })
        assertEquals(NodeKind.ATTRIBUTE, root.children.first().kind)
    }

    @Test
    fun `a self closing element holds nothing`() {
        val root = XmlTree.parse("<a><b/><c/></a>")!!

        assertEquals(listOf("b", "c"), root.children.map { it.name })
        assertEquals(listOf(0, 0), root.children.map { it.children.size })
    }

    @Test
    fun `a void element of HTML looks for no closing tag`() {
        val root = XmlTree.parse("<p><br><i>x</i></p>", html = true)!!

        assertEquals(listOf("br", "i"), root.children.map { it.name })
    }

    @Test
    fun `the same name in XML holds its content, as GPX writes it`() {
        val root = XmlTree.parse("<gpx><link href=\"a\"><text>x</text></link></gpx>")!!
        val link = root.children.single()

        assertEquals("link", link.name)
        assertEquals(listOf("@href", "text"), link.children.map { it.name })
    }

    @Test
    fun `declarations and comments are left out`() {
        val root = XmlTree.parse("<?xml version=\"1.0\"?><!-- note --><a/>")!!

        assertEquals("a", root.name)
    }

    @Test
    fun `a bracket inside an attribute does not end the tag`() {
        val root = XmlTree.parse("""<a title="x > y"><b/></a>""")!!

        assertEquals(listOf("@title", "b"), root.children.map { it.name })
    }

    @Test
    fun `several roots hang under one nameless node`() {
        val root = XmlTree.parse("<a/><b/>")!!

        assertEquals("", root.name)
        assertEquals(listOf("a", "b"), root.children.map { it.name })
    }

    @Test
    fun `a document whose tags do not close reads as nothing`() {
        assertNull(XmlTree.parse("<a><b></a>"))
        assertNull(XmlTree.parse("<a>"))
        assertNull(XmlTree.parse(""))
    }
}
