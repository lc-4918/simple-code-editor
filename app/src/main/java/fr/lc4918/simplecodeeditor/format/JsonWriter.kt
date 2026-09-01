package fr.lc4918.simplecodeeditor.format

import fr.lc4918.simplecodeeditor.model.NodeKind
import fr.lc4918.simplecodeeditor.model.TreeNode

/**
 * Writes a tree back out as JSON.
 *
 * Numbers and the three literals are written exactly as they were read, so a
 * document that went through the reader and back out again says the same
 * thing. Strings are escaped by the same rules the copy uses.
 */
object JsonWriter {

    /** A null [width] puts the whole document on one line. */
    fun write(node: TreeNode, width: Int?): String {
        val out = StringBuilder()
        append(out, node, width, depth = 0)
        return out.toString()
    }

    private fun append(out: StringBuilder, node: TreeNode, width: Int?, depth: Int) {
        when (node.kind) {
            NodeKind.OBJECT -> appendContainer(out, node, width, depth, '{', '}') { child ->
                out.append('"').append(DocumentFormatter.escape(child.name)).append("\":")
                if (width != null) out.append(' ')
            }

            NodeKind.ARRAY -> appendContainer(out, node, width, depth, '[', ']') { }

            NodeKind.STRING ->
                out.append('"').append(DocumentFormatter.escape(node.value.orEmpty())).append('"')

            else -> out.append(node.value.orEmpty())
        }
    }

    private inline fun appendContainer(
        out: StringBuilder,
        node: TreeNode,
        width: Int?,
        depth: Int,
        opening: Char,
        closing: Char,
        beforeChild: (TreeNode) -> Unit,
    ) {
        out.append(opening)
        if (node.children.isEmpty()) {
            out.append(closing)
            return
        }
        node.children.forEachIndexed { index, child ->
            if (index > 0) out.append(',')
            newLine(out, width, depth + 1)
            beforeChild(child)
            append(out, child, width, depth + 1)
        }
        newLine(out, width, depth)
        out.append(closing)
    }

    private fun newLine(out: StringBuilder, width: Int?, depth: Int) {
        if (width == null) return
        out.append('\n')
        repeat(depth * width) { out.append(' ') }
    }
}
