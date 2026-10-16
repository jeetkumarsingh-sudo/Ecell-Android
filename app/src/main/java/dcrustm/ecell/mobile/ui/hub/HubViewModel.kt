package dcrustm.ecell.mobile.ui.hub

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dcrustm.ecell.mobile.domain.model.Member
import dcrustm.ecell.mobile.domain.repository.PollRepository
import dcrustm.ecell.mobile.session.UserSessionManager
import dcrustm.ecell.mobile.ui.polls.visiblePollsFor
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/** Things on the Hub that need the member's attention. */
data class HubSummary(
    val pollsAwaitingVote: Int = 0
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HubViewModel @Inject constructor(
    sessionManager: UserSessionManager,
    private val pollRepository: PollRepository
) : ViewModel() {

    val currentUser: StateFlow<Member?> = sessionManager.currentUser

    private val pollsAwaitingVote: Flow<Int> = sessionManager.currentUser
        .distinctUntilChanged { old, new -> old?.uid == new?.uid && old?.committee == new?.committee && old?.role == new?.role }
        .flatMapLatest { user ->
            if (user == null) {
                flowOf(0)
            } else {
                combine(
                    pollRepository.observePolls(),
                    pollRepository.observeVotesBy(user.uid)
                ) { polls, myVotes ->
                    val voted = myVotes.map { it.pollId }.toSet()
                    visiblePollsFor(user, polls).count { it.isOpen() && it.id !in voted }
                }
            }
        }

    val summary: StateFlow<HubSummary> = pollsAwaitingVote
        .map { polls -> HubSummary(pollsAwaitingVote = polls) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HubSummary())
}
