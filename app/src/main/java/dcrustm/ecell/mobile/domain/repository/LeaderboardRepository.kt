package dcrustm.ecell.mobile.domain.repository

import dcrustm.ecell.mobile.domain.model.MemberStats

interface LeaderboardRepository {

    /**
     * Stats for each uid, counted on the server. Results are cached for a few minutes so
     * flipping in and out of the leaderboard doesn't re-count; [forceRefresh] skips the cache.
     */
    suspend fun statsFor(uids: List<String>, forceRefresh: Boolean = false): Map<String, MemberStats>
}
