package fr.lc4918.simplecodeeditor.format

import fr.lc4918.simplecodeeditor.model.FilterOperator
import fr.lc4918.simplecodeeditor.model.NodeKind
import fr.lc4918.simplecodeeditor.model.SortDirection
import fr.lc4918.simplecodeeditor.model.TreeNode

/**
 * Reorders and thins out a JSON document.
 *
 * An array is worked on element by element, an object key by key. A [field]
 * picks which member of an element to look at; without one the element is
 * taken as it stands, which is what an array of plain values needs.
 */
object JsonTransform {

    /**
     * The members the elements of an array have, in the order they first
     * appear, which is what the dialog offers as columns. An array of plain
     * values, or anything that is not an array, offers none.
     */
    fun fields(root: TreeNode): List<String> {
        if (root.kind != NodeKind.ARRAY) return emptyList()
        val names = LinkedHashSet<String>()
        root.children
            .filter { it.kind == NodeKind.OBJECT }
            .forEach { element -> element.children.forEach { names.add(it.name) } }
        return names.toList()
    }

    fun sort(root: TreeNode, field: String?, direction: SortDirection): TreeNode = when (root.kind) {
        NodeKind.ARRAY -> {
            val ordered = root.children.sortedWith { left, right ->
                ValueOrder.compare(subject(left, field), subject(right, field))
            }
            root.reindexed(direction.applyTo(ordered))
        }

        NodeKind.OBJECT -> {
            val ordered = root.children.sortedWith { left, right ->
                ValueOrder.compare(left.name, right.name)
            }
            root.copy(children = direction.applyTo(ordered))
        }

        else -> root
    }

    fun filter(
        root: TreeNode,
        field: String?,
        operator: FilterOperator,
        value: String,
    ): TreeNode = when (root.kind) {
        NodeKind.ARRAY -> root.reindexed(
            root.children.filter { ValueOrder.matches(subject(it, field), operator, value) },
        )

        // An object has no elements to keep or drop, so its keys are matched.
        NodeKind.OBJECT -> root.copy(
            children = root.children.filter { ValueOrder.matches(it.name, operator, value) },
        )

        else -> root
    }

    /** What the comparison looks at for one element. */
    private fun subject(element: TreeNode, field: String?): String {
        if (field.isNullOrEmpty()) return element.value.orEmpty()
        return element.children.firstOrNull { it.name == field }?.value.orEmpty()
    }

    /** Array elements are named by their place, so the places are handed out again. */
    private fun TreeNode.reindexed(children: List<TreeNode>): TreeNode =
        copy(children = children.mapIndexed { index, child -> child.copy(name = index.toString()) })

    private fun SortDirection.applyTo(ordered: List<TreeNode>): List<TreeNode> =
        if (this == SortDirection.ASCENDING) ordered else ordered.reversed()
}
