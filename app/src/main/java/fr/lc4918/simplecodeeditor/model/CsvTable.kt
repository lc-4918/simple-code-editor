package fr.lc4918.simplecodeeditor.model

/**
 * A CSV document seen as a grid.
 *
 * The first row is taken as the header, which is what the table view shows as
 * its column titles. Rows are kept exactly as they were read, ragged ones
 * included, so that editing one cell does not quietly pad every other row.
 */
data class CsvTable(
    val header: List<String>,
    val rows: List<List<String>>,
    val delimiter: Char,
) {
    /** Widest row, header included, which is how many columns the grid shows. */
    val columnCount: Int
        get() = maxOf(header.size, rows.maxOfOrNull { it.size } ?: 0)

    fun cell(row: Int, column: Int): String = rows[row].getOrElse(column) { "" }

    fun columnName(column: Int): String =
        header.getOrElse(column) { (column + 1).toString() }

    /** Replaces one cell, padding its row when the cell sits past its end. */
    fun withCell(row: Int, column: Int, value: String): CsvTable {
        val updated = rows[row].toMutableList()
        while (updated.size <= column) updated.add("")
        updated[column] = value
        return copy(rows = rows.toMutableList().also { it[row] = updated })
    }
}
