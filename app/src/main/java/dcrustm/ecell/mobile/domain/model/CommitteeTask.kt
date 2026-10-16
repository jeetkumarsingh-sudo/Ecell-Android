package dcrustm.ecell.mobile.domain.model

data class CommitteeTask(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    /** The assignee's committee — lets that committee's head see and manage the task. */
    val committee: Committee = Committee.NONE,
    val assigneeUid: String = "",
    val assigneeName: String = "",
    val assignedByUid: String = "",
    val assignedByName: String = "",
    val status: TaskStatus = TaskStatus.TODO,
    val priority: TaskPriority = TaskPriority.MEDIUM,
    /** 0 = no due date. */
    val dueAt: Long = 0L,
    val createdAt: Long = System.currentTimeMillis(),
    /** 0 until the task is marked done. */
    val completedAt: Long = 0L
) {
    val hasDueDate: Boolean get() = dueAt != 0L

    fun isOverdue(now: Long = System.currentTimeMillis()): Boolean =
        status != TaskStatus.DONE && hasDueDate && now > dueAt
}

enum class TaskStatus(val label: String) {
    TODO("To do"),
    IN_PROGRESS("In progress"),
    DONE("Done")
}

enum class TaskPriority(val label: String) {
    LOW("Low"),
    MEDIUM("Medium"),
    HIGH("High")
}
