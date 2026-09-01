package fr.lc4918.simplecodeeditor.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import fr.lc4918.simplecodeeditor.R
import fr.lc4918.simplecodeeditor.model.CopyVariant
import fr.lc4918.simplecodeeditor.model.LabelledOption
import fr.lc4918.simplecodeeditor.model.OpenSource
import fr.lc4918.simplecodeeditor.model.SaveTarget
import fr.lc4918.simplecodeeditor.ui.theme.LocalEditorColors

/**
 * First row of the sticky header: the document name and the document actions.
 *
 * The smart formatted copy of the reference editor is deliberately absent from
 * the copy menu, and so is its full screen toggle: on a phone the editor
 * already fills the screen, and the two rows it could win back are the ones
 * that are worked with.
 */
@Composable
fun EditorTitleBar(
    documentName: String,
    onDocumentNameChanged: (String) -> Unit,
    onNew: () -> Unit,
    onOpen: (OpenSource) -> Unit,
    onSave: (SaveTarget) -> Unit,
    onCopy: (CopyVariant) -> Unit,
    onSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalEditorColors.current

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.titleBarBackground)
            .padding(start = 12.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        DocumentNameField(
            name = documentName,
            onNameChanged = onDocumentNameChanged,
            modifier = Modifier.weight(1f),
        )
        TitleBarAction(
            icon = Icons.Filled.Description,
            label = stringResource(R.string.action_new),
            onClick = onNew,
        )
        TitleBarMenu(
            icon = Icons.Filled.FolderOpen,
            label = stringResource(R.string.action_open),
            options = OpenSource.entries,
            onSelected = onOpen,
        )
        TitleBarMenu(
            icon = Icons.Filled.Save,
            label = stringResource(R.string.action_save),
            options = SaveTarget.entries,
            onSelected = onSave,
        )
        TitleBarMenu(
            icon = Icons.Filled.ContentCopy,
            label = stringResource(R.string.action_copy),
            options = CopyVariant.entries,
            onSelected = onCopy,
        )
        TitleBarAction(
            icon = Icons.Filled.Settings,
            label = stringResource(R.string.action_settings),
            onClick = onSettings,
        )
    }
}

/**
 * Editable document name.
 *
 * The name is the field, with nothing beside it: tapping the name is what
 * starts editing, so a pencil next to it would only take room away from the
 * name itself, which is given every point the actions leave.
 *
 * The typed value is kept locally and only handed over when the field loses the
 * focus or the keyboard action is confirmed, because the view model normalises
 * the name and would otherwise fight every keystroke.
 */
@Composable
private fun DocumentNameField(
    name: String,
    onNameChanged: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalEditorColors.current
    val focusManager = LocalFocusManager.current
    val label = stringResource(R.string.document_name)
    var text by remember(name) { mutableStateOf(name) }
    var wasFocused by remember { mutableStateOf(false) }

    BasicTextField(
        value = text,
        onValueChange = { text = it },
        modifier = modifier
            .semantics { contentDescription = label }
            .onFocusChanged { focusState ->
                if (wasFocused && !focusState.isFocused) onNameChanged(text)
                wasFocused = focusState.isFocused
            },
        singleLine = true,
        textStyle = LocalTextStyle.current.merge(
            MaterialTheme.typography.titleMedium.copy(color = colors.titleBarContent)
        ),
        cursorBrush = SolidColor(colors.titleBarContent),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(
            onDone = {
                onNameChanged(text)
                focusManager.clearFocus()
            },
        ),
    )
}

@Composable
private fun TitleBarAction(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    val colors = LocalEditorColors.current
    IconButton(onClick = onClick, modifier = Modifier.size(40.dp)) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = colors.titleBarContent,
            modifier = Modifier.size(20.dp),
        )
    }
}

/** A title bar action that first asks which variant to run. */
@Composable
private fun <T : LabelledOption> TitleBarMenu(
    icon: ImageVector,
    label: String,
    options: List<T>,
    onSelected: (T) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        TitleBarAction(icon = icon, label = label, onClick = { expanded = true })
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(stringResource(option.labelRes)) },
                    onClick = {
                        expanded = false
                        onSelected(option)
                    },
                )
            }
        }
    }
}
