package fr.lc4918.simplecodeeditor.editor

import fr.lc4918.simplecodeeditor.R
import fr.lc4918.simplecodeeditor.fake.FakeDocumentRepository
import fr.lc4918.simplecodeeditor.fake.FakeSettingsRepository
import fr.lc4918.simplecodeeditor.model.DocumentFormat
import fr.lc4918.simplecodeeditor.model.CsvDelimiter
import fr.lc4918.simplecodeeditor.model.DocumentLocation
import fr.lc4918.simplecodeeditor.model.DocumentProblem
import fr.lc4918.simplecodeeditor.model.NodeKind
import fr.lc4918.simplecodeeditor.model.DocumentSource
import fr.lc4918.simplecodeeditor.format.JsonTree
import fr.lc4918.simplecodeeditor.model.EditorDocument
import fr.lc4918.simplecodeeditor.model.FilterOperator
import fr.lc4918.simplecodeeditor.model.SortDirection
import fr.lc4918.simplecodeeditor.ui.Where
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
    fun `changing a cell rewrites the document`() = runTest(dispatcher) {
        val model = viewModel()
        model.setDocument(
            EditorDocument.empty(DocumentFormat.CSV).copy(content = "name,city\nada,lyon"),
        )

        model.onCellChanged(row = 0, column = 1, value = "oslo")

        assertEquals("name,city\nada,oslo", model.uiState.value.document.content)
    }

    @Test
    fun `adding a row appends an empty one of the right width`() = runTest(dispatcher) {
        val model = viewModel()
        model.setDocument(
            EditorDocument.empty(DocumentFormat.CSV).copy(content = "a,b\n1,2"),
        )

        model.addRow()

        assertEquals("a,b\n1,2\n,", model.uiState.value.document.content)
    }

    @Test
    fun `sorting reorders the rows and leaves the header alone`() = runTest(dispatcher) {
        val model = viewModel()
        model.setDocument(
            EditorDocument.empty(DocumentFormat.CSV).copy(content = "n,s\nada,9\nlinus,10"),
        )

        model.sort(chosen = 1, direction = SortDirection.DESCENDING)

        assertEquals("n,s\nlinus,10\nada,9", model.uiState.value.document.content)
    }

    @Test
    fun `filtering drops the other rows and one undo brings them back`() = runTest(dispatcher) {
        val model = viewModel()
        model.setDocument(
            EditorDocument.empty(DocumentFormat.CSV).copy(content = "n,s\nada,9\nlinus,10"),
        )

        model.filter(chosen = 0, operator = FilterOperator.CONTAINS, value = "ada")
        assertEquals("n,s\nada,9", model.uiState.value.document.content)

        model.undo()
        assertEquals("n,s\nada,9\nlinus,10", model.uiState.value.document.content)
    }

    @Test
    fun `a table operation on a document that is not one changes nothing`() = runTest(dispatcher) {
        val model = viewModel()
        model.setDocument(EditorDocument.empty(DocumentFormat.CSV).copy(content = ""))

        model.addRow()

        assertEquals("", model.uiState.value.document.content)
    }

    @Test
    fun `sorting a JSON array uses the member the dialog offered`() = runTest(dispatcher) {
        val model = viewModel()
        model.setDocument(
            EditorDocument.empty(DocumentFormat.JSON)
                .copy(content = """[{"n":"b","s":9},{"n":"a","s":10}]"""),
        )

        model.sort(chosen = 0, direction = SortDirection.ASCENDING)

        assertEquals(
            """[{"n":"a","s":10},{"n":"b","s":9}]""",
            model.uiState.value.document.content,
        )
    }

    @Test
    fun `filtering a JSON array drops the elements that do not match`() = runTest(dispatcher) {
        val model = viewModel()
        model.setDocument(
            EditorDocument.empty(DocumentFormat.JSON)
                .copy(content = """[{"n":"ada"},{"n":"linus"}]"""),
        )

        model.filter(chosen = 0, operator = FilterOperator.CONTAINS, value = "ada")

        assertEquals("""[{"n":"ada"}]""", model.uiState.value.document.content)
    }

    @Test
    fun `a laid out JSON document stays laid out after a sort`() = runTest(dispatcher) {
        val model = viewModel()
        model.setDocument(
            EditorDocument.empty(DocumentFormat.JSON).copy(content = "[\n  2,\n  1\n]"),
        )

        model.sort(chosen = 0, direction = SortDirection.ASCENDING)

        assertEquals("[\n  1,\n  2\n]", model.uiState.value.document.content)
    }

    @Test
    fun `choosing a separator rewrites the open document with it`() = runTest(dispatcher) {
        val model = viewModel()
        model.setDocument(
            EditorDocument.empty(DocumentFormat.CSV).copy(content = "nom,ville\nada,lyon"),
        )

        model.setCsvDelimiter(CsvDelimiter.SEMICOLON)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals("nom;ville\nada;lyon", model.uiState.value.document.content)
        assertEquals(CsvDelimiter.SEMICOLON, model.uiState.value.csvDelimiter)
    }

    @Test
    fun `choosing a separator leaves a document that is not a table alone`() = runTest(dispatcher) {
        val model = viewModel()
        model.setDocument(
            EditorDocument.empty(DocumentFormat.JSON).copy(content = """{"a":1}"""),
        )

        model.setCsvDelimiter(CsvDelimiter.TAB)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals("""{"a":1}""", model.uiState.value.document.content)
    }

    @Test
    fun `a document that cannot be read is reported once the typing pauses`() =
        runTest(dispatcher) {
            val model = viewModel()
            model.setDocument(
                EditorDocument.empty(DocumentFormat.JSON).copy(content = """{"a":}"""),
            )
            dispatcher.scheduler.advanceUntilIdle()

            val diagnostic = model.uiState.value.diagnostic!!
            assertEquals(DocumentProblem.UNEXPECTED_CHARACTER, diagnostic.problem)
            assertEquals(1, diagnostic.line)
        }

    @Test
    fun `a document that reads has nothing reported`() = runTest(dispatcher) {
        val model = viewModel()
        model.setDocument(EditorDocument.empty(DocumentFormat.JSON).copy(content = """{"a":1}"""))
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(null, model.uiState.value.diagnostic)
    }

    @Test
    fun `an empty document is not reported as wrong`() = runTest(dispatcher) {
        val model = viewModel()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(null, model.uiState.value.diagnostic)
    }

    @Test
    fun `a report is dropped once the document reads again`() = runTest(dispatcher) {
        val model = viewModel()
        model.setDocument(EditorDocument.empty(DocumentFormat.JSON).copy(content = """{"a":"""))
        dispatcher.scheduler.advanceUntilIdle()
        assertTrue(model.uiState.value.diagnostic != null)

        model.onContentChanged("""{"a":1}""")
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(null, model.uiState.value.diagnostic)
    }

    @Test
    fun `a format with no reader of its own is never reported`() = runTest(dispatcher) {
        val model = viewModel()
        model.setDocument(
            EditorDocument.empty(DocumentFormat.CSS).copy(content = "body { never closed"),
        )
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(null, model.uiState.value.diagnostic)
    }

    @Test
    fun `renaming into another format has the document read again`() = runTest(dispatcher) {
        val model = viewModel()
        model.setDocument(EditorDocument.empty(DocumentFormat.CSS).copy(content = "{oops"))
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals(null, model.uiState.value.diagnostic)

        model.onDocumentNameChanged("thing.json")
        dispatcher.scheduler.advanceUntilIdle()

        assertTrue(model.uiState.value.diagnostic != null)
    }

    @Test
    fun `a repaired document replaces the broken one in a single step`() = runTest(dispatcher) {
        val model = viewModel()
        model.setDocument(EditorDocument.empty(DocumentFormat.JSON).copy(content = "{a:1,}"))
        dispatcher.scheduler.advanceUntilIdle()

        model.applyRepair("""{"a":1}""")
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals("""{"a":1}""", model.uiState.value.document.content)
        assertEquals(R.string.status_repaired, model.uiState.value.statusMessageRes)
        assertEquals(null, model.uiState.value.diagnostic)

        model.undo()
        assertEquals("{a:1,}", model.uiState.value.document.content)
    }

    @Test
    fun `a repair that answers nothing leaves the document alone`() = runTest(dispatcher) {
        val model = viewModel()
        model.setDocument(EditorDocument.empty(DocumentFormat.JSON).copy(content = "{oops"))

        model.applyRepair(null)

        assertEquals("{oops", model.uiState.value.document.content)
        assertEquals(R.string.error_repair, model.uiState.value.statusMessageRes)
    }

    @Test
    fun `a repair that changes nothing is reported as a failure`() = runTest(dispatcher) {
        val model = viewModel()
        model.setDocument(EditorDocument.empty(DocumentFormat.JSON).copy(content = "{oops"))

        model.applyRepair("{oops")

        assertEquals(R.string.error_repair, model.uiState.value.statusMessageRes)
    }

    @Test
    fun `a document that reads as JSON can still break the rules of GeoJSON`() =
        runTest(dispatcher) {
            val model = viewModel()
            model.setDocument(
                EditorDocument.empty(DocumentFormat.JSON).copy(
                    name = "export.geojson",
                    content = """{"type":"Point","coordinates":[999,0]}""",
                ),
            )
            dispatcher.scheduler.advanceUntilIdle()

            assertEquals(
                DocumentProblem.LONGITUDE_OUT_OF_RANGE,
                model.uiState.value.diagnostic?.problem,
            )
        }

    @Test
    fun `the same document is left alone when nothing says it is GeoJSON`() =
        runTest(dispatcher) {
            val model = viewModel()
            model.setDocument(
                EditorDocument.empty(DocumentFormat.JSON).copy(
                    name = "numbers",
                    content = """{"kind":"Point","coordinates":[999,0]}""",
                ),
            )
            dispatcher.scheduler.advanceUntilIdle()

            assertEquals(null, model.uiState.value.diagnostic)
        }

    @Test
    fun `a track is checked against the shape of GPX once it reads as XML`() =
        runTest(dispatcher) {
            val model = viewModel()
            model.setDocument(
                EditorDocument.empty(DocumentFormat.XML)
                    .copy(content = """<gpx><trkpt lat="45.76"/></gpx>"""),
            )
            dispatcher.scheduler.advanceUntilIdle()

            assertEquals(
                DocumentProblem.COORDINATE_MISSING,
                model.uiState.value.diagnostic?.problem,
            )
        }

    @Test
    fun `editing a value in the tree changes the document and nothing else`() =
        runTest(dispatcher) {
            val source = "{\n  \"a\": 1,\n  \"b\": 2\n}"
            val model = viewModel()
            model.setDocument(EditorDocument.empty(DocumentFormat.JSON).copy(content = source))

            val node = JsonTree.parse(source)!!.children.first { it.name == "a" }
            model.onTreeValueTyped(node, "9")

            assertEquals("{\n  \"a\": 9,\n  \"b\": 2\n}", model.uiState.value.document.content)
        }

    @Test
    fun `editing in the tree is one step in the history`() = runTest(dispatcher) {
        val source = """{"a":1}"""
        val model = viewModel()
        model.setDocument(EditorDocument.empty(DocumentFormat.JSON).copy(content = source))

        val node = JsonTree.parse(source)!!.children.single()
        model.onTreeValueTyped(node, "hello")
        assertEquals("""{"a":"hello"}""", model.uiState.value.document.content)

        model.undo()
        assertEquals(source, model.uiState.value.document.content)
    }

    @Test
    fun `renaming a key in the tree keeps the value`() = runTest(dispatcher) {
        val source = """{"a":1,"b":2}"""
        val model = viewModel()
        model.setDocument(EditorDocument.empty(DocumentFormat.JSON).copy(content = source))

        val node = JsonTree.parse(source)!!.children.first { it.name == "a" }
        model.onTreeNameTyped(node, "z")

        assertEquals("""{"z":1,"b":2}""", model.uiState.value.document.content)
    }

    @Test
    fun `a node with nowhere to write leaves the document alone`() = runTest(dispatcher) {
        val source = """{"a":{"b":1}}"""
        val model = viewModel()
        model.setDocument(EditorDocument.empty(DocumentFormat.JSON).copy(content = source))

        val container = JsonTree.parse(source)!!.children.single()
        model.onTreeValueTyped(container, "9")

        assertEquals(source, model.uiState.value.document.content)
    }

    @Test
    fun `removing a node from the tree is one step in the history`() = runTest(dispatcher) {
        val source = """{"a":1,"b":2}"""
        val model = viewModel()
        model.setDocument(EditorDocument.empty(DocumentFormat.JSON).copy(content = source))

        model.removeNode(JsonTree.parse(source)!!.children.first())
        assertEquals("""{"b":2}""", model.uiState.value.document.content)

        model.undo()
        assertEquals(source, model.uiState.value.document.content)
    }

    @Test
    fun `duplicating a member gives the copy a key of its own`() = runTest(dispatcher) {
        val source = """{"a":1}"""
        val model = viewModel()
        model.setDocument(EditorDocument.empty(DocumentFormat.JSON).copy(content = source))

        model.duplicateNode(JsonTree.parse(source)!!.children.single())

        assertEquals("""{"a":1,"a copy":1}""", model.uiState.value.document.content)
        assertEquals(null, model.uiState.value.diagnostic)
    }

    @Test
    fun `extracting a subtree keeps it alone`() = runTest(dispatcher) {
        val source = """{"a":{"b":1},"c":2}"""
        val model = viewModel()
        model.setDocument(EditorDocument.empty(DocumentFormat.JSON).copy(content = source))

        model.extractNode(JsonTree.parse(source)!!.children.first().children.single())

        assertEquals(""""b":1""", model.uiState.value.document.content)
    }

    @Test
    fun `inserting after a node puts the text beside it`() = runTest(dispatcher) {
        val source = """{"a":1}"""
        val model = viewModel()
        model.setDocument(EditorDocument.empty(DocumentFormat.JSON).copy(content = source))

        model.insertNode(JsonTree.parse(source)!!.children.single(), Where.AFTER, """"b":2""")

        assertEquals("""{"a":1,"b":2}""", model.uiState.value.document.content)
    }

    @Test
    fun `a move that has no meaning leaves the document alone`() = runTest(dispatcher) {
        val source = """{"a":1}"""
        val model = viewModel()
        model.setDocument(EditorDocument.empty(DocumentFormat.JSON).copy(content = source))

        model.removeNode(fr.lc4918.simplecodeeditor.model.TreeNode("x", NodeKind.STRING, "y"))

        assertEquals(source, model.uiState.value.document.content)
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
