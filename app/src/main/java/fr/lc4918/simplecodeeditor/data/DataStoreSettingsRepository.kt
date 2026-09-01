package fr.lc4918.simplecodeeditor.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import fr.lc4918.simplecodeeditor.model.AppLanguage
import fr.lc4918.simplecodeeditor.model.CsvDelimiter
import fr.lc4918.simplecodeeditor.model.ThemeOption
import fr.lc4918.simplecodeeditor.model.UpdateMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "editor_settings",
)

/** Preferences backed by a Jetpack DataStore file. */
class DataStoreSettingsRepository(private val context: Context) : SettingsRepository {

    override val theme: Flow<ThemeOption> = context.settingsDataStore.data.map { preferences ->
        ThemeOption.fromStorageKey(preferences[KEY_THEME])
    }

    override val language: Flow<AppLanguage> = context.settingsDataStore.data.map { preferences ->
        AppLanguage.fromStorageKey(preferences[KEY_LANGUAGE])
    }

    override val indentWidth: Flow<Int> = context.settingsDataStore.data.map { preferences ->
        preferences[KEY_INDENT_WIDTH] ?: SettingsRepository.DEFAULT_INDENT_WIDTH
    }

    override val csvDelimiter: Flow<CsvDelimiter> =
        context.settingsDataStore.data.map { preferences ->
            CsvDelimiter.fromStorageKey(preferences[KEY_CSV_DELIMITER])
        }

    override val updateMode: Flow<UpdateMode> = context.settingsDataStore.data.map { preferences ->
        UpdateMode.fromStorageKey(preferences[KEY_UPDATE_MODE])
    }

    override suspend fun setTheme(option: ThemeOption) {
        context.settingsDataStore.edit { it[KEY_THEME] = option.storageKey }
    }

    override suspend fun setLanguage(language: AppLanguage) {
        context.settingsDataStore.edit { it[KEY_LANGUAGE] = language.storageKey }
    }

    override suspend fun setIndentWidth(width: Int) {
        context.settingsDataStore.edit {
            it[KEY_INDENT_WIDTH] = width.coerceIn(
                SettingsRepository.MIN_INDENT,
                SettingsRepository.MAX_INDENT,
            )
        }
    }

    override suspend fun setCsvDelimiter(delimiter: CsvDelimiter) {
        context.settingsDataStore.edit { it[KEY_CSV_DELIMITER] = delimiter.storageKey }
    }

    override suspend fun setUpdateMode(mode: UpdateMode) {
        context.settingsDataStore.edit { it[KEY_UPDATE_MODE] = mode.storageKey }
    }

    private companion object {
        val KEY_THEME = stringPreferencesKey("theme")
        val KEY_LANGUAGE = stringPreferencesKey("language")
        val KEY_INDENT_WIDTH = intPreferencesKey("indent_width")
        val KEY_CSV_DELIMITER = stringPreferencesKey("csv_delimiter")
        val KEY_UPDATE_MODE = stringPreferencesKey("update_mode")
    }
}
