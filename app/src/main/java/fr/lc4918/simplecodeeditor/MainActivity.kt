package fr.lc4918.simplecodeeditor

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import fr.lc4918.simplecodeeditor.data.LocaleController
import fr.lc4918.simplecodeeditor.editor.EditorUiState
import fr.lc4918.simplecodeeditor.editor.EditorViewModel
import fr.lc4918.simplecodeeditor.format.DocumentFormatter
import fr.lc4918.simplecodeeditor.model.CopyVariant
import fr.lc4918.simplecodeeditor.model.DocumentLocation
import fr.lc4918.simplecodeeditor.model.EditorDocument
import fr.lc4918.simplecodeeditor.model.OpenSource
import fr.lc4918.simplecodeeditor.model.SaveTarget
import fr.lc4918.simplecodeeditor.ui.CreateDocumentAt
import fr.lc4918.simplecodeeditor.ui.EditorActions
import fr.lc4918.simplecodeeditor.ui.EditorScreen
import fr.lc4918.simplecodeeditor.ui.NewDocument
import fr.lc4918.simplecodeeditor.ui.OpenEditableDocument
import fr.lc4918.simplecodeeditor.ui.theme.SimpleCodeEditorTheme
import fr.lc4918.simplecodeeditor.ui.theme.isDarkTheme

class MainActivity : AppCompatActivity() {

    /**
     * Document handed over by a file browser, waiting to be read.
     *
     * It is only taken from a fresh start: on a recreation the same intent is
     * handed back, and reading it again would throw away what was edited.
     */
    private val pendingDocument = mutableStateOf<Uri?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) pendingDocument.value = intent.documentUri()
        enableEdgeToEdge()
        setContent {
            val viewModel: EditorViewModel = viewModel(factory = EditorViewModel.Factory)
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            val context = LocalContext.current

            // AppCompat keeps the language across starts, so this only has an
            // effect, and only recreates the activity, when the user changes it.
            LaunchedEffect(state.isLanguageLoaded, state.language) {
                if (state.isLanguageLoaded) {
                    LocaleController.apply(state.language)
                }
            }

            LaunchedEffect(pendingDocument.value) {
                pendingDocument.value?.let { uri ->
                    viewModel.open(DocumentLocation(uri.toString()))
                    pendingDocument.value = null
                }
            }

            LaunchedEffect(state.isFullScreen) {
                setSystemBarsVisible(!state.isFullScreen)
            }

            // The system bars sit over the application, not over the system,
            // so their icons have to follow the theme chosen here. Without
            // this a light application under a dark system draws white icons
            // on its own pale background, where nothing shows.
            val darkTheme = isDarkTheme(state.theme)
            LaunchedEffect(darkTheme) {
                setLightSystemBars(!darkTheme)
            }

            val openLauncher = rememberLauncherForActivityResult(
                remember { OpenEditableDocument() },
            ) { uri -> uri?.let { viewModel.open(DocumentLocation(it.toString())) } }

            val createLauncher = rememberLauncherForActivityResult(
                remember { CreateDocumentAt() },
            ) { uri -> uri?.let { viewModel.save(DocumentLocation(it.toString())) } }

            val snackbarHostState = remember { SnackbarHostState() }
            LaunchedEffect(state.statusMessageRes) {
                state.statusMessageRes?.let { messageRes ->
                    snackbarHostState.showSnackbar(context.getString(messageRes))
                    viewModel.statusShown()
                }
            }

            val actions = remember(viewModel) {
                EditorActions(
                    onDocumentNameChanged = viewModel::onDocumentNameChanged,
                    onContentChanged = viewModel::onContentChanged,
                    onNew = { viewModel.newDocument() },
                    onOpen = { source ->
                        when (source) {
                            // Every type is offered rather than the ones the
                            // application declares: providers index plenty of
                            // supported files, GPX among them, as a plain byte
                            // stream, and a narrower filter greys them out. The
                            // format is worked out from the name once open.
                            OpenSource.DEVICE -> openLauncher.launch(arrayOf("*/*"))
                            // Asked for by the screen, which then calls onOpenUrl.
                            OpenSource.URL -> Unit
                        }
                    },
                    onOpenUrl = viewModel::openUrl,
                    onSave = { target ->
                        val document = viewModel.uiState.value.document
                        when (target) {
                            // Writing back in place needs somewhere to write
                            // back to, so a document that has never been stored
                            // asks for a destination like the cloud one does.
                            SaveTarget.DEVICE -> document.origin
                                ?.let(viewModel::save)
                                ?: createLauncher.launch(document.newDocument())

                            SaveTarget.CLOUD -> createLauncher.launch(document.newDocument())
                            SaveTarget.URL -> Unit
                        }
                    },
                    onSaveUrl = viewModel::saveUrl,
                    onCopy = { variant ->
                        val state = viewModel.uiState.value
                        copyToClipboard(state.document.fileName(), state.copyText(variant))
                        // From Android 13 on the system shows its own notice,
                        // and a second one on top of it would only repeat it.
                        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                            viewModel.reportCopied()
                        }
                    },
                    onToggleFullScreen = viewModel::toggleFullScreen,
                    onViewModeSelected = viewModel::setViewMode,
                    onTool = viewModel::onTool,
                    onCellChanged = viewModel::onCellChanged,
                    onAddRow = viewModel::addRow,
                    onSort = viewModel::sort,
                    onFilter = viewModel::filter,
                    onThemeSelected = viewModel::setTheme,
                    onLanguageSelected = viewModel::setLanguage,
                    onIndentWidthSelected = viewModel::setIndentWidth,
                    onCsvDelimiterSelected = viewModel::setCsvDelimiter,
                )
            }

            SimpleCodeEditorTheme(themeOption = state.theme) {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    snackbarHost = { SnackbarHost(snackbarHostState) },
                    // Nothing is kept clear of the system bars in full screen,
                    // since they are not there: leaving the room they take
                    // would give the document a band of nothing instead.
                    contentWindowInsets = if (state.isFullScreen) {
                        WindowInsets(0)
                    } else {
                        ScaffoldDefaults.contentWindowInsets
                    },
                ) { innerPadding ->
                    EditorScreen(
                        state = state,
                        actions = actions,
                        modifier = Modifier.padding(innerPadding),
                    )
                }
            }
        }
    }

    private fun copyToClipboard(label: String, text: String) {
        val clipboard = getSystemService(ClipboardManager::class.java)
        clipboard.setPrimaryClip(ClipData.newPlainText(label, text))
    }

    /** A second document opened while the editor was already running. */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingDocument.value = intent.documentUri()
    }

    /** Dark icons in the system bars over a light application, and the reverse. */
    private fun setLightSystemBars(light: Boolean) {
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        controller.isAppearanceLightStatusBars = light
        controller.isAppearanceLightNavigationBars = light
    }

    /** Hides the status and navigation bars while the editor is in full screen. */
    private fun setSystemBarsVisible(visible: Boolean) {
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        if (visible) {
            controller.show(WindowInsetsCompat.Type.systemBars())
        } else {
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            controller.hide(WindowInsetsCompat.Type.systemBars())
        }
    }
}

/** The document a browser is asking to open, when that is what the intent says. */
private fun Intent.documentUri(): Uri? =
    data.takeIf { action == Intent.ACTION_VIEW || action == Intent.ACTION_EDIT }

/** The document in the shape the chosen copy variant asks for. */
private fun EditorUiState.copyText(variant: CopyVariant): String = when (variant) {
    CopyVariant.FORMATTED -> DocumentFormatter.indent(document.content, format, indentWidth)
    CopyVariant.COMPACTED -> DocumentFormatter.compact(document.content, format)
    CopyVariant.ESCAPED -> DocumentFormatter.escape(document.content)
    CopyVariant.AS_IS -> document.content
}

/** What to suggest to the picker when asking for a destination. */
private fun EditorDocument.newDocument(): NewDocument =
    NewDocument(name = fileName(), mimeType = format.mimeTypes.first())
