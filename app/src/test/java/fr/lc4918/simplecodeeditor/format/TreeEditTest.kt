package fr.lc4918.simplecodeeditor.format

import fr.lc4918.simplecodeeditor.model.DocumentFormat
import fr.lc4918.simplecodeeditor.model.NodeKind
import fr.lc4918.simplecodeeditor.model.TreeNode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TreeEditTest {

    private fun json(source: String) = JsonTree.parse(source)!!

    private fun xml(source: String) = XmlTree.parse(source)!!

    private fun TreeNode.child(name: String): TreeNode = children.first { it.name == name }

    @Test
    fun `changing a value leaves the rest of the document alone`() {
        val source = "{\n  \"a\": 1,\n  \"b\": 2\n}"
        val edited = TreeEdit.withValue(source, DocumentFormat.JSON, json(source).child("a"), "9")

        assertEquals("{\n  \"a\": 9,\n  \"b\": 2\n}", edited)
    }

    @Test
    fun `a value that reads as a number becomes one`() {
        val source = """{"a":"one"}"""
        val edited = TreeEdit.withValue(source, DocumentFormat.JSON, json(source).child("a"), "42")

        assertEquals("""{"a":42}""", edited)
    }

    @Test
    fun `a value that reads as a word becomes a string`() {
        val source = """{"a":1}"""
        val edited = TreeEdit.withValue(source, DocumentFormat.JSON, json(source).child("a"), "one")

        assertEquals("""{"a":"one"}""", edited)
    }

    @Test
    fun `true false and null are written as themselves`() {
        val source = """{"a":1}"""
        val node = json(source).child("a")

        assertEquals("""{"a":true}""", TreeEdit.withValue(source, DocumentFormat.JSON, node, "true"))
        assertEquals("""{"a":null}""", TreeEdit.withValue(source, DocumentFormat.JSON, node, "null"))
    }

    @Test
    fun `quotes keep a word that would otherwise be read as something else`() {
        val source = """{"a":1}"""
        val edited =
            TreeEdit.withValue(source, DocumentFormat.JSON, json(source).child("a"), "\"true\"")

        assertEquals("""{"a":"true"}""", edited)
        assertEquals("true", json(edited!!).child("a").value)
    }

    @Test
    fun `what a value would become is told before it is written`() {
        assertEquals(NodeKind.NUMBER, TreeEdit.jsonKindOf("1.5e3"))
        assertEquals(NodeKind.BOOLEAN, TreeEdit.jsonKindOf("false"))
        assertEquals(NodeKind.NULL, TreeEdit.jsonKindOf("null"))
        assertEquals(NodeKind.STRING, TreeEdit.jsonKindOf("\"12\""))
        assertEquals(NodeKind.STRING, TreeEdit.jsonKindOf("12 apples"))
    }

    @Test
    fun `a value holding a quote is escaped on its way in`() {
        val source = """{"a":1}"""
        val edited =
            TreeEdit.withValue(source, DocumentFormat.JSON, json(source).child("a"), "say \"hi\"")

        assertEquals("say \"hi\"", json(edited!!).child("a").value)
    }

    @Test
    fun `changing a key leaves its value alone`() {
        val source = """{"a":1,"b":2}"""
        val edited = TreeEdit.withName(source, DocumentFormat.JSON, json(source).child("a"), "z")

        assertEquals("""{"z":1,"b":2}""", edited)
    }

    @Test
    fun `a container has no value to change`() {
        val source = """{"a":{"b":1}}"""

        assertNull(TreeEdit.withValue(source, DocumentFormat.JSON, json(source).child("a"), "9"))
    }

    @Test
    fun `an empty key is refused`() {
        val source = """{"a":1}"""

        assertNull(TreeEdit.withName(source, DocumentFormat.JSON, json(source).child("a"), "  "))
    }

    @Test
    fun `changing the text of an element leaves the tags alone`() {
        val source = "<gpx>\n  <name>walk</name>\n</gpx>"
        val text = xml(source).children.single().children.single()
        val edited = TreeEdit.withValue(source, DocumentFormat.XML, text, "run")

        assertEquals("<gpx>\n  <name>run</name>\n</gpx>", edited)
    }

    @Test
    fun `text that would look like markup is escaped`() {
        val source = "<a><b>x</b></a>"
        val text = xml(source).children.single().children.single()
        val edited = TreeEdit.withValue(source, DocumentFormat.XML, text, "1 < 2 & 3")

        assertEquals("<a><b>1 &lt; 2 &amp; 3</b></a>", edited)
    }

    @Test
    fun `changing an attribute keeps its quotes`() {
        val source = """<wpt lat="45.76" lon="4.83"/>"""
        val attribute = xml(source).child("@lat")
        val edited = TreeEdit.withValue(source, DocumentFormat.XML, attribute, "45.80")

        assertEquals("""<wpt lat="45.80" lon="4.83"/>""", edited)
    }

    @Test
    fun `renaming an attribute drops the mark the view adds`() {
        val source = """<wpt lat="45.76"/>"""
        val edited =
            TreeEdit.withName(source, DocumentFormat.XML, xml(source).child("@lat"), "@latitude")

        assertEquals("""<wpt latitude="45.76"/>""", edited)
    }

    @Test
    fun `renaming an element carries the closing tag along`() {
        val source = "<gpx><trk><name>x</name></trk></gpx>"
        val track = xml(source).children.single()
        val edited = TreeEdit.withName(source, DocumentFormat.XML, track, "rte")

        assertEquals("<gpx><rte><name>x</name></rte></gpx>", edited)
    }

    @Test
    fun `renaming an element that closes itself has no second tag to carry`() {
        val source = "<gpx><wpt/></gpx>"
        val point = xml(source).children.single()
        val edited = TreeEdit.withName(source, DocumentFormat.XML, point, "rtept")

        assertEquals("<gpx><rtept/></gpx>", edited)
    }

    @Test
    fun `a comment and a declaration survive an edit`() {
        val source = "<?xml version=\"1.0\"?>\n<!-- kept -->\n<gpx><name>x</name></gpx>"
        val text = xml(source).children.single().children.single()
        val edited = TreeEdit.withValue(source, DocumentFormat.XML, text, "y")

        assertEquals("<?xml version=\"1.0\"?>\n<!-- kept -->\n<gpx><name>y</name></gpx>", edited)
    }
}
