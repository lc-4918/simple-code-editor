package fr.lc4918.simplecodeeditor.model

import androidx.annotation.StringRes
import fr.lc4918.simplecodeeditor.R

/**
 * The separator a CSV document is written with.
 *
 * A document that is opened keeps whatever separator it was written with,
 * which the reader works out on its own. This choice is what a new document
 * gets, what an opened one falls back to when nothing can be worked out, and
 * what the open document is rewritten with when the choice changes.
 */
enum class CsvDelimiter(
    val storageKey: String,
    val character: Char,
    @param:StringRes override val labelRes: Int,
) : LabelledOption {
    COMMA("comma", ',', R.string.delimiter_comma),
    SEMICOLON("semicolon", ';', R.string.delimiter_semicolon),
    TAB("tab", '\t', R.string.delimiter_tab);

    companion object {
        val DEFAULT = COMMA

        fun fromStorageKey(key: String?): CsvDelimiter =
            entries.firstOrNull { it.storageKey == key } ?: DEFAULT

        fun of(character: Char): CsvDelimiter =
            entries.firstOrNull { it.character == character } ?: DEFAULT
    }
}
