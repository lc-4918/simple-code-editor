package fr.lc4918.simplecodeeditor.model

import androidx.annotation.StringRes
import fr.lc4918.simplecodeeditor.R

/** User choice for the colour scheme. */
enum class ThemeOption(val storageKey: String, @param:StringRes override val labelRes: Int) : LabelledOption {
    SYSTEM("system", R.string.theme_system),
    LIGHT("light", R.string.theme_light),
    DARK("dark", R.string.theme_dark);

    companion object {
        val DEFAULT = SYSTEM

        fun fromStorageKey(key: String?): ThemeOption =
            entries.firstOrNull { it.storageKey == key } ?: DEFAULT
    }
}
