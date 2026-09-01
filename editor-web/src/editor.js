// Editing surface embedded in the Android application.
//
// The Kotlin side owns the document and the undo history, so this bundle keeps
// no history extension: it renders the text it is given, reports every edit
// back, and exposes the commands the toolbar needs.

import { Compartment, EditorState } from "@codemirror/state";
import { EditorView, drawSelection, highlightActiveLine, highlightActiveLineGutter, keymap, lineNumbers, rectangularSelection } from "@codemirror/view";
import { defaultKeymap, indentWithTab } from "@codemirror/commands";
import { HighlightStyle, bracketMatching, codeFolding, foldAll, foldGutter, foldKeymap, indentOnInput, indentUnit, syntaxHighlighting, unfoldAll } from "@codemirror/language";
import { closeSearchPanel, openSearchPanel, search, searchKeymap } from "@codemirror/search";
import { css } from "@codemirror/lang-css";
import { html } from "@codemirror/lang-html";
import { javascript } from "@codemirror/lang-javascript";
import { json } from "@codemirror/lang-json";
import { xml } from "@codemirror/lang-xml";
import { tags } from "@lezer/highlight";

const languageConf = new Compartment();
const themeConf = new Compartment();
const indentConf = new Compartment();
const phrasesConf = new Compartment();

// Keyed by the name of the Kotlin document format. A format that is absent,
// such as CSV, is edited as plain text.
const languages = {
    JSON: json,
    XML: xml,
    HTML: html,
    CSS: css,
    JAVASCRIPT: javascript,
};

const lightPalette = {
    keyword: "#7f0055",
    string: "#a31515",
    number: "#098658",
    property: "#0451a5",
    atom: "#0000ff",
    comment: "#008000",
    tag: "#800000",
    attribute: "#e50000",
    punctuation: "#383a42",
};

const darkPalette = {
    keyword: "#c586c0",
    string: "#ce9178",
    number: "#b5cea8",
    property: "#9cdcfe",
    atom: "#569cd6",
    comment: "#6a9955",
    tag: "#4ec9b0",
    attribute: "#9cdcfe",
    punctuation: "#d4d4d4",
};

/** Reports an edit to the Kotlin side, unless the edit came from it. */
let applyingFromHost = false;

function languageFor(format) {
    const factory = languages[format];
    return factory ? factory() : [];
}

function highlightFor(palette) {
    return HighlightStyle.define([
        { tag: tags.keyword, color: palette.keyword },
        { tag: [tags.string, tags.special(tags.string)], color: palette.string },
        { tag: [tags.number, tags.integer, tags.float], color: palette.number },
        { tag: [tags.propertyName, tags.definition(tags.propertyName)], color: palette.property },
        { tag: [tags.bool, tags.null, tags.atom], color: palette.atom },
        { tag: [tags.comment, tags.lineComment, tags.blockComment], color: palette.comment, fontStyle: "italic" },
        { tag: [tags.tagName, tags.angleBracket], color: palette.tag },
        { tag: [tags.attributeName], color: palette.attribute },
        { tag: [tags.punctuation, tags.separator, tags.bracket], color: palette.punctuation },
        { tag: tags.invalid, color: "#e45649" },
    ]);
}

function themeFor(colors) {
    const view = EditorView.theme(
        {
            "&": {
                height: "100%",
                color: colors.codeText,
                backgroundColor: colors.codeBackground,
                fontSize: "13px",
            },
            ".cm-scroller": {
                fontFamily: "monospace",
                lineHeight: "1.5",
            },
            ".cm-content": {
                caretColor: colors.codeText,
            },
            ".cm-cursor, .cm-dropCursor": {
                borderLeftColor: colors.codeText,
            },
            ".cm-gutters": {
                backgroundColor: colors.gutterBackground,
                color: colors.gutterText,
                border: "none",
            },
            ".cm-activeLine, .cm-activeLineGutter": {
                backgroundColor: colors.activeLine,
            },
            ".cm-panels": {
                backgroundColor: colors.gutterBackground,
                color: colors.codeText,
            },
            ".cm-panels input, .cm-panels button": {
                color: colors.codeText,
                backgroundColor: colors.codeBackground,
                border: "1px solid " + colors.gutterText,
            },
            "&.cm-focused .cm-selectionBackground, .cm-selectionBackground, ::selection": {
                backgroundColor: colors.selection,
            },
            ".cm-searchMatch": {
                backgroundColor: colors.selection,
            },
        },
        { dark: colors.dark },
    );
    return [view, syntaxHighlighting(highlightFor(colors.dark ? darkPalette : lightPalette))];
}

const startColors = {
    dark: true,
    codeBackground: "#383838",
    codeText: "#e6e6e6",
    gutterBackground: "#2f2f2f",
    gutterText: "#7a7a7a",
    activeLine: "#00000022",
    selection: "#3883fa66",
};

const view = new EditorView({
    parent: document.getElementById("editor"),
    state: EditorState.create({
        doc: "",
        extensions: [
            lineNumbers(),
            highlightActiveLine(),
            highlightActiveLineGutter(),
            drawSelection(),
            rectangularSelection(),
            bracketMatching(),
            indentOnInput(),
            codeFolding(),
            foldGutter(),
            search({ top: true }),
            keymap.of([...defaultKeymap, ...searchKeymap, ...foldKeymap, indentWithTab]),
            languageConf.of([]),
            indentConf.of(indentUnit.of("  ")),
            phrasesConf.of([]),
            themeConf.of(themeFor(startColors)),
            EditorView.updateListener.of((update) => {
                if (update.docChanged && !applyingFromHost && window.EditorHost) {
                    window.EditorHost.onContentChanged(update.state.doc.toString());
                }
            }),
        ],
    }),
});

/** Called from Kotlin. Every entry point is a no-op when nothing would change. */
window.SimpleCodeEditor = {
    setContent(text) {
        if (text === view.state.doc.toString()) return;
        applyingFromHost = true;
        view.dispatch({
            changes: { from: 0, to: view.state.doc.length, insert: text },
        });
        applyingFromHost = false;
    },

    setFormat(format) {
        view.dispatch({ effects: languageConf.reconfigure(languageFor(format)) });
    },

    setIndentWidth(width) {
        view.dispatch({ effects: indentConf.reconfigure(indentUnit.of(" ".repeat(width))) });
    },

    setColors(colors) {
        view.dispatch({ effects: themeConf.reconfigure(themeFor(colors)) });
    },

    /** Labels of the search panel, translated on the Kotlin side. */
    setPhrases(phrases) {
        view.dispatch({ effects: phrasesConf.reconfigure(EditorState.phrases.of(phrases)) });
    },

    setSearchVisible(visible) {
        if (visible) {
            openSearchPanel(view);
        } else {
            closeSearchPanel(view);
        }
    },

    foldAll() {
        foldAll(view);
    },

    unfoldAll() {
        unfoldAll(view);
    },
};

if (window.EditorHost) {
    window.EditorHost.onReady();
}
