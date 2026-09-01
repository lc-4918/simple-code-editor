package fr.lc4918.simplecodeeditor.model

import androidx.annotation.StringRes
import fr.lc4918.simplecodeeditor.R

/**
 * User choice for the interface language.
 *
 * English is the fallback shipped in the default resource folder, so [SYSTEM]
 * resolves to English on any device whose locale is neither French nor Spanish.
 */
enum class AppLanguage(
    val storageKey: String,
    /** BCP 47 tag, empty for the system default. */
    val languageTag: String,
    @param:StringRes override val labelRes: Int,
) : LabelledOption {
    SYSTEM("system", "", R.string.language_system),
    ENGLISH("en", "en", R.string.language_english),
    FRENCH("fr", "fr", R.string.language_french),
    SPANISH("es", "es", R.string.language_spanish);

    companion object {
        val DEFAULT = SYSTEM

        fun fromStorageKey(key: String?): AppLanguage =
            entries.firstOrNull { it.storageKey == key } ?: DEFAULT
    }
}
