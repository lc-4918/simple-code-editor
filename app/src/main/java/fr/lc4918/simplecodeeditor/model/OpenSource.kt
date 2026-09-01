package fr.lc4918.simplecodeeditor.model

import androidx.annotation.StringRes
import fr.lc4918.simplecodeeditor.R

/** Where the Open action reads a document from. */
enum class OpenSource(@param:StringRes override val labelRes: Int) : LabelledOption {
    /** Any provider reachable through the storage picker, local or cloud. */
    DEVICE(R.string.open_from_device),
    URL(R.string.open_from_url),
}
