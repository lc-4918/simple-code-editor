package fr.lc4918.simplecodeeditor.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Colours of the editing surface.
 *
 * These sit outside the Material scheme because the gutter and the code area
 * follow the reference layout rather than the Material roles, and because the
 * same values have to be handed to the embedded editor.
 */
@Immutable
data class EditorColors(
    val gutterBackground: Color,
    val gutterText: Color,
    val codeBackground: Color,
    val codeText: Color,
)

val LightEditorColors = EditorColors(
    gutterBackground = GutterBackgroundLight,
    gutterText = GutterTextLight,
    codeBackground = CodeBackgroundLight,
    codeText = CodeTextLight,
)

val DarkEditorColors = EditorColors(
    gutterBackground = GutterBackgroundDark,
    gutterText = GutterTextDark,
    codeBackground = CodeBackgroundDark,
    codeText = CodeTextDark,
)

val LocalEditorColors = staticCompositionLocalOf { LightEditorColors }
