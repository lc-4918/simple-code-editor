package fr.lc4918.simplecodeeditor.data

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import fr.lc4918.simplecodeeditor.model.AppLanguage

/**
 * Applies the interface language through the per-app language API.
 *
 * The AndroidX implementation backports the behaviour below Android 13, so the
 * same call works on every version the app supports. It stores the choice and
 * restores it at the next start, which the manifest enables, so applying the
 * stored language again on a later start changes nothing and leaves the
 * activity alone.
 */
object LocaleController {

    fun apply(language: AppLanguage) {
        val locales = if (language.languageTag.isEmpty()) {
            LocaleListCompat.getEmptyLocaleList()
        } else {
            LocaleListCompat.forLanguageTags(language.languageTag)
        }
        if (AppCompatDelegate.getApplicationLocales() != locales) {
            AppCompatDelegate.setApplicationLocales(locales)
        }
    }
}
