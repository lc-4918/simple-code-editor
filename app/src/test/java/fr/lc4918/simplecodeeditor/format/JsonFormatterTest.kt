package fr.lc4918.simplecodeeditor.format

import org.junit.Assert.assertEquals
import org.junit.Test

class JsonFormatterTest {

    @Test
    fun `a compacted object is laid out over several lines`() {
        val formatted = JsonFormatter.indent("""{"name":"ada","age":36}""", width = 2)

        assertEquals(
            """
            {
              "name": "ada",
              "age": 36
            }
            """.trimIndent(),
            formatted,
        )
    }

    @Test
    fun `nesting adds one level per container`() {
        val formatted = JsonFormatter.indent("""{"tags":[1,2],"ok":true}""", width = 4)

        assertEquals(
            """
            {
                "tags": [
                    1,
                    2
                ],
                "ok": true
            }
            """.trimIndent(),
            formatted,
        )
    }

    @Test
    fun `an empty container stays on one line`() {
        val formatted = JsonFormatter.indent("""{"a":{},"b":[  ]}""", width = 2)

        assertEquals("{\n  \"a\": {},\n  \"b\": []\n}", formatted)
    }

    @Test
    fun `separators inside a string are left alone`() {
        val source = """{"path":"a,b:{c}"}"""
        val formatted = JsonFormatter.indent(source, width = 2)

        assertEquals(
            """
            {
              "path": "a,b:{c}"
            }
            """.trimIndent(),
            formatted,
        )
    }

    @Test
    fun `an escaped quote does not end the string`() {
        val source = """{"quote":"say \"hi\", now"}"""

        assertEquals(source, JsonFormatter.compact(JsonFormatter.indent(source, width = 2)))
    }

    @Test
    fun `compacting removes every space outside the strings`() {
        val source = """
            {
              "a": 1,
              "b": [ 2, 3 ]
            }
        """.trimIndent()

        assertEquals("""{"a":1,"b":[2,3]}""", JsonFormatter.compact(source))
    }

    @Test
    fun `spaces inside a string survive compacting`() {
        assertEquals("""{"a":"two words"}""", JsonFormatter.compact("""{ "a" : "two words" }"""))
    }

    @Test
    fun `an unterminated string leaves the document untouched`() {
        val broken = """{"a":"unfinished"""

        assertEquals(broken, JsonFormatter.indent(broken, width = 2))
        assertEquals(broken, JsonFormatter.compact(broken))
    }

    @Test
    fun `indenting is stable when run twice`() {
        val once = JsonFormatter.indent("""{"a":[{"b":1}]}""", width = 2)

        assertEquals(once, JsonFormatter.indent(once, width = 2))
    }
}
