package fr.lc4918.simplecodeeditor.ui

import android.annotation.SuppressLint
import android.util.Log
import android.webkit.ConsoleMessage
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.viewinterop.AndroidView
import fr.lc4918.simplecodeeditor.R
import fr.lc4918.simplecodeeditor.model.Diagnostic
import fr.lc4918.simplecodeeditor.model.DocumentFormat
import fr.lc4918.simplecodeeditor.ui.theme.EditorColors
import fr.lc4918.simplecodeeditor.ui.theme.LocalEditorColors
import org.json.JSONObject
import java.util.Locale

private const val EDITOR_URL = "file:///android_asset/editor/index.html"

/** Name the bundle calls back on. */
private const val HOST_OBJECT = "EditorHost"

/**
 * Editing surface in text mode, backed by the editor bundled in the assets.
 *
 * The document and its history stay on the Kotlin side. This surface pushes
 * the state it is given into the bundle and reports back every edit made in
 * it, which keeps a single source of truth.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun CodeMirrorSurface(
    content: String,
    format: DocumentFormat,
    indentWidth: Int,
    isSearchVisible: Boolean,
    diagnostic: Diagnostic?,
    controller: CodeMirrorController,
    onContentChanged: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalEditorColors.current
    // Resolved here, where the language of the interface is at hand, and sent
    // over already said rather than as something for the bundle to look up.
    val problemText = diagnostic?.let { stringResource(it.problem.labelRes) }
    val phrases = searchPhrases()
    val currentOnContentChanged by rememberUpdatedState(onContentChanged)

    AndroidView(
        modifier = modifier.fillMaxSize(),
        factory = { context ->
            WebView(context).apply {
                settings.javaScriptEnabled = true
                // The editor draws its own background, and the default white
                // one would flash on every theme change.
                setBackgroundColor(Color.Transparent.toArgb())
                isVerticalScrollBarEnabled = false
                isHorizontalScrollBarEnabled = false
                webChromeClient = LoggingChromeClient()
                webViewClient = LoggingWebViewClient()
                addJavascriptInterface(
                    EditorHost(controller) { text -> currentOnContentChanged(text) },
                    HOST_OBJECT,
                )
                controller.attach(this)
                loadUrl(EDITOR_URL)
            }
        },
    )

    LaunchedEffect(controller, content) { controller.setContent(content) }
    LaunchedEffect(controller, format) { controller.setFormat(format) }
    LaunchedEffect(controller, indentWidth) { controller.setIndentWidth(indentWidth) }
    LaunchedEffect(controller, colors) { controller.setColors(colors) }
    LaunchedEffect(controller, phrases) { controller.setPhrases(phrases) }
    LaunchedEffect(controller, isSearchVisible) { controller.setSearchVisible(isSearchVisible) }
    LaunchedEffect(controller, diagnostic, problemText) {
        controller.setDiagnostic(diagnostic?.offset, problemText)
    }

    DisposableEffect(controller) {
        onDispose { controller.detach() }
    }
}

/**
 * Imperative handle on the embedded editor.
 *
 * Calls made before the bundle reports itself ready are queued, because the
 * screen sets the document and the colours as soon as it composes, which is
 * well before the page has finished loading.
 */
class CodeMirrorController {

    private var webView: WebView? = null
    private var isReady = false
    private val pending = ArrayDeque<String>()

    /** The text the bundle is known to hold, to avoid pushing back its own edits. */
    private var lastKnownContent: String? = null

    internal fun attach(view: WebView) {
        webView = view
        isReady = false
        lastKnownContent = null
    }

    internal fun detach() {
        webView = null
        isReady = false
        lastKnownContent = null
        pending.clear()
    }

    internal fun markReady() {
        val view = webView ?: return
        view.post {
            isReady = true
            while (pending.isNotEmpty()) {
                view.evaluateJavascript(pending.removeFirst(), null)
            }
        }
    }

    internal fun contentReported(text: String) {
        lastKnownContent = text
    }

    fun setContent(text: String) {
        if (text == lastKnownContent) return
        lastKnownContent = text
        call("SimpleCodeEditor.setContent(${text.toJs()});")
    }

    fun setFormat(format: DocumentFormat) {
        call("SimpleCodeEditor.setFormat(${format.name.toJs()});")
    }

    fun setIndentWidth(width: Int) {
        call("SimpleCodeEditor.setIndentWidth($width);")
    }

    fun setColors(colors: EditorColors) {
        call("SimpleCodeEditor.setColors(${colors.toJson()});")
    }

    fun setPhrases(phrases: String) {
        call("SimpleCodeEditor.setPhrases($phrases);")
    }

    fun setSearchVisible(visible: Boolean) {
        call("SimpleCodeEditor.setSearchVisible($visible);")
    }

    /** Marks the spot the document could not be read past, or clears the mark. */
    fun setDiagnostic(offset: Int?, message: String?) {
        val problem = if (offset == null || message == null) {
            "null"
        } else {
            JSONObject().put("offset", offset).put("message", message).toString()
        }
        call("SimpleCodeEditor.setDiagnostic($problem);")
    }

    fun foldAll() {
        call("SimpleCodeEditor.foldAll();")
    }

    fun unfoldAll() {
        call("SimpleCodeEditor.unfoldAll();")
    }

    private fun call(script: String) {
        val view = webView
        if (view == null || !isReady) {
            pending.addLast(script)
            return
        }
        view.post { view.evaluateJavascript(script, null) }
    }
}

/** Receives the callbacks of the bundle, on a thread of the web view. */
private class EditorHost(
    private val controller: CodeMirrorController,
    private val onChange: (String) -> Unit,
) {
    @JavascriptInterface
    fun onReady() {
        controller.markReady()
    }

    @JavascriptInterface
    fun onContentChanged(text: String) {
        controller.contentReported(text)
        onChange(text)
    }
}

/**
 * Labels of the search panel, keyed by the wording the editor asks for.
 *
 * The panel belongs to the bundle, so its labels have to travel with the rest
 * of the state rather than being resolved on the page.
 */
@Composable
private fun searchPhrases(): String = JSONObject().apply {
    put("Find", stringResource(R.string.search_find))
    put("Replace", stringResource(R.string.search_replace_field))
    put("next", stringResource(R.string.search_next))
    put("previous", stringResource(R.string.search_previous))
    put("all", stringResource(R.string.search_all))
    put("match case", stringResource(R.string.search_match_case))
    put("regexp", stringResource(R.string.search_regexp))
    put("by word", stringResource(R.string.search_by_word))
    put("replace", stringResource(R.string.search_replace))
    put("replace all", stringResource(R.string.search_replace_all))
    put("close", stringResource(R.string.search_close))
}.toString()

/** Sends whatever the bundle reports to the log, to keep it diagnosable. */
private class LoggingChromeClient : WebChromeClient() {
    override fun onConsoleMessage(message: ConsoleMessage): Boolean {
        Log.d(LOG_TAG, "${message.message()} (${message.sourceId()}:${message.lineNumber()})")
        return true
    }
}

private class LoggingWebViewClient : WebViewClient() {
    override fun onReceivedError(
        view: WebView,
        request: WebResourceRequest,
        error: WebResourceError,
    ) {
        Log.e(LOG_TAG, "${request.url}: ${error.errorCode} ${error.description}")
    }
}

private const val LOG_TAG = "EditorSurface"

private fun String.toJs(): String = JSONObject.quote(this)

private fun EditorColors.toJson(): String = JSONObject().apply {
    put("dark", dark)
    put("codeBackground", codeBackground.toCss())
    put("codeText", codeText.toCss())
    put("gutterBackground", gutterBackground.toCss())
    put("gutterText", gutterText.toCss())
    put("activeLine", activeLine.toCss())
    put("selection", selection.toCss())
}.toString()

/** Alpha is kept, because the active line and the selection are translucent. */
private fun Color.toCss(): String {
    val argb = toArgb()
    return String.format(
        Locale.ROOT,
        "rgba(%d, %d, %d, %.3f)",
        (argb shr 16) and 0xFF,
        (argb shr 8) and 0xFF,
        argb and 0xFF,
        (argb ushr 24) / 255f,
    )
}
