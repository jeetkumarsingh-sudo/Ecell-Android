package dcrustm.ecell.mobile.ui.leaderboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dcrustm.ecell.mobile.domain.model.Committee
import dcrustm.ecell.mobile.domain.model.LeaderboardEntry
import dcrustm.ecell.mobile.domain.model.Member
import dcrustm.ecell.mobile.domain.model.MemberStats
import dcrustm.ecell.mobile.domain.model.rankMembers
import dcrustm.ecell.mobile.domain.repository.LeaderboardRepository
import dcrustm.ecell.mobile.domain.repository.MemberRepository
import dcrustm.ecell.mobile.session.UserSessionManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class LeaderboardScope { EVERYONE, MY_COMMITTEE }

data class LeaderboardUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val scope: LeaderboardScope = LeaderboardScope.EVERYONE,
    /** Every approved member with their stats, before scoping. */
    val allStats: List<Pair<Member, MemberStats>> = emptyList(),
    val myUid: String? = null,
    val myCommittee: Committee = Committee.NONE
) {
    val entries: List<LeaderboardEntry>
        get() = rankMembers(
            when (scope) {
                LeaderboardScope.EVERYONE -> allStats
                LeaderboardScope.MY_COMMITTEE -> allStats.filter { it.first.committee == myCommittee }
            }
        )

    val me: LeaderboardEntry? get() = entries.firstOrNull { it.member.uid == myUid }

    val myStats: MemberStats get() = allStats.firstOrNull { it.first.uid == myUid }?.second ?: MemberStats()
}

@HiltViewModel
class LeaderboardViewModel @Inject constructor(
    private val sessionManager: UserSessionManager,
    private val memberRepository: MemberRepository,
    private val leaderboardRepository: LeaderboardRepository
) : ViewModel() {

    private val _state = MutableStateFlow(LeaderboardUiState())
    val state: StateFlow<LeaderboardUiState> = _state.asStateFlow()

    private var loadJob: Job? = null

    init {
        load(forceRefresh = false)
    }

    fun refresh() = load(forceRefresh = true)

    fun setScope(scope: LeaderboardScope) {
        _state.update { it.copy(scope = scope) }
    }

    private fun load(forceRefresh: Boolean) {
        loadJob?.cancel()
        _state.update { it.copy(isLoading = true, error = null) }
        loadJob = viewModelScope.launch {
            try {
                val me = sessionManager.currentUser.filterNotNull().first()
                val members = memberRepository.observeApprovedMembers().first()
                val stats = leaderboardRepository.statsFor(members.map { it.uid }, forceRefresh)
                _state.update {
                    it.copy(
                        isLoading = false,
                        allStats = members.map { m -> m to (stats[m.uid] ?: MemberStats()) },
                        myUid = me.uid,
                        myCommittee = me.committee
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update {
                    it.copy(isLoading = false, error = "Couldn't load the leaderboard. Tap refresh once you're online.")
                }
            }
        }
    }
}
