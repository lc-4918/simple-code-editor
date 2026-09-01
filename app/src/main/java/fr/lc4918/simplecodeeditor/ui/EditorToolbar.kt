package fr.lc4918.simplecodeeditor.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FormatIndentDecrease
import androidx.compose.material.icons.automirrored.filled.FormatIndentIncrease
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.UnfoldLess
import androidx.compose.material.icons.filled.UnfoldMore
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import fr.lc4918.simplecodeeditor.R
import fr.lc4918.simplecodeeditor.editor.EditorTool
import fr.lc4918.simplecodeeditor.editor.EditorUiState
import fr.lc4918.simplecodeeditor.model.ViewMode
import fr.lc4918.simplecodeeditor.ui.theme.LocalEditorColors

/**
 * Second row of the sticky header: the view mode selector and the tools.
 *
 * A tool that has no meaning for the open format is not drawn at all, and one
 * that is meaningful but unusable right now, such as undo on a fresh document,
 * is drawn disabled. The row scrolls sideways because a wide format such as
 * JSON offers more tools than a phone can show at once.
 */
@Composable
fun EditorToolbar(
    state: EditorUiState,
    onViewModeSelected: (ViewMode) -> Unit,
    onTool: (EditorTool) -> Unit,
    onExitFullScreen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalEditorColors.current
    val modes = state.capabilities.availableModes
    val tools = EditorTool.entries.filter(state::isToolVisible)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.toolbarBackground)
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 6.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        // The title bar, which holds the way out, is gone in full screen, so
        // the way back has to be here, and first, where it cannot be scrolled
        // out of sight.
        if (state.isFullScreen) {
            IconButton(onClick = onExitFullScreen, modifier = Modifier.size(36.dp)) {
                Icon(
                    imageVector = Icons.Filled.FullscreenExit,
                    contentDescription = stringResource(R.string.action_exit_full_screen),
                    tint = colors.toolbarContent,
                    modifier = Modifier.size(20.dp),
                )
            }
            Spacer(Modifier.width(6.dp))
        }
        if (modes.size > 1) {
            ViewModeSelector(
                modes = modes,
                selected = state.viewMode,
                onSelected = onViewModeSelected,
            )
            Spacer(Modifier.width(6.dp))
        }
        tools.forEach { tool ->
            ToolButton(
                tool = tool,
                enabled = state.isToolEnabled(tool),
                onClick = { onTool(tool) },
            )
        }
    }
}

/** The text, tree and table segments, limited to the modes the format allows. */
@Composable
private fun ViewModeSelector(
    modes: List<ViewMode>,
    selected: ViewMode,
    onSelected: (ViewMode) -> Unit,
) {
    val colors = LocalEditorColors.current

    Row(
        modifier = Modifier.clip(RoundedCornerShape(4.dp)),
        horizontalArrangement = Arrangement.spacedBy(1.dp),
    ) {
        modes.forEach { mode ->
            val isSelected = mode == selected
            Text(
                text = stringResource(mode.labelRes),
                style = MaterialTheme.typography.labelLarge,
                color = if (isSelected) colors.toolbarSelectedContent else colors.toolbarContent,
                modifier = Modifier
                    .clickable { onSelected(mode) }
                    .background(
                        if (isSelected) colors.toolbarSelectedBackground else colors.toolbarBackground
                    )
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            )
        }
    }
}

@Composable
private fun ToolButton(
    tool: EditorTool,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val colors = LocalEditorColors.current

    IconButton(onClick = onClick, enabled = enabled, modifier = Modifier.size(36.dp)) {
        Icon(
            imageVector = tool.icon,
            contentDescription = stringResource(tool.labelRes),
            tint = colors.toolbarContent.copy(alpha = if (enabled) 1f else 0.4f),
            modifier = Modifier.size(20.dp),
        )
    }
}

private val EditorTool.icon: ImageVector
    get() = when (this) {
        EditorTool.INDENT -> Icons.AutoMirrored.Filled.FormatIndentIncrease
        EditorTool.COMPACT -> Icons.AutoMirrored.Filled.FormatIndentDecrease
        EditorTool.EXPAND_ALL -> Icons.Filled.UnfoldMore
        EditorTool.COLLAPSE_ALL -> Icons.Filled.UnfoldLess
        EditorTool.SORT -> Icons.AutoMirrored.Filled.Sort
        EditorTool.FILTER -> Icons.Filled.FilterAlt
        EditorTool.SEARCH -> Icons.Filled.Search
        EditorTool.UNDO -> Icons.AutoMirrored.Filled.Undo
        EditorTool.REDO -> Icons.AutoMirrored.Filled.Redo
    }
