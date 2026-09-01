package fr.lc4918.simplecodeeditor.ui

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import fr.lc4918.simplecodeeditor.R
import fr.lc4918.simplecodeeditor.update.ReleaseInfo
import fr.lc4918.simplecodeeditor.update.UpdateInstaller

private const val NO_DOWNLOAD = -1L

/**
 * Offers a newer version, brings it down and hands it to the installer.
 *
 * Android asks its own permission before one application may install another,
 * and without it the installer opens and fails without a word, so it is asked
 * for before the download rather than after.
 */
@Composable
fun UpdateFlow(release: ReleaseInfo?, onDone: () -> Unit) {
    val context = LocalContext.current
    var downloadId by remember { mutableLongStateOf(NO_DOWNLOAD) }
    var askingPermission by remember { mutableStateOf(false) }
    val downloading = stringResource(R.string.update_downloading, release?.version.orEmpty())

    if (downloadId != NO_DOWNLOAD) {
        val running = downloadId
        WhenDownloaded(context, running) {
            downloadId = NO_DOWNLOAD
            UpdateInstaller.installIntent(context, running)?.let(context::startActivity)
            onDone()
        }
    }

    if (askingPermission) {
        AlertDialog(
            onDismissRequest = { askingPermission = false; onDone() },
            title = { Text(stringResource(R.string.settings_updates)) },
            text = { Text(stringResource(R.string.update_unknown_sources)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        askingPermission = false
                        context.startActivity(UpdateInstaller.permissionSettings(context))
                        onDone()
                    },
                ) { Text(stringResource(R.string.update_open_settings)) }
            },
            dismissButton = {
                TextButton(onClick = { askingPermission = false; onDone() }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
        return
    }

    if (release == null || downloadId != NO_DOWNLOAD) return

    AlertDialog(
        onDismissRequest = onDone,
        title = { Text(stringResource(R.string.update_available, release.version)) },
        text = { Text(release.changelog.ifBlank { release.releaseDate }) },
        confirmButton = {
            TextButton(
                onClick = {
                    if (UpdateInstaller.canInstall(context)) {
                        downloadId = UpdateInstaller.download(context, release, downloading)
                    } else {
                        askingPermission = true
                    }
                },
            ) { Text(stringResource(R.string.update_download)) }
        },
        dismissButton = {
            TextButton(onClick = onDone) { Text(stringResource(R.string.update_later)) }
        },
    )
}

/** Listens for the end of one download, and only while it is running. */
@Composable
private fun WhenDownloaded(context: Context, downloadId: Long, onFinished: () -> Unit) {
    DisposableEffect(downloadId) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(from: Context?, intent: Intent?) {
                val finished = intent?.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, NO_DOWNLOAD)
                if (finished == downloadId) onFinished()
            }
        }
        ContextCompat.registerReceiver(
            context,
            receiver,
            IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),
            ContextCompat.RECEIVER_EXPORTED,
        )
        onDispose { runCatching { context.unregisterReceiver(receiver) } }
    }
}
