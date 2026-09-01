package fr.lc4918.simplecodeeditor.model

/**
 * Which toolbar tools make sense for a given format.
 *
 * The second toolbar row reads this to disable or hide anything that would be
 * meaningless for the open document, for instance the table mode on HTML.
 */
data class FormatCapabilities(
    val textMode: Boolean = true,
    val treeMode: Boolean = false,
    val tableMode: Boolean = false,
    val indent: Boolean = false,
    val compact: Boolean = false,
    val expandCollapseAll: Boolean = false,
    val sort: Boolean = false,
    val filter: Boolean = false,
    val search: Boolean = true,
    val undoRedo: Boolean = true,
    /** Whether the document can be shown as it will be read, not as written. */
    val preview: Boolean = false,
) {
    fun supports(mode: ViewMode): Boolean = when (mode) {
        ViewMode.TEXT -> textMode
        ViewMode.TREE -> treeMode
        ViewMode.TABLE -> tableMode
    }

    /** The modes offered by the mode selector, in display order. */
    val availableModes: List<ViewMode>
        get() = ViewMode.entries.filter(::supports)

    companion object {
        fun of(format: DocumentFormat): FormatCapabilities = when (format) {
            DocumentFormat.JSON -> FormatCapabilities(
                treeMode = true,
                indent = true,
                compact = true,
                expandCollapseAll = true,
                sort = true,
                filter = true,
            )

            DocumentFormat.XML -> FormatCapabilities(
                treeMode = true,
                indent = true,
                compact = true,
                expandCollapseAll = true,
            )

            DocumentFormat.HTML -> FormatCapabilities(
                indent = true,
                compact = true,
            )

            DocumentFormat.CSS -> FormatCapabilities(
                indent = true,
                compact = true,
            )

            DocumentFormat.JAVASCRIPT -> FormatCapabilities(
                indent = true,
                compact = true,
            )

            DocumentFormat.MARKDOWN -> FormatCapabilities(
                preview = true,
            )

            DocumentFormat.CSV -> FormatCapabilities(
                tableMode = true,
                sort = true,
                filter = true,
            )

            DocumentFormat.PLAIN_TEXT -> FormatCapabilities()
        }
    }
}
