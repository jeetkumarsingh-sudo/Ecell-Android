package dcrustm.ecell.mobile.ui.directory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dcrustm.ecell.mobile.domain.model.Committee
import dcrustm.ecell.mobile.domain.model.Member
import dcrustm.ecell.mobile.domain.model.ROLE
import dcrustm.ecell.mobile.domain.repository.MemberRepository
import dcrustm.ecell.mobile.session.UserSessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class DirectoryFilter(
    val query: String = "",
    val committee: Committee? = null,
    val year: Int? = null
)

@HiltViewModel
class DirectoryViewModel @Inject constructor(
    private val sessionManager: UserSessionManager,
    memberRepository: MemberRepository
) : ViewModel() {

    val currentUser = sessionManager.currentUser

    private val allMembers = memberRepository.observeApprovedMembers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _filter = MutableStateFlow(DirectoryFilter())

    val filteredMembers: StateFlow<List<Member>> = combine(allMembers, _filter) { members, filter ->
        members.filter { member ->
            val matchesQuery = filter.query.isBlank() ||
                member.name.contains(filter.query, ignoreCase = true) ||
                member.branch.contains(filter.query, ignoreCase = true) ||
                member.email.contains(filter.query, ignoreCase = true)
            val matchesCommittee = filter.committee == null || member.committee == filter.committee
            val matchesYear = filter.year == null || member.year == filter.year
            matchesQuery && matchesCommittee && matchesYear
        }.sortedWith(
            compareBy<Member> { it.committee.ordinal }
                .thenByDescending { it.role.ordinal == 0 }
                .thenBy { it.name }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun search(query: String, committee: Committee?, year: Int?) {
        _filter.value = DirectoryFilter(query, committee, year)
    }

    fun canSeeContact(viewer: Member?, target: Member): Boolean {
        if (viewer == null) return false
        return when (viewer.role) {
            ROLE.COORDINATOR -> true
            ROLE.COMMITTEE_HEAD -> viewer.committee == target.committee
            ROLE.MEMBER -> viewer.uid == target.uid
            ROLE.PENDING -> false
        }
    }
}
