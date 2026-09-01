package fr.lc4918.simplecodeeditor.model

import androidx.annotation.StringRes
import fr.lc4918.simplecodeeditor.R

/** How a filter compares a cell with the value the user typed. */
enum class FilterOperator(@param:StringRes override val labelRes: Int) : LabelledOption {
    CONTAINS(R.string.filter_contains),
    EQUALS(R.string.filter_equals),
    GREATER(R.string.filter_greater),
    LESS(R.string.filter_less),
}
