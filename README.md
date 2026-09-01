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
- **Tree** for the nested formats, JSON and XML. Branches open and close one
  by one or all at once, XML attributes hang under their element marked with a
  leading sign, and a document that does not read cleanly says so rather than
  showing half of itself. The tree reads the document; editing stays in text
  mode, where the whole document is at hand.
- **Table** for CSV, which can be edited either as raw text or as a grid of
  rows and columns. The grid reads the first row as the column titles, keeps
  the quoting rules of the format on the way back out, and only quotes the
  fields that need it.

Sort and filter ask what to work on: a column for CSV, and for JSON the member
its elements share, or the values themselves when they have none. An object is
sorted and filtered by its keys. Both rewrite the document, so a filter that
drops rows is undone in one step like any other edit, and a JSON document
written on one line is written back on one line.

The second toolbar row adapts to the open document: a tool that has no meaning
for the current format, such as the table mode on an HTML file, is not offered.

## Opening and saving

A document can be opened straight from a file browser, which offers the editor
for the types it knows. A file a provider reports as a plain byte stream is
opened through the Open action instead.

Documents are read and written through the storage picker of the system, which
covers the storage of the device and every cloud service installed on it, so no
account and no provider specific integration is needed. A document opened that
way is written back in place; one that has never been stored is given a
destination first.

A document can also be read from and written to an address. Only secure
addresses are accepted, because the application sends nothing in the clear.

## Laying out and copying

The format and compact tools rewrite the whitespace of the open document, each
format by its own rules and none of them by parsing: the order of the keys and
the exact spelling of the numbers survive, and a document that cannot be
scanned to the end is left untouched rather than half rewritten. Compacting a
script keeps its line breaks, since dropping them would join two statements
that relied on the end of the line to close the first one.

Copy offers the document laid out, compacted, escaped for pasting between two
quotes, or exactly as it stands.

## Settings

- Interface language: English by default, French and Spanish available.
- Theme: system, light or dark.
- Indentation width.
- CSV separator: comma, semicolon or tab. A document that is opened keeps the
  separator it was written with, which the reader works out on its own; this
  choice is what a new document gets, what an opened one falls back to when
  nothing can be worked out, and what the open document is rewritten with when
  the choice changes.

## Build

Requires the Android SDK and a JDK 21 toolchain.

    ./gradlew assembleDebug
    ./gradlew testDebugUnitTest

The editing surface is CodeMirror, running in a web view and loaded from the
application assets, so the application builds and runs offline. The bundle in
`app/src/main/assets/editor` is committed; rebuild it after changing anything
under `editor-web`, which needs Node.

    cd editor-web
    npm install
    npm run build

## Licence

Not published under a licence yet.
