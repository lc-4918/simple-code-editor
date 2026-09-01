package fr.lc4918.simplecodeeditor.editor

import androidx.annotation.StringRes
import fr.lc4918.simplecodeeditor.R
import fr.lc4918.simplecodeeditor.model.AppLanguage
import fr.lc4918.simplecodeeditor.model.CsvDelimiter
import fr.lc4918.simplecodeeditor.model.Diagnostic
import fr.lc4918.simplecodeeditor.model.DocumentFormat
import fr.lc4918.simplecodeeditor.model.EditorDocument
import fr.lc4918.simplecodeeditor.model.FormatCapabilities
import fr.lc4918.simplecodeeditor.model.LabelledOption
import fr.lc4918.simplecodeeditor.model.ThemeOption
import fr.lc4918.simplecodeeditor.model.UpdateMode
import fr.lc4918.simplecodeeditor.update.ReleaseInfo
import fr.lc4918.simplecodeeditor.model.ViewMode
import fr.lc4918.simplecodeeditor.data.SettingsRepository

/** Everything the editor screen needs to draw itself. */
data class EditorUiState(
    val document: EditorDocument = EditorDocument.empty(),
    val viewMode: ViewMode = ViewMode.TEXT,
    val theme: ThemeOption = ThemeOption.DEFAULT,
    val language: AppLanguage = AppLanguage.DEFAULT,
    /**
     * False until the stored language has been read.
     *
     * Applying a language recreates the activity, so the default one must not
     * be applied before the stored choice is known.
     */
    val isLanguageLoaded: Boolean = false,
    val indentWidth: Int = SettingsRepository.DEFAULT_INDENT_WIDTH,
    val csvDelimiter: CsvDelimiter = CsvDelimiter.DEFAULT,
    val updateMode: UpdateMode = UpdateMode.DEFAULT,
    val isCheckingUpdate: Boolean = false,
    /**
     * What the last check came back with.
     *
     * Said in the settings rather than over the document: the settings are
     * where the check was asked for, and a message behind the sheet that
     * covers the document is a message nobody reads.
     */
    val updateMessageRes: Int? = null,
    /** The release worth taking, once one has been found. */
    val availableUpdate: ReleaseInfo? = null,
    val isSearchVisible: Boolean = false,
    val searchQuery: String = "",
    val canUndo: Boolean = false,
    val canRedo: Boolean = false,
    /** Why the document cannot be read, when it cannot. */
    val diagnostic: Diagnostic? = null,
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
enum class EditorTool(@param:StringRes override val labelRes: Int) : LabelledOption {
    INDENT(R.string.tool_indent),
    COMPACT(R.string.tool_compact),
    EXPAND_ALL(R.string.tool_expand_all),
    COLLAPSE_ALL(R.string.tool_collapse_all),
    SORT(R.string.tool_sort),
    FILTER(R.string.tool_filter),
    SEARCH(R.string.tool_search),
    UNDO(R.string.tool_undo),
    REDO(R.string.tool_redo),
}
