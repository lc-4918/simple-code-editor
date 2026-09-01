package fr.lc4918.simplecodeeditor.model

/**
 * Where a document lives, as the storage picker reported it.
 *
 * It is carried as text rather than as a parsed location so that the editor
 * state and the repository stay free of the Android URI type, which also makes
 * both testable without a device.
 */
@JvmInline
value class DocumentLocation(val value: String)
