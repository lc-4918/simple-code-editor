package fr.lc4918.simplecodeeditor.ui

import androidx.compose.runtime.Immutable
import fr.lc4918.simplecodeeditor.editor.EditorTool
import fr.lc4918.simplecodeeditor.model.AppLanguage
import fr.lc4918.simplecodeeditor.model.CopyVariant
import fr.lc4918.simplecodeeditor.model.OpenSource
import fr.lc4918.simplecodeeditor.model.SaveTarget
import fr.lc4918.simplecodeeditor.model.ThemeOption
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
    val onToggleFullScreen: () -> Unit,
    val onViewModeSelected: (ViewMode) -> Unit,
    val onTool: (EditorTool) -> Unit,
    val onThemeSelected: (ThemeOption) -> Unit,
    val onLanguageSelected: (AppLanguage) -> Unit,
    val onIndentWidthSelected: (Int) -> Unit,
)
