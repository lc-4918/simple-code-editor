package fr.lc4918.simplecodeeditor.model

import androidx.annotation.StringRes
import fr.lc4918.simplecodeeditor.R

/** The three ways a document can be presented in the main panel. */
enum class ViewMode(@param:StringRes val labelRes: Int) {
    /** Raw source text, with line numbers, highlighting and folding. */
    TEXT(R.string.mode_text),

    /** Hierarchical view, for the nested formats only. */
    TREE(R.string.mode_tree),

    /** Rows and columns grid, reserved for CSV. */
    TABLE(R.string.mode_table),
}
