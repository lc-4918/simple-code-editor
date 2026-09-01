package fr.lc4918.simplecodeeditor.ui

import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import fr.lc4918.simplecodeeditor.R
import fr.lc4918.simplecodeeditor.model.NodeKind
import fr.lc4918.simplecodeeditor.model.TreeNode

/**
 * What can be done to one node of the tree.
 *
 * Inserting asks a second question, which shape to insert, and asks it in the
 * same menu rather than in one hanging off it: a menu within a menu is hard to
 * aim at with a thumb.
 */
@Composable
fun TreeMenu(
    node: TreeNode,
    canPaste: Boolean,
    onEditName: () -> Unit,
    onEditValue: () -> Unit,
    onAction: (TreeAction) -> Unit,
    onDismiss: () -> Unit,
) {
    var inserting by remember { mutableStateOf<Where?>(null) }

    DropdownMenu(expanded = true, onDismissRequest = onDismiss) {
        val where = inserting
        if (where != null) {
            ShapeChoices { kind -> onAction(TreeAction.Insert(where, kind)) }
            return@DropdownMenu
        }

        if (node.nameSpan != null) {
            Item(R.string.tree_edit_key, onEditName)
        }
        if (node.valueSpan != null && node.children.isEmpty()) {
            Item(R.string.tree_edit_value, onEditValue)
        }
        HorizontalDivider()
        Item(R.string.action_copy) { onAction(TreeAction.Copy) }
        Item(R.string.tree_cut) { onAction(TreeAction.Cut) }
        Item(R.string.tree_duplicate) { onAction(TreeAction.Duplicate) }
        Item(R.string.tree_extract) { onAction(TreeAction.Extract) }
        Item(R.string.tree_remove) { onAction(TreeAction.Remove) }
        HorizontalDivider()
        Item(R.string.tree_insert_before) { inserting = Where.BEFORE }
        Item(R.string.tree_insert_after) { inserting = Where.AFTER }
        if (node.isContainer) {
            Item(R.string.tree_insert_into) { inserting = Where.INTO }
        }
        if (canPaste) {
            HorizontalDivider()
            Item(R.string.tree_paste) { onAction(TreeAction.Paste(Where.AFTER)) }
        }
    }
}

@Composable
private fun ShapeChoices(onChosen: (NodeKind) -> Unit) {
    Item(R.string.tree_insert_object) { onChosen(NodeKind.OBJECT) }
    Item(R.string.tree_insert_array) { onChosen(NodeKind.ARRAY) }
    Item(R.string.tree_insert_value) { onChosen(NodeKind.STRING) }
}

@Composable
private fun Item(labelRes: Int, onClick: () -> Unit) {
    DropdownMenuItem(text = { Text(stringResource(labelRes)) }, onClick = onClick)
}
