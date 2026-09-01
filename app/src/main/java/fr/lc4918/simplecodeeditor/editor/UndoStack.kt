package fr.lc4918.simplecodeeditor.editor

/**
 * Document level undo history.
 *
 * Snapshots are whole document contents rather than diffs, which keeps the tree
 * and table views on the same history as the text view. The depth is capped so a
 * long editing session on a large file cannot grow without bound.
 */
class UndoStack(private val maxDepth: Int = DEFAULT_MAX_DEPTH) {

    private val past = ArrayDeque<String>()
    private val future = ArrayDeque<String>()

    val canUndo: Boolean get() = past.isNotEmpty()
    val canRedo: Boolean get() = future.isNotEmpty()

    /** Records [previous] as a restorable state and drops any redo history. */
    fun record(previous: String) {
        if (past.lastOrNull() == previous) return
        past.addLast(previous)
        while (past.size > maxDepth) {
            past.removeFirst()
        }
        future.clear()
    }

    /** Returns the previous content, pushing [current] onto the redo history. */
    fun undo(current: String): String? {
        val restored = past.removeLastOrNull() ?: return null
        future.addLast(current)
        return restored
    }

    /** Returns the next content, pushing [current] back onto the undo history. */
    fun redo(current: String): String? {
        val restored = future.removeLastOrNull() ?: return null
        past.addLast(current)
        return restored
    }

    fun clear() {
        past.clear()
        future.clear()
    }

    companion object {
        const val DEFAULT_MAX_DEPTH = 100
    }
}
