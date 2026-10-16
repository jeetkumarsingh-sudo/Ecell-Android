package dcrustm.ecell.mobile.data.firebase

import com.google.firebase.firestore.DocumentSnapshot
import dcrustm.ecell.mobile.domain.model.Committee
import dcrustm.ecell.mobile.domain.model.Poll
import dcrustm.ecell.mobile.domain.model.PollVote

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
