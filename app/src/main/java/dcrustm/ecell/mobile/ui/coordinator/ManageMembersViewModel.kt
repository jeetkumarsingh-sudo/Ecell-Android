package dcrustm.ecell.mobile.ui.coordinator

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dcrustm.ecell.mobile.domain.model.Committee
import dcrustm.ecell.mobile.domain.model.Member
import dcrustm.ecell.mobile.domain.model.ROLE
import dcrustm.ecell.mobile.domain.repository.MemberRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ManageMembersViewModel @Inject constructor(
    private val memberRepository: MemberRepository
) : ViewModel() {

    val allMembers: StateFlow<List<Member>> = memberRepository.observeApprovedMembers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val pendingMembers: StateFlow<List<Member>> = memberRepository.observePendingMembers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Only the coordinator is permitted to approve/reject (enforced by Firestore rules);
    // these are caught rather than left to crash the app if tapped by anyone else.
    fun approve(uid: String, committee: Committee, role: ROLE = ROLE.MEMBER) {
        viewModelScope.launch {
            runCatching { memberRepository.approveMember(uid, committee, role) }
        }
    }

    fun reject(uid: String) {
        viewModelScope.launch {
            runCatching { memberRepository.rejectMember(uid) }
        }
    }
}
