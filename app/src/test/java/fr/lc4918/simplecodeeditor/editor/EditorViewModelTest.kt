package fr.lc4918.simplecodeeditor.editor

import fr.lc4918.simplecodeeditor.fake.FakeSettingsRepository
import fr.lc4918.simplecodeeditor.model.DocumentFormat
import fr.lc4918.simplecodeeditor.model.EditorDocument
import fr.lc4918.simplecodeeditor.model.ThemeOption
import fr.lc4918.simplecodeeditor.model.ViewMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class EditorViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private var now = 0L

    private fun viewModel() = EditorViewModel(
        settings = FakeSettingsRepository(),
        clock = { now },
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        now = 0L
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `editing marks the document as modified`() = runTest(dispatcher) {
        val model = viewModel()
        model.onContentChanged("{}")

        assertEquals("{}", model.uiState.value.document.content)
        assertTrue(model.uiState.value.document.isModified)
    }

    @Test
    fun `edits inside the coalescing window share one undo entry`() = runTest(dispatcher) {
        val model = viewModel()
        model.onContentChanged("a")
        now += 10
        model.onContentChanged("ab")
        now += 10
        model.onContentChanged("abc")

        model.undo()
        assertEquals("", model.uiState.value.document.content)
        assertFalse(model.uiState.value.canUndo)
    }

    @Test
    fun `edits beyond the coalescing window are separate undo entries`() = runTest(dispatcher) {
        val model = viewModel()
        model.onContentChanged("first")
        now += EditorViewModel.UNDO_COALESCE_MILLIS + 1
        model.onContentChanged("second")

        model.undo()
        assertEquals("first", model.uiState.value.document.content)
        model.undo()
        assertEquals("", model.uiState.value.document.content)
    }

    @Test
    fun `redo replays an undone edit`() = runTest(dispatcher) {
        val model = viewModel()
        model.onContentChanged("value")
        model.undo()
        assertTrue(model.uiState.value.canRedo)

        model.redo()
        assertEquals("value", model.uiState.value.document.content)
    }

    @Test
    fun `renaming with a known extension switches the format`() = runTest(dispatcher) {
        val model = viewModel()
        model.onDocumentNameChanged("track.gpx")

        assertEquals(DocumentFormat.XML, model.uiState.value.format)
    }

    @Test
    fun `an unusable view mode falls back to text`() = runTest(dispatcher) {
        val model = viewModel()
        model.setDocument(EditorDocument.empty(DocumentFormat.CSV))
        model.setViewMode(ViewMode.TABLE)
        assertEquals(ViewMode.TABLE, model.uiState.value.viewMode)

        model.setDocument(EditorDocument.empty(DocumentFormat.HTML))
        assertEquals(ViewMode.TEXT, model.uiState.value.viewMode)
    }

    @Test
    fun `a view mode the format does not offer is refused`() = runTest(dispatcher) {
        val model = viewModel()
        model.setDocument(EditorDocument.empty(DocumentFormat.HTML))
        model.setViewMode(ViewMode.TREE)

        assertEquals(ViewMode.TEXT, model.uiState.value.viewMode)
    }

    @Test
    fun `opening a document clears the history`() = runTest(dispatcher) {
        val model = viewModel()
        model.onContentChanged("something")
        assertTrue(model.uiState.value.canUndo)

        model.setDocument(EditorDocument.empty(DocumentFormat.JSON))
        assertFalse(model.uiState.value.canUndo)
        assertFalse(model.uiState.value.canRedo)
    }

    @Test
    fun `tools follow the open format`() = runTest(dispatcher) {
        val model = viewModel()
        model.setDocument(EditorDocument.empty(DocumentFormat.CSV))
        val csv = model.uiState.value
        assertFalse(csv.isToolVisible(EditorTool.INDENT))
        assertTrue(csv.isToolVisible(EditorTool.SORT))

        model.setDocument(EditorDocument.empty(DocumentFormat.CSS))
        val css = model.uiState.value
        assertTrue(css.isToolVisible(EditorTool.INDENT))
        assertFalse(css.isToolVisible(EditorTool.SORT))
    }

    @Test
    fun `theme changes are persisted and reflected in the state`() = runTest(dispatcher) {
        val model = viewModel()
        model.setTheme(ThemeOption.DARK)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(ThemeOption.DARK, model.uiState.value.theme)
    }

    @Test
    fun `format can be redetected from the content alone`() = runTest(dispatcher) {
        val model = viewModel()
        model.onContentChanged("body { color: red; }")
        model.redetectFormat()

        assertEquals(DocumentFormat.CSS, model.uiState.value.format)
    }
}
