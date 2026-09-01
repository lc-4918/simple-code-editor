package fr.lc4918.simplecodeeditor.data

import fr.lc4918.simplecodeeditor.model.AppLanguage
import fr.lc4918.simplecodeeditor.model.CsvDelimiter
import fr.lc4918.simplecodeeditor.model.ThemeOption
import kotlinx.coroutines.flow.Flow

/** Persisted user preferences: colour scheme, language, indentation, CSV separator. */
interface SettingsRepository {

    val theme: Flow<ThemeOption>
    val language: Flow<AppLanguage>
    val indentWidth: Flow<Int>
    val csvDelimiter: Flow<CsvDelimiter>

    suspend fun setTheme(option: ThemeOption)
    suspend fun setLanguage(language: AppLanguage)
    suspend fun setIndentWidth(width: Int)
    suspend fun setCsvDelimiter(delimiter: CsvDelimiter)

    companion object {
        const val DEFAULT_INDENT_WIDTH = 2
        const val MIN_INDENT = 1
        const val MAX_INDENT = 8
    }
}
