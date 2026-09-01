package fr.lc4918.simplecodeeditor.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts

/**
 * Opens a document with write access.
 *
 * The stock contract only asks for read access, which would stop the Save
 * action from writing back to the file the document came from.
 */
class OpenEditableDocument : ActivityResultContracts.OpenDocument() {
    override fun createIntent(context: Context, input: Array<String>): Intent =
        super.createIntent(context, input)
            .addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
}

/** Name and type of the document a destination is being asked for. */
data class NewDocument(val name: String, val mimeType: String)

/**
 * Asks where to create a document.
 *
 * The type travels with the request rather than being fixed once, because it
 * follows the format of whatever is open.
 */
class CreateDocumentAt : ActivityResultContract<NewDocument, Uri?>() {

    override fun createIntent(context: Context, input: NewDocument): Intent =
        Intent(Intent.ACTION_CREATE_DOCUMENT)
            .addCategory(Intent.CATEGORY_OPENABLE)
            .setType(input.mimeType)
            .putExtra(Intent.EXTRA_TITLE, input.name)

    override fun parseResult(resultCode: Int, intent: Intent?): Uri? =
        intent.takeIf { resultCode == Activity.RESULT_OK }?.data
}
