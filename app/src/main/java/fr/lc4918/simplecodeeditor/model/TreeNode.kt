package fr.lc4918.simplecodeeditor.model

/** What a node of the hierarchical view holds. */
enum class NodeKind {
    OBJECT,
    ARRAY,
    STRING,
    NUMBER,
    BOOLEAN,
    NULL,

    /** An XML element, which is a container like an object. */
    ELEMENT,

    /** The text held by an element. */
    TEXT,

    /** An attribute of an element, shown as a leaf under it. */
    ATTRIBUTE,
}

/**
 * One line of the hierarchical view.
 *
 * A node either holds a [value] and no children, or children and no value.
 * The [name] is the key of an object, the index inside an array, or the tag of
 * an element, which is what the view shows on the left of each line.
 *
 * [offset] is where the node starts in the document it was read from, which is
 * what lets a rule broken deep inside a tree be pointed at in the text.
 */
data class TreeNode(
    val name: String,
    val kind: NodeKind,
    val value: String? = null,
    val children: List<TreeNode> = emptyList(),
    val offset: Int = 0,
) {
    val isContainer: Boolean
        get() = kind == NodeKind.OBJECT || kind == NodeKind.ARRAY || kind == NodeKind.ELEMENT

    /** How many nodes hang below this one, itself excluded. */
    fun descendantCount(): Int = children.sumOf { 1 + it.descendantCount() }
}
