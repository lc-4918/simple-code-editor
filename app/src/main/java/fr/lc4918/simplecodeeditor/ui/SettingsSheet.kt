package fr.lc4918.simplecodeeditor.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import fr.lc4918.simplecodeeditor.R
import fr.lc4918.simplecodeeditor.data.SettingsRepository
import fr.lc4918.simplecodeeditor.model.AppLanguage
import fr.lc4918.simplecodeeditor.model.CsvDelimiter
import fr.lc4918.simplecodeeditor.model.LabelledOption
import fr.lc4918.simplecodeeditor.model.ThemeOption
import fr.lc4918.simplecodeeditor.model.UpdateMode

/** Bottom sheet holding the three persisted preferences. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSheet(
    theme: ThemeOption,
    language: AppLanguage,
    indentWidth: Int,
    csvDelimiter: CsvDelimiter,
    updateMode: UpdateMode,
    isCheckingUpdate: Boolean,
    updateMessageRes: Int?,
    onThemeSelected: (ThemeOption) -> Unit,
    onLanguageSelected: (AppLanguage) -> Unit,
    onIndentWidthSelected: (Int) -> Unit,
    onCsvDelimiterSelected: (CsvDelimiter) -> Unit,
    onUpdateModeSelected: (UpdateMode) -> Unit,
    onCheckForUpdate: () -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        // The settings are taller than a half open sheet, so there is no half
        // open state: it opens the whole way and the content scrolls inside
        // it. With a stop in between, dragging is answered by the sheet and by
        // the content at once, and the two fight over the same gesture.
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                // Without this the content does not scroll at all: what looked
                // like scrolling was the sheet being dragged, and once it had
                // nowhere left to go the drag had nowhere to be answered.
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Text(
                text = stringResource(R.string.action_settings),
                style = MaterialTheme.typography.titleLarge,
            )
            OptionSection(
                title = stringResource(R.string.settings_theme),
                options = ThemeOption.entries,
                selected = theme,
                onSelected = onThemeSelected,
            )
            OptionSection(
                title = stringResource(R.string.settings_language),
                options = AppLanguage.entries,
                selected = language,
                onSelected = onLanguageSelected,
            )
            IndentSection(
                selected = indentWidth,
                onSelected = onIndentWidthSelected,
            )
            OptionSection(
                title = stringResource(R.string.settings_delimiter),
                options = CsvDelimiter.entries,
                selected = csvDelimiter,
                onSelected = onCsvDelimiterSelected,
            )
            OptionSection(
                title = stringResource(R.string.settings_updates),
                options = UpdateMode.entries,
                selected = updateMode,
                onSelected = onUpdateModeSelected,
            )
            // Left to itself, the application looks as it opens and says
            // nothing unless there is something to say; asked to wait, it
            // needs to be asked, and this is where.
            if (updateMode == UpdateMode.MANUAL) {
                UpdateCheckButton(isChecking = isCheckingUpdate, onCheck = onCheckForUpdate)
            }
            updateMessageRes?.let { messageRes ->
                Text(
                    text = stringResource(messageRes),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

@Composable
private fun UpdateCheckButton(isChecking: Boolean, onCheck: () -> Unit) {
    OutlinedButton(onClick = onCheck, enabled = !isChecking) {
        if (isChecking) {
            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
            Spacer(Modifier.width(12.dp))
            Text(stringResource(R.string.update_checking))
        } else {
            Text(stringResource(R.string.update_check))
        }
    }
}

@Composable
private fun <T : LabelledOption> OptionSection(
    title: String,
    options: List<T>,
    selected: T,
    onSelected: (T) -> Unit,
) {
    SettingSection(title) {
        options.forEach { option ->
            FilterChip(
                selected = option == selected,
                onClick = { onSelected(option) },
                label = { Text(stringResource(option.labelRes)) },
            )
        }
    }
}

/** Number of spaces the format tool and the editing surface indent with. */
@Composable
private fun IndentSection(
    selected: Int,
    onSelected: (Int) -> Unit,
) {
    SettingSection(stringResource(R.string.settings_indentation)) {
        (SettingsRepository.MIN_INDENT..SettingsRepository.MAX_INDENT).forEach { width ->
            FilterChip(
                selected = width == selected,
                onClick = { onSelected(width) },
                label = { Text(width.toString()) },
            )
        }
    }
}

@Composable
private fun SettingSection(
    title: String,
    content: @Composable () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(text = title, style = MaterialTheme.typography.titleSmall)
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            content()
        }
    }
}
