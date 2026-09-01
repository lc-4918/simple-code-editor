package fr.lc4918.simplecodeeditor.format

import org.junit.Assert.assertEquals
import org.junit.Test

class JsonWriterTest {

    private fun roundTrip(source: String, width: Int?) =
        JsonWriter.write(JsonTree.parse(source)!!, width)

    @Test
    fun `a compact document comes back compact`() {
        val source = """{"a":1,"b":[true,null],"c":"x"}"""

        assertEquals(source, roundTrip(source, width = null))
    }

    @Test
    fun `a width lays the document out`() {
        assertEquals("{\n  \"a\": 1\n}", roundTrip("""{"a":1}""", width = 2))
    }

    @Test
    fun `an empty container stays on one line`() {
        assertEquals("""{"a":{},"b":[]}""", roundTrip("""{"a":{},"b":[]}""", width = null))
    }

    @Test
    fun `a number keeps the way it was written`() {
        assertEquals("""[1.50,1e3]""", roundTrip("""[1.50,1e3]""", width = null))
    }

    @Test
    fun `a string is escaped again on the way out`() {
        val source = """{"a":"line\nbreak \"quoted\""}"""

        assertEquals(source, roundTrip(source, width = null))
    }

    @Test
    fun `the order of the keys survives`() {
        val source = """{"z":1,"a":2,"m":3}"""

        assertEquals(source, roundTrip(source, width = null))
    }
}
