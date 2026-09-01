package fr.lc4918.simplecodeeditor.model

import androidx.annotation.StringRes

/** Anything the interface offers as a named choice, in a menu or in a selector. */
interface LabelledOption {
    @get:StringRes
    val labelRes: Int
}
