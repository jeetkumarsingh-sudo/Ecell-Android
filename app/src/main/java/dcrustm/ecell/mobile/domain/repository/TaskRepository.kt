package dcrustm.ecell.mobile.domain.repository

import dcrustm.ecell.mobile.domain.model.Committee
import dcrustm.ecell.mobile.domain.model.CommitteeTask
import dcrustm.ecell.mobile.domain.model.TaskStatus
import kotlinx.coroutines.flow.Flow

interface TaskRepository {

    /** Coordinator only. */
    fun observeAllTasks(): Flow<List<CommitteeTask>>

    /** A committee head's board: every task assigned to someone in their committee. */
    fun observeCommitteeTasks(committee: Committee): Flow<List<CommitteeTask>>

    /** A member's own tasks. */
    fun observeTasksAssignedTo(uid: String): Flow<List<CommitteeTask>>

    suspend fun createTask(task: CommitteeTask)

    suspend fun updateStatus(taskId: String, status: TaskStatus)

    suspend fun deleteTask(taskId: String)
}
