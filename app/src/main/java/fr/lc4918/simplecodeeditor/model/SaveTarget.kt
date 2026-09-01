package fr.lc4918.simplecodeeditor.model

import androidx.annotation.StringRes
import fr.lc4918.simplecodeeditor.R

/** Where the Save action writes the open document to. */
enum class SaveTarget(@param:StringRes override val labelRes: Int) : LabelledOption {
    DEVICE(R.string.save_to_device),

    /** A cloud provider, reached through the storage picker rather than a dedicated SDK. */
    CLOUD(R.string.save_to_cloud),
    URL(R.string.save_to_url),
}
