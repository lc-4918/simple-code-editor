package fr.lc4918.simplecodeeditor.format

import fr.lc4918.simplecodeeditor.model.NodeKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class JsonTreeTest {

    @Test
    fun `an object becomes one node per key, in order`() {
        val root = JsonTree.parse("""{"b":1,"a":2}""")!!

        assertEquals(NodeKind.OBJECT, root.kind)
        assertEquals(listOf("b", "a"), root.children.map { it.name })
    }

    @Test
    fun `an array names its children by their place`() {
        val root = JsonTree.parse("""["x","y"]""")!!

        assertEquals(NodeKind.ARRAY, root.kind)
        assertEquals(listOf("0", "1"), root.children.map { it.name })
        assertEquals(listOf("x", "y"), root.children.map { it.value })
    }

    @Test
    fun `each kind of value is recognised`() {
        val root = JsonTree.parse("""{"s":"t","n":1.5,"b":true,"z":null}""")!!

        assertEquals(
            listOf(NodeKind.STRING, NodeKind.NUMBER, NodeKind.BOOLEAN, NodeKind.NULL),
            root.children.map { it.kind },
        )
    }

    @Test
    fun `a number keeps the way it was written`() {
        val root = JsonTree.parse("""{"a":1.50,"b":1e3}""")!!

        assertEquals(listOf("1.50", "1e3"), root.children.map { it.value })
    }

    @Test
    fun `escapes are turned back into characters`() {
        val root = JsonTree.parse("""{"a":"line\nbreak A \"quoted\""}""")!!

        assertEquals("line\nbreak A \"quoted\"", root.children.single().value)
    }

    @Test
    fun `nesting is kept`() {
        val root = JsonTree.parse("""{"a":{"b":[1]}}""")!!

        assertEquals(1, root.children.single().children.single().children.size)
        assertEquals(3, root.descendantCount())
    }

    @Test
    fun `an empty container has no children`() {
        val root = JsonTree.parse("""{"a":{},"b":[]}""")!!

        assertEquals(listOf(0, 0), root.children.map { it.children.size })
    }

    @Test
    fun `a broken document reads as nothing`() {
        assertNull(JsonTree.parse("""{"a":}"""))
        assertNull(JsonTree.parse("""{"a":1"""))
        assertNull(JsonTree.parse(""))
    }

    @Test
    fun `content after the document is refused`() {
        assertNull(JsonTree.parse("""{"a":1} and more"""))
    }
}
