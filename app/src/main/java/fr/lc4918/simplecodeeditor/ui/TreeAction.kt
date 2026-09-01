package fr.lc4918.simplecodeeditor.ui

import fr.lc4918.simplecodeeditor.model.NodeKind

/** Where a node goes in relation to the one the menu was opened on. */
enum class Where { BEFORE, AFTER, INTO }

/** What the menu of a node in the tree asks for. */
sealed interface TreeAction {
    data object Copy : TreeAction
    data object Cut : TreeAction
    data object Duplicate : TreeAction
    data object Extract : TreeAction
    data object Remove : TreeAction
    data class Paste(val where: Where) : TreeAction
    data class Insert(val where: Where, val kind: NodeKind) : TreeAction
}
