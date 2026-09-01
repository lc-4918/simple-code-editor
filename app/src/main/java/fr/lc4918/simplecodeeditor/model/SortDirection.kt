package fr.lc4918.simplecodeeditor.model

import androidx.annotation.StringRes
import fr.lc4918.simplecodeeditor.R

/** Which way a sort runs. */
enum class SortDirection(@param:StringRes override val labelRes: Int) : LabelledOption {
    ASCENDING(R.string.sort_ascending),
    DESCENDING(R.string.sort_descending),
}
