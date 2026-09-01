package fr.lc4918.simplecodeeditor.format

import fr.lc4918.simplecodeeditor.model.Diagnostic
import fr.lc4918.simplecodeeditor.model.SyntaxProblem
import fr.lc4918.simplecodeeditor.model.TreeNode

/** What reading a document gave: a tree, or the reason there is none. */
sealed interface TreeReading {

    data class Tree(val tree: TreeNode) : TreeReading

    data class Refused(val reason: Diagnostic) : TreeReading
}

/** The tree, or null when the document was refused. */
val TreeReading.root: TreeNode?
    get() = (this as? TreeReading.Tree)?.tree

/** Why the document was refused, or null when it was read. */
val TreeReading.diagnostic: Diagnostic?
    get() = (this as? TreeReading.Refused)?.reason

/**
 * Thrown by a reader at the spot it gave up.
 *
 * It carries the place rather than a sentence, so the reason can be said in
 * the language of the interface and the surface can point at the character.
 */
internal class SyntaxException(
    val problem: SyntaxProblem,
    val offset: Int,
) : Exception()
