package fr.lc4918.simplecodeeditor.format

import fr.lc4918.simplecodeeditor.model.DocumentFormat
import fr.lc4918.simplecodeeditor.model.NodeKind
import fr.lc4918.simplecodeeditor.model.TreeNode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TreeStructureTest {

    private val json = DocumentFormat.JSON
    private val xml = DocumentFormat.XML

    private fun tree(source: String) = JsonTree.parse(source)!!

    private fun TreeNode.child(name: String): TreeNode = children.first { it.name == name }

    @Test
    fun `extracting a subtree gives it on its own`() {
        val source = """{"a":{"b":1},"c":2}"""
        val member = tree(source).child("a")

        assertEquals(""""a":{"b":1}""", TreeStructure.extract(source, member))
    }

    @Test
    fun `removing a member takes the comma that followed it`() {
        val source = """{"a":1,"b":2}"""

        assertEquals("""{"b":2}""", TreeStructure.remove(source, json, tree(source).child("a")))
    }

    @Test
    fun `removing the last member takes the comma that preceded it`() {
        val source = """{"a":1,"b":2}"""

        assertEquals("""{"a":1}""", TreeStructure.remove(source, json, tree(source).child("b")))
    }

    @Test
    fun `removing the only member leaves an empty container`() {
        val source = """{"a":1}"""

        assertEquals("{}", TreeStructure.remove(source, json, tree(source).child("a")))
    }

    @Test
    fun `removing an element of an array keeps the others in order`() {
        val source = """[1,2,3]"""
        val second = tree(source).children[1]

        assertEquals("[1,3]", TreeStructure.remove(source, json, second))
    }

    @Test
    fun `a duplicated member is given a key of its own`() {
        val source = """{"a":1}"""
        val edited = TreeStructure.duplicate(source, json, tree(source).child("a"))!!

        assertEquals("""{"a":1,"a copy":1}""", edited)
        assertEquals(2, tree(edited).children.size)
    }

    @Test
    fun `a duplicated element of an array keeps its shape`() {
        val source = """[{"a":1}]"""
        val edited = TreeStructure.duplicate(source, json, tree(source).children.single())!!

        assertEquals("""[{"a":1},{"a":1}]""", edited)
    }

    @Test
    fun `a duplicate follows the layout of what it copies`() {
        val source = "{\n  \"a\": 1\n}"
        val edited = TreeStructure.duplicate(source, json, tree(source).child("a"))!!

        assertEquals("{\n  \"a\": 1,\n  \"a copy\": 1\n}", edited)
    }

    @Test
    fun `inserting after puts the text on the next line of a laid out document`() {
        val source = "{\n  \"a\": 1\n}"
        val edited = TreeStructure.insertAfter(source, json, tree(source).child("a"), "\"b\": 2")!!

        assertEquals("{\n  \"a\": 1,\n  \"b\": 2\n}", edited)
    }

    @Test
    fun `inserting before puts the text ahead of the node`() {
        val source = """{"b":2}"""
        val edited = TreeStructure.insertBefore(source, json, tree(source).child("b"), "\"a\":1")!!

        assertEquals("""{"a":1,"b":2}""", edited)
    }

    @Test
    fun `inserting into an empty object puts the text between its brackets`() {
        val source = """{"a":{}}"""
        val edited = TreeStructure.insertInto(source, json, tree(source).child("a"), "\"b\":1")!!

        assertEquals("""{"a":{"b":1}}""", edited)
    }

    @Test
    fun `inserting into a container that holds something puts the text last`() {
        val source = """{"a":[1]}"""
        val edited = TreeStructure.insertInto(source, json, tree(source).child("a"), "2")!!

        assertEquals("""{"a":[1,2]}""", edited)
    }

    @Test
    fun `the skeletons read as what they claim to be`() {
        assertEquals("""{"key": {}}""", "{" + TreeStructure.skeleton(json, NodeKind.OBJECT, named = true) + "}")
        assertEquals("[[]]", "[" + TreeStructure.skeleton(json, NodeKind.ARRAY, named = false) + "]")
        assertEquals("[\"\"]", "[" + TreeStructure.skeleton(json, NodeKind.STRING, named = false) + "]")
    }

    @Test
    fun `removing an element takes the line it sat on`() {
        val source = "<gpx>\n  <wpt/>\n  <trk/>\n</gpx>"
        val point = XmlTree.parse(source)!!.children.first()

        assertEquals("<gpx>\n  <trk/>\n</gpx>", TreeStructure.remove(source, xml, point))
    }

    @Test
    fun `duplicating an element writes it again on the next line`() {
        val source = "<gpx>\n  <wpt/>\n</gpx>"
        val point = XmlTree.parse(source)!!.children.single()
        val edited = TreeStructure.duplicate(source, xml, point)!!

        assertEquals("<gpx>\n  <wpt/>\n  <wpt/>\n</gpx>", edited)
    }

    @Test
    fun `inserting into an element puts the text after its opening tag`() {
        val source = "<gpx></gpx>"
        val root = XmlTree.parse(source)!!
        val edited = TreeStructure.insertInto(source, xml, root, "<wpt/>")!!

        assertEquals("<gpx><wpt/></gpx>", edited)
    }

    @Test
    fun `a node with nothing written for it cannot be moved`() {
        val bare = TreeNode(name = "x", kind = NodeKind.STRING, value = "y")

        assertNull(TreeStructure.remove("{}", json, bare))
        assertNull(TreeStructure.extract("{}", bare))
        assertNull(TreeStructure.duplicate("{}", json, bare))
    }
}
