package fr.lc4918.simplecodeeditor.editor

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UndoStackTest {

    @Test
    fun `a fresh stack has nothing to undo or redo`() {
        val stack = UndoStack()
        assertFalse(stack.canUndo)
        assertFalse(stack.canRedo)
        assertNull(stack.undo("current"))
        assertNull(stack.redo("current"))
    }

    @Test
    fun `undo then redo returns to the starting point`() {
        val stack = UndoStack()
        stack.record("one")
        stack.record("two")

        assertEquals("two", stack.undo("three"))
        assertEquals("one", stack.undo("two"))
        assertFalse(stack.canUndo)

        assertEquals("two", stack.redo("one"))
        assertEquals("three", stack.redo("two"))
        assertFalse(stack.canRedo)
    }

    @Test
    fun `recording a new state clears the redo history`() {
        val stack = UndoStack()
        stack.record("one")
        stack.undo("two")
        assertTrue(stack.canRedo)

        stack.record("edited")
        assertFalse(stack.canRedo)
    }

    @Test
    fun `identical consecutive snapshots are not recorded twice`() {
        val stack = UndoStack()
        stack.record("same")
        stack.record("same")

        assertEquals("same", stack.undo("current"))
        assertFalse(stack.canUndo)
    }

    @Test
    fun `depth is capped and the oldest entries are dropped`() {
        val stack = UndoStack(maxDepth = 3)
        listOf("a", "b", "c", "d", "e").forEach(stack::record)

        assertEquals("e", stack.undo("f"))
        assertEquals("d", stack.undo("e"))
        assertEquals("c", stack.undo("d"))
        assertFalse(stack.canUndo)
    }
}
