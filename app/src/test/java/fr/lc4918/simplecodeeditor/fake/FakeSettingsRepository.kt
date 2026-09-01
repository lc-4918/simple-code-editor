package fr.lc4918.simplecodeeditor.fake

import fr.lc4918.simplecodeeditor.data.SettingsRepository
import fr.lc4918.simplecodeeditor.model.AppLanguage
import fr.lc4918.simplecodeeditor.model.CsvDelimiter
import fr.lc4918.simplecodeeditor.model.ThemeOption
import fr.lc4918.simplecodeeditor.model.UpdateMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/** In-memory preferences, so the view model can be tested without Android. */
class FakeSettingsRepository : SettingsRepository {

    private val themeState = MutableStateFlow(ThemeOption.DEFAULT)
    private val languageState = MutableStateFlow(AppLanguage.DEFAULT)
    private val indentState = MutableStateFlow(SettingsRepository.DEFAULT_INDENT_WIDTH)
    private val delimiterState = MutableStateFlow(CsvDelimiter.DEFAULT)
    private val updateState = MutableStateFlow(UpdateMode.DEFAULT)

    override val theme: Flow<ThemeOption> = themeState
    override val language: Flow<AppLanguage> = languageState
    override val indentWidth: Flow<Int> = indentState
    override val csvDelimiter: Flow<CsvDelimiter> = delimiterState
    override val updateMode: Flow<UpdateMode> = updateState

    override suspend fun setTheme(option: ThemeOption) {
        themeState.value = option
    }

    override suspend fun setLanguage(language: AppLanguage) {
        languageState.value = language
    }

    override suspend fun setCsvDelimiter(delimiter: CsvDelimiter) {
        delimiterState.value = delimiter
    }

    override suspend fun setUpdateMode(mode: UpdateMode) {
        updateState.value = mode
    }

    override suspend fun setIndentWidth(width: Int) {
        indentState.value = width.coerceIn(
            SettingsRepository.MIN_INDENT,
            SettingsRepository.MAX_INDENT,
        )
    }
}
