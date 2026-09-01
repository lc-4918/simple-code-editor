package fr.lc4918.simplecodeeditor.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import fr.lc4918.simplecodeeditor.editor.EditorUiState
import fr.lc4918.simplecodeeditor.model.ViewMode

/**
 * Main editor screen.
 *
 * At this stage it only reports the document state so the foundation can be
 * exercised. The sticky title bar, the contextual toolbar and the editing
 * surface replace this body in the following steps.
 */
@Composable
fun EditorScreen(
    state: EditorUiState,
    onViewModeSelected: (ViewMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = state.document.fileName(),
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            text = stringResource(state.format.labelRes),
            style = MaterialTheme.typography.bodyMedium,
        )
        state.capabilities.availableModes.forEach { mode ->
            FilterChip(
                selected = state.viewMode == mode,
                onClick = { onViewModeSelected(mode) },
                label = { Text(stringResource(mode.labelRes)) },
            )
        }
    }
}
