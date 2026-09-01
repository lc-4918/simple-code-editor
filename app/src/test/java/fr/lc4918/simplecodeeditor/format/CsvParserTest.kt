package fr.lc4918.simplecodeeditor.format

import fr.lc4918.simplecodeeditor.model.CsvTable
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CsvParserTest {

    @Test
    fun `the first row becomes the header`() {
        val table = CsvParser.parse("name,city\nada,lyon\nlinus,oslo")!!

        assertEquals(listOf("name", "city"), table.header)
        assertEquals(listOf(listOf("ada", "lyon"), listOf("linus", "oslo")), table.rows)
    }

    @Test
    fun `a quoted field may hold the delimiter`() {
        val table = CsvParser.parse("name,city\n\"doe, ada\",lyon")!!

        assertEquals(listOf("doe, ada", "lyon"), table.rows.single())
    }

    @Test
    fun `a doubled quote stands for one quote`() {
        val table = CsvParser.parse("a\n\"say \"\"hi\"\"\"")!!

        assertEquals(listOf("say \"hi\""), table.rows.single())
    }

    @Test
    fun `a quoted field may hold a line break`() {
        val table = CsvParser.parse("a,b\n\"two\nlines\",x")!!

        assertEquals(listOf("two\nlines", "x"), table.rows.single())
    }

    @Test
    fun `the semicolon is picked up as a delimiter`() {
        val table = CsvParser.parse("name;city\nada;lyon")!!

        assertEquals(';', table.delimiter)
        assertEquals(listOf("ada", "lyon"), table.rows.single())
    }

    @Test
    fun `carriage returns are dropped`() {
        val table = CsvParser.parse("a,b\r\n1,2\r\n")!!

        assertEquals(listOf(listOf("1", "2")), table.rows)
    }

    @Test
    fun `a ragged row keeps its own length`() {
        val table = CsvParser.parse("a,b,c\n1,2")!!

        assertEquals(listOf("1", "2"), table.rows.single())
        assertEquals(3, table.columnCount)
        assertEquals("", table.cell(0, 2))
    }

    @Test
    fun `an unterminated quote is not a table`() {
        assertNull(CsvParser.parse("a,b\n\"never closed"))
    }

    @Test
    fun `a blank document is not a table`() {
        assertNull(CsvParser.parse("   \n  "))
    }

    @Test
    fun `writing quotes only what needs it`() {
        val table = CsvTable(
            header = listOf("name", "note"),
            rows = listOf(listOf("ada", "one, two"), listOf("linus", "plain")),
            delimiter = ',',
        )

        assertEquals("name,note\nada,\"one, two\"\nlinus,plain", CsvParser.format(table))
    }

    @Test
    fun `a document survives a round trip`() {
        val source = "name,note\nada,\"one, two\"\nlinus,\"say \"\"hi\"\"\""

        assertEquals(source, CsvParser.format(CsvParser.parse(source)!!))
    }

    @Test
    fun `changing a cell pads only its own row`() {
        val table = CsvParser.parse("a,b,c\n1,2")!!.withCell(row = 0, column = 2, value = "3")

        assertEquals(listOf("1", "2", "3"), table.rows.single())
    }
}
