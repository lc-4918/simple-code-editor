package fr.lc4918.simplecodeeditor.ui

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import fr.lc4918.simplecodeeditor.R

/** Whether the address being asked for is where to read from or where to write to. */
enum class UrlPrompt(val titleRes: Int) {
    OPEN(R.string.open_from_url),
    SAVE(R.string.save_to_url),
}

/** Asks for the address of a document. */
@Composable
fun UrlDialog(
    prompt: UrlPrompt,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var url by remember { mutableStateOf("") }
    val isValid = url.isValidAddress()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(prompt.titleRes)) },
        text = {
            OutlinedTextField(
                value = url,
                onValueChange = { url = it },
                singleLine = true,
                label = { Text(stringResource(R.string.url_address)) },
                placeholder = { Text(stringResource(R.string.url_hint)) },
                isError = url.isNotEmpty() && !isValid,
                supportingText = {
                    if (url.isNotEmpty() && !isValid) {
                        Text(stringResource(R.string.error_invalid_url))
                    }
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Uri,
                    imeAction = ImeAction.Done,
                ),
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(url.trim()) }, enabled = isValid) {
                Text(stringResource(R.string.action_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        },
    )
}

/**
 * Only secure addresses are accepted.
 *
 * The application does not allow traffic in the clear, so a plain http address
 * would fail once the request left: refusing it here says so straight away, and
 * a document is not worth sending unencrypted anyway.
 */
private fun String.isValidAddress(): Boolean {
    val trimmed = trim()
    return trimmed.startsWith("https://") && trimmed.substringAfter("://").isNotEmpty()
}
