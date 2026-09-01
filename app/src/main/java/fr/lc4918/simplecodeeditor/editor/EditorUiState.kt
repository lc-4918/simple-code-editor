package fr.lc4918.simplecodeeditor.editor

import fr.lc4918.simplecodeeditor.model.AppLanguage
import fr.lc4918.simplecodeeditor.model.DocumentFormat
import fr.lc4918.simplecodeeditor.model.EditorDocument
import fr.lc4918.simplecodeeditor.model.FormatCapabilities
import fr.lc4918.simplecodeeditor.model.ThemeOption
import fr.lc4918.simplecodeeditor.model.ViewMode
import fr.lc4918.simplecodeeditor.data.SettingsRepository

/** Everything the editor screen needs to draw itself. */
data class EditorUiState(
    val document: EditorDocument = EditorDocument.empty(),
    val viewMode: ViewMode = ViewMode.TEXT,
    val theme: ThemeOption = ThemeOption.DEFAULT,
    val language: AppLanguage = AppLanguage.DEFAULT,
    val indentWidth: Int = SettingsRepository.DEFAULT_INDENT_WIDTH,
    val isFullScreen: Boolean = false,
    val isSearchVisible: Boolean = false,
    val searchQuery: String = "",
    val canUndo: Boolean = false,
    val canRedo: Boolean = false,
    val statusMessageRes: Int? = null,
) {
    val format: DocumentFormat get() = document.format
    val capabilities: FormatCapabilities get() = document.capabilities

    /** True when the tool is both meaningful for the format and usable right now. */
    fun isToolEnabled(tool: EditorTool): Boolean = when (tool) {
        EditorTool.INDENT -> capabilities.indent && viewMode == ViewMode.TEXT
        EditorTool.COMPACT -> capabilities.compact && viewMode == ViewMode.TEXT
        EditorTool.EXPAND_ALL, EditorTool.COLLAPSE_ALL ->
            capabilities.expandCollapseAll
        EditorTool.SORT -> capabilities.sort
        EditorTool.FILTER -> capabilities.filter
        EditorTool.SEARCH -> capabilities.search
        EditorTool.UNDO -> capabilities.undoRedo && canUndo
        EditorTool.REDO -> capabilities.undoRedo && canRedo
    }

    /** True when the tool should appear at all for the open format. */
    fun isToolVisible(tool: EditorTool): Boolean = when (tool) {
        EditorTool.INDENT -> capabilities.indent
        EditorTool.COMPACT -> capabilities.compact
        EditorTool.EXPAND_ALL, EditorTool.COLLAPSE_ALL -> capabilities.expandCollapseAll
        EditorTool.SORT -> capabilities.sort
        EditorTool.FILTER -> capabilities.filter
        EditorTool.SEARCH -> capabilities.search
        EditorTool.UNDO, EditorTool.REDO -> capabilities.undoRedo
    }
}

/** The tools of the second toolbar row, excluding the mode selector. */
enum class EditorTool {
    INDENT,
    COMPACT,
    EXPAND_ALL,
    COLLAPSE_ALL,
    SORT,
    FILTER,
    SEARCH,
    UNDO,
    REDO,
}
