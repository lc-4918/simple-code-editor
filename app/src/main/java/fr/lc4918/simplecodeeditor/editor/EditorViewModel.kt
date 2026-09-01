package fr.lc4918.simplecodeeditor.editor

import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import fr.lc4918.simplecodeeditor.R
import fr.lc4918.simplecodeeditor.data.AndroidDocumentRepository
import fr.lc4918.simplecodeeditor.data.DataStoreSettingsRepository
import fr.lc4918.simplecodeeditor.data.DocumentRepository
import fr.lc4918.simplecodeeditor.data.SettingsRepository
import fr.lc4918.simplecodeeditor.format.CsvParser
import fr.lc4918.simplecodeeditor.format.CsvTransform
import fr.lc4918.simplecodeeditor.format.DocumentFormatter
import fr.lc4918.simplecodeeditor.format.DerivedFormatDetector
import fr.lc4918.simplecodeeditor.format.GeoJsonValidator
import fr.lc4918.simplecodeeditor.format.GpxValidator
import fr.lc4918.simplecodeeditor.format.JsonTransform
import fr.lc4918.simplecodeeditor.format.KmlValidator
import fr.lc4918.simplecodeeditor.format.TreeEdit
import fr.lc4918.simplecodeeditor.format.JsonTree
import fr.lc4918.simplecodeeditor.format.XmlTree
import fr.lc4918.simplecodeeditor.format.diagnostic
import fr.lc4918.simplecodeeditor.format.root
import fr.lc4918.simplecodeeditor.format.JsonWriter
import fr.lc4918.simplecodeeditor.format.FormatDetector
import fr.lc4918.simplecodeeditor.model.AppLanguage
import fr.lc4918.simplecodeeditor.model.CsvDelimiter
import fr.lc4918.simplecodeeditor.model.DerivedFormat
import fr.lc4918.simplecodeeditor.model.CsvTable
import fr.lc4918.simplecodeeditor.model.Diagnostic
import fr.lc4918.simplecodeeditor.model.FilterOperator
import fr.lc4918.simplecodeeditor.model.SortDirection
import fr.lc4918.simplecodeeditor.model.TreeNode
import fr.lc4918.simplecodeeditor.model.DocumentFormat
import fr.lc4918.simplecodeeditor.model.DocumentLocation
import fr.lc4918.simplecodeeditor.model.DocumentSource
import fr.lc4918.simplecodeeditor.model.EditorDocument
import fr.lc4918.simplecodeeditor.model.ThemeOption
import fr.lc4918.simplecodeeditor.model.ViewMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Holds the open document and the user settings for the editor screen. */
@OptIn(kotlinx.coroutines.FlowPreview::class)
class EditorViewModel(
    private val settings: SettingsRepository,
    private val documents: DocumentRepository,
    private val clock: () -> Long = SystemClock::elapsedRealtime,
) : ViewModel() {

    private val undoStack = UndoStack()
    /** Null until the first edit, so the opening state is always recorded. */
    private var lastRecordedAt: Long? = null

    private val _uiState = MutableStateFlow(EditorUiState())
    val uiState: StateFlow<EditorUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            settings.theme.collect { option -> _uiState.update { it.copy(theme = option) } }
        }
        viewModelScope.launch {
            settings.language.collect { value ->
                _uiState.update { it.copy(language = value, isLanguageLoaded = true) }
            }
        }
        viewModelScope.launch {
            settings.indentWidth.collect { width -> _uiState.update { it.copy(indentWidth = width) } }
        }
        viewModelScope.launch {
            settings.csvDelimiter.collect { value ->
                _uiState.update { it.copy(csvDelimiter = value) }
            }
        }
        // Reading the state rather than being told covers every way the
        // document changes at once: typing, pasting, opening, renaming into
        // another format, and the tools that rewrite it.
        viewModelScope.launch {
            uiState.map { Triple(it.document.content, it.format, it.document.name) }
                .distinctUntilChanged()
                .debounce(VALIDATION_DELAY_MILLIS)
                .collect { (content, format, name) ->
                    val diagnostic = validate(content, format, name)
                    _uiState.update { it.copy(diagnostic = diagnostic) }
                }
        }
    }

    /**
     * What is wrong with the document, when anything is.
     *
     * A document with nothing in it yet is not wrong, only empty, so it is
     * left alone rather than greeted with an error.
     */
    private fun validate(content: String, format: DocumentFormat, name: String): Diagnostic? {
        if (content.isBlank()) return null
        val reading = when (format) {
            DocumentFormat.JSON -> JsonTree.read(content)
            DocumentFormat.XML -> XmlTree.read(content)
            else -> return null
        }
        reading.diagnostic?.let { return it }
        val root = reading.root ?: return null

        // The grammar holds. A document written for one of the formats built
        // on it has a second set of rules to answer to, which a document that
        // reads perfectly well can still break.
        val derived = DerivedFormatDetector.detect(name, content)?.takeIf { it.base == format }
        return when (derived) {
            DerivedFormat.GEOJSON -> GeoJsonValidator.validate(content, root)
            DerivedFormat.GPX -> GpxValidator.validate(content, root)
            DerivedFormat.KML -> KmlValidator.validate(content, root)
            null -> null
        }
    }

    // Document editing

    fun onContentChanged(content: String) {
        val current = _uiState.value
        if (current.document.content == content) return

        val now = clock()
        val previousRecord = lastRecordedAt
        if (previousRecord == null || now - previousRecord > UNDO_COALESCE_MILLIS) {
            undoStack.record(current.document.content)
            lastRecordedAt = now
        }

        _uiState.update {
            it.copy(
                document = it.document.copy(content = content, isModified = true),
                canUndo = undoStack.canUndo,
                canRedo = undoStack.canRedo,
            )
        }
    }

    fun onDocumentNameChanged(name: String) {
        val trimmed = name.trim().ifEmpty { EditorDocument.DEFAULT_NAME }
        _uiState.update { state ->
            val extension = trimmed.substringAfterLast('.', "")
            val format = DocumentFormat.fromExtension(extension) ?: state.document.format
            state.copy(document = state.document.copy(name = trimmed, format = format))
                .coerceViewMode()
        }
    }

    /** Replaces the whole document, for instance after an open or a new action. */
    fun setDocument(document: EditorDocument) {
        undoStack.clear()
        lastRecordedAt = null
        _uiState.update {
            it.copy(document = document, canUndo = false, canRedo = false).coerceViewMode()
        }
    }

    fun newDocument(format: DocumentFormat = DocumentFormat.JSON) {
        setDocument(EditorDocument.empty(format))
    }

    /** Re-runs format detection on the current content, ignoring the file name. */
    fun redetectFormat() {
        _uiState.update { state ->
            val detected = FormatDetector.detectFromContent(state.document.content)
            state.copy(document = state.document.copy(format = detected)).coerceViewMode()
        }
    }

    // Storage

    /** Reads a document from a location handed over by the storage picker. */
    fun open(location: DocumentLocation) {
        load(origin = location) { documents.read(location) }
    }

    /** Reads a document from an address. */
    fun openUrl(url: String) {
        load(origin = null) { documents.read(url) }
    }

    /** Writes the document to a location, which becomes the one it came from. */
    fun save(location: DocumentLocation) {
        store(origin = location) { documents.write(location, _uiState.value.document.content) }
    }

    /** Writes the document to an address, which it cannot be reopened from. */
    fun saveUrl(url: String) {
        store(origin = null) { documents.write(url, _uiState.value.document.content) }
    }

    private fun load(origin: DocumentLocation?, read: suspend () -> DocumentSource) {
        viewModelScope.launch {
            runCatching { read() }
                .onSuccess { source -> setDocument(source.toDocument(origin)) }
                .onFailure { setStatus(R.string.error_open) }
        }
    }

    private fun store(origin: DocumentLocation?, write: suspend () -> Unit) {
        viewModelScope.launch {
            runCatching { write() }
                .onSuccess {
                    _uiState.update { state ->
                        state.copy(
                            document = state.document.copy(
                                origin = origin ?: state.document.origin,
                                isModified = false,
                            ),
                            statusMessageRes = R.string.status_saved,
                        )
                    }
                }
                .onFailure { setStatus(R.string.error_save) }
        }
    }

    private fun DocumentSource.toDocument(origin: DocumentLocation?): EditorDocument {
        val format = FormatDetector.detect(
            fileName = name,
            mimeType = mimeType,
            content = content,
        )
        return EditorDocument(
            name = name?.substringBeforeLast('.')?.ifEmpty { null } ?: EditorDocument.DEFAULT_NAME,
            content = content,
            format = format,
            origin = origin,
        )
    }

    private fun setStatus(messageRes: Int) {
        _uiState.update { it.copy(statusMessageRes = messageRes) }
    }

    /** Called once the message has been shown, so it is not shown again. */
    fun statusShown() {
        _uiState.update { it.copy(statusMessageRes = null) }
    }

    // History

    fun undo() {
        val current = _uiState.value.document.content
        val restored = undoStack.undo(current) ?: return
        lastRecordedAt = null
        _uiState.update {
            it.copy(
                document = it.document.copy(content = restored, isModified = true),
                canUndo = undoStack.canUndo,
                canRedo = undoStack.canRedo,
            )
        }
    }

    fun redo() {
        val current = _uiState.value.document.content
        val restored = undoStack.redo(current) ?: return
        lastRecordedAt = null
        _uiState.update {
            it.copy(
                document = it.document.copy(content = restored, isModified = true),
                canUndo = undoStack.canUndo,
                canRedo = undoStack.canRedo,
            )
        }
    }

    /**
     * Runs a tool of the second toolbar row.
     *
     * Folding, sorting and filtering never reach here: the screen takes them
     * first, since it holds the fold state and asks the questions the last two
     * need answered before they can run.
     */
    fun onTool(tool: EditorTool) {
        when (tool) {
            EditorTool.UNDO -> undo()
            EditorTool.REDO -> redo()
            EditorTool.SEARCH -> setSearchVisible(!_uiState.value.isSearchVisible)
            EditorTool.INDENT -> rewrite { state ->
                DocumentFormatter.indent(state.document.content, state.format, state.indentWidth)
            }

            EditorTool.COMPACT -> rewrite { state ->
                DocumentFormatter.compact(state.document.content, state.format)
            }

            EditorTool.EXPAND_ALL,
            EditorTool.COLLAPSE_ALL,
            EditorTool.SORT,
            EditorTool.FILTER,
            -> Unit
        }
    }

    /**
     * Replaces the content with a rewritten version of itself.
     *
     * The coalescing window is closed first, so that a rewrite is always its
     * own step in the history rather than joining the keystroke before it.
     */
    private fun rewrite(transform: (EditorUiState) -> String) {
        val state = _uiState.value
        val rewritten = transform(state)
        if (rewritten == state.document.content) return
        lastRecordedAt = null
        onContentChanged(rewritten)
    }

    // Hierarchy

    /**
     * Sets the name of one node, in the document rather than over it.
     *
     * A name or a value that changes nothing, or that has nowhere in the
     * document to be written, leaves the document as it was.
     */
    fun onTreeNameTyped(node: TreeNode, typed: String) {
        val state = _uiState.value
        val edited = TreeEdit.withName(state.document.content, state.format, node, typed) ?: return
        rewrite { edited }
    }

    fun onTreeValueTyped(node: TreeNode, typed: String) {
        val state = _uiState.value
        val edited = TreeEdit.withValue(state.document.content, state.format, node, typed) ?: return
        rewrite { edited }
    }

    // Table

    /** Replaces one cell of the grid, which rewrites the whole document. */
    fun onCellChanged(row: Int, column: Int, value: String) {
        rewriteTable { table -> table.withCell(row, column, value) }
    }

    fun addRow() {
        rewriteTable { table -> table.copy(rows = table.rows + listOf(List(table.columnCount) { "" })) }
    }

    /**
     * Reorders the document, by column for a grid and by member for a tree.
     *
     * The dialog hands back which of the offered names was chosen, which is a
     * column for CSV and a member of the elements for JSON.
     */
    fun sort(chosen: Int, direction: SortDirection) {
        when (_uiState.value.format) {
            DocumentFormat.CSV ->
                rewriteTable { table -> CsvTransform.sort(table, chosen, direction) }

            DocumentFormat.JSON ->
                rewriteJson(chosen) { root, field -> JsonTransform.sort(root, field, direction) }

            else -> Unit
        }
    }

    /** Drops what the filter does not keep, which one undo brings back. */
    fun filter(chosen: Int, operator: FilterOperator, value: String) {
        when (_uiState.value.format) {
            DocumentFormat.CSV ->
                rewriteTable { table -> CsvTransform.filter(table, chosen, operator, value) }

            DocumentFormat.JSON -> rewriteJson(chosen) { root, field ->
                JsonTransform.filter(root, field, operator, value)
            }

            else -> Unit
        }
    }

    /**
     * Reads the document as a tree, transforms it, and writes it back.
     *
     * A document written on one line is written back on one line, so that
     * sorting does not quietly lay out a compact document.
     */
    private fun rewriteJson(chosen: Int, transform: (TreeNode, String?) -> TreeNode) {
        val state = _uiState.value
        val root = JsonTree.parse(state.document.content) ?: return
        val field = JsonTransform.fields(root).getOrNull(chosen)
        val width = if (state.document.content.contains('\n')) state.indentWidth else null
        rewrite { JsonWriter.write(transform(root, field), width) }
    }

    private fun rewriteTable(transform: (CsvTable) -> CsvTable) {
        val state = _uiState.value
        if (state.format != DocumentFormat.CSV) return
        val table = CsvParser.parse(state.document.content, state.csvDelimiter.character) ?: return
        rewrite { CsvParser.format(transform(table)) }
    }

    /**
     * Puts back a document the bundle managed to repair.
     *
     * A repair that changes nothing did not work, whatever it answered, so it
     * is reported as a failure rather than announced as a success.
     */
    fun applyRepair(repaired: String?) {
        if (repaired == null || repaired == _uiState.value.document.content) {
            setStatus(R.string.error_repair)
            return
        }
        rewrite { repaired }
        setStatus(R.string.status_repaired)
    }

    /** Reports that the document has just been put on the clipboard. */
    fun reportCopied() {
        setStatus(R.string.status_copied)
    }

    // View state

    fun setViewMode(mode: ViewMode) {
        if (!_uiState.value.capabilities.supports(mode)) return
        _uiState.update { it.copy(viewMode = mode) }
    }

    fun setSearchVisible(visible: Boolean) {
        _uiState.update {
            it.copy(isSearchVisible = visible, searchQuery = if (visible) it.searchQuery else "")
        }
    }

    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    // Settings

    fun setTheme(option: ThemeOption) {
        viewModelScope.launch { settings.setTheme(option) }
    }

    fun setLanguage(language: AppLanguage) {
        viewModelScope.launch { settings.setLanguage(language) }
    }

    fun setIndentWidth(width: Int) {
        viewModelScope.launch { settings.setIndentWidth(width) }
    }

    /**
     * Chooses the separator, and rewrites the open document with it.
     *
     * Without the rewrite the choice would only show on the next document,
     * which is not what picking a separator while looking at a grid means.
     */
    fun setCsvDelimiter(delimiter: CsvDelimiter) {
        viewModelScope.launch { settings.setCsvDelimiter(delimiter) }
        rewriteTable { table -> table.copy(delimiter = delimiter.character) }
    }

    /** Falls back to the text mode when the current one is not offered by the format. */
    private fun EditorUiState.coerceViewMode(): EditorUiState =
        if (capabilities.supports(viewMode)) this else copy(viewMode = ViewMode.TEXT)

    companion object {
        /** Keystrokes closer together than this share a single undo entry. */
        const val UNDO_COALESCE_MILLIS = 700L

        /** Reading the whole document waits for the typing to pause. */
        const val VALIDATION_DELAY_MILLIS = 300L

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = checkNotNull(
                    this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]
                )
                EditorViewModel(
                    settings = DataStoreSettingsRepository(application),
                    documents = AndroidDocumentRepository(application),
                )
            }
        }
    }
}
