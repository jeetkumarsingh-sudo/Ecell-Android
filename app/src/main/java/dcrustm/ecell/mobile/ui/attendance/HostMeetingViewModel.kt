package dcrustm.ecell.mobile.ui.attendance

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dcrustm.ecell.mobile.domain.model.AttendanceRecord
import dcrustm.ecell.mobile.domain.model.Meeting
import dcrustm.ecell.mobile.domain.model.Member
import dcrustm.ecell.mobile.domain.model.ROLE
import dcrustm.ecell.mobile.domain.repository.AttendanceRepository
import dcrustm.ecell.mobile.domain.repository.MemberRepository
import dcrustm.ecell.mobile.session.UserSessionManager
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HostMeetingViewModel @Inject constructor(
    sessionManager: UserSessionManager,
    private val attendanceRepository: AttendanceRepository,
    memberRepository: MemberRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val meetingId: String = checkNotNull(savedStateHandle["meetingId"])

    val meeting: StateFlow<Meeting?> = attendanceRepository.observeMeeting(meetingId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    /** null for anyone who isn't the host or coordinator — the rules deny the read. */
    val code: StateFlow<String?> = attendanceRepository.observeCheckInCode(meetingId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val attendees: StateFlow<List<AttendanceRecord>> = attendanceRepository.observeAttendance(meetingId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** People expected at this meeting who aren't marked present yet. */
    val absentees: StateFlow<List<Member>> = combine(
        memberRepository.observeApprovedMembers(),
        meeting,
        attendees
    ) { members, m, present ->
        if (m == null) {
            emptyList()
        } else {
            val presentIds = present.map { it.uid }.toSet()
            members
                .filter { (m.committee == null || it.committee == m.committee) && it.uid !in presentIds }
                .sortedBy { it.name }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val canManage: StateFlow<Boolean> = combine(meeting, sessionManager.currentUser) { m, user ->
        m != null && user != null && (user.role == ROLE.COORDINATOR || m.hostUid == user.uid)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    private fun launchAction(failure: String, success: String? = null, block: suspend () -> Unit) {
        viewModelScope.launch {
            runCatching { block() }
                .onSuccess { success?.let { _messages.tryEmit(it) } }
                .onFailure { _messages.tryEmit(failure) }
        }
    }

    fun regenerateCode() =
        launchAction("Couldn't change the code", "New code ready") { attendanceRepository.regenerateCode(meetingId) }

    fun setCheckInOpen(open: Boolean) =
        launchAction("Couldn't update check-in") { attendanceRepository.setCheckInOpen(meetingId, open) }

    fun endMeeting() =
        launchAction("Couldn't end the meeting", "Meeting ended") { attendanceRepository.endMeeting(meetingId) }

    fun markPresent(member: Member) =
        launchAction("Couldn't mark ${member.name} present") { attendanceRepository.markPresent(meetingId, member) }

    fun remove(record: AttendanceRecord) =
        launchAction("Couldn't remove ${record.name}", "Removed ${record.name}") {
            attendanceRepository.removeAttendance(meetingId, record.uid)
        }
}
