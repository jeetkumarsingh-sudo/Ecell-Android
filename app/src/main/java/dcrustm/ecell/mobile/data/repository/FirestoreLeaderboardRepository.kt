package dcrustm.ecell.mobile.data.repository

import com.google.firebase.firestore.AggregateSource
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import dcrustm.ecell.mobile.data.firebase.awaitResult
import dcrustm.ecell.mobile.domain.model.MemberStats
import dcrustm.ecell.mobile.domain.model.TaskStatus
import dcrustm.ecell.mobile.domain.repository.LeaderboardRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import javax.inject.Inject
import javax.inject.Singleton

private const val CACHE_TTL_MILLIS = 5 * 60 * 1000L
private const val MAX_PARALLEL_COUNTS = 16

/**
 * Points come from server-side count() aggregations — three per member, each billed as a
 * single read — instead of downloading every attendance record, task and vote.
 */
@Singleton
class FirestoreLeaderboardRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) : LeaderboardRepository {

    private val cacheLock = Mutex()
    private val cache = mutableMapOf<String, Pair<Long, MemberStats>>()
    private val permits = Semaphore(MAX_PARALLEL_COUNTS)

    private suspend fun Query.serverCount(): Int = permits.withPermit {
        count().get(AggregateSource.SERVER).awaitResult().count.toInt()
    }

    private suspend fun countFor(uid: String): MemberStats = coroutineScope {
        val meetings = async {
            firestore.collection("attendance").whereEqualTo("uid", uid).serverCount()
        }
        val tasks = async {
            firestore.collection("tasks")
                .whereEqualTo("assigneeUid", uid)
                .whereEqualTo("status", TaskStatus.DONE.name)
                .serverCount()
        }
        val votes = async {
            firestore.collection("pollVotes").whereEqualTo("voterUid", uid).serverCount()
        }
        MemberStats(
            meetingsAttended = meetings.await(),
            tasksCompleted = tasks.await(),
            pollsVoted = votes.await()
        )
    }

    override suspend fun statsFor(uids: List<String>, forceRefresh: Boolean): Map<String, MemberStats> {
        val now = System.currentTimeMillis()
        val fresh = cacheLock.withLock {
            if (forceRefresh) emptyMap()
            else cache.filter { (uid, entry) -> uid in uids && now - entry.first < CACHE_TTL_MILLIS }
                .mapValues { it.value.second }
        }
        val missing = uids.distinct().filter { it !in fresh }
        val counted = coroutineScope {
            missing.map { uid -> async { uid to countFor(uid) } }.awaitAll()
        }.toMap()
        cacheLock.withLock {
            counted.forEach { (uid, stats) -> cache[uid] = now to stats }
        }
        return fresh + counted
    }
}
