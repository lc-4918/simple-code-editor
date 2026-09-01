package fr.lc4918.simplecodeeditor.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import fr.lc4918.simplecodeeditor.R
import fr.lc4918.simplecodeeditor.model.NodeKind
import fr.lc4918.simplecodeeditor.model.TreeNode
import fr.lc4918.simplecodeeditor.ui.theme.CodeTextStyle
import fr.lc4918.simplecodeeditor.ui.theme.LocalEditorColors

private val INDENT_PER_LEVEL = 16.dp

/**
 * The document as a hierarchy, one node per line.
 *
 * A name or a value is edited by tapping it, which is the gesture a phone has
 * where the editor this follows has a double click. What is typed is handed
 * over when the field is left or the keyboard is done with, and taken back by
 * one undo; there is no separate way to cancel.
 *
 * Which branches are open is held by the screen, so the expand and collapse
 * tools of the toolbar can reach it.
 */
@Composable
fun TreeSurface(
    root: TreeNode?,
    collapsed: Set<String>,
    onToggle: (String) -> Unit,
    onNameTyped: (TreeNode, String) -> Unit,
    onValueTyped: (TreeNode, String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalEditorColors.current
    var editing by remember { mutableStateOf<Editing?>(null) }

    if (root == null) {
        // The same ground as the document it stands in for, so an empty view
        // still reads as part of the editor.
        Box(
            modifier = modifier.fillMaxSize().background(colors.codeBackground),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = stringResource(R.string.tree_not_readable),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.codeText,
            )
        }
        return
    }

    val rows = remember(root, collapsed) { flatten(root, collapsed) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(colors.codeBackground)
            .horizontalScroll(rememberScrollState()),
    ) {
        items(rows.size, key = { rows[it].path }) { index ->
            val row = rows[index]
            TreeRow(
                row = row,
                editing = editing?.takeIf { it.path == row.path }?.field,
                onToggle = onToggle,
                onEdit = { field -> editing = Editing(row.path, field) },
                onTyped = { field, typed ->
                    editing = null
                    when (field) {
                        Field.NAME -> onNameTyped(row.node, typed)
                        Field.VALUE -> onValueTyped(row.node, typed)
                    }
                },
            )
        }
    }
}

/** A node together with where it sits, which is what the list draws. */
data class TreeRow(
    val path: String,
    val node: TreeNode,
    val depth: Int,
    /** True when the branch below is showing, which the chevron reflects. */
    val isOpen: Boolean,
)

/** Which half of a row is being typed into. */
private enum class Field { NAME, VALUE }

private data class Editing(val path: String, val field: Field)

@Composable
private fun TreeRow(
    row: TreeRow,
    editing: Field?,
    onToggle: (String) -> Unit,
    onEdit: (Field) -> Unit,
    onTyped: (Field, String) -> Unit,
) {
    val colors = LocalEditorColors.current
    val node = row.node

    Row(
        modifier = Modifier
            .padding(start = INDENT_PER_LEVEL * row.depth, top = 3.dp, bottom = 3.dp, end = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        if (node.isContainer) {
            Icon(
                imageVector = if (row.isOpen) {
                    Icons.Filled.ExpandMore
                } else {
                    Icons.AutoMirrored.Filled.KeyboardArrowRight
                },
                contentDescription = null,
                tint = colors.gutterText,
                modifier = Modifier
                    .size(18.dp)
                    .clickable { onToggle(row.path) },
            )
        } else {
            Box(Modifier.width(18.dp))
        }

        if (node.name.isNotEmpty()) {
            if (editing == Field.NAME) {
                InlineField(
                    initial = node.name,
                    color = MaterialTheme.colorScheme.primary,
                    onTyped = { typed -> onTyped(Field.NAME, typed) },
                )
            } else {
                Text(
                    text = node.name,
                    style = CodeTextStyle,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary,
                    // An array names its children by their place, which is not
                    // something to be renamed.
                    modifier = if (node.nameSpan == null) {
                        Modifier
                    } else {
                        Modifier.clickable { onEdit(Field.NAME) }
                    },
                )
            }
        }

        when {
            node.isContainer -> Text(
                text = node.summary(),
                style = CodeTextStyle,
                color = colors.gutterText,
                modifier = Modifier.clickable { onToggle(row.path) },
            )

            editing == Field.VALUE -> InlineField(
                initial = node.value.orEmpty(),
                color = colors.codeText,
                onTyped = { typed -> onTyped(Field.VALUE, typed) },
            )

            else -> Text(
                text = node.value.orEmpty(),
                style = CodeTextStyle,
                color = colors.codeText,
                modifier = if (node.valueSpan == null) {
                    Modifier
                } else {
                    Modifier.clickable { onEdit(Field.VALUE) }
                },
            )
        }
    }
}

/**
 * The field a name or a value is typed into.
 *
 * It takes the focus as it appears, and hands over what was typed when it
 * loses it or the keyboard is done with. Both happen on the way out, one
 * after the other, and it must only be handed over once: the second would
 * write at a place the first has already moved.
 */
@Composable
private fun InlineField(
    initial: String,
    color: Color,
    onTyped: (String) -> Unit,
) {
    val colors = LocalEditorColors.current
    val focusRequester = remember { FocusRequester() }
    var text by remember(initial) { mutableStateOf(initial) }
    var wasFocused by remember { mutableStateOf(false) }
    var handedOver by remember { mutableStateOf(false) }

    fun handOver() {
        if (handedOver) return
        handedOver = true
        onTyped(text)
    }

    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    BasicTextField(
        value = text,
        onValueChange = { text = it },
        modifier = Modifier
            .background(colors.gutterBackground)
            .padding(horizontal = 4.dp)
            .focusRequester(focusRequester)
            .onFocusChanged { state ->
                if (wasFocused && !state.isFocused) handOver()
                wasFocused = state.isFocused
            },
        singleLine = true,
        textStyle = CodeTextStyle.copy(color = color),
        cursorBrush = SolidColor(color),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { handOver() }),
    )
}

/**
 * Rows in the order they are drawn, leaving out what sits under a closed node.
 */
private fun flatten(root: TreeNode, collapsed: Set<String>): List<TreeRow> {
    val rows = mutableListOf<TreeRow>()

    fun walk(node: TreeNode, path: String, depth: Int) {
        val open = node.isContainer && path !in collapsed
        rows.add(TreeRow(path, node, depth, open))
        if (!open) return
        node.children.forEachIndexed { index, child ->
            walk(child, "$path/$index", depth + 1)
        }
    }

    walk(root, "", 0)
    return rows
}

/** Every container of the tree, which is what collapsing all of them needs. */
fun containerPaths(root: TreeNode): Set<String> {
    val paths = mutableSetOf<String>()

    fun walk(node: TreeNode, path: String) {
        if (node.isContainer) paths.add(path)
        node.children.forEachIndexed { index, child -> walk(child, "$path/$index") }
    }

    walk(root, "")
    return paths
}

private fun TreeNode.summary(): String = when (kind) {
    NodeKind.ARRAY -> "[${children.size}]"
    else -> "{${children.size}}"
}
