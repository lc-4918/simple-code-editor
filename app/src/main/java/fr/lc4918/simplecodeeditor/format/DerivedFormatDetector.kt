package fr.lc4918.simplecodeeditor.format

import fr.lc4918.simplecodeeditor.model.DerivedFormat

/**
 * Works out whether a document follows one of the formats built on JSON or XML.
 *
 * The name is trusted first, as it is for the base format, and the content is
 * only looked at when the name says nothing: a document typed into a nameless
 * editor still deserves to be checked against the rules it is written for.
 */
object DerivedFormatDetector {

    fun detect(fileName: String? = null, content: String? = null): DerivedFormat? {
        fileName?.let { name ->
            when (name.substringAfterLast('/').substringAfterLast('.', "").lowercase()) {
                "geojson" -> return DerivedFormat.GEOJSON
                "gpx" -> return DerivedFormat.GPX
                "kml" -> return DerivedFormat.KML
            }
        }
        return content?.let(::detectFromContent)
    }

    private fun detectFromContent(content: String): DerivedFormat? {
        val head = content.take(SNIFF_LIMIT)
        return when {
            GEOJSON_MARK.containsMatchIn(head) -> DerivedFormat.GEOJSON
            GPX_MARK.containsMatchIn(head) -> DerivedFormat.GPX
            KML_MARK.containsMatchIn(head) -> DerivedFormat.KML
            else -> null
        }
    }

    private const val SNIFF_LIMIT = 4096

    /** The two types that only a GeoJSON document carries at its root. */
    private val GEOJSON_MARK =
        Regex("""["']type["']\s*:\s*["'](FeatureCollection|Feature)["']""")

    /** The opening element, whether or not it carries the namespace. */
    private val GPX_MARK = Regex("""<(\w+:)?gpx[\s>]""", RegexOption.IGNORE_CASE)

    private val KML_MARK = Regex("""<(\w+:)?kml[\s>]""", RegexOption.IGNORE_CASE)
}
