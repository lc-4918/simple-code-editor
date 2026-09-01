package fr.lc4918.simplecodeeditor.format

import fr.lc4918.simplecodeeditor.model.FilterOperator

/**
 * How two values written as text compare, and whether one passes a filter.
 *
 * Values that read as numbers are compared as numbers, so that ten comes after
 * nine rather than before it. Anything else is compared as text without regard
 * to case, which is what someone sorting a column of names expects. The rule
 * is shared by the grid and by the hierarchy so that both order alike.
 */
object ValueOrder {

    fun compare(left: String, right: String): Int {
        val leftNumber = left.trim().toDoubleOrNull()
        val rightNumber = right.trim().toDoubleOrNull()
        return if (leftNumber != null && rightNumber != null) {
            leftNumber.compareTo(rightNumber)
        } else {
            left.compareTo(right, ignoreCase = true)
        }
    }

    fun matches(subject: String, operator: FilterOperator, value: String): Boolean =
        when (operator) {
            FilterOperator.CONTAINS -> subject.contains(value, ignoreCase = true)
            FilterOperator.EQUALS -> subject.equals(value, ignoreCase = true)
            FilterOperator.GREATER -> compare(subject, value) > 0
            FilterOperator.LESS -> compare(subject, value) < 0
        }
}
