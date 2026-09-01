package fr.lc4918.simplecodeeditor.format

import fr.lc4918.simplecodeeditor.model.CsvTable

/**
 * Reads a CSV document into a grid, and writes a grid back out.
 *
 * Quoting follows the usual rules: a field may be wrapped in quotes, a doubled
 * quote inside stands for one quote, and a quoted field may hold the delimiter
 * and line breaks. Writing quotes only the fields that need it, so a document
 * that needed no quoting comes back out as it went in.
 */
object CsvParser {

    /** Tried in this order, and the first one that divides the rows evenly wins. */
    private val DELIMITERS = listOf(',', ';', '\t', '|')

    private const val QUOTE = '"'

    fun parse(content: String): CsvTable? {
        if (content.isBlank()) return null
        val delimiter = detectDelimiter(content)
        val records = readRecords(content, delimiter) ?: return null
        if (records.isEmpty()) return null

        return CsvTable(
            header = records.first(),
            rows = records.drop(1),
            delimiter = delimiter,
        )
    }

    fun format(table: CsvTable): String {
        val lines = (listOf(table.header) + table.rows).map { record ->
            record.joinToString(table.delimiter.toString()) { field ->
                quoteIfNeeded(field, table.delimiter)
            }
        }
        return lines.joinToString("\n")
    }

    /**
     * The separator that divides the first rows into the same number of
     * fields, falling back to the comma when none of them does.
     */
    fun detectDelimiter(content: String): Char {
        val sample = content.lineSequence().filter { it.isNotBlank() }.take(10).toList()
        if (sample.isEmpty()) return DELIMITERS.first()

        return DELIMITERS.firstOrNull { delimiter ->
            val counts = sample.map { line -> countOutsideQuotes(line, delimiter) }
            counts.first() > 0 && counts.all { it == counts.first() }
        } ?: DELIMITERS.first()
    }

    /** Null when a quoted field is left open, which means this is not a table. */
    private fun readRecords(content: String, delimiter: Char): List<List<String>>? {
        val records = mutableListOf<List<String>>()
        var record = mutableListOf<String>()
        val field = StringBuilder()
        var quoted = false
        var index = 0

        fun endField() {
            record.add(field.toString())
            field.setLength(0)
        }

        fun endRecord() {
            endField()
            records.add(record)
            record = mutableListOf()
        }

        while (index < content.length) {
            val character = content[index]
            when {
                quoted -> when {
                    character == QUOTE && content.getOrNull(index + 1) == QUOTE -> {
                        field.append(QUOTE)
                        index += 2
                    }

                    character == QUOTE -> {
                        quoted = false
                        index++
                    }

                    else -> {
                        field.append(character)
                        index++
                    }
                }

                character == QUOTE && field.isEmpty() -> {
                    quoted = true
                    index++
                }

                character == delimiter -> {
                    endField()
                    index++
                }

                character == '\r' -> index++

                character == '\n' -> {
                    endRecord()
                    index++
                }

                else -> {
                    field.append(character)
                    index++
                }
            }
        }
        if (quoted) return null
        if (field.isNotEmpty() || record.isNotEmpty()) endRecord()
        return records
    }

    private fun quoteIfNeeded(field: String, delimiter: Char): String {
        val needsQuotes = field.any { it == delimiter || it == QUOTE || it == '\n' || it == '\r' }
        if (!needsQuotes) return field
        return QUOTE + field.replace(QUOTE.toString(), "$QUOTE$QUOTE") + QUOTE
    }

    private fun countOutsideQuotes(line: String, delimiter: Char): Int {
        var inQuotes = false
        var count = 0
        line.forEach { character ->
            when {
                character == QUOTE -> inQuotes = !inQuotes
                character == delimiter && !inQuotes -> count++
            }
        }
        return count
    }
}
