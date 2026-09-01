package fr.lc4918.simplecodeeditor.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import fr.lc4918.simplecodeeditor.R
import fr.lc4918.simplecodeeditor.model.FilterOperator
import fr.lc4918.simplecodeeditor.model.LabelledOption
import fr.lc4918.simplecodeeditor.model.SortDirection

/** Asks which column to sort on, and which way. */
@Composable
fun SortDialog(
    columns: List<String>,
    onConfirm: (column: Int, direction: SortDirection) -> Unit,
    onDismiss: () -> Unit,
) {
    var column by remember { mutableIntStateOf(0) }
    var direction by remember { mutableStateOf(SortDirection.ASCENDING) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.tool_sort)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                ColumnChooser(columns, column) { column = it }
                Section(stringResource(R.string.label_direction)) {
                    SortDirection.entries.forEach { option ->
                        OptionChip(option, option == direction) { direction = option }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(column, direction) }) {
                Text(stringResource(R.string.action_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}

/** Asks which rows to keep. */
@Composable
fun FilterDialog(
    columns: List<String>,
    onConfirm: (column: Int, operator: FilterOperator, value: String) -> Unit,
    onDismiss: () -> Unit,
) {
    var column by remember { mutableIntStateOf(0) }
    var operator by remember { mutableStateOf(FilterOperator.CONTAINS) }
    var value by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.tool_filter)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                ColumnChooser(columns, column) { column = it }
                Section(stringResource(R.string.tool_filter)) {
                    FilterOperator.entries.forEach { option ->
                        OptionChip(option, option == operator) { operator = option }
                    }
                }
                OutlinedTextField(
                    value = value,
                    onValueChange = { value = it },
                    singleLine = true,
                    label = { Text(stringResource(R.string.label_value)) },
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(column, operator, value) }) {
                Text(stringResource(R.string.action_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}

@Composable
private fun ColumnChooser(columns: List<String>, selected: Int, onSelected: (Int) -> Unit) {
    Section(stringResource(R.string.label_column)) {
        columns.forEachIndexed { index, name ->
            FilterChip(
                selected = index == selected,
                onClick = { onSelected(index) },
                label = { Text(name) },
            )
        }
    }
}

@Composable
private fun <T : LabelledOption> OptionChip(option: T, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(stringResource(option.labelRes)) },
    )
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
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
