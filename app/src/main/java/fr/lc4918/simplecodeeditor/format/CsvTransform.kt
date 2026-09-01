package fr.lc4918.simplecodeeditor.format

import fr.lc4918.simplecodeeditor.model.CsvTable
import fr.lc4918.simplecodeeditor.model.FilterOperator
import fr.lc4918.simplecodeeditor.model.SortDirection

/**
 * Reorders and thins out the rows of a table.
 *
 * Cells that read as numbers are compared as numbers, so that ten sorts after
 * nine rather than before it. Anything else is compared as text, ignoring the
 * case, which is what someone sorting a column of names expects.
 */
object CsvTransform {

    fun sort(table: CsvTable, column: Int, direction: SortDirection): CsvTable {
        val ordered = table.rows.sortedWith { left, right ->
            compareCells(left.getOrElse(column) { "" }, right.getOrElse(column) { "" })
        }
        return table.copy(
            rows = if (direction == SortDirection.ASCENDING) ordered else ordered.reversed(),
        )
    }

    fun filter(
        table: CsvTable,
        column: Int,
        operator: FilterOperator,
        value: String,
    ): CsvTable {
        val kept = table.rows.filter { row -> matches(row.getOrElse(column) { "" }, operator, value) }
        return table.copy(rows = kept)
    }

    private fun matches(cell: String, operator: FilterOperator, value: String): Boolean =
        when (operator) {
            FilterOperator.CONTAINS -> cell.contains(value, ignoreCase = true)
            FilterOperator.EQUALS -> cell.equals(value, ignoreCase = true)
            FilterOperator.GREATER -> compareCells(cell, value) > 0
            FilterOperator.LESS -> compareCells(cell, value) < 0
        }

    private fun compareCells(left: String, right: String): Int {
        val leftNumber = left.trim().toDoubleOrNull()
        val rightNumber = right.trim().toDoubleOrNull()
        return if (leftNumber != null && rightNumber != null) {
            leftNumber.compareTo(rightNumber)
        } else {
            left.compareTo(right, ignoreCase = true)
        }
    }
}
