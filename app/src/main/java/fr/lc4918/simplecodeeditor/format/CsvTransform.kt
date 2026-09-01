package fr.lc4918.simplecodeeditor.format

import fr.lc4918.simplecodeeditor.model.CsvTable
import fr.lc4918.simplecodeeditor.model.FilterOperator
import fr.lc4918.simplecodeeditor.model.SortDirection

/**
 * Reorders and thins out the rows of a table.
 *
 * How two cells compare is left to [ValueOrder], which the hierarchy uses as
 * well so that both views order alike.
 */
object CsvTransform {

    fun sort(table: CsvTable, column: Int, direction: SortDirection): CsvTable {
        val ordered = table.rows.sortedWith { left, right ->
            ValueOrder.compare(left.getOrElse(column) { "" }, right.getOrElse(column) { "" })
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
        val kept = table.rows.filter { row ->
            ValueOrder.matches(row.getOrElse(column) { "" }, operator, value)
        }
        return table.copy(rows = kept)
    }
}
