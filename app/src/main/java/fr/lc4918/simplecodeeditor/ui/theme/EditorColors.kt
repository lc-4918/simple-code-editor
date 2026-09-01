package fr.lc4918.simplecodeeditor.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Colours of the sticky header and of the editing surface.
 *
 * These sit outside the Material scheme because the header, the gutter and the
 * code area follow the reference layout rather than the Material roles, and
 * because the same values have to be handed to the embedded editor.
 */
@Immutable
data class EditorColors(
    val titleBarBackground: Color,
    val titleBarContent: Color,
    val toolbarBackground: Color,
    val toolbarContent: Color,
    val toolbarSelectedBackground: Color,
    val toolbarSelectedContent: Color,
    val gutterBackground: Color,
    val gutterText: Color,
    val codeBackground: Color,
    val codeText: Color,
)

val LightEditorColors = EditorColors(
    titleBarBackground = TitleBarBackgroundLight,
    titleBarContent = TitleBarContentLight,
    toolbarBackground = ToolbarBackgroundLight,
    toolbarContent = ToolbarContentLight,
    toolbarSelectedBackground = ToolbarSelectedBackgroundLight,
    toolbarSelectedContent = ToolbarSelectedContentLight,
    gutterBackground = GutterBackgroundLight,
    gutterText = GutterTextLight,
    codeBackground = CodeBackgroundLight,
    codeText = CodeTextLight,
)

val DarkEditorColors = EditorColors(
    titleBarBackground = TitleBarBackgroundDark,
    titleBarContent = TitleBarContentDark,
    toolbarBackground = ToolbarBackgroundDark,
    toolbarContent = ToolbarContentDark,
    toolbarSelectedBackground = ToolbarSelectedBackgroundDark,
    toolbarSelectedContent = ToolbarSelectedContentDark,
    gutterBackground = GutterBackgroundDark,
    gutterText = GutterTextDark,
    codeBackground = CodeBackgroundDark,
    codeText = CodeTextDark,
)

val LocalEditorColors = staticCompositionLocalOf { LightEditorColors }
