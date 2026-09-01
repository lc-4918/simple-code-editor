package fr.lc4918.simplecodeeditor.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import fr.lc4918.simplecodeeditor.editor.EditorTool
import fr.lc4918.simplecodeeditor.editor.EditorUiState
import fr.lc4918.simplecodeeditor.model.OpenSource
import fr.lc4918.simplecodeeditor.model.SaveTarget
import fr.lc4918.simplecodeeditor.model.ViewMode

/**
 * Main editor screen: the sticky two row header above the editing surface.
 *
 * The header sits outside the scrolling area, which is what keeps it visible
 * while the document is scrolled.
 */
@Composable
fun EditorScreen(
    state: EditorUiState,
    actions: EditorActions,
    modifier: Modifier = Modifier,
) {
    var settingsVisible by remember { mutableStateOf(false) }
    var urlPrompt by remember { mutableStateOf<UrlPrompt?>(null) }
    val editor = remember { CodeMirrorController() }

    Column(modifier = modifier.fillMaxSize()) {
        EditorTitleBar(
            documentName = state.document.name,
            isFullScreen = state.isFullScreen,
            onDocumentNameChanged = actions.onDocumentNameChanged,
            onNew = actions.onNew,
            // An address is asked for here, because it is a question to the
            // user rather than a trip through the storage picker.
            onOpen = { source ->
                if (source == OpenSource.URL) urlPrompt = UrlPrompt.OPEN else actions.onOpen(source)
            },
            onSave = { target ->
                if (target == SaveTarget.URL) urlPrompt = UrlPrompt.SAVE else actions.onSave(target)
            },
            onCopy = actions.onCopy,
            onToggleFullScreen = actions.onToggleFullScreen,
            onSettings = { settingsVisible = true },
        )
        EditorToolbar(
            state = state,
            onViewModeSelected = actions.onViewModeSelected,
            onTool = { tool ->
                when (tool) {
                    EditorTool.COLLAPSE_ALL -> editor.foldAll()
                    EditorTool.EXPAND_ALL -> editor.unfoldAll()
                    else -> actions.onTool(tool)
                }
            },
        )
        Box(modifier = Modifier.weight(1f)) {
            when (state.viewMode) {
                ViewMode.TEXT -> CodeMirrorSurface(
                    content = state.document.content,
                    format = state.format,
                    indentWidth = state.indentWidth,
                    isSearchVisible = state.isSearchVisible,
                    controller = editor,
                    onContentChanged = actions.onContentChanged,
                )

                ViewMode.TREE, ViewMode.TABLE -> ModePlaceholder(state.viewMode)
            }
        }
    }

    urlPrompt?.let { prompt ->
        UrlDialog(
            prompt = prompt,
            onConfirm = { url ->
                urlPrompt = null
                when (prompt) {
                    UrlPrompt.OPEN -> actions.onOpenUrl(url)
                    UrlPrompt.SAVE -> actions.onSaveUrl(url)
                }
            },
            onDismiss = { urlPrompt = null },
        )
    }

    if (settingsVisible) {
        SettingsSheet(
            theme = state.theme,
            language = state.language,
            indentWidth = state.indentWidth,
            onThemeSelected = actions.onThemeSelected,
            onLanguageSelected = actions.onLanguageSelected,
            onIndentWidthSelected = actions.onIndentWidthSelected,
            onDismiss = { settingsVisible = false },
        )
    }
}

/** Stands in for the tree and table views until they are built. */
@Composable
private fun ModePlaceholder(mode: ViewMode) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = stringResource(mode.labelRes),
            style = MaterialTheme.typography.titleMedium,
        )
    }
}
