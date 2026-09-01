package fr.lc4918.simplecodeeditor

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import fr.lc4918.simplecodeeditor.data.LocaleController
import fr.lc4918.simplecodeeditor.editor.EditorViewModel
import fr.lc4918.simplecodeeditor.ui.EditorActions
import fr.lc4918.simplecodeeditor.ui.EditorScreen
import fr.lc4918.simplecodeeditor.ui.theme.SimpleCodeEditorTheme

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: EditorViewModel = viewModel(factory = EditorViewModel.Factory)
            val state by viewModel.uiState.collectAsStateWithLifecycle()

            // AppCompat keeps the language across starts, so this only has an
            // effect, and only recreates the activity, when the user changes it.
            LaunchedEffect(state.isLanguageLoaded, state.language) {
                if (state.isLanguageLoaded) {
                    LocaleController.apply(state.language)
                }
            }

            LaunchedEffect(state.isFullScreen) {
                setSystemBarsVisible(!state.isFullScreen)
            }

            val actions = remember(viewModel) {
                EditorActions(
                    onDocumentNameChanged = viewModel::onDocumentNameChanged,
                    onContentChanged = viewModel::onContentChanged,
                    onNew = { viewModel.newDocument() },
                    // Opening, saving and copying need the activity result
                    // launchers and the clipboard, which come with the storage
                    // step.
                    onOpen = {},
                    onSave = {},
                    onCopy = {},
                    onToggleFullScreen = viewModel::toggleFullScreen,
                    onViewModeSelected = viewModel::setViewMode,
                    onTool = viewModel::onTool,
                    onThemeSelected = viewModel::setTheme,
                    onLanguageSelected = viewModel::setLanguage,
                    onIndentWidthSelected = viewModel::setIndentWidth,
                )
            }

            SimpleCodeEditorTheme(themeOption = state.theme) {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    EditorScreen(
                        state = state,
                        actions = actions,
                        modifier = Modifier.padding(innerPadding),
                    )
                }
            }
        }
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
