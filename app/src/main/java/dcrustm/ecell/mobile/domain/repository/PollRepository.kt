package dcrustm.ecell.mobile.domain.repository

import dcrustm.ecell.mobile.domain.model.Poll
import dcrustm.ecell.mobile.domain.model.PollVote
import kotlinx.coroutines.flow.Flow

interface PollRepository {

    fun observePolls(): Flow<List<Poll>>

    fun observeVotes(pollId: String): Flow<List<PollVote>>

    /** Every vote [uid] has cast — used to tell which open polls still need their vote. */
    fun observeVotesBy(uid: String): Flow<List<PollVote>>

    suspend fun createPoll(poll: Poll)

    /** Casting again overwrites the previous choice while the poll is open. */
    suspend fun vote(pollId: String, uid: String, optionIndex: Int)

    suspend fun closePoll(pollId: String)

    /** Deletes the poll together with all of its votes. */
    suspend fun deletePoll(pollId: String)
}
