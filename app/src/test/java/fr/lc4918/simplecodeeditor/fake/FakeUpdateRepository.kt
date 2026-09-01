package fr.lc4918.simplecodeeditor.fake

import fr.lc4918.simplecodeeditor.update.ReleaseInfo
import fr.lc4918.simplecodeeditor.update.UpdateRepository

/** A manifest that is whatever the test says, so no network is needed. */
class FakeUpdateRepository : UpdateRepository {

    var latest: ReleaseInfo? = null

    /** When set, the reading fails as it would with no network. */
    var failure: Throwable? = null

    override suspend fun latest(): ReleaseInfo? {
        failure?.let { throw it }
        return latest
    }
}
