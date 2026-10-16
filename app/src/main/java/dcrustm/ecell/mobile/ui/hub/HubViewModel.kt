package dcrustm.ecell.mobile.ui.hub

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dcrustm.ecell.mobile.domain.model.Member
import dcrustm.ecell.mobile.domain.model.TaskStatus
import dcrustm.ecell.mobile.domain.repository.AttendanceRepository
import dcrustm.ecell.mobile.domain.repository.PollRepository
import dcrustm.ecell.mobile.domain.repository.TaskRepository
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
    val pollsAwaitingVote: Int = 0,
    val myOpenTasks: Int = 0,
    val overdueTasks: Int = 0,
    /** Titles of live meetings meant for me that I haven't checked in to yet. */
    val liveMeetingsToJoin: List<String> = emptyList()
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HubViewModel @Inject constructor(
    sessionManager: UserSessionManager,
    private val pollRepository: PollRepository,
    private val taskRepository: TaskRepository,
    private val attendanceRepository: AttendanceRepository
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

    /** Open tasks assigned to me, and how many of those are overdue. */
    private val myTaskCounts: Flow<Pair<Int, Int>> = sessionManager.currentUser
        .distinctUntilChanged { old, new -> old?.uid == new?.uid }
        .flatMapLatest { user ->
            if (user == null) {
                flowOf(0 to 0)
            } else {
                taskRepository.observeTasksAssignedTo(user.uid).map { tasks ->
                    val open = tasks.filter { it.status != TaskStatus.DONE }
                    open.size to open.count { it.isOverdue() }
                }
            }
        }

    private val liveMeetingsToJoin: Flow<List<String>> = sessionManager.currentUser
        .distinctUntilChanged { old, new -> old?.uid == new?.uid && old?.committee == new?.committee && old?.role == new?.role }
        .flatMapLatest { user ->
            if (user == null) {
                flowOf(emptyList<String>())
            } else {
                combine(
                    attendanceRepository.observeMeetings(),
                    attendanceRepository.observeAttendanceOf(user.uid)
                ) { meetings, mine ->
                    val attended = mine.map { it.meetingId }.toSet()
                    meetings
                        .filter { it.isLive && it.checkInOpen && it.id !in attended && it.hostUid != user.uid }
                        .filter { it.committee == null || it.committee == user.committee }
                        .map { it.title }
                }
            }
        }

    val summary: StateFlow<HubSummary> = combine(
        pollsAwaitingVote,
        myTaskCounts,
        liveMeetingsToJoin
    ) { polls, (openTasks, overdue), live ->
        HubSummary(
            pollsAwaitingVote = polls,
            myOpenTasks = openTasks,
            overdueTasks = overdue,
            liveMeetingsToJoin = live
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HubSummary())
}
