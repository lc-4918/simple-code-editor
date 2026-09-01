package fr.lc4918.simplecodeeditor.format

import fr.lc4918.simplecodeeditor.model.CsvTable
import fr.lc4918.simplecodeeditor.model.FilterOperator
import fr.lc4918.simplecodeeditor.model.SortDirection
import org.junit.Assert.assertEquals
import org.junit.Test

class CsvTransformTest {

    private val table = CsvTable(
        header = listOf("name", "score"),
        rows = listOf(
            listOf("ada", "9"),
            listOf("Linus", "10"),
            listOf("grace", "2"),
        ),
        delimiter = ',',
    )

    @Test
    fun `a column of numbers sorts by value and not by spelling`() {
        val sorted = CsvTransform.sort(table, column = 1, direction = SortDirection.ASCENDING)

        assertEquals(listOf("2", "9", "10"), sorted.rows.map { it[1] })
    }

    @Test
    fun `a column of words sorts without regard to case`() {
        val sorted = CsvTransform.sort(table, column = 0, direction = SortDirection.ASCENDING)

        assertEquals(listOf("ada", "grace", "Linus"), sorted.rows.map { it[0] })
    }

    @Test
    fun `sorting the other way reverses the order`() {
        val sorted = CsvTransform.sort(table, column = 1, direction = SortDirection.DESCENDING)

        assertEquals(listOf("10", "9", "2"), sorted.rows.map { it[1] })
    }

    @Test
    fun `sorting leaves the header where it is`() {
        val sorted = CsvTransform.sort(table, column = 0, direction = SortDirection.DESCENDING)

        assertEquals(listOf("name", "score"), sorted.header)
    }

    @Test
    fun `a filter keeps the rows that contain the value`() {
        val filtered = CsvTransform.filter(table, 0, FilterOperator.CONTAINS, "A")

        assertEquals(listOf("ada", "grace"), filtered.rows.map { it[0] })
    }

    @Test
    fun `a filter on equality ignores the case`() {
        val filtered = CsvTransform.filter(table, 0, FilterOperator.EQUALS, "linus")

        assertEquals(listOf("Linus"), filtered.rows.map { it[0] })
    }

    @Test
    fun `a filter on a number compares by value`() {
        val filtered = CsvTransform.filter(table, 1, FilterOperator.GREATER, "9")

        assertEquals(listOf("Linus"), filtered.rows.map { it[0] })
    }

    @Test
    fun `a missing cell counts as empty`() {
        val ragged = table.copy(rows = table.rows + listOf(listOf("short")))
        val filtered = CsvTransform.filter(ragged, 1, FilterOperator.EQUALS, "")

        assertEquals(listOf("short"), filtered.rows.map { it[0] })
    }
}
