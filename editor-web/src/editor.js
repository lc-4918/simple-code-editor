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
import { lintGutter, setDiagnostics } from "@codemirror/lint";
import { jsonrepair } from "jsonrepair";
import { css } from "@codemirror/lang-css";
import { html } from "@codemirror/lang-html";
import { javascript } from "@codemirror/lang-javascript";
import { json } from "@codemirror/lang-json";
import { markdown } from "@codemirror/lang-markdown";
import { xml } from "@codemirror/lang-xml";
import { marked } from "marked";
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
    MARKDOWN: markdown,
};

const lightPalette = {
    heading: "#0451a5",
    marker: "#7f7f7f",
    link: "#3883fa",
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
    heading: "#9cdcfe",
    marker: "#9a9a9a",
    link: "#7cb7ff",
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

        // What markdown is made of. None of the tags above apply to it, which
        // is why it came out as plain text.
        { tag: tags.heading, color: palette.heading, fontWeight: "bold" },
        { tag: tags.strong, color: palette.property, fontWeight: "bold" },
        { tag: tags.emphasis, color: palette.property, fontStyle: "italic" },
        { tag: tags.strikethrough, textDecoration: "line-through" },
        { tag: [tags.link, tags.url], color: palette.link, textDecoration: "underline" },
        { tag: tags.monospace, color: palette.string },
        { tag: tags.quote, color: palette.comment, fontStyle: "italic" },
        { tag: tags.list, color: palette.marker },
        // The hashes, dashes and backticks that carry the meaning rather than
        // the text: dimmed, so the text they mark stands out from them.
        { tag: [tags.processingInstruction, tags.contentSeparator], color: palette.marker },
    ]);
}

function previewStyle(colors) {
    return `<style>
      body { margin: 0; padding: 12px 16px; background: ${colors.codeBackground};
             color: ${colors.codeText}; font-family: system-ui, sans-serif;
             line-height: 1.5; overflow-wrap: break-word; }
      a { color: ${colors.dark ? "#9ec1fd" : "#3883fa"}; }
      code, pre { background: ${colors.gutterBackground}; border-radius: 4px; }
      code { padding: 1px 4px; font-family: monospace; }
      pre { padding: 10px; overflow-x: auto; }
      pre code { background: none; padding: 0; }
      blockquote { margin: 0 0 0 4px; padding-left: 12px;
                   border-left: 3px solid ${colors.gutterText}; color: ${colors.gutterText}; }
      table { border-collapse: collapse; }
      th, td { border: 1px solid ${colors.gutterText}; padding: 4px 8px; }
      img { max-width: 100%; }
      hr { border: none; border-top: 1px solid ${colors.gutterText}; }
    </style>`;
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

/**
 * What the rendered document is dressed in.
 *
 * Written into the frame rather than linked, the frame being allowed no
 * connection of its own, and following the colours of the surface so that the
 * two ways of looking at one document belong to the same editor.
 */
let PREVIEW_STYLE = "";

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
            lintGutter(),
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
        PREVIEW_STYLE = previewStyle(colors);
        const frame = document.getElementById("preview");
        if (frame && !frame.hidden) {
            frame.srcdoc = PREVIEW_STYLE + marked.parse(view.state.doc.toString());
        }
    },

    /**
     * Shows the document as it will be read, or hides that again.
     *
     * What is rendered goes into a frame that is allowed nothing: no script
     * of its own runs and it cannot reach back here. The document belongs to
     * whoever opened it, but a document can come from anywhere, and the page
     * it is rendered on holds the bridge to the application.
     */
    setPreview(visible) {
        const frame = document.getElementById("preview");
        const editor = document.getElementById("editor");
        if (!visible) {
            frame.hidden = true;
            editor.hidden = false;
            frame.srcdoc = "";
            return;
        }
        frame.srcdoc = PREVIEW_STYLE + marked.parse(view.state.doc.toString());
        editor.hidden = true;
        frame.hidden = false;
    },

    /**
     * Makes a broken JSON document readable again, and returns it.
     *
     * Returns null when nothing can be made of it, which leaves the document
     * as it was rather than replacing it with a guess.
     */
    repair(text) {
        try {
            return jsonrepair(text);
        } catch (error) {
            return null;
        }
    },

    /**
     * Marks what is wrong with the document, or clears the mark.
     *
     * The reading is done on the Kotlin side, which knows the formats and
     * says why in the language of the interface; only the place and the
     * sentence travel here.
     */
    setDiagnostic(problem) {
        const length = view.state.doc.length;
        let diagnostics = [];
        if (problem !== null && length > 0) {
            // A place at the very end has nothing after it to underline, so
            // the character before it is marked instead.
            let from = Math.min(problem.offset, length);
            let to = Math.min(from + 1, length);
            if (from === to) from = Math.max(0, to - 1);
            diagnostics = [{ from, to, severity: "error", message: problem.message }];
        }
        view.dispatch(setDiagnostics(view.state, diagnostics));
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
