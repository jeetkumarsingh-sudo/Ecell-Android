package dcrustm.ecell.mobile.ui.attendance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dcrustm.ecell.mobile.domain.model.CheckInResult
import dcrustm.ecell.mobile.domain.model.Committee
import dcrustm.ecell.mobile.domain.model.Meeting
import dcrustm.ecell.mobile.domain.model.Member
import dcrustm.ecell.mobile.domain.model.ROLE
import dcrustm.ecell.mobile.domain.repository.AttendanceRepository
import dcrustm.ecell.mobile.session.UserSessionManager
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MeetingRow(
    val meeting: Meeting,
    val attended: Boolean,
    /** Host or coordinator — can open the host view. */
    val canManage: Boolean
)

data class AttendanceStats(val attended: Int = 0, val total: Int = 0) {
    val percent: Int get() = if (total == 0) 0 else attended * 100 / total
}

data class AttendanceUiState(
    val live: List<MeetingRow> = emptyList(),
    val past: List<MeetingRow> = emptyList(),
    val stats: AttendanceStats = AttendanceStats()
)

private const val ONE_DAY = 24L * 60 * 60 * 1000

/**
 * Builds the member's view: live and past meetings they can see, plus their attendance rate.
 * The rate only counts meetings they were expected at (org-wide + their committee) since they
 * joined; a live meeting only counts once they've checked in.
 */
fun buildAttendanceState(
    user: Member,
    meetings: List<Meeting>,
    attendedMeetingIds: Set<String>
): AttendanceUiState {
    val visible = meetings.filter { it.isFor(user) }
    val rows = visible.map { meeting ->
        MeetingRow(
            meeting = meeting,
            attended = meeting.id in attendedMeetingIds,
            canManage = user.role == ROLE.COORDINATOR || meeting.hostUid == user.uid
        )
    }
    val joinedCutoff = if (user.joinedAt > 0) user.joinedAt - ONE_DAY else 0L
    val counted = meetings.filter { meeting ->
        val expected = meeting.committee == null || meeting.committee == user.committee
        expected && meeting.startedAt >= joinedCutoff &&
            (!meeting.isLive || meeting.id in attendedMeetingIds)
    }
    return AttendanceUiState(
        live = rows.filter { it.meeting.isLive },
        past = rows.filter { !it.meeting.isLive },
        stats = AttendanceStats(
            attended = counted.count { it.id in attendedMeetingIds },
            total = counted.size
        )
    )
}

@HiltViewModel
class AttendanceViewModel @Inject constructor(
    private val sessionManager: UserSessionManager,
    private val attendanceRepository: AttendanceRepository
) : ViewModel() {

    val currentUser: StateFlow<Member?> = sessionManager.currentUser

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    private val _isBusy = MutableStateFlow(false)
    val isBusy: StateFlow<Boolean> = _isBusy.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val state: StateFlow<AttendanceUiState> = sessionManager.currentUser
        .distinctUntilChanged { old, new ->
            old?.uid == new?.uid && old?.role == new?.role && old?.committee == new?.committee
        }
        .flatMapLatest { user ->
            if (user == null) {
                flowOf(AttendanceUiState())
            } else {
                combine(
                    attendanceRepository.observeMeetings(),
                    attendanceRepository.observeAttendanceOf(user.uid)
                ) { meetings, mine ->
                    buildAttendanceState(user, meetings, mine.map { it.meetingId }.toSet())
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AttendanceUiState())

    fun startMeeting(title: String, committee: Committee?, onStarted: (String) -> Unit) {
        val user = sessionManager.currentUser.value ?: return
        // Heads can only host their own committee — the security rules enforce the same thing.
        val audience = if (user.role == ROLE.COORDINATOR) committee else user.committee
        _isBusy.value = true
        viewModelScope.launch {
            runCatching { attendanceRepository.startMeeting(title.trim(), audience, user) }
                .onSuccess { id ->
                    _isBusy.value = false
                    onStarted(id)
                }
                .onFailure {
                    _isBusy.value = false
                    _messages.tryEmit("Couldn't start the meeting")
                }
        }
    }

    fun checkIn(meeting: Meeting, code: String, onResult: (CheckInResult) -> Unit) {
        val user = sessionManager.currentUser.value ?: return
        _isBusy.value = true
        viewModelScope.launch {
            val result = attendanceRepository.checkIn(meeting, user, code)
            _isBusy.value = false
            if (result == CheckInResult.Success) _messages.tryEmit("You're checked in to ${meeting.title}")
            onResult(result)
        }
    }
}
