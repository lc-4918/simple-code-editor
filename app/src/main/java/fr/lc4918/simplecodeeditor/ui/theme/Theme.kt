package fr.lc4918.simplecodeeditor.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import fr.lc4918.simplecodeeditor.model.ThemeOption

/**
 * Whether the chosen option comes out dark.
 *
 * The system bars need the same answer as the theme, so both read it here
 * rather than each working it out.
 */
@Composable
fun isDarkTheme(option: ThemeOption): Boolean = when (option) {
    ThemeOption.SYSTEM -> isSystemInDarkTheme()
    ThemeOption.LIGHT -> false
    ThemeOption.DARK -> true
}

private val DarkColorScheme = darkColorScheme(
    primary = EditorBlueLight,
    secondary = EditorGreenLight,
    tertiary = EditorSlateLight,
)

private val LightColorScheme = lightColorScheme(
    primary = EditorBlue,
    secondary = EditorGreen,
    tertiary = EditorSlate,
)

/**
 * Applies the colour scheme chosen by the user.
 *
 * Dynamic colour is deliberately left out: the editing surface has to stay
 * readable and consistent with the syntax highlighting, which a wallpaper
 * derived palette would not guarantee.
 */
@Composable
fun SimpleCodeEditorTheme(
    themeOption: ThemeOption = ThemeOption.DEFAULT,
    content: @Composable () -> Unit,
) {
    val darkTheme = isDarkTheme(themeOption)

    CompositionLocalProvider(
        LocalEditorColors provides if (darkTheme) DarkEditorColors else LightEditorColors,
    ) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
            typography = Typography,
            content = content,
        )
    }
}
