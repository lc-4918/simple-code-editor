package fr.lc4918.simplecodeeditor.model

import androidx.annotation.StringRes
import fr.lc4918.simplecodeeditor.R

/**
 * A document format the editor can open, highlight and manipulate.
 *
 * Derived formats such as GeoJSON, GPX or KML are not separate entries: they are
 * carried by their base format through [extensions], because they share the same
 * grammar and therefore the same set of available tools.
 */
enum class DocumentFormat(
    @param:StringRes val labelRes: Int,
    val extensions: List<String>,
    val mimeTypes: List<String>,
    val defaultExtension: String,
) {
    JSON(
        labelRes = R.string.format_json,
        extensions = listOf("json", "geojson", "topojson", "jsonc", "map"),
        mimeTypes = listOf("application/json", "application/geo+json", "text/json"),
        defaultExtension = "json",
    ),
    XML(
        labelRes = R.string.format_xml,
        extensions = listOf("xml", "gpx", "kml", "svg", "rss", "atom", "xsd", "xsl", "plist"),
        mimeTypes = listOf("application/xml", "text/xml", "application/gpx+xml", "application/vnd.google-earth.kml+xml"),
        defaultExtension = "xml",
    ),
    HTML(
        labelRes = R.string.format_html,
        extensions = listOf("html", "htm", "xhtml"),
        mimeTypes = listOf("text/html", "application/xhtml+xml"),
        defaultExtension = "html",
    ),
    CSS(
        labelRes = R.string.format_css,
        extensions = listOf("css"),
        mimeTypes = listOf("text/css"),
        defaultExtension = "css",
    ),
    JAVASCRIPT(
        labelRes = R.string.format_javascript,
        extensions = listOf("js", "mjs", "cjs"),
        mimeTypes = listOf("text/javascript", "application/javascript", "application/x-javascript"),
        defaultExtension = "js",
    ),
    CSV(
        labelRes = R.string.format_csv,
        extensions = listOf("csv", "tsv"),
        mimeTypes = listOf("text/csv", "text/tab-separated-values"),
        defaultExtension = "csv",
    ),
    PLAIN_TEXT(
        labelRes = R.string.format_plain_text,
        extensions = listOf("txt", "text", "log"),
        mimeTypes = listOf("text/plain"),
        defaultExtension = "txt",
    );

    val capabilities: FormatCapabilities
        get() = FormatCapabilities.of(this)

    companion object {
        /** Every MIME type the app declares it can open. */
        fun allMimeTypes(): Array<String> =
            entries.flatMap { it.mimeTypes }.distinct().toTypedArray()

        fun fromExtension(extension: String): DocumentFormat? {
            val normalized = extension.trimStart('.').lowercase()
            return entries.firstOrNull { normalized in it.extensions }
        }

        fun fromMimeType(mimeType: String): DocumentFormat? {
            val normalized = mimeType.substringBefore(';').trim().lowercase()
            return entries.firstOrNull { normalized in it.mimeTypes }
        }
    }
}
