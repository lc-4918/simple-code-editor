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
import fr.lc4918.simplecodeeditor.format.FormatDetector
import fr.lc4918.simplecodeeditor.model.AppLanguage
import fr.lc4918.simplecodeeditor.model.DocumentFormat
import fr.lc4918.simplecodeeditor.model.DocumentLocation
import fr.lc4918.simplecodeeditor.model.DocumentSource
import fr.lc4918.simplecodeeditor.model.EditorDocument
import fr.lc4918.simplecodeeditor.model.ThemeOption
import fr.lc4918.simplecodeeditor.model.ViewMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Holds the open document and the user settings for the editor screen. */
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
     * Folding is run by the editing surface, which owns the fold state, so it
     * never reaches here. The tools that rewrite the document wait for their
     * formatter.
     */
    fun onTool(tool: EditorTool) {
        when (tool) {
            EditorTool.UNDO -> undo()
            EditorTool.REDO -> redo()
            EditorTool.SEARCH -> setSearchVisible(!_uiState.value.isSearchVisible)
            EditorTool.EXPAND_ALL,
            EditorTool.COLLAPSE_ALL,
            EditorTool.INDENT,
            EditorTool.COMPACT,
            EditorTool.SORT,
            EditorTool.FILTER,
            -> Unit
        }
    }

    // View state

    fun setViewMode(mode: ViewMode) {
        if (!_uiState.value.capabilities.supports(mode)) return
        _uiState.update { it.copy(viewMode = mode) }
    }

    fun toggleFullScreen() {
        _uiState.update { it.copy(isFullScreen = !it.isFullScreen) }
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

    /** Falls back to the text mode when the current one is not offered by the format. */
    private fun EditorUiState.coerceViewMode(): EditorUiState =
        if (capabilities.supports(viewMode)) this else copy(viewMode = ViewMode.TEXT)

    companion object {
        /** Keystrokes closer together than this share a single undo entry. */
        const val UNDO_COALESCE_MILLIS = 700L

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
