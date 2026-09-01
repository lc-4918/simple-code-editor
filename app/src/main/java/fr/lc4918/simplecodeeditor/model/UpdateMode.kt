package fr.lc4918.simplecodeeditor.model

import androidx.annotation.StringRes
import fr.lc4918.simplecodeeditor.R

/** Whether the application looks for a newer version of itself on its own. */
enum class UpdateMode(
    val storageKey: String,
    @param:StringRes override val labelRes: Int,
) : LabelledOption {
    AUTOMATIC("automatic", R.string.update_automatic),
    MANUAL("manual", R.string.update_manual);

    companion object {
        val DEFAULT = AUTOMATIC

        fun fromStorageKey(key: String?): UpdateMode =
            entries.firstOrNull { it.storageKey == key } ?: DEFAULT
    }
}
