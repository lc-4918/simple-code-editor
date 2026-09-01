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
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import fr.lc4918.simplecodeeditor.data.LocaleController
import fr.lc4918.simplecodeeditor.editor.EditorViewModel
import fr.lc4918.simplecodeeditor.ui.EditorScreen
import fr.lc4918.simplecodeeditor.ui.theme.SimpleCodeEditorTheme

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: EditorViewModel = viewModel(factory = EditorViewModel.Factory)
            val state by viewModel.uiState.collectAsStateWithLifecycle()

            LaunchedEffect(state.language) {
                LocaleController.apply(state.language)
            }

            SimpleCodeEditorTheme(themeOption = state.theme) {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    EditorScreen(
                        state = state,
                        onViewModeSelected = viewModel::setViewMode,
                        modifier = Modifier.padding(innerPadding),
                    )
                }
            }
        }
    }
}
