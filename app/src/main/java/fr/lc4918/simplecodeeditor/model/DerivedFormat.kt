package fr.lc4918.simplecodeeditor.model

/**
 * A format that rides on JSON or XML but adds rules of its own.
 *
 * A document of one of these can be perfectly good JSON or XML and still be a
 * bad GeoJSON, GPX or KML, which is why reading it is not the end of the
 * checking.
 */
enum class DerivedFormat(val base: DocumentFormat) {
    GEOJSON(DocumentFormat.JSON),
    GPX(DocumentFormat.XML),
    KML(DocumentFormat.XML),
}
