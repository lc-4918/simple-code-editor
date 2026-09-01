package fr.lc4918.simplecodeeditor.data

import fr.lc4918.simplecodeeditor.model.AppLanguage
import fr.lc4918.simplecodeeditor.model.CsvDelimiter
import fr.lc4918.simplecodeeditor.model.ThemeOption
import fr.lc4918.simplecodeeditor.model.UpdateMode
import kotlinx.coroutines.flow.Flow

/** Persisted user preferences: look, language, layout, separator, updates. */
interface SettingsRepository {

    val theme: Flow<ThemeOption>
    val language: Flow<AppLanguage>
    val indentWidth: Flow<Int>
    val csvDelimiter: Flow<CsvDelimiter>
    val updateMode: Flow<UpdateMode>

    suspend fun setTheme(option: ThemeOption)
    suspend fun setLanguage(language: AppLanguage)
    suspend fun setIndentWidth(width: Int)
    suspend fun setCsvDelimiter(delimiter: CsvDelimiter)
    suspend fun setUpdateMode(mode: UpdateMode)

    companion object {
        const val DEFAULT_INDENT_WIDTH = 2
        const val MIN_INDENT = 1
        const val MAX_INDENT = 8
    }
}
