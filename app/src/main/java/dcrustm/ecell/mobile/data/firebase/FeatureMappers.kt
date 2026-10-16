package dcrustm.ecell.mobile.data.firebase

import com.google.firebase.firestore.DocumentSnapshot
import dcrustm.ecell.mobile.domain.model.AttendanceRecord
import dcrustm.ecell.mobile.domain.model.Committee
import dcrustm.ecell.mobile.domain.model.CommitteeTask
import dcrustm.ecell.mobile.domain.model.Meeting
import dcrustm.ecell.mobile.domain.model.Poll
import dcrustm.ecell.mobile.domain.model.PollVote
import dcrustm.ecell.mobile.domain.model.ResourceType
import dcrustm.ecell.mobile.domain.model.SharedResource
import dcrustm.ecell.mobile.domain.model.TaskPriority
import dcrustm.ecell.mobile.domain.model.TaskStatus

private inline fun <reified T : Enum<T>> enumOrDefault(name: String?, default: T): T =
    try {
        if (name == null) default else enumValueOf<T>(name)
    } catch (e: IllegalArgumentException) {
        default
    }

private fun DocumentSnapshot.optionalCommittee(field: String): Committee? =
    getString(field)?.let { enumOrDefault(it, Committee.NONE) }

private fun DocumentSnapshot.stringList(field: String): List<String> =
    (get(field) as? List<*>)?.filterIsInstance<String>() ?: emptyList()

// ---- Polls ----

fun Poll.toFirestoreMap(): Map<String, Any?> = mapOf(
    "question" to question,
    "options" to options,
    "createdByUid" to createdByUid,
    "createdByName" to createdByName,
    "committee" to committee?.name,
    "createdAt" to createdAt,
    "closesAt" to closesAt,
    "isClosed" to isClosed
)

fun DocumentSnapshot.toPoll(): Poll? {
    if (!exists()) return null
    return Poll(
        id = id,
        question = getString("question") ?: "",
        options = stringList("options"),
        createdByUid = getString("createdByUid") ?: "",
        createdByName = getString("createdByName") ?: "",
        committee = optionalCommittee("committee"),
        createdAt = getLong("createdAt") ?: 0L,
        closesAt = getLong("closesAt") ?: 0L,
        isClosed = getBoolean("isClosed") ?: false
    )
}

fun PollVote.toFirestoreMap(): Map<String, Any?> = mapOf(
    "pollId" to pollId,
    "voterUid" to voterUid,
    "optionIndex" to optionIndex,
    "votedAt" to votedAt
)

fun DocumentSnapshot.toPollVote(): PollVote? {
    if (!exists()) return null
    return PollVote(
        pollId = getString("pollId") ?: "",
        voterUid = getString("voterUid") ?: "",
        optionIndex = (getLong("optionIndex") ?: 0L).toInt(),
        votedAt = getLong("votedAt") ?: 0L
    )
}

// ---- Tasks ----

fun CommitteeTask.toFirestoreMap(): Map<String, Any?> = mapOf(
    "title" to title,
    "description" to description,
    "committee" to committee.name,
    "assigneeUid" to assigneeUid,
    "assigneeName" to assigneeName,
    "assignedByUid" to assignedByUid,
    "assignedByName" to assignedByName,
    "status" to status.name,
    "priority" to priority.name,
    "dueAt" to dueAt,
    "createdAt" to createdAt,
    "completedAt" to completedAt
)

fun DocumentSnapshot.toCommitteeTask(): CommitteeTask? {
    if (!exists()) return null
    return CommitteeTask(
        id = id,
        title = getString("title") ?: "",
        description = getString("description") ?: "",
        committee = enumOrDefault(getString("committee"), Committee.NONE),
        assigneeUid = getString("assigneeUid") ?: "",
        assigneeName = getString("assigneeName") ?: "",
        assignedByUid = getString("assignedByUid") ?: "",
        assignedByName = getString("assignedByName") ?: "",
        status = enumOrDefault(getString("status"), TaskStatus.TODO),
        priority = enumOrDefault(getString("priority"), TaskPriority.MEDIUM),
        dueAt = getLong("dueAt") ?: 0L,
        createdAt = getLong("createdAt") ?: 0L,
        completedAt = getLong("completedAt") ?: 0L
    )
}

// ---- Meetings & attendance ----

fun Meeting.toFirestoreMap(): Map<String, Any?> = mapOf(
    "title" to title,
    "committee" to committee?.name,
    "hostUid" to hostUid,
    "hostName" to hostName,
    "startedAt" to startedAt,
    "endedAt" to endedAt,
    "checkInOpen" to checkInOpen
)

fun DocumentSnapshot.toMeeting(): Meeting? {
    if (!exists()) return null
    return Meeting(
        id = id,
        title = getString("title") ?: "",
        committee = optionalCommittee("committee"),
        hostUid = getString("hostUid") ?: "",
        hostName = getString("hostName") ?: "",
        startedAt = getLong("startedAt") ?: 0L,
        endedAt = getLong("endedAt") ?: 0L,
        checkInOpen = getBoolean("checkInOpen") ?: false
    )
}

fun AttendanceRecord.toFirestoreMap(): Map<String, Any?> = mapOf(
    "meetingId" to meetingId,
    "uid" to uid,
    "name" to name,
    "committee" to committee.name,
    "checkedInAt" to checkedInAt,
    "markedByHost" to markedByHost
)

fun DocumentSnapshot.toAttendanceRecord(): AttendanceRecord? {
    if (!exists()) return null
    return AttendanceRecord(
        meetingId = getString("meetingId") ?: "",
        uid = getString("uid") ?: "",
        name = getString("name") ?: "",
        committee = enumOrDefault(getString("committee"), Committee.NONE),
        checkedInAt = getLong("checkedInAt") ?: 0L,
        markedByHost = getBoolean("markedByHost") ?: false
    )
}

// ---- Resources ----

fun SharedResource.toFirestoreMap(): Map<String, Any?> = mapOf(
    "title" to title,
    "description" to description,
    "url" to url,
    "type" to type.name,
    "committee" to committee?.name,
    "addedByUid" to addedByUid,
    "addedByName" to addedByName,
    "createdAt" to createdAt
)

fun DocumentSnapshot.toSharedResource(): SharedResource? {
    if (!exists()) return null
    return SharedResource(
        id = id,
        title = getString("title") ?: "",
        description = getString("description") ?: "",
        url = getString("url") ?: "",
        type = enumOrDefault(getString("type"), ResourceType.LINK),
        committee = optionalCommittee("committee"),
        addedByUid = getString("addedByUid") ?: "",
        addedByName = getString("addedByName") ?: "",
        createdAt = getLong("createdAt") ?: 0L
    )
}
