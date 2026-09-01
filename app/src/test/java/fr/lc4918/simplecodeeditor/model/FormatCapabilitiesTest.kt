package fr.lc4918.simplecodeeditor.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FormatCapabilitiesTest {

    @Test
    fun `only markdown has a shape of its own to show`() {
        DocumentFormat.entries.forEach { format ->
            val expected = format == DocumentFormat.MARKDOWN
            assertEquals(format.name, expected, format.capabilities.preview)
        }
    }


    @Test
    fun `table mode is offered for csv only`() {
        DocumentFormat.entries.forEach { format ->
            val expected = format == DocumentFormat.CSV
            assertEquals(
                "table mode for $format",
                expected,
                format.capabilities.tableMode,
            )
        }
    }

    @Test
    fun `tree mode is offered for the nested formats only`() {
        val withTree = DocumentFormat.entries.filter { it.capabilities.treeMode }
        assertEquals(listOf(DocumentFormat.JSON, DocumentFormat.XML), withTree)
    }

    @Test
    fun `csv offers text and table but never tree`() {
        val capabilities = DocumentFormat.CSV.capabilities
        assertTrue(capabilities.textMode)
        assertTrue(capabilities.tableMode)
        assertFalse(capabilities.treeMode)
        assertEquals(listOf(ViewMode.TEXT, ViewMode.TABLE), capabilities.availableModes)
    }

    @Test
    fun `every format keeps text mode search and history`() {
        DocumentFormat.entries.forEach { format ->
            assertTrue("text mode for $format", format.capabilities.textMode)
            assertTrue("search for $format", format.capabilities.search)
            assertTrue("history for $format", format.capabilities.undoRedo)
        }
    }

    @Test
    fun `sorting is offered for json and csv only`() {
        val sortable = DocumentFormat.entries.filter { it.capabilities.sort }
        assertEquals(listOf(DocumentFormat.JSON, DocumentFormat.CSV), sortable)
    }

    @Test
    fun `html never offers a table mode`() {
        assertFalse(DocumentFormat.HTML.capabilities.supports(ViewMode.TABLE))
    }
}
