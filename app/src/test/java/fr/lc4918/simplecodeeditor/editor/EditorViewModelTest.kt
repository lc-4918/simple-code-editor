package fr.lc4918.simplecodeeditor.editor

import fr.lc4918.simplecodeeditor.R
import fr.lc4918.simplecodeeditor.fake.FakeDocumentRepository
import fr.lc4918.simplecodeeditor.fake.FakeSettingsRepository
import fr.lc4918.simplecodeeditor.model.DocumentFormat
import fr.lc4918.simplecodeeditor.model.DocumentLocation
import fr.lc4918.simplecodeeditor.model.DocumentSource
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
    private val documents = FakeDocumentRepository()
    private var now = 0L

    private fun viewModel() = EditorViewModel(
        settings = FakeSettingsRepository(),
        documents = documents,
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
    fun `the undo and redo tools drive the history`() = runTest(dispatcher) {
        val model = viewModel()
        model.onContentChanged("value")

        model.onTool(EditorTool.UNDO)
        assertEquals("", model.uiState.value.document.content)

        model.onTool(EditorTool.REDO)
        assertEquals("value", model.uiState.value.document.content)
    }

    @Test
    fun `the format tool lays the document out and can be undone in one step`() =
        runTest(dispatcher) {
            val model = viewModel()
            model.onContentChanged("""{"a":1}""")

            model.onTool(EditorTool.INDENT)
            assertEquals("{\n  \"a\": 1\n}", model.uiState.value.document.content)

            model.undo()
            assertEquals("""{"a":1}""", model.uiState.value.document.content)
        }

    @Test
    fun `the compact tool squeezes the document`() = runTest(dispatcher) {
        val model = viewModel()
        model.onContentChanged("{\n  \"a\": 1\n}")

        model.onTool(EditorTool.COMPACT)
        assertEquals("""{"a":1}""", model.uiState.value.document.content)
    }

    @Test
    fun `a tool that changes nothing adds no step to the history`() = runTest(dispatcher) {
        val model = viewModel()
        model.setDocument(EditorDocument.empty(DocumentFormat.JSON).copy(content = """{"a":1}"""))

        model.onTool(EditorTool.COMPACT)
        assertFalse(model.uiState.value.canUndo)
    }

    @Test
    fun `the search tool toggles the search bar`() = runTest(dispatcher) {
        val model = viewModel()
        model.onTool(EditorTool.SEARCH)
        assertTrue(model.uiState.value.isSearchVisible)

        model.onTool(EditorTool.SEARCH)
        assertFalse(model.uiState.value.isSearchVisible)
    }

    @Test
    fun `theme changes are persisted and reflected in the state`() = runTest(dispatcher) {
        val model = viewModel()
        model.setTheme(ThemeOption.DARK)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(ThemeOption.DARK, model.uiState.value.theme)
    }

    @Test
    fun `the language is only reported as loaded once it has been read`() = runTest(dispatcher) {
        val model = viewModel()
        assertFalse(model.uiState.value.isLanguageLoaded)

        dispatcher.scheduler.advanceUntilIdle()
        assertTrue(model.uiState.value.isLanguageLoaded)
    }

    @Test
    fun `opening reads the content and works out the format from the name`() = runTest(dispatcher) {
        val location = DocumentLocation("content://documents/1")
        documents.stored[location.value] = DocumentSource(
            name = "track.gpx",
            mimeType = null,
            content = "<gpx></gpx>",
        )

        val model = viewModel()
        model.open(location)
        dispatcher.scheduler.advanceUntilIdle()

        val state = model.uiState.value
        assertEquals("<gpx></gpx>", state.document.content)
        assertEquals("track", state.document.name)
        assertEquals(DocumentFormat.XML, state.format)
        assertEquals(location, state.document.origin)
        assertFalse(state.document.isModified)
    }

    @Test
    fun `a document that fails to open leaves the current one alone`() = runTest(dispatcher) {
        val model = viewModel()
        model.onContentChanged("kept")

        model.open(DocumentLocation("content://documents/missing"))
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals("kept", model.uiState.value.document.content)
        assertEquals(R.string.error_open, model.uiState.value.statusMessageRes)
    }

    @Test
    fun `saving writes the content and clears the modified flag`() = runTest(dispatcher) {
        val location = DocumentLocation("content://documents/2")
        val model = viewModel()
        model.onContentChanged("written")

        model.save(location)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals("written", documents.stored.getValue(location.value).content)
        assertEquals(location, model.uiState.value.document.origin)
        assertFalse(model.uiState.value.document.isModified)
        assertEquals(R.string.status_saved, model.uiState.value.statusMessageRes)
    }

    @Test
    fun `saving to an address does not make it the origin`() = runTest(dispatcher) {
        val model = viewModel()
        model.onContentChanged("sent")

        model.saveUrl("https://example.com/data.json")
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals("sent", documents.stored.getValue("https://example.com/data.json").content)
        assertEquals(null, model.uiState.value.document.origin)
    }

    @Test
    fun `a failed save is reported and keeps the document modified`() = runTest(dispatcher) {
        documents.failure = java.io.IOException("no room")
        val model = viewModel()
        model.onContentChanged("unsaved")

        model.save(DocumentLocation("content://documents/3"))
        dispatcher.scheduler.advanceUntilIdle()

        assertTrue(model.uiState.value.document.isModified)
        assertEquals(R.string.error_save, model.uiState.value.statusMessageRes)
    }

    @Test
    fun `a shown message is not shown twice`() = runTest(dispatcher) {
        val model = viewModel()
        model.save(DocumentLocation("content://documents/4"))
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals(R.string.status_saved, model.uiState.value.statusMessageRes)

        model.statusShown()
        assertEquals(null, model.uiState.value.statusMessageRes)
    }

    @Test
    fun `format can be redetected from the content alone`() = runTest(dispatcher) {
        val model = viewModel()
        model.onContentChanged("body { color: red; }")
        model.redetectFormat()

        assertEquals(DocumentFormat.CSS, model.uiState.value.format)
    }
}
