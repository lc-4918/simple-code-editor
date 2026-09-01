package fr.lc4918.simplecodeeditor.model

import androidx.annotation.StringRes
import fr.lc4918.simplecodeeditor.R

/**
 * The four shapes the document can be copied in.
 *
 * The smart formatted variant of the reference editor is deliberately absent.
 */
enum class CopyVariant(@param:StringRes override val labelRes: Int) : LabelledOption {
    /** Indented with the width chosen in the settings. */
    FORMATTED(R.string.copy_formatted),

    /** All optional whitespace removed. */
    COMPACTED(R.string.copy_compacted),

    /** Escaped so the document can be pasted inside a string literal. */
    ESCAPED(R.string.copy_escaped),
    AS_IS(R.string.copy_as_is),
}
