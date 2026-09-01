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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
 * The view reads the document rather than writing to it: editing stays in text
 * mode, where the whole document is at hand. Which branches are open is held
 * by the screen, so the expand and collapse tools of the toolbar can reach it.
 */
@Composable
fun TreeSurface(
    root: TreeNode?,
    collapsed: Set<String>,
    onToggle: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalEditorColors.current

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
            TreeRow(row = rows[index], onToggle = onToggle)
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

@Composable
private fun TreeRow(row: TreeRow, onToggle: (String) -> Unit) {
    val colors = LocalEditorColors.current
    val node = row.node

    Row(
        modifier = Modifier
            .then(if (node.isContainer) Modifier.clickable { onToggle(row.path) } else Modifier)
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
                modifier = Modifier.size(18.dp),
            )
        } else {
            Box(Modifier.width(18.dp))
        }

        if (node.name.isNotEmpty()) {
            Text(
                text = node.name,
                style = CodeTextStyle,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.primary,
            )
        }

        when {
            node.isContainer -> Text(
                text = node.summary(),
                style = CodeTextStyle,
                color = colors.gutterText,
            )

            else -> Text(
                text = node.value.orEmpty(),
                style = CodeTextStyle,
                color = colors.codeText,
            )
        }
    }
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
