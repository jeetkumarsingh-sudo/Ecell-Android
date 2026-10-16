package dcrustm.ecell.mobile.domain.model

data class Meeting(
    val id: String = "",
    val title: String = "",
    /** null = org-wide meeting; otherwise one committee's meeting. */
    val committee: Committee? = null,
    val hostUid: String = "",
    val hostName: String = "",
    val startedAt: Long = System.currentTimeMillis(),
    /** 0 while the meeting is live. */
    val endedAt: Long = 0L,
    /** Members can check themselves in with the code only while this is on. */
    val checkInOpen: Boolean = true
) {
    val isLive: Boolean get() = endedAt == 0L

    val isOrgWide: Boolean get() = committee == null

    /** Who is expected at this meeting. */
    fun isFor(member: Member): Boolean =
        member.role == ROLE.COORDINATOR || committee == null || committee == member.committee
}

/** Stored at attendance/{meetingId}_{uid}. */
data class AttendanceRecord(
    val meetingId: String = "",
    val uid: String = "",
    val name: String = "",
    val committee: Committee = Committee.NONE,
    val checkedInAt: Long = System.currentTimeMillis(),
    /** true when the host added this person by hand instead of a code check-in. */
    val markedByHost: Boolean = false
)

sealed class CheckInResult {
    object Success : CheckInResult()
    object WrongCode : CheckInResult()
    object AlreadyCheckedIn : CheckInResult()
    object Closed : CheckInResult()
    data class Failed(val message: String) : CheckInResult()
}
