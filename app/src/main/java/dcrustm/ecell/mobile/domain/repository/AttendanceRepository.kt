package dcrustm.ecell.mobile.domain.repository

import dcrustm.ecell.mobile.domain.model.AttendanceRecord
import dcrustm.ecell.mobile.domain.model.CheckInResult
import dcrustm.ecell.mobile.domain.model.Committee
import dcrustm.ecell.mobile.domain.model.Meeting
import dcrustm.ecell.mobile.domain.model.Member
import kotlinx.coroutines.flow.Flow

interface AttendanceRepository {

    fun observeMeetings(): Flow<List<Meeting>>

    fun observeMeeting(meetingId: String): Flow<Meeting?>

    fun observeAttendance(meetingId: String): Flow<List<AttendanceRecord>>

    fun observeAttendanceOf(uid: String): Flow<List<AttendanceRecord>>

    /** Host / coordinator only — everyone else is denied by the security rules. */
    fun observeCheckInCode(meetingId: String): Flow<String?>

    /** Starts a live meeting with a fresh check-in code and marks the host present. Returns its id. */
    suspend fun startMeeting(title: String, committee: Committee?, host: Member): String

    suspend fun regenerateCode(meetingId: String)

    suspend fun setCheckInOpen(meetingId: String, open: Boolean)

    suspend fun endMeeting(meetingId: String)

    suspend fun checkIn(meeting: Meeting, member: Member, code: String): CheckInResult

    suspend fun markPresent(meetingId: String, member: Member)

    suspend fun removeAttendance(meetingId: String, uid: String)
}
