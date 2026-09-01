# Simple Code Editor

An Android editor and viewer for structured text files, built with Kotlin and
Jetpack Compose.

The layout follows the single panel editor of jsoneditoronline.org: a sticky
two row header above an editing surface with line numbers, syntax highlighting
and block folding.

## Supported formats

| Format | Also covers |
| --- | --- |
| JSON | GeoJSON, TopoJSON |
| XML | GPX, KML, SVG, RSS |
| HTML | XHTML |
| CSS | |
| JavaScript | |
| CSV | TSV |

## View modes

- **Text** for every format.
- **Tree** for the nested formats, JSON and XML.
- **Table** for CSV, which can be edited either as raw text or as a grid of
  rows and columns.

The second toolbar row adapts to the open document: a tool that has no meaning
for the current format, such as the table mode on an HTML file, is not offered.

## Settings

- Interface language: English by default, French and Spanish available.
- Theme: system, light or dark.

## Build

Requires the Android SDK and a JDK 21 toolchain.

    ./gradlew assembleDebug
    ./gradlew testDebugUnitTest

## Licence

Not published under a licence yet.
