package dcrustm.ecell.mobile.ui.tasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dcrustm.ecell.mobile.domain.model.CommitteeTask
import dcrustm.ecell.mobile.domain.model.Member
import dcrustm.ecell.mobile.domain.model.ROLE
import dcrustm.ecell.mobile.domain.model.TaskPriority
import dcrustm.ecell.mobile.domain.repository.MemberRepository
import dcrustm.ecell.mobile.domain.repository.TaskRepository
import dcrustm.ecell.mobile.session.UserSessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CreateTaskViewModel @Inject constructor(
    private val sessionManager: UserSessionManager,
    private val taskRepository: TaskRepository,
    memberRepository: MemberRepository
) : ViewModel() {

    /** Coordinator can assign to anyone; a head only to people in their committee. */
    val assignableMembers: StateFlow<List<Member>> = combine(
        memberRepository.observeApprovedMembers(),
        sessionManager.currentUser
    ) { members, user ->
        when (user?.role) {
            ROLE.COORDINATOR -> members
            ROLE.COMMITTEE_HEAD -> members.filter { it.committee == user?.committee }
            else -> emptyList()
        }.sortedWith(compareBy<Member> { it.committee.ordinal }.thenBy { it.name })
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun create(
        title: String,
        description: String,
        assignee: Member,
        priority: TaskPriority,
        dueAt: Long,
        onDone: () -> Unit
    ) {
        val user = sessionManager.currentUser.value ?: return
        if (title.isBlank()) return
        _isSaving.value = true
        _error.value = null
        viewModelScope.launch {
            runCatching {
                taskRepository.createTask(
                    CommitteeTask(
                        title = title.trim(),
                        description = description.trim(),
                        committee = assignee.committee,
                        assigneeUid = assignee.uid,
                        assigneeName = assignee.name,
                        assignedByUid = user.uid,
                        assignedByName = user.name,
                        priority = priority,
                        dueAt = dueAt
                    )
                )
            }.onSuccess {
                _isSaving.value = false
                onDone()
            }.onFailure {
                _isSaving.value = false
                _error.value = "Couldn't assign the task. Check your connection and try again."
            }
        }
    }
}
