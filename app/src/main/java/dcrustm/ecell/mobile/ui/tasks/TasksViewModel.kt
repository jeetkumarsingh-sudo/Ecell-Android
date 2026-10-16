package dcrustm.ecell.mobile.ui.tasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dcrustm.ecell.mobile.domain.model.Committee
import dcrustm.ecell.mobile.domain.model.CommitteeTask
import dcrustm.ecell.mobile.domain.model.Member
import dcrustm.ecell.mobile.domain.model.ROLE
import dcrustm.ecell.mobile.domain.model.TaskStatus
import dcrustm.ecell.mobile.domain.repository.TaskRepository
import dcrustm.ecell.mobile.session.UserSessionManager
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
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

data class TaskFilter(
    /** Only tasks assigned to the viewer. Always on for plain members. */
    val mineOnly: Boolean = false,
    /** Coordinator-only narrowing to one committee. */
    val committee: Committee? = null
)

/** Tasks a member's role lets them see, picked to match the Firestore rules for /tasks. */
@OptIn(ExperimentalCoroutinesApi::class)
fun TaskRepository.observeVisibleTasks(user: Flow<Member?>): Flow<List<CommitteeTask>> = user
    .distinctUntilChanged { old, new ->
        old?.uid == new?.uid && old?.role == new?.role && old?.committee == new?.committee
    }
    .flatMapLatest { member ->
        if (member == null) {
            flowOf(emptyList<CommitteeTask>())
        } else {
            when (member.role) {
                ROLE.COORDINATOR -> observeAllTasks()
                ROLE.COMMITTEE_HEAD -> observeCommitteeTasks(member.committee)
                ROLE.MEMBER -> observeTasksAssignedTo(member.uid)
                ROLE.PENDING -> flowOf(emptyList<CommitteeTask>())
            }
        }
    }

@HiltViewModel
class TasksViewModel @Inject constructor(
    private val sessionManager: UserSessionManager,
    private val taskRepository: TaskRepository
) : ViewModel() {

    val currentUser: StateFlow<Member?> = sessionManager.currentUser

    private val _filter = MutableStateFlow(TaskFilter())
    val filter: StateFlow<TaskFilter> = _filter.asStateFlow()

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    val tasks: StateFlow<List<CommitteeTask>> = combine(
        taskRepository.observeVisibleTasks(sessionManager.currentUser),
        _filter,
        sessionManager.currentUser
    ) { tasks, filter, user ->
        val managesTasks = user?.role == ROLE.COORDINATOR || user?.role == ROLE.COMMITTEE_HEAD
        val restrictToMine = filter.mineOnly || !managesTasks
        tasks.filter { task ->
            val mineOk = !restrictToMine || task.assigneeUid == user?.uid
            val committeeOk = filter.committee == null || task.committee == filter.committee
            mineOk && committeeOk
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setFilter(filter: TaskFilter) {
        _filter.value = filter
    }

    fun canManage(task: CommitteeTask): Boolean {
        val user = sessionManager.currentUser.value ?: return false
        return user.role == ROLE.COORDINATOR ||
            (user.role == ROLE.COMMITTEE_HEAD && task.committee == user.committee)
    }

    fun canUpdateStatus(task: CommitteeTask): Boolean =
        canManage(task) || task.assigneeUid == sessionManager.currentUser.value?.uid

    fun setStatus(task: CommitteeTask, status: TaskStatus) {
        if (task.status == status) return
        viewModelScope.launch {
            runCatching { taskRepository.updateStatus(task.id, status) }
                .onSuccess { if (status == TaskStatus.DONE) _messages.tryEmit("Nice — \"${task.title}\" is done") }
                .onFailure { _messages.tryEmit("Couldn't update the task") }
        }
    }

    fun delete(task: CommitteeTask) {
        viewModelScope.launch {
            runCatching { taskRepository.deleteTask(task.id) }
                .onSuccess { _messages.tryEmit("Task deleted") }
                .onFailure { _messages.tryEmit("Couldn't delete the task") }
        }
    }
}

/** Board ordering: overdue first, then soonest due, then highest priority, then newest. */
fun List<CommitteeTask>.sortedForBoard(status: TaskStatus): List<CommitteeTask> =
    if (status == TaskStatus.DONE) {
        sortedByDescending { it.completedAt }
    } else {
        sortedWith(
            compareByDescending<CommitteeTask> { it.isOverdue() }
                .thenBy { if (it.hasDueDate) it.dueAt else Long.MAX_VALUE }
                .thenByDescending { it.priority.ordinal }
                .thenByDescending { it.createdAt }
        )
    }
