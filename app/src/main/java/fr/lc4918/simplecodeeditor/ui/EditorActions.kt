package fr.lc4918.simplecodeeditor.ui

import androidx.compose.runtime.Immutable
import fr.lc4918.simplecodeeditor.editor.EditorTool
import fr.lc4918.simplecodeeditor.model.AppLanguage
import fr.lc4918.simplecodeeditor.model.CopyVariant
import fr.lc4918.simplecodeeditor.model.CsvDelimiter
import fr.lc4918.simplecodeeditor.model.FilterOperator
import fr.lc4918.simplecodeeditor.model.OpenSource
import fr.lc4918.simplecodeeditor.model.SaveTarget
import fr.lc4918.simplecodeeditor.model.SortDirection
import fr.lc4918.simplecodeeditor.model.TreeNode
import fr.lc4918.simplecodeeditor.model.ThemeOption
import fr.lc4918.simplecodeeditor.model.UpdateMode
import fr.lc4918.simplecodeeditor.model.ViewMode

/**
 * Everything the editor screen can ask for.
 *
 * Grouping the callbacks keeps the screen stateless and its signature short,
 * while the activity remains the only place that knows about the view model.
 */
@Immutable
class EditorActions(
    val onDocumentNameChanged: (String) -> Unit,
    val onContentChanged: (String) -> Unit,
    val onNew: () -> Unit,
    val onOpen: (OpenSource) -> Unit,
    val onOpenUrl: (String) -> Unit,
    val onSave: (SaveTarget) -> Unit,
    val onSaveUrl: (String) -> Unit,
    val onCopy: (CopyVariant) -> Unit,
    val onViewModeSelected: (ViewMode) -> Unit,
    val onTool: (EditorTool) -> Unit,
    val onRepaired: (String?) -> Unit,
    val onRepairXml: () -> Unit,
    val onTreeNameTyped: (TreeNode, String) -> Unit,
    val onTreeValueTyped: (TreeNode, String) -> Unit,
    val onTreeAction: (TreeNode, TreeAction) -> Unit,
    /** Whether the clipboard holds something a node could be made of. */
    val canPasteIntoTree: () -> Boolean,
    val onCellChanged: (row: Int, column: Int, value: String) -> Unit,
    val onAddRow: () -> Unit,
    val onSort: (column: Int, direction: SortDirection) -> Unit,
    val onFilter: (column: Int, operator: FilterOperator, value: String) -> Unit,
    val onThemeSelected: (ThemeOption) -> Unit,
    val onLanguageSelected: (AppLanguage) -> Unit,
    val onIndentWidthSelected: (Int) -> Unit,
    val onCsvDelimiterSelected: (CsvDelimiter) -> Unit,
    val onUpdateModeSelected: (UpdateMode) -> Unit,
    val onCheckForUpdate: () -> Unit,
    val onUpdateHandled: () -> Unit,
)
