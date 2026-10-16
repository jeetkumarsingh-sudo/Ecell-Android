package dcrustm.ecell.mobile.ui.polls

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dcrustm.ecell.mobile.domain.model.Member
import dcrustm.ecell.mobile.domain.model.Poll
import dcrustm.ecell.mobile.domain.model.PollWithResults
import dcrustm.ecell.mobile.domain.model.ROLE
import dcrustm.ecell.mobile.domain.repository.PollRepository
import dcrustm.ecell.mobile.session.UserSessionManager
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Polls a member is allowed to see: org-wide ones plus their own committee's. */
fun visiblePollsFor(user: Member?, polls: List<Poll>): List<Poll> = when {
    user == null -> emptyList()
    user.role == ROLE.COORDINATOR -> polls
    else -> polls.filter { it.isOrgWide || it.committee == user.committee }
}

@HiltViewModel
class PollsViewModel @Inject constructor(
    private val sessionManager: UserSessionManager,
    private val pollRepository: PollRepository
) : ViewModel() {

    val currentUser: StateFlow<Member?> = sessionManager.currentUser

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val polls: StateFlow<List<PollWithResults>> = combine(
        pollRepository.observePolls(),
        sessionManager.currentUser
    ) { all, user -> visiblePollsFor(user, all) to user?.uid }
        .distinctUntilChanged()
        .flatMapLatest { (visible, uid) ->
            if (visible.isEmpty()) {
                flowOf(emptyList<PollWithResults>())
            } else {
                combine(visible.map { poll ->
                    pollRepository.observeVotes(poll.id).map { votes ->
                        PollWithResults(
                            poll = poll,
                            votes = votes,
                            myVote = votes.firstOrNull { it.voterUid == uid }?.optionIndex
                        )
                    }
                }) { results ->
                    results.sortedWith(
                        compareByDescending<PollWithResults> { it.poll.isOpen() }
                            .thenByDescending { it.poll.createdAt }
                    )
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun canManage(poll: Poll): Boolean {
        val user = sessionManager.currentUser.value ?: return false
        return user.role == ROLE.COORDINATOR || poll.createdByUid == user.uid
    }

    fun vote(poll: Poll, optionIndex: Int) {
        val uid = sessionManager.currentUser.value?.uid ?: return
        if (!poll.isOpen()) return
        viewModelScope.launch {
            runCatching { pollRepository.vote(poll.id, uid, optionIndex) }
                .onFailure { _messages.tryEmit("Couldn't record your vote — the poll may have just closed.") }
        }
    }

    fun close(poll: Poll) {
        viewModelScope.launch {
            runCatching { pollRepository.closePoll(poll.id) }
                .onSuccess { _messages.tryEmit("Poll closed") }
                .onFailure { _messages.tryEmit("Couldn't close the poll") }
        }
    }

    fun delete(poll: Poll) {
        viewModelScope.launch {
            runCatching { pollRepository.deletePoll(poll.id) }
                .onSuccess { _messages.tryEmit("Poll deleted") }
                .onFailure { _messages.tryEmit("Couldn't delete the poll") }
        }
    }
}
