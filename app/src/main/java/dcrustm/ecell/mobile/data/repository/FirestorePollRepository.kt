package dcrustm.ecell.mobile.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import dcrustm.ecell.mobile.data.firebase.awaitResult
import dcrustm.ecell.mobile.data.firebase.snapshots
import dcrustm.ecell.mobile.data.firebase.toFirestoreMap
import dcrustm.ecell.mobile.data.firebase.toPoll
import dcrustm.ecell.mobile.data.firebase.toPollVote
import dcrustm.ecell.mobile.domain.model.Poll
import dcrustm.ecell.mobile.domain.model.PollVote
import dcrustm.ecell.mobile.domain.repository.PollRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class FirestorePollRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) : PollRepository {

    private val pollsRef get() = firestore.collection("polls")
    private val votesRef get() = firestore.collection("pollVotes")

    override fun observePolls(): Flow<List<Poll>> =
        pollsRef.snapshots()
            .map { snap -> snap.documents.mapNotNull { it.toPoll() } }
            .catch { emit(emptyList()) }

    override fun observeVotes(pollId: String): Flow<List<PollVote>> =
        votesRef.whereEqualTo("pollId", pollId).snapshots()
            .map { snap -> snap.documents.mapNotNull { it.toPollVote() } }
            .catch { emit(emptyList()) }

    override fun observeVotesBy(uid: String): Flow<List<PollVote>> =
        votesRef.whereEqualTo("voterUid", uid).snapshots()
            .map { snap -> snap.documents.mapNotNull { it.toPollVote() } }
            .catch { emit(emptyList()) }

    override suspend fun createPoll(poll: Poll) {
        pollsRef.add(poll.toFirestoreMap()).awaitResult()
    }

    override suspend fun vote(pollId: String, uid: String, optionIndex: Int) {
        val vote = PollVote(pollId = pollId, voterUid = uid, optionIndex = optionIndex)
        votesRef.document("${pollId}_$uid").set(vote.toFirestoreMap()).awaitResult()
    }

    override suspend fun closePoll(pollId: String) {
        pollsRef.document(pollId).update("isClosed", true).awaitResult()
    }

    override suspend fun deletePoll(pollId: String) {
        val votes = votesRef.whereEqualTo("pollId", pollId).get().awaitResult()
        // A batch holds at most 500 writes; leave room for the poll document itself.
        votes.documents.chunked(450).forEach { chunk ->
            val batch = firestore.batch()
            chunk.forEach { batch.delete(it.reference) }
            batch.commit().awaitResult()
        }
        pollsRef.document(pollId).delete().awaitResult()
    }
}
