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
  showing half of itself.

  A name or a value is edited by tapping it, which is the gesture a phone has
  where the editor this follows has a double click. What is typed is written
  into the document in place of the few characters the name or the value
  occupied, and nothing else is touched: comments, declaration and layout come
  through an edit unchanged. The type of a JSON value follows what is typed,
  so a value becomes a number, a boolean or null by being written as one; text
  that would be read as one of those and is meant as a string is typed between
  quotes. Renaming an XML element carries its closing tag along. An edit is one
  step in the history, and there is no separate way to cancel one: undoing is
  the way back.

  A long press on a node opens what can be done to it, which is the gesture a
  phone has where the editor this follows has a right click: edit the key or
  the value, copy, cut, paste, duplicate, extract the subtree as the whole
  document, remove, and insert an object, an array or a value before, after or
  inside. Copying and pasting go through the clipboard of the system, so a
  subtree travels to and from anywhere else. Each of these is one step in the
  history, and each is a handful of characters moved in the document, the
  punctuation between entries included: what surrounds the node keeps its
  comments and its layout.

  Sorting and transforming are not offered on a node. The toolbar sorts and
  filters the whole document, and doing it to one branch would mean writing
  that branch out again, which would cost it the layout every other move here
  is careful to keep.
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

## Reading and reporting

JSON and XML documents are read as they are typed, a short pause after the last
keystroke, and the same reading runs when a document is opened or pasted. What
stops the reading is said in words under the toolbar, with the line and the
column, and marked in the margin of the text at the spot.

The readers are strict and stop at the first problem: they report one at a
time, not a list. Reporting several at once needs a reader that recovers from a
problem and carries on, which these do not do. The other formats are not read
this way, so nothing is reported for them.

A broken JSON document can be repaired, from the report itself, where the
reason it is needed already is. The repair is `jsonrepair`, which travels in
the bundle with the editor: it quotes bare keys and values, drops trailing
commas and comments, turns single quotes into double ones and reads a document
written one object per line. It follows that repairing is offered in text mode
only, where the bundle is on screen, and for JSON only. A repair that changes
nothing is reported as a failure, whatever it answered, and a repair that works
is one step in the history: one undo brings the broken document back.

What is repaired is not shown side by side with what it replaced. Undoing is
the way to compare the two.

XML is read the same way but not repaired, there being no equivalent to lean
on.

## Formats built on JSON and XML

A GeoJSON, GPX or KML document can be perfect JSON or XML and still be a bad
one of those, so reading it is not the end of the checking. The format is
recognised by the file name, and by the content when the name says nothing: the
two types only a GeoJSON carries at its root, and the element a GPX or a KML
opens with, namespace or not.

**GeoJSON** is checked against RFC 7946 on the points a reader can judge
without knowing what the data means: the nine types, the members each requires,
the shape of the coordinates each geometry takes, the two or three numbers of a
position and the bounds they lie within, the two positions a line takes, and
the four a ring takes and its closing where it started. Left out on purpose,
since a document that breaks them is still usable and a reader cannot tell
intent from mistake: the winding order of the rings, the crossing of the
antimeridian, and the bounding box.

**GPX and KML are not validated against their schema.** Android carries no
implementation of W3C XML Schema: asking for one throws, so the official XSD
cannot be applied without bringing a parser along. What is checked instead is
the part of each format that carries the data and that a wrong file gets wrong:
the opening element, and the coordinates, which GPX writes as attributes of its
points and KML as the text of an element. The order of the elements, and the
ones the schema forbids, go unchecked.

Only the first broken rule is reported, as with the readers, and a rule of a
format is not something repairing could address: the document already reads.

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

## Releases

The version is taken from the git tag rather than written in the build script,
so every release declares its own version code and name. Pushing a tag shaped
`vMAJOR.MINOR.PATCH` builds a signed APK and publishes it as a release; a push
to any branch builds a debug APK and runs the unit tests.

## Licence

Not published under a licence yet.
