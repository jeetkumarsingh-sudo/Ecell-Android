package dcrustm.ecell.mobile.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import dcrustm.ecell.mobile.data.firebase.awaitResult
import dcrustm.ecell.mobile.data.firebase.snapshots
import dcrustm.ecell.mobile.data.firebase.toCommitteeTask
import dcrustm.ecell.mobile.data.firebase.toFirestoreMap
import dcrustm.ecell.mobile.domain.model.Committee
import dcrustm.ecell.mobile.domain.model.CommitteeTask
import dcrustm.ecell.mobile.domain.model.TaskStatus
import dcrustm.ecell.mobile.domain.repository.TaskRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class FirestoreTaskRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) : TaskRepository {

    private val tasksRef get() = firestore.collection("tasks")

    // Queries match the security rules exactly (all / by committee / by assignee) so Firestore
    // can prove every returned document is readable. Sorting happens client-side to avoid
    // needing composite indexes.
    private fun Query.observeTasks(): Flow<List<CommitteeTask>> =
        snapshots()
            .map { snap -> snap.documents.mapNotNull { it.toCommitteeTask() } }
            .catch { emit(emptyList()) }

    override fun observeAllTasks(): Flow<List<CommitteeTask>> = tasksRef.observeTasks()

    override fun observeCommitteeTasks(committee: Committee): Flow<List<CommitteeTask>> =
        tasksRef.whereEqualTo("committee", committee.name).observeTasks()

    override fun observeTasksAssignedTo(uid: String): Flow<List<CommitteeTask>> =
        tasksRef.whereEqualTo("assigneeUid", uid).observeTasks()

    override suspend fun createTask(task: CommitteeTask) {
        tasksRef.add(task.toFirestoreMap()).awaitResult()
    }

    override suspend fun updateStatus(taskId: String, status: TaskStatus) {
        tasksRef.document(taskId).update(
            mapOf(
                "status" to status.name,
                "completedAt" to if (status == TaskStatus.DONE) System.currentTimeMillis() else 0L
            )
        ).awaitResult()
    }

    override suspend fun deleteTask(taskId: String) {
        tasksRef.document(taskId).delete().awaitResult()
    }
}
